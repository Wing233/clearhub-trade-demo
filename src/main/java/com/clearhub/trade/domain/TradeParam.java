package com.clearhub.trade.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "trade_param")
public class TradeParam {

    @Id
    @Column(name = "commodity_id")
    private Long commodityId;

    @Column(name = "fee_rate", nullable = false, precision = 10, scale = 6)
    private BigDecimal feeRate = new BigDecimal("0.000500");

    @Column(name = "margin_rate", nullable = false, precision = 10, scale = 6)
    private BigDecimal marginRate = BigDecimal.ZERO;

    @Column(name = "max_order_qty", nullable = false, precision = 18, scale = 4)
    private BigDecimal maxOrderQty = new BigDecimal("100000");

    @Column(name = "price_limit_pct", nullable = false, precision = 8, scale = 4)
    private BigDecimal priceLimitPct = new BigDecimal("0.1000");

    @UpdateTimestamp
    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
