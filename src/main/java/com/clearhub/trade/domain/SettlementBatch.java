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
@Table(name = "clear_settlement_batch")
public class SettlementBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_no", nullable = false, length = 40)
    private String batchNo;

    @Column(name = "trade_day", nullable = false, length = 16)
    private String tradeDay;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "total_trades", nullable = false)
    private Integer totalTrades = 0;

    @Column(name = "total_amount", nullable = false, precision = 20, scale = 4)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "total_fee", nullable = false, precision = 20, scale = 4)
    private BigDecimal totalFee = BigDecimal.ZERO;

    @Column(name = "firm_count", nullable = false)
    private Integer firmCount = 0;

    @Column(name = "external_ref", length = 64)
    private String externalRef;

    @Column(name = "external_status", length = 16)
    private String externalStatus;

    @Column(name = "external_message", length = 255)
    private String externalMessage;

    @Column(name = "external_time")
    private LocalDateTime externalTime;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;
}
