package com.clearhub.trade.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "sec_order")
public class TradeOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no", nullable = false, length = 40)
    private String orderNo;

    @Column(name = "firm_id", nullable = false)
    private Long firmId;

    @Column(name = "commodity_id", nullable = false)
    private Long commodityId;

    @Column(name = "bs_flag", nullable = false, length = 1)
    private String bsFlag;

    @Column(name = "oc_flag", nullable = false, length = 1)
    private String ocFlag = "O";

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal price;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal quantity;

    @Column(name = "filled_quantity", nullable = false, precision = 18, scale = 4)
    private BigDecimal filledQuantity = BigDecimal.ZERO;

    @Column(name = "order_status", nullable = false)
    private Integer orderStatus = 0;

    @Column(name = "frozen_funds", nullable = false, precision = 20, scale = 4)
    private BigDecimal frozenFunds = BigDecimal.ZERO;

    @Column(name = "frozen_quantity", nullable = false, precision = 18, scale = 4)
    private BigDecimal frozenQuantity = BigDecimal.ZERO;

    @Column(name = "fee_rate", nullable = false, precision = 10, scale = 6)
    private BigDecimal feeRate = BigDecimal.ZERO;

    @Column(nullable = false, precision = 20, scale = 4)
    private BigDecimal fee = BigDecimal.ZERO;

    @Column(name = "settled_funds", nullable = false, precision = 20, scale = 4)
    private BigDecimal settledFunds = BigDecimal.ZERO;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "trade_day", nullable = false, length = 16)
    private String tradeDay;

    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    @UpdateTimestamp
    @Column(name = "update_time")
    private LocalDateTime updateTime;

    public BigDecimal remainingQuantity() {
        return quantity.subtract(filledQuantity);
    }

    public boolean isOpen() {
        return orderStatus != null && (orderStatus == 0 || orderStatus == 1);
    }
}
