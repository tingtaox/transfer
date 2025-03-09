package com.tingtaox.transfer.services;

import com.tingtaox.transfer.models.TransferRequest;
import java.math.BigDecimal;

public interface TransferService {
    BigDecimal getBalance(String account);
    String transfer(TransferRequest request);
}
