package com.clearhub.adapter.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class ClearingSettleRequest {

    @NotBlank
    private String batchNo;

    @NotBlank
    private String tradeDay;

    @NotNull
    private Integer totalTrades;

    @NotNull
    private BigDecimal totalAmount;

    @NotNull
    private BigDecimal totalFee;

    @Valid
    private List<Adjustment> adjustments = new ArrayList<>();

    @Data
    public static class Adjustment {
        @NotNull
        private Long firmId;
        @NotBlank
        private String accountNo;
        @NotNull
        private BigDecimal amount;
    }
}
