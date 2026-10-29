package com.clearhub.trade.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CandleDto {
    private String time;
    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal close;
    private BigDecimal volume;
}
