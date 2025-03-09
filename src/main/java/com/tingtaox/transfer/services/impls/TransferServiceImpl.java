package com.tingtaox.transfer.services.impls;

import static com.tingtaox.transfer.models.Constants.TRANSACTION_FAILURE_QUEUE;
import static com.tingtaox.transfer.models.Constants.EXCHANGE;

import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.tingtaox.transfer.databases.dbo.AuditEntity;
import com.tingtaox.transfer.databases.dbo.BalanceEntity;
import com.tingtaox.transfer.databases.dbo.BalanceRedisEntity;
import com.tingtaox.transfer.databases.dbo.FailureEntity;
import com.tingtaox.transfer.databases.redis.BalanceRedisRepository;
import com.tingtaox.transfer.databases.repositories.AuditRepository;
import com.tingtaox.transfer.databases.repositories.BalanceRepository;
import com.tingtaox.transfer.databases.repositories.FailureRepository;
import com.tingtaox.transfer.models.BalanceOperation;
import com.tingtaox.transfer.models.TransferRequest;
import com.tingtaox.transfer.services.BalanceService;
import com.tingtaox.transfer.services.TransferService;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TransferServiceImpl implements TransferService {

    Logger logger = LogManager.getLogger(TransferServiceImpl.class);

    @Autowired
    private BalanceRepository balanceRepository;

    @Autowired
    private BalanceRedisRepository balanceRedisRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private BalanceService balanceService;

    @Autowired
    private AuditRepository auditRepository;

    @Autowired
    private FailureRepository failureRepository;

    private static final Retryer<Object> RETRYER = RetryerBuilder.newBuilder()
            .retryIfException()
            .withWaitStrategy(WaitStrategies.fixedWait(500, TimeUnit.MILLISECONDS))
            .withStopStrategy(StopStrategies.stopAfterAttempt(3))
            .build();

    /**
     * Get account balance by account.
     * @param account account number
     * @return balance
     */
    @Override
    public BigDecimal getBalance(String account) {
        Optional<BalanceRedisEntity> balanceCacheEntity = balanceRedisRepository.findById(account);

        if (balanceCacheEntity.isPresent()) {
            return balanceCacheEntity.get().getAmount();
        }

        BalanceEntity balanceEntity = balanceRepository.findByAccount(account);

        if (balanceEntity == null) {
            logger.error("Account {} not found", account);
            throw new RuntimeException("Account not found");
        }

        BalanceRedisEntity cacheEntity = new BalanceRedisEntity();
        cacheEntity.setAccount(account);
        cacheEntity.setAmount(balanceEntity.getAmount());
        balanceRedisRepository.save(cacheEntity);
        return balanceEntity.getAmount();
    }

    /**
     * Update source and destination account balance in database and cache.
     * @param request   transfer request
     * @return          transaction id
     */
    @Override
    public String transfer(TransferRequest request) {
        BigDecimal transferAmount = BigDecimal.valueOf(request.getAmount());
        String fromAccount = request.getFromAccount();
        String toAccount = request.getToAccount();

        // balance check
        BigDecimal fromBalance = getBalance(fromAccount);
        if (fromBalance.doubleValue() < transferAmount.doubleValue()) {
            logger.error("Not enough balance in account {} to pay {}", fromAccount, transferAmount);
            throw new RuntimeException("Low balance");
        }

        String transactionId = generateTransactionId(fromAccount, toAccount);

        // save the transaction in audit table
        try {
            AuditEntity auditEntity = createAudit(request, transactionId);
            RETRYER.call(() -> auditRepository.save(auditEntity));
        } catch (Exception e) {
            logger.error("Failed to save transaction from acct {} to {}", fromAccount, toAccount, e);
            throw new RuntimeException("Failed to save the transaction", e);
        }

        // synchronously update balances in database
        try {
            RETRYER.call(() -> {
                balanceService.updateDbBalance(fromAccount, transferAmount, BalanceOperation.SUBTRACT);
                return null;
            });
            balanceService.deleteCacheBalance(transactionId, fromAccount);
        } catch (Exception e) {
            logger.error("Failed to update from account {}", fromAccount, e);
            FailureEntity failure = saveFailureRecord(transactionId, fromAccount, toAccount, transferAmount, false, false);
            rabbitTemplate.convertAndSend(EXCHANGE, TRANSACTION_FAILURE_QUEUE, failure);  // send to MQ for asynchronous retry
            return transactionId;
        }

        try {
            RETRYER.call(() -> {
                balanceService.updateDbBalance(toAccount, transferAmount, BalanceOperation.ADD);
                return null;
            });
            balanceService.deleteCacheBalance(transactionId, toAccount);
        } catch (Exception e) {
            logger.error("Failed to update to account {}", toAccount, e);
            FailureEntity failure = saveFailureRecord(transactionId, fromAccount, toAccount, transferAmount, true, false);
            rabbitTemplate.convertAndSend(EXCHANGE, TRANSACTION_FAILURE_QUEUE, failure);  // send to MQ for asynchronous retry
            return transactionId;
        }

        logger.info("Finished transaction {} from {} to {}", transactionId, fromAccount, toAccount);
        return transactionId;
    }

    /**
     * Generate unique transaction id by concatenating timestamp, from and to account and a serial number.
     *
     * @param fromAccount source account
     * @param toAccount   destination account
     * @return unique transaction id
     */
    private String generateTransactionId(String fromAccount, String toAccount) {
        return System.currentTimeMillis() + fromAccount + toAccount;
    }

    /**
     * Create a transaction history for audit.
     * @param request           transfer request
     * @param transactionId     transaction id
     * @return                  Audit Entity
     */
    private AuditEntity createAudit(TransferRequest request, String transactionId) {
        AuditEntity auditEntity = new AuditEntity();
        auditEntity.setTransactionId(transactionId);
        auditEntity.setFromAccount(request.getFromAccount());
        auditEntity.setToAccount(request.getToAccount());
        auditEntity.setAmount(BigDecimal.valueOf(request.getAmount()));
        auditEntity.setCreationTime(new Timestamp(System.currentTimeMillis()));
        return auditEntity;
    }

    /**
     * Save a record for asynchronous account balance update.
     */
    private FailureEntity saveFailureRecord(String transactionId, String from, String to, BigDecimal amount,
                                   boolean fromStatus, boolean toStatus) {
        FailureEntity failureEntity = new FailureEntity();
        failureEntity.setTransactionId(transactionId);
        failureEntity.setFromAccount(from);
        failureEntity.setToAccount(to);
        failureEntity.setAmount(amount);
        failureEntity.setFromStatus(fromStatus);
        failureEntity.setToStatus(toStatus);
        failureRepository.save(failureEntity);
        return failureEntity;
    }
}
