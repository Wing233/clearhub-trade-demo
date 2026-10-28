package com.clearhub.trade.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class FlowDto {
    private Long id;
    private String bizCode;
    private String bizLabel;
    private BigDecimal amount;
    private BigDecimal frozenAmount;
    private BigDecimal balanceAfter;
    private BigDecimal frozenAfter;
    private String note;
    private LocalDateTime createTime;
}
