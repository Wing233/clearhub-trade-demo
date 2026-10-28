package com.clearhub.trade.web;

import com.clearhub.trade.common.ApiResponse;
import com.clearhub.trade.dto.DemoStatusDto;
import com.clearhub.trade.service.DemoResetService;
import com.clearhub.trade.service.MarketDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/demo")
@RequiredArgsConstructor
public class DemoController {

    private final DemoResetService demoResetService;
    private final MarketDataService marketDataService;

    @PostMapping("/reset")
    public ApiResponse<String> reset() {
        demoResetService.reset();
        return ApiResponse.ok("reset");
    }

    @GetMapping("/status")
    public ApiResponse<DemoStatusDto> status() {
        return ApiResponse.ok(marketDataService.demoStatus());
    }
}
