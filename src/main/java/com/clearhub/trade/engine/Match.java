package com.clearhub.trade.engine;

import java.math.BigDecimal;

public record Match(
        long buyOrderId,
        long sellOrderId,
        long buyFirmId,
        long sellFirmId,
        long commodityId,
        BigDecimal price,
        BigDecimal quantity
) {
}
