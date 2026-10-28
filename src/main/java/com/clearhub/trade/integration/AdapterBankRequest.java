package com.clearhub.trade.integration;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdapterBankRequest {
    private String requestId;
    private Long firmId;
    private String accountNo;
    private BigDecimal amount;
}
