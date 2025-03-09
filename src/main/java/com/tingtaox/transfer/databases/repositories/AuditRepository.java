package com.tingtaox.transfer.databases.repositories;

import com.tingtaox.transfer.databases.dbo.AuditEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditRepository extends JpaRepository<AuditEntity, Long> {
}
