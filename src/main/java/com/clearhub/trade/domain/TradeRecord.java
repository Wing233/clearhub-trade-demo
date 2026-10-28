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
@Table(name = "sec_trade")
public class TradeRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trade_no", nullable = false, length = 40)
    private String tradeNo;

    @Column(name = "commodity_id", nullable = false)
    private Long commodityId;

    @Column(name = "buy_order_id", nullable = false)
    private Long buyOrderId;

    @Column(name = "sell_order_id", nullable = false)
    private Long sellOrderId;

    @Column(name = "buy_firm_id", nullable = false)
    private Long buyFirmId;

    @Column(name = "sell_firm_id", nullable = false)
    private Long sellFirmId;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal price;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 20, scale = 4)
    private BigDecimal amount;

    @Column(name = "buy_fee", nullable = false, precision = 20, scale = 4)
    private BigDecimal buyFee = BigDecimal.ZERO;

    @Column(name = "sell_fee", nullable = false, precision = 20, scale = 4)
    private BigDecimal sellFee = BigDecimal.ZERO;

    @Column(name = "trade_day", nullable = false, length = 16)
    private String tradeDay;

    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;
}
