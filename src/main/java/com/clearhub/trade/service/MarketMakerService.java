package com.clearhub.trade.service;

import com.clearhub.trade.common.BizException;
import com.clearhub.trade.common.Money;
import com.clearhub.trade.config.DemoProperties;
import com.clearhub.trade.domain.Commodity;
import com.clearhub.trade.domain.TradeOrder;
import com.clearhub.trade.dto.PlaceOrderRequest;
import com.clearhub.trade.repository.CommodityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketMakerService {

    private final DemoProperties properties;
    private final CommodityRepository commodityRepository;
    private final QuotationService quotationService;
    private final OrderService orderService;
    private final MatcherService matcherService;

    private final Map<Long, List<Long>> liveOrders = new ConcurrentHashMap<>();
    private final Map<Long, BigDecimal> lastReference = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("MarketMakerService initialized, enabled={}, firmId={}",
                properties.getMarketMaker().isEnabled(), properties.getMarketMaker().getFirmId());
    }

    @Scheduled(initialDelayString = "5000", fixedDelayString = "${demo.market-maker.interval-ms:3000}")
    public void quote() {
        if (!matcherService.isReady()) {
            return;
        }
        DemoProperties.MarketMaker config = properties.getMarketMaker();
        if (config == null || !config.isEnabled() || config.getFirmId() == null) {
            return;
        }
        List<Commodity> commodities = commodityRepository.findByStatusOrderByIdAsc(1);
        for (Commodity commodity : commodities) {
            try {
                quoteOne(commodity, config);
            } catch (Exception ex) {
                log.debug("Market maker skipped commodity {}: {}", commodity.getId(), ex.getMessage());
            }
        }
    }

    private void quoteOne(Commodity commodity, DemoProperties.MarketMaker config) {
        BigDecimal last = quotationService.lastPrice(commodity.getId());
        if (last == null || last.signum() == 0) {
            return;
        }
        BigDecimal tick = commodity.getMinPriceChange();
        double drift = (ThreadLocalRandom.current().nextDouble() * 2 - 1) * config.getMaxPriceDrift();
        BigDecimal reference = last.multiply(BigDecimal.valueOf(1 + drift));
        reference = reference.divide(tick, 0, RoundingMode.HALF_UP).multiply(tick);
        if (reference.signum() <= 0) {
            return;
        }
        BigDecimal previous = lastReference.get(commodity.getId());
        if (previous != null && previous.compareTo(reference) == 0) {
            return;
        }

        cancelLiveOrders(commodity.getId());

        BigDecimal spread = tick.multiply(BigDecimal.valueOf(config.getSpreadTicks()));
        List<Long> ids = new ArrayList<>();
        int levels = Math.max(1, config.getLevels());
        for (int level = 1; level <= levels; level++) {
            BigDecimal offset = spread.multiply(BigDecimal.valueOf(level));
            BigDecimal bidPrice = reference.subtract(offset);
            BigDecimal askPrice = reference.add(offset);
            if (bidPrice.signum() <= 0) {
                continue;
            }
            BigDecimal bidQuantity = randomQuantity(commodity, config);
            TradeOrder bid = place(config.getFirmId(), commodity.getId(), "BUY", bidPrice, bidQuantity);
            if (bid != null) {
                ids.add(bid.getId());
            }
            BigDecimal askQuantity = randomQuantity(commodity, config);
            TradeOrder ask = place(config.getFirmId(), commodity.getId(), "SELL", askPrice, askQuantity);
            if (ask != null) {
                ids.add(ask.getId());
            }
        }
        liveOrders.put(commodity.getId(), ids);
        lastReference.put(commodity.getId(), reference);
    }

    private BigDecimal randomQuantity(Commodity commodity, DemoProperties.MarketMaker config) {
        int min = Math.max(1, config.getMinQuantity());
        int max = Math.max(min, config.getMaxQuantity());
        BigDecimal quantity = BigDecimal.valueOf(ThreadLocalRandom.current().nextInt(min, max + 1));
        BigDecimal step = commodity.getMinAmountChange();
        quantity = quantity.divide(step, 0, RoundingMode.HALF_UP).multiply(step);
        return Money.qty(quantity.max(step));
    }

    private TradeOrder place(Long firmId, Long commodityId, String side, BigDecimal price, BigDecimal quantity) {
        PlaceOrderRequest request = new PlaceOrderRequest();
        request.setCommodityId(commodityId);
        request.setSide(side);
        request.setPrice(price);
        request.setQuantity(quantity);
        try {
            return orderService.placeOrder(firmId, request);
        } catch (BizException ex) {
            log.debug("Market maker {} {} rejected: {}", side, commodityId, ex.getMessage());
            return null;
        }
    }

    private void cancelLiveOrders(Long commodityId) {
        List<Long> ids = liveOrders.remove(commodityId);
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (Long orderId : ids) {
            try {
                orderService.cancelOrder(null, orderId);
            } catch (Exception ignored) {
                // already filled or removed by reset
            }
        }
    }

    public void clearQuotes() {
        liveOrders.clear();
        lastReference.clear();
    }
}
