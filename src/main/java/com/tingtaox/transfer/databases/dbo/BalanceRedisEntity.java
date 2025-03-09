package com.tingtaox.transfer.databases.dbo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.io.Serializable;
import java.math.BigDecimal;

@RedisHash("Balance")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BalanceRedisEntity implements Serializable {
    @Id
    private String account;
    private BigDecimal amount;
}
