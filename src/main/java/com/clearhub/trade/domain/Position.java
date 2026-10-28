package com.clearhub.trade.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

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
@Table(name = "sec_hold")
public class Position {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "firm_id", nullable = false)
    private Long firmId;

    @Column(name = "commodity_id", nullable = false)
    private Long commodityId;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal quantity = BigDecimal.ZERO;

    @Column(name = "frozen_quantity", nullable = false, precision = 18, scale = 4)
    private BigDecimal frozenQuantity = BigDecimal.ZERO;

    @Column(name = "avg_price", nullable = false, precision = 18, scale = 4)
    private BigDecimal avgPrice = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 20, scale = 4)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @UpdateTimestamp
    @Column(name = "update_time")
    private LocalDateTime updateTime;

    public BigDecimal available() {
        return quantity.subtract(frozenQuantity);
    }
}
