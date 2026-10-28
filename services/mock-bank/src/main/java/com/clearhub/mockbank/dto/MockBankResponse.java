package com.clearhub.mockbank.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MockBankResponse {
    private boolean success;
    private String txnNo;
    private String accountNo;
    private BigDecimal balance;
    private String message;

    public static MockBankResponse ok(String txnNo, String accountNo, BigDecimal balance) {
        return new MockBankResponse(true, txnNo, accountNo, balance, "ok");
    }

    public static MockBankResponse fail(String message) {
        return new MockBankResponse(false, null, null, null, message);
    }
}
