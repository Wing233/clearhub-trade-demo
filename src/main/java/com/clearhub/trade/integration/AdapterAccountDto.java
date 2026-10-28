package com.clearhub.trade.integration;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdapterAccountDto {
    private Long id;
    private Long firmId;
    private String accountNo;
    private String bankName;
    private String currency;
    private BigDecimal balance;
    private Integer status;
}
