package com.tingtaox.transfer.databases.redis;

import com.tingtaox.transfer.databases.dbo.BalanceRedisEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BalanceRedisRepository extends CrudRepository<BalanceRedisEntity, String> {
}
