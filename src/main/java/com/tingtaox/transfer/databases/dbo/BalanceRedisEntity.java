package com.tingtaox.transfer.databases.dbo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.concurrent.ThreadLocalRandom;

@RedisHash(value = "Balance")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BalanceRedisEntity implements Serializable {
    @Id
    private String account;
    private BigDecimal amount;

    @TimeToLive
    public long getTimeToLive() {
        // random expiration time between 12 hours and 24 hours to avoid cache avalanche
        return ThreadLocalRandom.current().nextInt(43200, 86401);
    }
}
