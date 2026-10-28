package com.clearhub.trade.service;

import com.clearhub.trade.common.BizException;
import com.clearhub.trade.common.OrderStatus;
import com.clearhub.trade.domain.FundsAccount;
import com.clearhub.trade.domain.Position;
import com.clearhub.trade.domain.TradeOrder;
import com.clearhub.trade.domain.TradeRecord;
import com.clearhub.trade.dto.PlaceOrderRequest;
import com.clearhub.trade.repository.FundsAccountRepository;
import com.clearhub.trade.repository.PositionRepository;
import com.clearhub.trade.repository.TradeOrderRepository;
import com.clearhub.trade.repository.TradeRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

@SpringBootTest(properties = "demo.market-maker.enabled=false")
@ActiveProfiles("h2")
class OrderFlowIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private MatcherService matcherService;

    @Autowired
    private DemoDataService demoDataService;

    @Autowired
    private TradeRecordRepository tradeRepository;

    @Autowired
    private TradeOrderRepository orderRepository;

    @Autowired
    private FundsAccountRepository fundsRepository;

    @Autowired
    private PositionRepository positionRepository;

    @BeforeEach
    void reset() {
        matcherService.stopAll();
        demoDataService.resetTradingData();
        matcherService.startAll();
        await().atMost(5, TimeUnit.SECONDS).until(matcherService::isReady);
    }

    @Test
    void buyMatchesRestingSellAndSettlesFundsAndPositions() {
        BigDecimal buyerCashBefore = funds(2L).getBalance();
        BigDecimal sellerCashBefore = funds(3L).getBalance();
        BigDecimal buyerPositionBefore = position(2L).getQuantity();
        BigDecimal sellerPositionBefore = position(3L).getQuantity();

        TradeOrder sell = place(3L, "SELL", "100.00", "5");
        TradeOrder buy = place(2L, "BUY", "100.00", "5");

        await().atMost(5, TimeUnit.SECONDS).until(() -> tradeRepository.count() == 1);

        List<TradeRecord> trades = tradeRepository.findAll();
        assertThat(trades).hasSize(1);
        TradeRecord trade = trades.get(0);
        assertThat(trade.getPrice()).isEqualByComparingTo("100.00");
        assertThat(trade.getQuantity()).isEqualByComparingTo("5");
        assertThat(trade.getAmount()).isEqualByComparingTo("500.00");
        assertThat(trade.getBuyFee()).isEqualByComparingTo("0.25");
        assertThat(trade.getSellFee()).isEqualByComparingTo("0.25");

        assertThat(funds(2L).getBalance()).isEqualByComparingTo(buyerCashBefore.subtract(new BigDecimal("500.25")));
        assertThat(funds(3L).getBalance()).isEqualByComparingTo(sellerCashBefore.add(new BigDecimal("499.75")));
        assertThat(position(2L).getQuantity()).isEqualByComparingTo(buyerPositionBefore.add(new BigDecimal("5")));
        assertThat(position(3L).getQuantity()).isEqualByComparingTo(sellerPositionBefore.subtract(new BigDecimal("5")));

        assertThat(orderRepository.findById(buy.getId()).orElseThrow().getOrderStatus())
                .isEqualTo(OrderStatus.FILLED.getCode());
        assertThat(orderRepository.findById(sell.getId()).orElseThrow().getOrderStatus())
                .isEqualTo(OrderStatus.FILLED.getCode());
        assertThat(funds(2L).getFrozenFunds()).isEqualByComparingTo("0");
        assertThat(position(3L).getFrozenQuantity()).isEqualByComparingTo("0");
    }

    @Test
    void cancelReleasesFrozenFunds() {
        TradeOrder buy = place(2L, "BUY", "90.00", "10");
        assertThat(funds(2L).getFrozenFunds()).isPositive();

        TradeOrder cancelled = orderService.cancelOrder(2L, buy.getId());

        assertThat(cancelled.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED.getCode());
        assertThat(funds(2L).getFrozenFunds()).isEqualByComparingTo("0");
        assertThat(orderRepository.findById(buy.getId()).orElseThrow().getOrderStatus())
                .isEqualTo(OrderStatus.CANCELLED.getCode());
    }

    @Test
    void insufficientFundsIsRejected() {
        assertThatThrownBy(() -> place(2L, "BUY", "100.00", "100000"))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(40003));
    }

    @Test
    void priceOutsideLimitIsRejected() {
        assertThatThrownBy(() -> place(2L, "BUY", "200.00", "1"))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(40007));
    }

    @Test
    void insufficientPositionIsRejected() {
        place(2L, "SELL", "100.00", "60000");
        assertThatThrownBy(() -> place(2L, "SELL", "100.00", "60000"))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(40004));
    }

    private TradeOrder place(Long firmId, String side, String price, String quantity) {
        PlaceOrderRequest request = new PlaceOrderRequest();
        request.setCommodityId(1L);
        request.setSide(side);
        request.setPrice(new BigDecimal(price));
        request.setQuantity(new BigDecimal(quantity));
        return orderService.placeOrder(firmId, request);
    }

    private FundsAccount funds(Long firmId) {
        return fundsRepository.findByFirmIdAndSymbolId(firmId, 1L).orElseThrow();
    }

    private Position position(Long firmId) {
        return positionRepository.findByFirmIdAndCommodityId(firmId, 1L).orElseThrow();
    }
}
