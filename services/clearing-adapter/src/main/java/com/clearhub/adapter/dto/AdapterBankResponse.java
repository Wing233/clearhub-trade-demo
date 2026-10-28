package com.clearhub.adapter.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdapterBankResponse {
    private boolean success;
    private String externalRef;
    private BigDecimal balance;
    private String message;

    public static AdapterBankResponse ok(String externalRef, BigDecimal balance) {
        return new AdapterBankResponse(true, externalRef, balance, "ok");
    }

    public static AdapterBankResponse fail(String message) {
        return new AdapterBankResponse(false, null, null, message);
    }
}
