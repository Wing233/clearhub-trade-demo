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
@Table(name = "sec_quotation")
public class Quotation {

    @Id
    @Column(name = "commodity_id")
    private Long commodityId;

    @Column(name = "last_price", nullable = false, precision = 18, scale = 4)
    private BigDecimal lastPrice = BigDecimal.ZERO;

    @Column(name = "open_price", nullable = false, precision = 18, scale = 4)
    private BigDecimal openPrice = BigDecimal.ZERO;

    @Column(name = "high_price", nullable = false, precision = 18, scale = 4)
    private BigDecimal highPrice = BigDecimal.ZERO;

    @Column(name = "low_price", nullable = false, precision = 18, scale = 4)
    private BigDecimal lowPrice = BigDecimal.ZERO;

    @Column(nullable = false, precision = 20, scale = 4)
    private BigDecimal volume = BigDecimal.ZERO;

    @Column(nullable = false, precision = 20, scale = 4)
    private BigDecimal amount = BigDecimal.ZERO;

    @UpdateTimestamp
    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
