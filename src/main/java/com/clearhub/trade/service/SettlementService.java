package com.clearhub.trade.service;

import com.clearhub.trade.common.Money;
import com.clearhub.trade.common.OrderStatus;
import com.clearhub.trade.domain.TradeOrder;
import com.clearhub.trade.domain.TradeRecord;
import com.clearhub.trade.dto.SettlementResult;
import com.clearhub.trade.engine.Match;
import com.clearhub.trade.repository.TradeOrderRepository;
import com.clearhub.trade.repository.TradeRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementService {

    private static final DateTimeFormatter TRADE_NO_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final TradeOrderRepository orderRepository;
    private final TradeRecordRepository tradeRepository;
    private final PositionService positionService;
    private final MarginLedger marginLedger;
    private final QuotationService quotationService;

    @Transactional
    public SettlementResult settle(Match match) {
        TradeOrder buy = orderRepository.findForUpdate(match.buyOrderId())
                .orElseThrow(() -> new IllegalStateException("buy order missing: " + match.buyOrderId()));
        TradeOrder sell = orderRepository.findForUpdate(match.sellOrderId())
                .orElseThrow(() -> new IllegalStateException("sell order missing: " + match.sellOrderId()));

        if (!buy.isOpen() || !sell.isOpen()) {
            log.warn("Skip settlement for closed order, buy={}, sell={}", buy.getId(), sell.getId());
            return null;
        }

        BigDecimal quantity = match.quantity();
        if (buy.remainingQuantity().compareTo(quantity) < 0 || sell.remainingQuantity().compareTo(quantity) < 0) {
            quantity = buy.remainingQuantity().min(sell.remainingQuantity());
            if (quantity.signum() <= 0) {
                return null;
            }
        }

        BigDecimal price = Money.price(match.price());
        BigDecimal amount = Money.money(price.multiply(quantity));
        BigDecimal buyFee = Money.money(amount.multiply(buy.getFeeRate()));
        BigDecimal sellFee = Money.money(amount.multiply(sell.getFeeRate()));

        TradeRecord trade = new TradeRecord();
        trade.setTradeNo("T" + LocalDateTime.now().format(TRADE_NO_FORMAT)
                + String.format("%03d", ThreadLocalRandom.current().nextInt(1000)));
        trade.setCommodityId(match.commodityId());
        trade.setBuyOrderId(buy.getId());
        trade.setSellOrderId(sell.getId());
        trade.setBuyFirmId(buy.getFirmId());
        trade.setSellFirmId(sell.getFirmId());
        trade.setPrice(price);
        trade.setQuantity(Money.qty(quantity));
        trade.setAmount(amount);
        trade.setBuyFee(buyFee);
        trade.setSellFee(sellFee);
        trade.setTradeDay(buy.getTradeDay());
        trade.setTakerSide(match.takerSide());
        tradeRepository.save(trade);

        BigDecimal newBuyFilled = buy.getFilledQuantity().add(quantity);
        BigDecimal deduct = Money.money(amount.add(buyFee));
        buy.setFilledQuantity(Money.qty(newBuyFilled));
        buy.setSettledFunds(Money.money(buy.getSettledFunds().add(deduct)));
        BigDecimal releaseExtra = BigDecimal.ZERO;
        if (newBuyFilled.compareTo(buy.getQuantity()) >= 0) {
            buy.setOrderStatus(OrderStatus.FILLED.getCode());
            releaseExtra = Money.zeroIfNegative(Money.money(buy.getFrozenFunds().subtract(buy.getSettledFunds())));
        } else {
            buy.setOrderStatus(OrderStatus.PARTIAL.getCode());
        }
        orderRepository.save(buy);

        BigDecimal newSellFilled = sell.getFilledQuantity().add(quantity);
        sell.setFilledQuantity(Money.qty(newSellFilled));
        sell.setFrozenQuantity(Money.zeroIfNegative(sell.getFrozenQuantity().subtract(quantity)));
        if (newSellFilled.compareTo(sell.getQuantity()) >= 0) {
            sell.setOrderStatus(OrderStatus.FILLED.getCode());
            sell.setFrozenQuantity(BigDecimal.ZERO);
        } else {
            sell.setOrderStatus(OrderStatus.PARTIAL.getCode());
        }
        orderRepository.save(sell);

        positionService.applyBuy(buy.getFirmId(), match.commodityId(), quantity, price, amount, trade.getId());
        positionService.applySell(sell.getFirmId(), match.commodityId(), quantity);

        marginLedger.settleBuy(buy.getFirmId(), deduct, releaseExtra, "TRADE", trade.getId(), trade.getTradeNo());
        marginLedger.creditSell(sell.getFirmId(), Money.money(amount.subtract(sellFee)),
                "TRADE", trade.getId(), trade.getTradeNo());

        quotationService.updateOnTrade(match.commodityId(), price, quantity, amount);

        return new SettlementResult(
                trade.getId(),
                trade.getTradeNo(),
                trade.getCommodityId(),
                price,
                quantity,
                amount,
                trade.getBuyFirmId(),
                trade.getSellFirmId(),
                trade.getTakerSide(),
                trade.getCreateTime()
        );
    }
}
