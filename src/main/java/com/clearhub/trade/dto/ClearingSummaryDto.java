package com.clearhub.trade.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ClearingSummaryDto {
    private long unsettledTrades;
    private BigDecimal unsettledAmount;
    private String lastBatchNo;
    private LocalDateTime lastBatchTime;
    private BigDecimal bankTotal;
    private BigDecimal ledgerTotal;
}
