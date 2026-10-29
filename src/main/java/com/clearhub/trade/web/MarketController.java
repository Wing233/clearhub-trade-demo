package com.clearhub.trade.web;

import com.clearhub.trade.common.ApiResponse;
import com.clearhub.trade.domain.Quotation;
import com.clearhub.trade.dto.CommodityDto;
import com.clearhub.trade.dto.FirmDto;
import com.clearhub.trade.dto.OrderBookDto;
import com.clearhub.trade.dto.TradeDto;
import com.clearhub.trade.dto.CandleDto;
import com.clearhub.trade.service.MarketDataService;
import com.clearhub.trade.service.QuotationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MarketController {

    private final MarketDataService marketDataService;
    private final QuotationService quotationService;

    @GetMapping("/firms")
    public ApiResponse<List<FirmDto>> firms() {
        return ApiResponse.ok(marketDataService.listFirms());
    }

    @GetMapping("/commodities")
    public ApiResponse<List<CommodityDto>> commodities() {
        return ApiResponse.ok(marketDataService.listCommodities());
    }

    @GetMapping("/market/{commodityId}/orderbook")
    public ApiResponse<OrderBookDto> orderBook(@PathVariable Long commodityId,
                                               @RequestParam(defaultValue = "10") int depth) {
        return ApiResponse.ok(marketDataService.orderBook(commodityId, depth));
    }

    @GetMapping("/market/{commodityId}/trades")
    public ApiResponse<List<TradeDto>> trades(@PathVariable Long commodityId,
                                              @RequestParam(defaultValue = "50") int limit) {
        return ApiResponse.ok(marketDataService.recentTrades(commodityId, limit));
    }

    @GetMapping("/market/{commodityId}/candles")
    public ApiResponse<List<CandleDto>> candles(@PathVariable Long commodityId,
                                                @RequestParam(defaultValue = "60") int limit) {
        return ApiResponse.ok(marketDataService.candles(commodityId, limit));
    }

    @GetMapping("/market/{commodityId}/quotation")
    public ApiResponse<Quotation> quotation(@PathVariable Long commodityId) {
        return ApiResponse.ok(quotationService.getOrCreate(commodityId));
    }
}
