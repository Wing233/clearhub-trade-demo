package com.clearhub.trade.service;

import com.clearhub.trade.common.Money;
import com.clearhub.trade.common.OrderStatus;
import com.clearhub.trade.common.Side;
import com.clearhub.trade.domain.Commodity;
import com.clearhub.trade.domain.CommodityStatus;
import com.clearhub.trade.domain.Firm;
import com.clearhub.trade.domain.FundsAccount;
import com.clearhub.trade.domain.FundsFlow;
import com.clearhub.trade.domain.Position;
import com.clearhub.trade.domain.Quotation;
import com.clearhub.trade.domain.TradeOrder;
import com.clearhub.trade.domain.TradeParam;
import com.clearhub.trade.domain.TradeRecord;
import com.clearhub.trade.dto.CommodityDto;
import com.clearhub.trade.dto.DemoStatusDto;
import com.clearhub.trade.dto.FirmDto;
import com.clearhub.trade.dto.FlowDto;
import com.clearhub.trade.dto.FundsDto;
import com.clearhub.trade.dto.OrderBookDto;
import com.clearhub.trade.dto.OrderDto;
import com.clearhub.trade.dto.PositionDto;
import com.clearhub.trade.dto.TradeDto;
import com.clearhub.trade.engine.BookSnapshot;
import com.clearhub.trade.repository.CommodityRepository;
import com.clearhub.trade.repository.CommodityStatusRepository;
import com.clearhub.trade.repository.FirmRepository;
import com.clearhub.trade.repository.FundsAccountRepository;
import com.clearhub.trade.repository.FundsFlowRepository;
import com.clearhub.trade.repository.PositionRepository;
import com.clearhub.trade.repository.TradeOrderRepository;
import com.clearhub.trade.repository.TradeParamRepository;
import com.clearhub.trade.repository.TradeRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MarketDataService {

    private final FirmRepository firmRepository;
    private final CommodityRepository commodityRepository;
    private final CommodityStatusRepository commodityStatusRepository;
    private final TradeParamRepository tradeParamRepository;
    private final TradeOrderRepository orderRepository;
    private final TradeRecordRepository tradeRecordRepository;
    private final PositionRepository positionRepository;
    private final FundsAccountRepository fundsRepository;
    private final FundsFlowRepository flowRepository;
    private final QuotationService quotationService;
    private final MatcherService matcherService;

    public List<FirmDto> listFirms() {
        return firmRepository.findAllByOrderByIdAsc().stream()
                .map(firm -> new FirmDto(firm.getId(), firm.getCode(), firm.getName()))
                .toList();
    }

    public List<CommodityDto> listCommodities() {
        Map<Long, Commodity> commodities = commodityRepository.findAll().stream()
                .collect(Collectors.toMap(Commodity::getId, Function.identity()));
        return commodityRepository.findByStatusOrderByIdAsc(1).stream()
                .map(commodity -> toCommodityDto(commodity, commodities))
                .toList();
    }

    private CommodityDto toCommodityDto(Commodity commodity, Map<Long, Commodity> all) {
        Quotation quotation = quotationService.getOrCreate(commodity.getId());
        TradeParam param = tradeParamRepository.findById(commodity.getId()).orElse(null);
        CommodityStatus status = commodityStatusRepository.findById(commodity.getId()).orElse(null);
        BookSnapshot snapshot = matcherService.snapshot(commodity.getId(), 1);

        CommodityDto dto = new CommodityDto();
        dto.setId(commodity.getId());
        dto.setCode(commodity.getCode());
        dto.setName(commodity.getName());
        dto.setMarketId(commodity.getMarketId());
        dto.setLastPrice(quotation.getLastPrice());
        dto.setOpenPrice(quotation.getOpenPrice());
        dto.setHighPrice(quotation.getHighPrice());
        dto.setLowPrice(quotation.getLowPrice());
        dto.setVolume(quotation.getVolume());
        dto.setAmount(quotation.getAmount());
        dto.setMinPriceChange(commodity.getMinPriceChange());
        dto.setMinAmountChange(commodity.getMinAmountChange());
        dto.setFeeRate(param == null ? BigDecimal.ZERO : param.getFeeRate());
        dto.setBestBid(snapshot.bestBid());
        dto.setBestAsk(snapshot.bestAsk());
        dto.setTradable(status != null && status.isTradable());
        if (quotation.getOpenPrice() != null && quotation.getOpenPrice().signum() > 0
                && quotation.getLastPrice() != null) {
            dto.setChangePct(quotation.getLastPrice()
                    .subtract(quotation.getOpenPrice())
                    .multiply(BigDecimal.valueOf(100))
                    .divide(quotation.getOpenPrice(), 2, RoundingMode.HALF_UP));
        } else {
            dto.setChangePct(BigDecimal.ZERO);
        }
        return dto;
    }

    public OrderBookDto orderBook(Long commodityId, int depth) {
        BookSnapshot snapshot = matcherService.snapshot(commodityId, Math.max(1, Math.min(depth, 50)));
        Quotation quotation = quotationService.getOrCreate(commodityId);
        return new OrderBookDto(commodityId, snapshot.bids(), snapshot.asks(),
                quotation.getLastPrice(), snapshot.timestamp());
    }

    public List<TradeDto> recentTrades(Long commodityId, int limit) {
        Map<Long, Commodity> commodities = commodityMap();
        return tradeRecordRepository.findByCommodityIdOrderByIdDesc(commodityId,
                        PageRequest.of(0, Math.max(1, Math.min(limit, 200))))
                .stream()
                .map(trade -> toTradeDto(trade, commodities, null))
                .toList();
    }

    public List<TradeDto> firmTrades(Long firmId, int limit) {
        Map<Long, Commodity> commodities = commodityMap();
        return tradeRecordRepository.findFirmTrades(firmId,
                        PageRequest.of(0, Math.max(1, Math.min(limit, 200))))
                .stream()
                .map(trade -> toTradeDto(trade, commodities, firmId))
                .toList();
    }

    public List<OrderDto> firmOrders(Long firmId, Integer status, int limit) {
        Map<Long, Commodity> commodities = commodityMap();
        return orderRepository.findFirmOrders(firmId, status,
                        PageRequest.of(0, Math.max(1, Math.min(limit, 200))))
                .stream()
                .map(order -> toOrderDto(order, commodities))
                .toList();
    }

    public OrderDto toOrderDto(TradeOrder order) {
        return toOrderDto(order, commodityMap());
    }

    private OrderDto toOrderDto(TradeOrder order, Map<Long, Commodity> commodities) {
        Commodity commodity = commodities.get(order.getCommodityId());
        OrderDto dto = new OrderDto();
        dto.setId(order.getId());
        dto.setOrderNo(order.getOrderNo());
        dto.setFirmId(order.getFirmId());
        dto.setCommodityId(order.getCommodityId());
        dto.setCommodityCode(commodity == null ? "" : commodity.getCode());
        dto.setCommodityName(commodity == null ? "" : commodity.getName());
        dto.setSide(order.getBsFlag());
        dto.setSideLabel(Side.BUY.flag().equals(order.getBsFlag()) ? "买入" : "卖出");
        dto.setPrice(order.getPrice());
        dto.setQuantity(order.getQuantity());
        dto.setFilledQuantity(order.getFilledQuantity());
        dto.setRemainingQuantity(order.remainingQuantity());
        dto.setStatus(order.getOrderStatus());
        dto.setStatusLabel(OrderStatus.of(order.getOrderStatus()).getLabel());
        dto.setFrozenFunds(order.getFrozenFunds());
        dto.setFrozenQuantity(order.getFrozenQuantity());
        dto.setFee(order.getFee());
        dto.setCreateTime(order.getCreateTime());
        return dto;
    }

    private TradeDto toTradeDto(TradeRecord trade, Map<Long, Commodity> commodities, Long firmId) {
        Commodity commodity = commodities.get(trade.getCommodityId());
        TradeDto dto = new TradeDto();
        dto.setId(trade.getId());
        dto.setTradeNo(trade.getTradeNo());
        dto.setCommodityId(trade.getCommodityId());
        dto.setCommodityCode(commodity == null ? "" : commodity.getCode());
        dto.setCommodityName(commodity == null ? "" : commodity.getName());
        dto.setPrice(trade.getPrice());
        dto.setQuantity(trade.getQuantity());
        dto.setAmount(trade.getAmount());
        dto.setBuyFee(trade.getBuyFee());
        dto.setSellFee(trade.getSellFee());
        dto.setBuyFirmId(trade.getBuyFirmId());
        dto.setSellFirmId(trade.getSellFirmId());
        if (firmId != null) {
            if (firmId.equals(trade.getBuyFirmId())) {
                dto.setSide("B");
                dto.setSideLabel("买入");
            } else if (firmId.equals(trade.getSellFirmId())) {
                dto.setSide("S");
                dto.setSideLabel("卖出");
            }
        }
        dto.setCreateTime(trade.getCreateTime());
        return dto;
    }

    public List<PositionDto> positions(Long firmId) {
        Map<Long, Commodity> commodities = commodityMap();
        return positionRepository.findByFirmIdOrderByCommodityIdAsc(firmId).stream()
                .map(position -> {
                    Commodity commodity = commodities.get(position.getCommodityId());
                    BigDecimal last = quotationService.lastPrice(position.getCommodityId());
                    PositionDto dto = new PositionDto();
                    dto.setCommodityId(position.getCommodityId());
                    dto.setCommodityCode(commodity == null ? "" : commodity.getCode());
                    dto.setCommodityName(commodity == null ? "" : commodity.getName());
                    dto.setQuantity(position.getQuantity());
                    dto.setFrozenQuantity(position.getFrozenQuantity());
                    dto.setAvailable(position.available());
                    dto.setAvgPrice(position.getAvgPrice());
                    dto.setLastPrice(last);
                    dto.setMarketValue(Money.money(position.getQuantity().multiply(last)));
                    BigDecimal pnl = last.subtract(position.getAvgPrice()).multiply(position.getQuantity());
                    dto.setPnl(Money.money(pnl));
                    if (position.getAvgPrice() != null && position.getAvgPrice().signum() > 0) {
                        dto.setPnlPct(last.subtract(position.getAvgPrice())
                                .multiply(BigDecimal.valueOf(100))
                                .divide(position.getAvgPrice(), 2, RoundingMode.HALF_UP));
                    } else {
                        dto.setPnlPct(BigDecimal.ZERO);
                    }
                    return dto;
                })
                .toList();
    }

    public FundsDto funds(Long firmId) {
        FundsAccount account = fundsRepository.findByFirmIdAndSymbolId(firmId, 1L).orElse(null);
        FundsDto dto = new FundsDto();
        dto.setFirmId(firmId);
        dto.setBalance(account == null ? BigDecimal.ZERO : account.getBalance());
        dto.setFrozenFunds(account == null ? BigDecimal.ZERO : account.getFrozenFunds());
        dto.setAvailable(account == null ? BigDecimal.ZERO : account.available());
        return dto;
    }

    public List<FlowDto> flows(Long firmId, int limit) {
        return flowRepository.findByFirmIdOrderByIdDesc(firmId,
                        PageRequest.of(0, Math.max(1, Math.min(limit, 200))))
                .stream()
                .map(this::toFlowDto)
                .toList();
    }

    private FlowDto toFlowDto(FundsFlow flow) {
        FlowDto dto = new FlowDto();
        dto.setId(flow.getId());
        dto.setBizCode(flow.getBizCode());
        dto.setBizLabel(bizLabel(flow.getBizCode()));
        dto.setAmount(flow.getAmount());
        dto.setFrozenAmount(flow.getFrozenAmount());
        dto.setBalanceAfter(flow.getBalanceAfter());
        dto.setFrozenAfter(flow.getFrozenAfter());
        dto.setNote(flow.getNote());
        dto.setCreateTime(flow.getCreateTime());
        return dto;
    }

    private String bizLabel(String bizCode) {
        return switch (bizCode) {
            case "ORDER_FREEZE" -> "委托冻结";
            case "ORDER_CANCEL" -> "撤单解冻";
            case "TRADE_BUY" -> "买入成交";
            case "TRADE_SELL" -> "卖出成交";
            default -> bizCode;
        };
    }

    public DemoStatusDto demoStatus() {
        DemoStatusDto dto = new DemoStatusDto();
        dto.setStatus("UP");
        dto.setFirms(firmRepository.count());
        dto.setCommodities(commodityRepository.count());
        dto.setOrders(orderRepository.count());
        dto.setOpenOrders(matcherService.openOrderCount());
        dto.setTrades(tradeRecordRepository.count());
        dto.setFlows(flowRepository.count());
        dto.setTotalVolume(tradeRecordRepository.sumQuantity());
        dto.setServerTime(LocalDateTime.now());
        return dto;
    }

    private Map<Long, Commodity> commodityMap() {
        return commodityRepository.findAll().stream()
                .collect(Collectors.toMap(Commodity::getId, Function.identity()));
    }
}
