package com.clearhub.trade.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
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
@Table(name = "sec_commodity")
public class Commodity {

    @Id
    private Long id;

    @Column(name = "market_id", nullable = false)
    private Long marketId;

    @Column(nullable = false, length = 32)
    private String code;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(name = "unit_numeral", nullable = false)
    private Integer unitNumeral = 1;

    @Column(name = "min_price_change", nullable = false, precision = 18, scale = 4)
    private BigDecimal minPriceChange;

    @Column(name = "min_amount_change", nullable = false, precision = 18, scale = 4)
    private BigDecimal minAmountChange;

    @Column(name = "price_limit_pct", nullable = false, precision = 8, scale = 4)
    private BigDecimal priceLimitPct;

    @Column(name = "symbol_id", nullable = false)
    private Long symbolId = 1L;

    @Column(nullable = false)
    private Integer status = 1;

    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    @UpdateTimestamp
    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
