package com.tingtaox.transfer.services.impls;

import com.tingtaox.transfer.databases.dbo.FailureEntity;
import com.tingtaox.transfer.databases.repositories.FailureRepository;

import com.tingtaox.transfer.services.BalanceService;
import java.math.BigDecimal;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ReconciliationProcess {

    Logger logger = LogManager.getLogger(ReconciliationProcess.class);

    @Autowired
    private FailureRepository failureRepository;

    @Autowired
    private BalanceService balanceService;

    /**
     * Recon process runs periodically to scan unfinished failing transactions.
     * We could run it every 12 hours to handle failure in the past day. these
     * transactions could be left out because missing message in MQ or other issues.
     */
    public void reconciliation() {
        List<FailureEntity> entities = failureRepository.findAllByOldFailures();
        entities.stream().parallel().forEach(entity -> {
            String transactionId = entity.getTransactionId();
            String fromAccount = entity.getFromAccount();
            String toAccount = entity.getToAccount();
            BigDecimal amount = entity.getAmount();
            try {
                balanceService.retryUpdateFailure(transactionId, fromAccount, amount, 0);
                balanceService.retryUpdateFailure(transactionId, toAccount, amount, 1);
            } catch (Exception e) {
                logger.error("Failed to retry transactionId {}", transactionId, e);
            }
        });
    }
}
