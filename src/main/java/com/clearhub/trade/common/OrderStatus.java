package com.clearhub.trade.common;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum OrderStatus {

    NEW(0, "待成交"),
    PARTIAL(1, "部分成交"),
    FILLED(2, "全部成交"),
    CANCELLED(3, "已撤单"),
    REJECTED(4, "废单");

    private final int code;
    private final String label;

    OrderStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public static OrderStatus of(int code) {
        return Arrays.stream(values())
                .filter(s -> s.code == code)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unknown order status " + code));
    }
}
