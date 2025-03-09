package com.tingtaox.transfer.services;

import com.tingtaox.transfer.models.BalanceOperation;

import java.math.BigDecimal;

public interface BalanceService {
    void updateDbBalance(String account, BigDecimal amount, BalanceOperation operation);
    void retryUpdateFailure(String transactionId, String account, BigDecimal amount, int accountType);
    void deleteCacheBalance(String transactionId, String account);
}
