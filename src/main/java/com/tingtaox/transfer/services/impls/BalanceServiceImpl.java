package com.tingtaox.transfer.services.impls;

import static com.tingtaox.transfer.models.Constants.EXCHANGE;
import static com.tingtaox.transfer.models.Constants.RETRY_CACHE_EVICTION_QUEUE;

import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.tingtaox.transfer.databases.dbo.BalanceEntity;
import com.tingtaox.transfer.databases.dbo.BalanceRedisEntity;
import com.tingtaox.transfer.databases.dbo.FailureEntity;
import com.tingtaox.transfer.databases.redis.BalanceRedisRepository;
import com.tingtaox.transfer.databases.repositories.BalanceRepository;
import com.tingtaox.transfer.databases.repositories.FailureRepository;
import com.tingtaox.transfer.models.BalanceOperation;
import com.tingtaox.transfer.models.CacheRetry;
import com.tingtaox.transfer.services.BalanceService;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BalanceServiceImpl implements BalanceService {

    @Autowired
    private BalanceRepository balanceRepository;

    @Autowired
    private BalanceRedisRepository balanceRedisRepository;

    @Autowired
    private FailureRepository failureRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private static final Retryer<Object> RETRYER = RetryerBuilder.newBuilder()
            .retryIfException()
            .withWaitStrategy(WaitStrategies.fixedWait(500, TimeUnit.MILLISECONDS))
            .withStopStrategy(StopStrategies.stopAfterAttempt(3))
            .build();

    /**
     * Update balance in mysql database.
     *
     * @param account   account number
     * @param amount    transfer amount
     * @param operation to add or subtract
     */
    @Override
    @Transactional
    public void updateDbBalance(String account, BigDecimal amount, BalanceOperation operation) {
        BalanceEntity balanceEntity = balanceRepository.findByAccountForUpdate(account);

        if (operation == BalanceOperation.SUBTRACT) {
            balanceEntity.setAmount(balanceEntity.getAmount().subtract(amount));
        } else if (operation == BalanceOperation.ADD) {
            balanceEntity.setAmount(balanceEntity.getAmount().add(amount));
        }

        balanceRepository.save(balanceEntity);
    }

    /**
     * Retry account balance update asynchronously.
     *
     * @param transactionId transaction id
     * @param account       account
     * @param amount        transfer amount
     * @param accountType   from account or to account
     */
    @Override
    @Transactional
    public void retryUpdateFailure(String transactionId, String account, BigDecimal amount, int accountType) {
        FailureEntity failureEntity = failureRepository.findByTransactionIdForUpdate(transactionId);
        if (!failureEntity.isFromStatus() && accountType == 0) {
            // from account isn't updated
            updateDbBalance(account, amount, BalanceOperation.SUBTRACT);
            failureEntity.setFromStatus(true);
            failureRepository.save(failureEntity);
        } else if (!failureEntity.isToStatus() && accountType == 1) {
            // to account isn't updated
            updateDbBalance(account, amount, BalanceOperation.ADD);
            failureEntity.setToStatus(true);
            failureRepository.save(failureEntity);
        }
    }

    /**
     * Delete the balance record for the given account in cache.
     *
     * @param account account number
     */
    @Override
    @Transactional
    public void deleteCacheBalance(String transactionId, String account) {
        try {
            RETRYER.call(() -> {
                Optional<BalanceRedisEntity> cacheEntity = balanceRedisRepository.findById(account);
                if (cacheEntity.isPresent()) {
                    balanceRedisRepository.deleteById(account);
                }
                return null;
            });
        } catch (Exception e) {
            CacheRetry cacheRetry = new CacheRetry();
            cacheRetry.setAccount(account);
            cacheRetry.setTransactionId(transactionId);
            rabbitTemplate.convertAndSend(EXCHANGE, RETRY_CACHE_EVICTION_QUEUE, cacheRetry);
        }
    }
}
