package com.clearhub.trade.service;

import com.clearhub.trade.common.BizException;
import com.clearhub.trade.config.DemoProperties;
import com.clearhub.trade.domain.Commodity;
import com.clearhub.trade.dto.PlaceOrderRequest;
import com.clearhub.trade.engine.BookSnapshot;
import com.clearhub.trade.repository.CommodityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Generates real trades through the matching engine so the chart and tape are
 * populated continuously. The flow trader uses its own firm and never touches
 * the demo user's account.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "demo.flow-trader", name = "enabled", havingValue = "true", matchIfMissing = true)
public class FlowTraderService {

    private final DemoProperties properties;
    private final CommodityRepository commodityRepository;
    private final MatcherService matcherService;
    private final QuotationService quotationService;
    private final OrderService orderService;

    @Scheduled(initialDelayString = "4000", fixedDelayString = "${demo.flow-trader.interval-ms:2500}")
    public void trade() {
        if (!matcherService.isReady()) {
            return;
        }
        DemoProperties.FlowTrader config = properties.getFlowTrader();
        if (config == null || !config.isEnabled() || config.getFirmId() == null) {
            return;
        }
        List<Commodity> commodities = commodityRepository.findByStatusOrderByIdAsc(1);
        if (commodities.isEmpty()) {
            return;
        }
        Commodity commodity = commodities.get(ThreadLocalRandom.current().nextInt(commodities.size()));
        try {
            tradeOne(commodity, config);
        } catch (BizException ex) {
            log.debug("Flow trader skipped {}: {}", commodity.getCode(), ex.getMessage());
        } catch (Exception ex) {
            log.debug("Flow trader error on {}: {}", commodity.getCode(), ex.getMessage());
        }
    }

    private void tradeOne(Commodity commodity, DemoProperties.FlowTrader config) {
        BookSnapshot snapshot = matcherService.snapshot(commodity.getId(), 1);
        BigDecimal tick = commodity.getMinPriceChange();
        boolean buy = ThreadLocalRandom.current().nextBoolean();
        BigDecimal price;
        if (buy) {
            price = snapshot.bestAsk() != null
                    ? snapshot.bestAsk().add(tick)
                    : quotationService.lastPrice(commodity.getId()).add(tick);
        } else {
            price = snapshot.bestBid() != null
                    ? snapshot.bestBid().subtract(tick)
                    : quotationService.lastPrice(commodity.getId()).subtract(tick);
        }
        if (price == null || price.signum() <= 0) {
            return;
        }
        BigDecimal quantity = BigDecimal.valueOf(ThreadLocalRandom.current().nextInt(
                Math.max(1, config.getMinQuantity()),
                Math.max(config.getMinQuantity(), config.getMaxQuantity()) + 1));
        BigDecimal step = commodity.getMinAmountChange();
        quantity = quantity.divide(step, 0, RoundingMode.HALF_UP).multiply(step).max(step);

        PlaceOrderRequest request = new PlaceOrderRequest();
        request.setCommodityId(commodity.getId());
        request.setSide(buy ? "BUY" : "SELL");
        request.setPrice(price);
        request.setQuantity(quantity);
        orderService.placeOrder(config.getFirmId(), request);
    }
}
