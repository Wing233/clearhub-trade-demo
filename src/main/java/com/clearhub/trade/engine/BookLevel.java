package com.clearhub.trade.engine;

import java.math.BigDecimal;

public record BookLevel(BigDecimal price, BigDecimal quantity, int orderCount) {
}
