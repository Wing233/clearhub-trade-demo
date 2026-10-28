package com.clearhub.trade.dto;

import com.clearhub.trade.engine.BookLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderBookDto {
    private Long commodityId;
    private List<BookLevel> bids;
    private List<BookLevel> asks;
    private BigDecimal lastPrice;
    private Long timestamp;
}
