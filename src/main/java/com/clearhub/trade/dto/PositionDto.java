package com.clearhub.trade.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PositionDto {
    private Long commodityId;
    private String commodityCode;
    private String commodityName;
    private BigDecimal quantity;
    private BigDecimal frozenQuantity;
    private BigDecimal available;
    private BigDecimal avgPrice;
    private BigDecimal lastPrice;
    private BigDecimal marketValue;
    private BigDecimal pnl;
    private BigDecimal pnlPct;
}
