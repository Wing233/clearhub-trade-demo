package com.clearhub.trade.integration;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdapterSettleResponse {
    private boolean success;
    private String clearingRef;
    private String status;
    private LocalDateTime acceptedAt;
    private String message;
}
