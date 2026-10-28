package com.clearhub.adapter.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BankTxnResponse {
    private boolean success;
    private String txnNo;
    private String accountNo;
    private BigDecimal balance;
    private String message;
}
