package com.clearhub.trade.engine;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class BookOrder {

    private final long orderId;
    private final long firmId;
    private final String side;
    private final BigDecimal price;
    private final BigDecimal originalQuantity;
    private final LocalDateTime createdAt;
    private BigDecimal remaining;

    public BookOrder(long orderId, long firmId, String side, BigDecimal price,
                     BigDecimal quantity, LocalDateTime createdAt) {
        this.orderId = orderId;
        this.firmId = firmId;
        this.side = side;
        this.price = price;
        this.originalQuantity = quantity;
        this.remaining = quantity;
        this.createdAt = createdAt;
    }

    public boolean isBuy() {
        return "B".equalsIgnoreCase(side);
    }
}
