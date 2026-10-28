package com.clearhub.trade.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class DemoStatusDto {
    private String status;
    private long firms;
    private long commodities;
    private long orders;
    private long openOrders;
    private long trades;
    private long flows;
    private BigDecimal totalVolume;
    private LocalDateTime serverTime;
}
