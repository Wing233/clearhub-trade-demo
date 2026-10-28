package com.clearhub.trade.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class FundsDto {
    private Long firmId;
    private BigDecimal balance;
    private BigDecimal frozenFunds;
    private BigDecimal available;
}
