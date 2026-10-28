package com.clearhub.trade.integration;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class AdapterSettleRequest {
    private String batchNo;
    private String tradeDay;
    private Integer totalTrades;
    private BigDecimal totalAmount;
    private BigDecimal totalFee;
    private List<Adjustment> adjustments = new ArrayList<>();

    @Data
    public static class Adjustment {
        private Long firmId;
        private String accountNo;
        private BigDecimal amount;
    }
}
