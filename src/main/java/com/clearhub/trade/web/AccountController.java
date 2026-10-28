package com.clearhub.trade.web;

import com.clearhub.trade.common.ApiResponse;
import com.clearhub.trade.dto.FlowDto;
import com.clearhub.trade.dto.FundsDto;
import com.clearhub.trade.dto.OrderDto;
import com.clearhub.trade.dto.PositionDto;
import com.clearhub.trade.dto.TradeDto;
import com.clearhub.trade.service.MarketDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AccountController {

    private final MarketDataService marketDataService;

    @GetMapping("/funds")
    public ApiResponse<FundsDto> funds(@RequestParam Long firmId) {
        return ApiResponse.ok(marketDataService.funds(firmId));
    }

    @GetMapping("/positions")
    public ApiResponse<List<PositionDto>> positions(@RequestParam Long firmId) {
        return ApiResponse.ok(marketDataService.positions(firmId));
    }

    @GetMapping("/flows")
    public ApiResponse<List<FlowDto>> flows(@RequestParam Long firmId,
                                            @RequestParam(defaultValue = "100") int limit) {
        return ApiResponse.ok(marketDataService.flows(firmId, limit));
    }

    @GetMapping("/orders")
    public ApiResponse<List<OrderDto>> orders(@RequestParam Long firmId,
                                              @RequestParam(required = false) Integer status,
                                              @RequestParam(defaultValue = "100") int limit) {
        return ApiResponse.ok(marketDataService.firmOrders(firmId, status, limit));
    }

    @GetMapping("/trades")
    public ApiResponse<List<TradeDto>> trades(@RequestParam Long firmId,
                                              @RequestParam(defaultValue = "100") int limit) {
        return ApiResponse.ok(marketDataService.firmTrades(firmId, limit));
    }
}
