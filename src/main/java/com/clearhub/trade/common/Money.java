package com.clearhub.trade.common;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Money {

    public static final int MONEY_SCALE = 2;
    public static final int QTY_SCALE = 4;

    private Money() {
    }

    public static BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal qty(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(QTY_SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal price(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(QTY_SCALE, RoundingMode.HALF_UP);
    }

    public static boolean isMultiple(BigDecimal value, BigDecimal step) {
        if (value == null || step == null || step.signum() <= 0) {
            return false;
        }
        return value.remainder(step).abs().compareTo(new BigDecimal("0.00000001")) < 0;
    }

    public static BigDecimal zeroIfNegative(BigDecimal value) {
        return value.signum() < 0 ? BigDecimal.ZERO : value;
    }
}
