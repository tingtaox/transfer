package com.tingtaox.transfer.databases.repositories;

import com.tingtaox.transfer.databases.dbo.BalanceEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public interface BalanceRepository extends JpaRepository<BalanceEntity, Long> {
    BalanceEntity findByAccount(String account);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from BalanceEntity b where b.account = :account")
    BalanceEntity findByAccountForUpdate(@Param("account") String account);
}
