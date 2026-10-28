package com.clearhub.trade.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class SettlementBatchDto {
    private Long id;
    private String batchNo;
    private String tradeDay;
    private String status;
    private Integer totalTrades;
    private BigDecimal totalAmount;
    private BigDecimal totalFee;
    private Integer firmCount;
    private String externalRef;
    private String externalStatus;
    private String externalMessage;
    private LocalDateTime externalTime;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createTime;
    private List<SettlementDetailDto> details;
    private List<ReconciliationDto> reconciliations;
}
