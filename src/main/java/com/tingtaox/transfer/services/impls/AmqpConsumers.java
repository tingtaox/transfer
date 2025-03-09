package com.tingtaox.transfer.services.impls;

import static com.tingtaox.transfer.models.Constants.EXCHANGE;
import static com.tingtaox.transfer.models.Constants.TRANSACTION_FAILURE_QUEUE;
import static com.tingtaox.transfer.models.Constants.RETRY_CACHE_EVICTION_QUEUE;

import com.rabbitmq.client.Channel;
import com.tingtaox.transfer.databases.dbo.FailureEntity;
import com.tingtaox.transfer.models.CacheRetry;

import java.math.BigDecimal;
import java.util.Map;

import com.tingtaox.transfer.services.BalanceService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class AmqpConsumers {

    Logger logger = LogManager.getLogger(AmqpConsumers.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private BalanceService balanceService;

    /**
     * Delete record of account balance in cache for data consistency.
     * if record eviction still fails, push a message to MQ to retry again until
     * it succeeds.
     */
    @RabbitListener(queues = RETRY_CACHE_EVICTION_QUEUE)
    @RabbitHandler
    public void retryCacheEviction(@Payload CacheRetry retry, Channel channel,
                                   @Headers Map<String, Object> headers) {
        logger.info("Retry delete cache for account {}", retry.getAccount());
        try {
            balanceService.deleteCacheBalance(retry.getTransactionId(), retry.getAccount());
        } catch (Exception e) {
            logger.error("Failed to delete cache record with acct", e);
            rabbitTemplate.convertAndSend(EXCHANGE, RETRY_CACHE_EVICTION_QUEUE, retry);
        }
    }

    /**
     * retry updating account balance. if failure retry fails again, push a message to MQ
     * for further retry.
     */
    @RabbitListener(queues = TRANSACTION_FAILURE_QUEUE)
    @RabbitHandler
    public void retryFailures(@Payload FailureEntity failureEntity, Channel channel,
                              @Headers Map<String, Object> headers) {
        String transactionId = failureEntity.getTransactionId();
        String fromAccount = failureEntity.getFromAccount();
        String toAccount = failureEntity.getToAccount();
        BigDecimal amount = failureEntity.getAmount();
        logger.info("Retry transaction id {}, from account {} to account {} with amount {}",
                transactionId, fromAccount, toAccount, amount);
        try {
            balanceService.retryUpdateFailure(transactionId, fromAccount, amount, 0);
            balanceService.deleteCacheBalance(transactionId, fromAccount);
            balanceService.retryUpdateFailure(transactionId, toAccount, amount, 1);
            balanceService.deleteCacheBalance(transactionId, toAccount);
        } catch (Exception e) {
            logger.error("Retry transaction id {} failed again", transactionId);
            rabbitTemplate.convertAndSend(EXCHANGE, TRANSACTION_FAILURE_QUEUE, failureEntity);
        }
    }
}
