package com.clearhub.adapter.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class ClearingSettleResponse {
    private boolean success;
    private String clearingRef;
    private String status;
    private LocalDateTime acceptedAt;
    private String message;
    private List<AdjustmentResult> adjustments = new ArrayList<>();

    @Data
    public static class AdjustmentResult {
        private String accountNo;
        private Long firmId;
        private boolean success;
        private String externalRef;
        private String message;
    }
}
