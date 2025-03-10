package com.tingtaox.transfer.services;

import com.tingtaox.transfer.models.TransferRequest;
import com.tingtaox.transfer.models.TransferResponse;

import java.math.BigDecimal;

public interface TransferService {
    BigDecimal getBalance(String account);
    TransferResponse transfer(TransferRequest request);
}
