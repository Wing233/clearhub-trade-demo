package com.clearhub.trade.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ReconciliationDto {
    private Long firmId;
    private String firmCode;
    private String firmName;
    private BigDecimal ledgerBalance;
    private BigDecimal bankBalance;
    private BigDecimal settledAmount;
    private BigDecimal difference;
    private String status;
}
