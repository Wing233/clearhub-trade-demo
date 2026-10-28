package com.clearhub.trade.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CommodityDto {
    private Long id;
    private String code;
    private String name;
    private Long marketId;
    private BigDecimal lastPrice;
    private BigDecimal openPrice;
    private BigDecimal highPrice;
    private BigDecimal lowPrice;
    private BigDecimal changePct;
    private BigDecimal volume;
    private BigDecimal amount;
    private BigDecimal minPriceChange;
    private BigDecimal minAmountChange;
    private BigDecimal feeRate;
    private BigDecimal bestBid;
    private BigDecimal bestAsk;
    private boolean tradable;
}
