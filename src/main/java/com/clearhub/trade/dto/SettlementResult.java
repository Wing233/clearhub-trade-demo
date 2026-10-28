package com.clearhub.trade.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SettlementResult(
        Long tradeId,
        String tradeNo,
        Long commodityId,
        BigDecimal price,
        BigDecimal quantity,
        BigDecimal amount,
        Long buyFirmId,
        Long sellFirmId,
        LocalDateTime createTime
) {
}
