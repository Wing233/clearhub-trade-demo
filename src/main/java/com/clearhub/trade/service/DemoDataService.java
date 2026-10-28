package com.clearhub.trade.service;

import com.clearhub.trade.config.DemoProperties;
import com.clearhub.trade.domain.Commodity;
import com.clearhub.trade.domain.CommodityStatus;
import com.clearhub.trade.domain.Firm;
import com.clearhub.trade.domain.FundsAccount;
import com.clearhub.trade.domain.Market;
import com.clearhub.trade.domain.Position;
import com.clearhub.trade.domain.Quotation;
import com.clearhub.trade.domain.Symbol;
import com.clearhub.trade.domain.SystemStatus;
import com.clearhub.trade.domain.TradeDay;
import com.clearhub.trade.domain.TradeParam;
import com.clearhub.trade.repository.CommodityRepository;
import com.clearhub.trade.repository.CommodityStatusRepository;
import com.clearhub.trade.repository.FirmRepository;
import com.clearhub.trade.repository.FundsAccountRepository;
import com.clearhub.trade.repository.FundsFlowRepository;
import com.clearhub.trade.repository.HoldDetailRepository;
import com.clearhub.trade.repository.MarketRepository;
import com.clearhub.trade.repository.PositionRepository;
import com.clearhub.trade.repository.QuotationRepository;
import com.clearhub.trade.repository.SymbolRepository;
import com.clearhub.trade.repository.SystemStatusRepository;
import com.clearhub.trade.repository.TradeDayRepository;
import com.clearhub.trade.repository.TradeOrderRepository;
import com.clearhub.trade.repository.TradeParamRepository;
import com.clearhub.trade.repository.TradeRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DemoDataService {

    public static final String TRADE_DAY = "20260101";

    private final DemoProperties properties;
    private final MarketRepository marketRepository;
    private final CommodityRepository commodityRepository;
    private final CommodityStatusRepository commodityStatusRepository;
    private final SystemStatusRepository systemStatusRepository;
    private final TradeDayRepository tradeDayRepository;
    private final FirmRepository firmRepository;
    private final SymbolRepository symbolRepository;
    private final FundsAccountRepository fundsRepository;
    private final PositionRepository positionRepository;
    private final QuotationRepository quotationRepository;
    private final TradeParamRepository tradeParamRepository;
    private final TradeOrderRepository orderRepository;
    private final TradeRecordRepository tradeRecordRepository;
    private final HoldDetailRepository holdDetailRepository;
    private final FundsFlowRepository fundsFlowRepository;

    @Transactional
    public void seedIfEmpty() {
        if (firmRepository.count() > 0) {
            log.info("Demo data already present, skip seeding");
            return;
        }
        log.info("Seeding demo data ...");

        Market market = new Market();
        market.setId(1L);
        market.setCode("DEMO");
        market.setName("演示市场");
        market.setTradingModel("TS");
        marketRepository.save(market);

        Symbol symbol = new Symbol();
        symbol.setId(1L);
        symbol.setName("人民币");
        symbolRepository.save(symbol);

        Map<Long, BigDecimal> initialPrices = new LinkedHashMap<>();
        initialPrices.put(1L, new BigDecimal("100.00"));
        initialPrices.put(2L, new BigDecimal("200.00"));

        commodityRepository.save(commodity(1L, "DEMO-A", "示例商品 A",
                new BigDecimal("0.01"), new BigDecimal("1"), new BigDecimal("100.00")));
        commodityRepository.save(commodity(2L, "DEMO-B", "示例商品 B",
                new BigDecimal("0.05"), new BigDecimal("1"), new BigDecimal("200.00")));

        for (Long commodityId : initialPrices.keySet()) {
            CommodityStatus status = new CommodityStatus();
            status.setCommodityId(commodityId);
            status.setMarketStatus(2);
            status.setTradeStatus(1);
            commodityStatusRepository.save(status);

            TradeParam param = new TradeParam();
            param.setCommodityId(commodityId);
            param.setFeeRate(new BigDecimal("0.000500"));
            param.setMarginRate(BigDecimal.ZERO);
            param.setMaxOrderQty(new BigDecimal("100000"));
            param.setPriceLimitPct(new BigDecimal("0.1000"));
            tradeParamRepository.save(param);
        }

        SystemStatus systemStatus = new SystemStatus();
        systemStatus.setId(1L);
        systemStatus.setStatus("T");
        systemStatus.setTradeDay(TRADE_DAY);
        systemStatusRepository.save(systemStatus);

        for (String day : List.of("20260101", "20260102", "20260103")) {
            TradeDay tradeDay = new TradeDay();
            tradeDay.setTradeDay(day);
            tradeDay.setIsOpen(1);
            tradeDayRepository.save(tradeDay);
        }

        List<DemoProperties.AccountSeed> accounts = resolvedAccounts();
        for (DemoProperties.AccountSeed seed : accounts) {
            Firm firm = new Firm();
            firm.setId(seed.getId());
            firm.setCode(seed.getCode());
            firm.setName(seed.getName());
            firm.setStatus(0);
            firmRepository.save(firm);
        }

        seedAccountsAndQuotations(accounts, initialPrices);
        log.info("Demo data seeded: {} firms, {} commodities", accounts.size(), initialPrices.size());
    }

    @Transactional
    public void resetTradingData() {
        fundsFlowRepository.deleteAllInBatch();
        holdDetailRepository.deleteAllInBatch();
        tradeRecordRepository.deleteAllInBatch();
        orderRepository.deleteAllInBatch();
        positionRepository.deleteAllInBatch();
        fundsRepository.deleteAllInBatch();

        Map<Long, BigDecimal> initialPrices = new LinkedHashMap<>();
        initialPrices.put(1L, new BigDecimal("100.00"));
        initialPrices.put(2L, new BigDecimal("200.00"));
        seedAccountsAndQuotations(resolvedAccounts(), initialPrices);
        log.info("Demo trading data reset");
    }

    private void seedAccountsAndQuotations(List<DemoProperties.AccountSeed> accounts,
                                           Map<Long, BigDecimal> initialPrices) {
        for (DemoProperties.AccountSeed seed : accounts) {
            FundsAccount funds = new FundsAccount();
            funds.setFirmId(seed.getId());
            funds.setSymbolId(1L);
            funds.setBalance(seed.getCash() == null ? BigDecimal.ZERO : seed.getCash());
            funds.setFrozenFunds(BigDecimal.ZERO);
            fundsRepository.save(funds);

            for (Map.Entry<Long, BigDecimal> entry : initialPrices.entrySet()) {
                Position position = new Position();
                position.setFirmId(seed.getId());
                position.setCommodityId(entry.getKey());
                position.setQuantity(seed.getPosition() == null ? BigDecimal.ZERO : seed.getPosition());
                position.setFrozenQuantity(BigDecimal.ZERO);
                position.setAvgPrice(entry.getValue());
                position.setTotalAmount(entry.getValue().multiply(
                        seed.getPosition() == null ? BigDecimal.ZERO : seed.getPosition()));
                positionRepository.save(position);
            }
        }

        for (Map.Entry<Long, BigDecimal> entry : initialPrices.entrySet()) {
            Quotation quotation = new Quotation();
            quotation.setCommodityId(entry.getKey());
            quotation.setLastPrice(entry.getValue());
            quotation.setOpenPrice(entry.getValue());
            quotation.setHighPrice(entry.getValue());
            quotation.setLowPrice(entry.getValue());
            quotation.setVolume(BigDecimal.ZERO);
            quotation.setAmount(BigDecimal.ZERO);
            quotationRepository.save(quotation);
        }
    }

    private Commodity commodity(Long id, String code, String name, BigDecimal priceTick,
                                BigDecimal qtyTick, BigDecimal price) {
        Commodity commodity = new Commodity();
        commodity.setId(id);
        commodity.setMarketId(1L);
        commodity.setCode(code);
        commodity.setName(name);
        commodity.setUnitNumeral(1);
        commodity.setMinPriceChange(priceTick);
        commodity.setMinAmountChange(qtyTick);
        commodity.setPriceLimitPct(new BigDecimal("0.1000"));
        commodity.setSymbolId(1L);
        commodity.setStatus(1);
        return commodity;
    }

    private List<DemoProperties.AccountSeed> resolvedAccounts() {
        if (properties.getAccounts() != null && !properties.getAccounts().isEmpty()) {
            return properties.getAccounts();
        }
        List<DemoProperties.AccountSeed> defaults = new ArrayList<>();
        defaults.add(account(1L, "MM001", "做市商账户", new BigDecimal("100000000.00"), new BigDecimal("1000000")));
        defaults.add(account(2L, "DEMO001", "演示用户", new BigDecimal("10000000.00"), new BigDecimal("100000")));
        defaults.add(account(3L, "DEMO002", "演示用户 B", new BigDecimal("10000000.00"), new BigDecimal("100000")));
        return defaults;
    }

    private DemoProperties.AccountSeed account(Long id, String code, String name,
                                               BigDecimal cash, BigDecimal position) {
        DemoProperties.AccountSeed seed = new DemoProperties.AccountSeed();
        seed.setId(id);
        seed.setCode(code);
        seed.setName(name);
        seed.setCash(cash);
        seed.setPosition(position);
        return seed;
    }
}
