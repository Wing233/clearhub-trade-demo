package com.clearhub.trade.service;

import com.clearhub.trade.common.BizException;
import com.clearhub.trade.domain.BankAccount;
import com.clearhub.trade.domain.FundsAccount;
import com.clearhub.trade.dto.BankRequest;
import com.clearhub.trade.dto.SettlementBatchDto;
import com.clearhub.trade.dto.SettlementDetailDto;
import com.clearhub.trade.dto.PlaceOrderRequest;
import com.clearhub.trade.repository.BankAccountRepository;
import com.clearhub.trade.repository.BankTransactionRepository;
import com.clearhub.trade.repository.FundsAccountRepository;
import com.clearhub.trade.repository.TradeRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

@SpringBootTest(properties = {
        "demo.market-maker.enabled=false",
        "demo.flow-trader.enabled=false",
        "integration.enabled=false"
})
@ActiveProfiles("h2")
class ClearingIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private MatcherService matcherService;

    @Autowired
    private DemoDataService demoDataService;

    @Autowired
    private ClearingService clearingService;

    @Autowired
    private BankService bankService;

    @Autowired
    private TradeRecordRepository tradeRecordRepository;

    @Autowired
    private FundsAccountRepository fundsRepository;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    @Autowired
    private BankTransactionRepository bankTransactionRepository;

    @BeforeEach
    void reset() {
        matcherService.stopAll();
        demoDataService.resetTradingData();
        matcherService.startAll();
        await().atMost(5, TimeUnit.SECONDS).until(matcherService::isReady);
    }

    @Test
    void clearingAggregatesTradesAndSettlesBankAccounts() {
        place(3L, "SELL", "100.00", "5");
        place(2L, "BUY", "100.00", "5");
        await().atMost(5, TimeUnit.SECONDS).until(() -> tradeRecordRepository.count() == 1);

        SettlementBatchDto batch = clearingService.run();

        assertThat(batch.getTotalTrades()).isEqualTo(1);
        assertThat(batch.getTotalAmount()).isEqualByComparingTo("500.00");
        assertThat(batch.getTotalFee()).isEqualByComparingTo("0.50");
        assertThat(batch.getDetails()).hasSize(2);

        SettlementDetailDto buyer = batch.getDetails().stream()
                .filter(item -> item.getFirmId() == 2L)
                .findFirst().orElseThrow();
        assertThat(buyer.getBuyQty()).isEqualByComparingTo("5");
        assertThat(buyer.getBuyAmount()).isEqualByComparingTo("500.00");
        assertThat(buyer.getFee()).isEqualByComparingTo("0.25");
        assertThat(buyer.getNetAmount()).isEqualByComparingTo("-500.25");

        SettlementDetailDto seller = batch.getDetails().stream()
                .filter(item -> item.getFirmId() == 3L)
                .findFirst().orElseThrow();
        assertThat(seller.getSellQty()).isEqualByComparingTo("5");
        assertThat(seller.getSellAmount()).isEqualByComparingTo("500.00");
        assertThat(seller.getNetAmount()).isEqualByComparingTo("499.75");

        assertThat(batch.getReconciliations()).hasSize(2);
        assertThat(batch.getReconciliations()).allMatch(item -> "SETTLED".equals(item.getStatus()));
        assertThat(bank(2L).getBalance()).isEqualByComparingTo(funds(2L).getBalance());
        assertThat(bank(3L).getBalance()).isEqualByComparingTo(funds(3L).getBalance());
        assertThat(tradeRecordRepository.findBySettledBatchIdIsNullOrderByIdAsc()).isEmpty();

        SettlementBatchDto empty = clearingService.run();
        assertThat(empty.getTotalTrades()).isZero();
        assertThat(empty.getDetails()).isEmpty();
        assertThat(empty.getReconciliations()).isEmpty();
    }

    @Test
    void depositAndWithdrawKeepLedgerAndBankInSync() {
        BigDecimal ledgerBefore = funds(2L).getBalance();
        BigDecimal bankBefore = bank(2L).getBalance();

        bankService.deposit(request(2L, "1000.00", "测试入金"));
        assertThat(funds(2L).getBalance()).isEqualByComparingTo(ledgerBefore.add(new BigDecimal("1000")));
        assertThat(bank(2L).getBalance()).isEqualByComparingTo(bankBefore.add(new BigDecimal("1000")));
        assertThat(bankTransactionRepository.findByFirmIdOrderByIdDesc(2L,
                org.springframework.data.domain.PageRequest.of(0, 10)))
                .anyMatch(txn -> "DEPOSIT".equals(txn.getType()));

        bankService.withdraw(request(2L, "400.00", "测试出金"));
        assertThat(funds(2L).getBalance()).isEqualByComparingTo(ledgerBefore.add(new BigDecimal("600")));
        assertThat(bank(2L).getBalance()).isEqualByComparingTo(bankBefore.add(new BigDecimal("600")));
    }

    @Test
    void withdrawMoreThanAvailableIsRejected() {
        assertThatThrownBy(() -> bankService.withdraw(request(2L, "999999999.00", "超额出金")))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(40003));
    }

    private BankRequest request(Long firmId, String amount, String remark) {
        BankRequest request = new BankRequest();
        request.setFirmId(firmId);
        request.setAmount(new BigDecimal(amount));
        request.setRemark(remark);
        return request;
    }

    private void place(Long firmId, String side, String price, String quantity) {
        PlaceOrderRequest request = new PlaceOrderRequest();
        request.setCommodityId(1L);
        request.setSide(side);
        request.setPrice(new BigDecimal(price));
        request.setQuantity(new BigDecimal(quantity));
        orderService.placeOrder(firmId, request);
    }

    private FundsAccount funds(Long firmId) {
        return fundsRepository.findByFirmIdAndSymbolId(firmId, 1L).orElseThrow();
    }

    private BankAccount bank(Long firmId) {
        return bankAccountRepository.findByFirmId(firmId).orElseThrow();
    }
}
