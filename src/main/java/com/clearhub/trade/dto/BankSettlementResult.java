package com.clearhub.trade.dto;

import java.math.BigDecimal;

public record BankSettlementResult(
        BigDecimal bankBefore,
        BigDecimal settledAmount,
        BigDecimal bankAfter
) {
}
