package com.clearhub.trade.integration;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdapterBankResponse {
    private boolean success;
    private String externalRef;
    private BigDecimal balance;
    private String message;
}
