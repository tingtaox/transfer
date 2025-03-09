package com.tingtaox.transfer.databases.repositories;

import com.tingtaox.transfer.databases.dbo.FailureEntity;
import jakarta.persistence.LockModeType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FailureRepository extends JpaRepository<FailureEntity, Long> {
    @Query(nativeQuery = true, value = "SELECT * FROM Failures " +
            "WHERE creation_time <= NOW() - INTERVAL 12 HOUR " +
            "and to_status = false or from_status = false")
    List<FailureEntity> findAllByOldFailures();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from FailureEntity f where f.transactionId = :transactionId")
    FailureEntity findByTransactionIdForUpdate(@Param("transactionId") String transactionId);
}
