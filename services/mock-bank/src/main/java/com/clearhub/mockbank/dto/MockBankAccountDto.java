package com.clearhub.mockbank.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MockBankAccountDto {
    private Long id;
    private Long firmId;
    private String accountNo;
    private String bankName;
    private String currency;
    private BigDecimal balance;
    private Integer status;
}
