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
@Table(name = "sec_hold_detail")
public class HoldDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hold_id", nullable = false)
    private Long holdId;

    @Column(name = "trade_id", nullable = false)
    private Long tradeId;

    @Column(name = "firm_id", nullable = false)
    private Long firmId;

    @Column(name = "commodity_id", nullable = false)
    private Long commodityId;

    @Column(name = "init_price", nullable = false, precision = 18, scale = 4)
    private BigDecimal initPrice;

    @Column(name = "init_quantity", nullable = false, precision = 18, scale = 4)
    private BigDecimal initQuantity;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal quantity;

    @Column(name = "hold_day", nullable = false)
    private Integer holdDay = 0;

    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;
}
