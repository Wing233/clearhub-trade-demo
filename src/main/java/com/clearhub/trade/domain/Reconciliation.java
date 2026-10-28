package com.clearhub.trade.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "clear_reconciliation")
public class Reconciliation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_id", nullable = false)
    private Long batchId;

    @Column(name = "firm_id", nullable = false)
    private Long firmId;

    @Column(name = "ledger_balance", nullable = false, precision = 20, scale = 4)
    private BigDecimal ledgerBalance = BigDecimal.ZERO;

    @Column(name = "bank_balance", nullable = false, precision = 20, scale = 4)
    private BigDecimal bankBalance = BigDecimal.ZERO;

    @Column(name = "settled_amount", nullable = false, precision = 20, scale = 4)
    private BigDecimal settledAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 20, scale = 4)
    private BigDecimal difference = BigDecimal.ZERO;

    @Column(nullable = false, length = 16)
    private String status;

    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;
}
