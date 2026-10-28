package com.clearhub.trade.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class BankTransactionDto {
    private Long id;
    private String txnNo;
    private Long firmId;
    private String firmName;
    private String type;
    private String typeLabel;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String remark;
    private LocalDateTime createTime;
}
