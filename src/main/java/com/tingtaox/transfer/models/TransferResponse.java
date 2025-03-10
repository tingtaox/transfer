package com.tingtaox.transfer.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class TransferResponse {
    String transactionId;
    Integer transferStatus;
}
