package com.tingtaox.transfer.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class TransferRequest {
    private String fromAccount;
    private String toAccount;
    private Double amount;
}
