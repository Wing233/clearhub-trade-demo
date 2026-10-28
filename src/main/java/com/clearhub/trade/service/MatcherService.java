package com.clearhub.trade.service;

import com.clearhub.trade.domain.Commodity;
import com.clearhub.trade.domain.TradeOrder;
import com.clearhub.trade.dto.SettlementResult;
import com.clearhub.trade.engine.BookLevel;
import com.clearhub.trade.engine.BookOrder;
import com.clearhub.trade.engine.BookSnapshot;
import com.clearhub.trade.engine.Match;
import com.clearhub.trade.engine.MatchEngine;
import com.clearhub.trade.engine.MatcherWorker;
import com.clearhub.trade.repository.CommodityRepository;
import com.clearhub.trade.repository.TradeOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatcherService {

    private final CommodityRepository commodityRepository;
    private final TradeOrderRepository orderRepository;
    private final SettlementService settlementService;
    private final MarketBroadcastService broadcastService;

    private final Map<Long, MatcherWorker> workers = new ConcurrentHashMap<>();
    private volatile boolean ready;

    @EventListener(ApplicationReadyEvent.class)
    public synchronized void startAll() {
        stopAll();
        for (Commodity commodity : commodityRepository.findByStatusOrderByIdAsc(1)) {
            long commodityId = commodity.getId();
            MatchEngine engine = new MatchEngine(commodityId);
            MatcherWorker worker = new MatcherWorker(
                    engine,
                    match -> {
                        SettlementResult result = settlementService.settle(match);
                        if (result != null) {
                            broadcastService.broadcastTrade(result);
                        }
                    },
                    snapshot -> broadcastService.broadcastBook(commodityId, snapshot)
            );
            worker.start();
            workers.put(commodityId, worker);
        }

        List<TradeOrder> openOrders = orderRepository.findByOrderStatusInOrderByIdAsc(List.of(0, 1));
        for (TradeOrder order : openOrders) {
            MatcherWorker worker = workers.get(order.getCommodityId());
            if (worker != null) {
                worker.restore(toBookOrder(order));
            }
        }
        ready = true;
        log.info("Matcher started: commodities={}, restoredOrders={}", workers.size(), openOrders.size());
    }

    public boolean submit(TradeOrder order) {
        if (!ready) {
            log.warn("Matcher not ready, order {} not submitted", order.getId());
            return false;
        }
        MatcherWorker worker = workers.get(order.getCommodityId());
        if (worker == null) {
            log.warn("No matcher for commodity {}, order {} not submitted", order.getCommodityId(), order.getId());
            return false;
        }
        worker.submit(toBookOrder(order));
        return true;
    }

    public CompletableFuture<Boolean> cancel(Long commodityId, Long orderId) {
        MatcherWorker worker = workers.get(commodityId);
        if (worker == null) {
            return CompletableFuture.completedFuture(false);
        }
        return worker.cancel(orderId);
    }

    public BookSnapshot snapshot(Long commodityId, int depth) {
        MatcherWorker worker = workers.get(commodityId);
        if (worker == null) {
            return new BookSnapshot(commodityId, List.of(), List.of(), null, null, System.currentTimeMillis());
        }
        return worker.snapshot(depth);
    }

    public BookLevel[] bestLevels(Long commodityId) {
        BookSnapshot snapshot = snapshot(commodityId, 1);
        BookLevel bid = snapshot.bids().isEmpty() ? null : snapshot.bids().get(0);
        BookLevel ask = snapshot.asks().isEmpty() ? null : snapshot.asks().get(0);
        return new BookLevel[]{bid, ask};
    }

    public int openOrderCount() {
        return workers.values().stream().mapToInt(MatcherWorker::openOrderCount).sum();
    }

    public boolean isReady() {
        return ready;
    }

    public synchronized void stopAll() {
        ready = false;
        workers.values().forEach(MatcherWorker::stop);
        workers.clear();
    }

    public synchronized void restart() {
        stopAll();
        startAll();
    }

    @PreDestroy
    public void shutdown() {
        stopAll();
    }

    private BookOrder toBookOrder(TradeOrder order) {
        LocalDateTime createdAt = order.getCreateTime() == null ? LocalDateTime.now() : order.getCreateTime();
        BigDecimal remaining = order.remainingQuantity();
        return new BookOrder(
                order.getId(),
                order.getFirmId(),
                order.getBsFlag(),
                order.getPrice(),
                remaining,
                createdAt
        );
    }
}
