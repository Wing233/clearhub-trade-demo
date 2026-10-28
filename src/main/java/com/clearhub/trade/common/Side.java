package com.clearhub.trade.common;

public enum Side {
    BUY,
    SELL;

    public static Side of(String value) {
        if (value == null) {
            throw new IllegalArgumentException("side is required");
        }
        return switch (value.trim().toUpperCase()) {
            case "B", "BUY", "买" -> BUY;
            case "S", "SELL", "卖" -> SELL;
            default -> throw new IllegalArgumentException("unknown side: " + value);
        };
    }

    public String flag() {
        return this == BUY ? "B" : "S";
    }
}
