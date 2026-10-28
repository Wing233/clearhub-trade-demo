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
@Table(name = "clear_settlement_detail")
public class SettlementDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_id", nullable = false)
    private Long batchId;

    @Column(name = "firm_id", nullable = false)
    private Long firmId;

    @Column(name = "commodity_id", nullable = false)
    private Long commodityId;

    @Column(name = "opening_qty", nullable = false, precision = 18, scale = 4)
    private BigDecimal openingQty = BigDecimal.ZERO;

    @Column(name = "closing_qty", nullable = false, precision = 18, scale = 4)
    private BigDecimal closingQty = BigDecimal.ZERO;

    @Column(name = "buy_qty", nullable = false, precision = 18, scale = 4)
    private BigDecimal buyQty = BigDecimal.ZERO;

    @Column(name = "sell_qty", nullable = false, precision = 18, scale = 4)
    private BigDecimal sellQty = BigDecimal.ZERO;

    @Column(name = "buy_amount", nullable = false, precision = 20, scale = 4)
    private BigDecimal buyAmount = BigDecimal.ZERO;

    @Column(name = "sell_amount", nullable = false, precision = 20, scale = 4)
    private BigDecimal sellAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 20, scale = 4)
    private BigDecimal fee = BigDecimal.ZERO;

    @Column(name = "net_amount", nullable = false, precision = 20, scale = 4)
    private BigDecimal netAmount = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;
}
