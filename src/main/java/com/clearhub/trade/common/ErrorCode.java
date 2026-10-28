package com.clearhub.trade.common;

import lombok.Getter;

@Getter
public enum ErrorCode {

    VALIDATION(40001, "参数校验失败"),
    MARKET_CLOSED(40002, "市场未开市"),
    INSUFFICIENT_FUNDS(40003, "可用资金不足"),
    INSUFFICIENT_POSITION(40004, "可用持仓不足"),
    ORDER_NOT_FOUND(40005, "委托不存在"),
    ORDER_NOT_CANCELLABLE(40006, "当前委托不可撤销"),
    PRICE_OUT_OF_LIMIT(40007, "价格超出涨跌停范围"),
    QUANTITY_INVALID(40008, "数量不符合最小变动单位"),
    PRICE_INVALID(40009, "价格不符合最小变动单位"),
    ACCOUNT_NOT_FOUND(40010, "交易账户不存在"),
    COMMODITY_NOT_FOUND(40011, "商品不存在"),
    DUPLICATE_REQUEST(40012, "重复请求"),
    INTERNAL(50000, "系统内部错误"),
    CHANNEL_ERROR(50001, "外部通道失败");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
