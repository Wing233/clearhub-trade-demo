package com.clearhub.trade.web;

import com.clearhub.trade.common.ApiResponse;
import com.clearhub.trade.common.BizException;
import com.clearhub.trade.common.ErrorCode;
import com.clearhub.trade.domain.TradeOrder;
import com.clearhub.trade.dto.OrderDto;
import com.clearhub.trade.dto.PlaceOrderRequest;
import com.clearhub.trade.service.MarketDataService;
import com.clearhub.trade.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final MarketDataService marketDataService;

    @PostMapping
    public ApiResponse<OrderDto> place(@RequestHeader(name = "X-Firm-Id", required = false) Long firmId,
                                       @Valid @RequestBody PlaceOrderRequest request) {
        Long resolvedFirmId = requireFirm(firmId);
        TradeOrder order = orderService.placeOrder(resolvedFirmId, request);
        return ApiResponse.ok(marketDataService.toOrderDto(order));
    }

    @PostMapping("/{orderId}/cancel")
    public ApiResponse<OrderDto> cancel(@RequestHeader(name = "X-Firm-Id", required = false) Long firmId,
                                        @PathVariable Long orderId) {
        Long resolvedFirmId = requireFirm(firmId);
        TradeOrder order = orderService.cancelOrder(resolvedFirmId, orderId);
        return ApiResponse.ok(marketDataService.toOrderDto(order));
    }

    private Long requireFirm(Long firmId) {
        if (firmId == null) {
            throw new BizException(ErrorCode.ACCOUNT_NOT_FOUND, "缺少 X-Firm-Id 请求头");
        }
        return firmId;
    }
}
