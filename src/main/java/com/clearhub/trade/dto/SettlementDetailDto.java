package com.clearhub.trade.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SettlementDetailDto {
    private Long firmId;
    private String firmCode;
    private String firmName;
    private Long commodityId;
    private String commodityCode;
    private String commodityName;
    private BigDecimal openingQty;
    private BigDecimal closingQty;
    private BigDecimal buyQty;
    private BigDecimal sellQty;
    private BigDecimal buyAmount;
    private BigDecimal sellAmount;
    private BigDecimal fee;
    private BigDecimal netAmount;
}
