package com.clearhub.trade.service;

import com.clearhub.trade.common.BizException;
import com.clearhub.trade.common.ErrorCode;
import com.clearhub.trade.common.Money;
import com.clearhub.trade.common.OrderStatus;
import com.clearhub.trade.common.Side;
import com.clearhub.trade.domain.Commodity;
import com.clearhub.trade.domain.CommodityStatus;
import com.clearhub.trade.domain.SystemStatus;
import com.clearhub.trade.domain.TradeOrder;
import com.clearhub.trade.domain.TradeParam;
import com.clearhub.trade.dto.PlaceOrderRequest;
import com.clearhub.trade.repository.CommodityRepository;
import com.clearhub.trade.repository.CommodityStatusRepository;
import com.clearhub.trade.repository.FirmRepository;
import com.clearhub.trade.repository.SystemStatusRepository;
import com.clearhub.trade.repository.TradeOrderRepository;
import com.clearhub.trade.repository.TradeParamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final DateTimeFormatter ORDER_NO_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final FirmRepository firmRepository;
    private final CommodityRepository commodityRepository;
    private final CommodityStatusRepository commodityStatusRepository;
    private final SystemStatusRepository systemStatusRepository;
    private final TradeParamRepository tradeParamRepository;
    private final TradeOrderRepository orderRepository;
    private final QuotationService quotationService;
    private final MarginLedger marginLedger;
    private final PositionService positionService;
    private final MatcherService matcherService;

    @Transactional
    public TradeOrder placeOrder(Long firmId, PlaceOrderRequest request) {
        firmRepository.findById(firmId)
                .orElseThrow(() -> new BizException(ErrorCode.ACCOUNT_NOT_FOUND, "firmId=" + firmId));
        Commodity commodity = commodityRepository.findById(request.getCommodityId())
                .orElseThrow(() -> new BizException(ErrorCode.COMMODITY_NOT_FOUND,
                        "commodityId=" + request.getCommodityId()));
        if (!Integer.valueOf(1).equals(commodity.getStatus())) {
            throw new BizException(ErrorCode.MARKET_CLOSED, "商品已停牌");
        }
        CommodityStatus status = commodityStatusRepository.findById(commodity.getId())
                .orElseThrow(() -> new BizException(ErrorCode.MARKET_CLOSED, "商品状态未配置"));
        if (!status.isTradable()) {
            throw new BizException(ErrorCode.MARKET_CLOSED);
        }
        SystemStatus systemStatus = systemStatusRepository.findById(1L)
                .orElseThrow(() -> new BizException(ErrorCode.MARKET_CLOSED, "系统状态未配置"));
        if (!systemStatus.isTrading()) {
            throw new BizException(ErrorCode.MARKET_CLOSED);
        }

        Side side = Side.of(request.getSide());
        BigDecimal price = Money.price(request.getPrice());
        BigDecimal quantity = Money.qty(request.getQuantity());
        if (!Money.isMultiple(price, commodity.getMinPriceChange())) {
            throw new BizException(ErrorCode.PRICE_INVALID,
                    "最小变动价位 " + commodity.getMinPriceChange());
        }
        if (!Money.isMultiple(quantity, commodity.getMinAmountChange())) {
            throw new BizException(ErrorCode.QUANTITY_INVALID,
                    "最小变动数量 " + commodity.getMinAmountChange());
        }
        if (price.signum() <= 0 || quantity.signum() <= 0) {
            throw new BizException(ErrorCode.VALIDATION);
        }

        TradeParam param = tradeParamRepository.findById(commodity.getId())
                .orElseGet(() -> defaultParam(commodity.getId()));
        if (quantity.compareTo(param.getMaxOrderQty()) > 0) {
            throw new BizException(ErrorCode.VALIDATION,
                    "单笔最大数量 " + param.getMaxOrderQty());
        }
        checkPriceLimit(commodity.getId(), price, param);

        BigDecimal amount = Money.money(price.multiply(quantity));
        BigDecimal fee = Money.money(amount.multiply(param.getFeeRate()));

        TradeOrder order = new TradeOrder();
        order.setOrderNo("O" + LocalDateTime.now().format(ORDER_NO_FORMAT)
                + String.format("%03d", ThreadLocalRandom.current().nextInt(1000)));
        order.setFirmId(firmId);
        order.setCommodityId(commodity.getId());
        order.setBsFlag(side.flag());
        order.setOcFlag("O");
        order.setPrice(price);
        order.setQuantity(quantity);
        order.setFilledQuantity(BigDecimal.ZERO);
        order.setOrderStatus(OrderStatus.NEW.getCode());
        order.setFeeRate(param.getFeeRate());
        order.setFee(fee);
        order.setSettledFunds(BigDecimal.ZERO);
        order.setTradeDay(systemStatus.getTradeDay());

        if (side == Side.BUY) {
            BigDecimal frozen = Money.money(amount.add(fee));
            order.setFrozenFunds(frozen);
            order.setFrozenQuantity(BigDecimal.ZERO);
            orderRepository.save(order);
            marginLedger.freeze(firmId, frozen, "ORDER_FREEZE", "ORDER", order.getId(), order.getOrderNo());
        } else {
            order.setFrozenFunds(BigDecimal.ZERO);
            order.setFrozenQuantity(quantity);
            orderRepository.save(order);
            positionService.freeze(firmId, commodity.getId(), quantity);
        }

        submitAfterCommit(order);
        return order;
    }

    @Transactional
    public TradeOrder cancelOrder(Long firmId, Long orderId) {
        TradeOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BizException(ErrorCode.ORDER_NOT_FOUND, "orderId=" + orderId));
        if (firmId != null && !firmId.equals(order.getFirmId())) {
            throw new BizException(ErrorCode.ORDER_NOT_FOUND, "orderId=" + orderId);
        }
        if (!order.isOpen()) {
            throw new BizException(ErrorCode.ORDER_NOT_CANCELLABLE);
        }
        if (order.getCommodityId() == null) {
            throw new BizException(ErrorCode.INTERNAL, "委托缺少商品");
        }
        try {
            Boolean removed = matcherService.cancel(order.getCommodityId(), orderId)
                    .get(3, TimeUnit.SECONDS);
            if (!Boolean.TRUE.equals(removed)) {
                throw new BizException(ErrorCode.ORDER_NOT_CANCELLABLE, "委托已成交或不在撮合队列");
            }
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BizException(ErrorCode.INTERNAL, "撤单超时");
        }

        TradeOrder locked = orderRepository.findForUpdate(orderId)
                .orElseThrow(() -> new BizException(ErrorCode.ORDER_NOT_FOUND, "orderId=" + orderId));
        if (!locked.isOpen()) {
            throw new BizException(ErrorCode.ORDER_NOT_CANCELLABLE);
        }
        if (Side.BUY.flag().equals(locked.getBsFlag())) {
            BigDecimal release = Money.zeroIfNegative(
                    Money.money(locked.getFrozenFunds().subtract(locked.getSettledFunds())));
            marginLedger.release(locked.getFirmId(), release, "ORDER_CANCEL",
                    "ORDER", locked.getId(), locked.getOrderNo());
            locked.setFrozenFunds(locked.getSettledFunds());
        } else {
            positionService.release(locked.getFirmId(), locked.getCommodityId(), locked.getFrozenQuantity());
            locked.setFrozenQuantity(BigDecimal.ZERO);
        }
        locked.setOrderStatus(OrderStatus.CANCELLED.getCode());
        orderRepository.save(locked);
        log.info("Order cancelled: id={}, firmId={}, side={}", locked.getId(), locked.getFirmId(), locked.getBsFlag());
        return locked;
    }

    private void checkPriceLimit(Long commodityId, BigDecimal price, TradeParam param) {
        BigDecimal last = quotationService.lastPrice(commodityId);
        if (last == null || last.signum() == 0 || param.getPriceLimitPct() == null
                || param.getPriceLimitPct().signum() <= 0) {
            return;
        }
        BigDecimal lower = last.multiply(BigDecimal.ONE.subtract(param.getPriceLimitPct()));
        BigDecimal upper = last.multiply(BigDecimal.ONE.add(param.getPriceLimitPct()));
        if (price.compareTo(lower) < 0 || price.compareTo(upper) > 0) {
            throw new BizException(ErrorCode.PRICE_OUT_OF_LIMIT,
                    "允许范围 " + Money.price(lower) + " ~ " + Money.price(upper));
        }
    }

    private TradeParam defaultParam(Long commodityId) {
        TradeParam param = new TradeParam();
        param.setCommodityId(commodityId);
        return param;
    }

    private void submitAfterCommit(TradeOrder order) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    matcherService.submit(order);
                }
            });
        } else {
            matcherService.submit(order);
        }
    }
}
