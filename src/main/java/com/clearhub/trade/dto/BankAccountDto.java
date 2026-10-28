package com.clearhub.trade.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BankAccountDto {
    private Long id;
    private Long firmId;
    private String firmCode;
    private String firmName;
    private String accountNo;
    private String bankName;
    private BigDecimal balance;
    private Integer status;
}
