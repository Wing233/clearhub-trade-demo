package com.clearhub.trade.engine;

import java.math.BigDecimal;
import java.util.List;

public record BookSnapshot(
        long commodityId,
        List<BookLevel> bids,
        List<BookLevel> asks,
        BigDecimal bestBid,
        BigDecimal bestAsk,
        long timestamp
) {
}
