package com.clearhub.mockbank.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class MockBankTransactionDto {
    private Long id;
    private String txnNo;
    private String requestId;
    private Long firmId;
    private String accountNo;
    private String type;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String refNo;
    private String remark;
    private LocalDateTime createTime;
}
