package com.clearhub.trade.web;

import com.clearhub.trade.common.ApiResponse;
import com.clearhub.trade.dto.ClearingSummaryDto;
import com.clearhub.trade.dto.SettlementBatchDto;
import com.clearhub.trade.service.ClearingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/clearing")
@RequiredArgsConstructor
public class ClearingController {

    private final ClearingService clearingService;

    @GetMapping("/summary")
    public ApiResponse<ClearingSummaryDto> summary() {
        return ApiResponse.ok(clearingService.summary());
    }

    @PostMapping("/run")
    public ApiResponse<SettlementBatchDto> run() {
        return ApiResponse.ok(clearingService.run());
    }

    @GetMapping("/batches")
    public ApiResponse<List<SettlementBatchDto>> batches(@RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.ok(clearingService.listBatches(limit));
    }

    @GetMapping("/batches/{batchId}")
    public ApiResponse<SettlementBatchDto> batch(@PathVariable Long batchId) {
        return ApiResponse.ok(clearingService.getBatch(batchId));
    }

    @PostMapping("/batches/{batchId}/report")
    public ApiResponse<SettlementBatchDto> report(@PathVariable Long batchId) {
        return ApiResponse.ok(clearingService.retryExternal(batchId));
    }
}
