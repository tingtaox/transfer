package com.tingtaox.transfer.databases.dbo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.sql.Timestamp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "failures")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FailureEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "transactionid")
    private String transactionId;
    @Column(name = "from_account")
    private String fromAccount;
    @Column(name = "to_account")
    private String toAccount;
    @Column(name = "from_status")
    private boolean fromStatus;
    @Column(name = "to_status")
    private boolean toStatus;
    private BigDecimal amount;
    @Column(name = "creation_time")
    private Timestamp creationTime;
}
