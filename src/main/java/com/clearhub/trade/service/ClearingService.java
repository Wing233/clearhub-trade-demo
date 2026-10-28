package com.clearhub.trade.service;

import com.clearhub.trade.common.BizException;
import com.clearhub.trade.common.ErrorCode;
import com.clearhub.trade.common.Money;
import com.clearhub.trade.domain.BankAccount;
import com.clearhub.trade.domain.Commodity;
import com.clearhub.trade.domain.Firm;
import com.clearhub.trade.domain.FundsAccount;
import com.clearhub.trade.domain.Position;
import com.clearhub.trade.domain.Reconciliation;
import com.clearhub.trade.domain.SettlementBatch;
import com.clearhub.trade.domain.SettlementDetail;
import com.clearhub.trade.domain.SystemStatus;
import com.clearhub.trade.domain.TradeRecord;
import com.clearhub.trade.dto.BankSettlementResult;
import com.clearhub.trade.dto.ClearingSummaryDto;
import com.clearhub.trade.dto.ReconciliationDto;
import com.clearhub.trade.dto.SettlementBatchDto;
import com.clearhub.trade.dto.SettlementDetailDto;
import com.clearhub.trade.repository.BankAccountRepository;
import com.clearhub.trade.repository.CommodityRepository;
import com.clearhub.trade.repository.FirmRepository;
import com.clearhub.trade.repository.FundsAccountRepository;
import com.clearhub.trade.repository.PositionRepository;
import com.clearhub.trade.repository.ReconciliationRepository;
import com.clearhub.trade.repository.SettlementBatchRepository;
import com.clearhub.trade.repository.SettlementDetailRepository;
import com.clearhub.trade.repository.SystemStatusRepository;
import com.clearhub.trade.repository.TradeRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClearingService {

    private static final DateTimeFormatter BATCH_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final TradeRecordRepository tradeRecordRepository;
    private final PositionRepository positionRepository;
    private final FundsAccountRepository fundsAccountRepository;
    private final FirmRepository firmRepository;
    private final CommodityRepository commodityRepository;
    private final SystemStatusRepository systemStatusRepository;
    private final SettlementBatchRepository batchRepository;
    private final SettlementDetailRepository detailRepository;
    private final ReconciliationRepository reconciliationRepository;
    private final BankAccountRepository bankAccountRepository;
    private final BankService bankService;

    @Transactional
    public SettlementBatchDto run() {
        List<TradeRecord> trades = tradeRecordRepository.findBySettledBatchIdIsNullOrderByIdAsc();
        SystemStatus systemStatus = systemStatusRepository.findById(1L)
                .orElseThrow(() -> new BizException(ErrorCode.MARKET_CLOSED, "系统状态未配置"));

        SettlementBatch batch = new SettlementBatch();
        batch.setBatchNo("CL" + LocalDateTime.now().format(BATCH_FORMAT)
                + String.format("%03d", ThreadLocalRandom.current().nextInt(1000)));
        batch.setTradeDay(systemStatus.getTradeDay());
        batch.setStatus("RUNNING");
        batch.setStartTime(LocalDateTime.now());
        batchRepository.save(batch);

        Map<Key, Acc> accumulated = new LinkedHashMap<>();
        for (TradeRecord trade : trades) {
            accumulated.computeIfAbsent(new Key(trade.getBuyFirmId(), trade.getCommodityId()), key -> new Acc())
                    .addBuy(trade.getQuantity(), trade.getAmount(), trade.getBuyFee());
            accumulated.computeIfAbsent(new Key(trade.getSellFirmId(), trade.getCommodityId()), key -> new Acc())
                    .addSell(trade.getQuantity(), trade.getAmount(), trade.getSellFee());
        }

        List<SettlementDetail> details = new ArrayList<>();
        for (Map.Entry<Key, Acc> entry : accumulated.entrySet()) {
            Key key = entry.getKey();
            Acc acc = entry.getValue();
            Position position = positionRepository
                    .findByFirmIdAndCommodityId(key.firmId(), key.commodityId())
                    .orElse(null);
            BigDecimal closing = position == null ? BigDecimal.ZERO : position.getQuantity();
            BigDecimal opening = Money.qty(closing.subtract(acc.buyQty).add(acc.sellQty));

            SettlementDetail detail = new SettlementDetail();
            detail.setBatchId(batch.getId());
            detail.setFirmId(key.firmId());
            detail.setCommodityId(key.commodityId());
            detail.setOpeningQty(opening);
            detail.setClosingQty(Money.qty(closing));
            detail.setBuyQty(Money.qty(acc.buyQty));
            detail.setSellQty(Money.qty(acc.sellQty));
            detail.setBuyAmount(Money.money(acc.buyAmount));
            detail.setSellAmount(Money.money(acc.sellAmount));
            detail.setFee(Money.money(acc.fee));
            detail.setNetAmount(Money.money(acc.sellAmount.subtract(acc.buyAmount).subtract(acc.fee)));
            details.add(detail);
        }
        detailRepository.saveAll(details);

        Set<Long> firmIds = details.stream()
                .map(SettlementDetail::getFirmId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<Reconciliation> reconciliations = new ArrayList<>();
        for (Long firmId : firmIds) {
            FundsAccount fundsAccount = fundsAccountRepository.findByFirmIdAndSymbolId(firmId, 1L)
                    .orElseThrow(() -> new BizException(ErrorCode.ACCOUNT_NOT_FOUND, "firmId=" + firmId));
            BankSettlementResult bankResult = bankService.settle(
                    firmId, fundsAccount.getBalance(), batch.getBatchNo());

            Reconciliation reconciliation = new Reconciliation();
            reconciliation.setBatchId(batch.getId());
            reconciliation.setFirmId(firmId);
            reconciliation.setLedgerBalance(Money.money(fundsAccount.getBalance()));
            reconciliation.setBankBalance(Money.money(bankResult.bankBefore()));
            reconciliation.setSettledAmount(Money.money(bankResult.settledAmount()));
            BigDecimal difference = Money.money(fundsAccount.getBalance().subtract(bankResult.bankBefore()));
            reconciliation.setDifference(difference);
            reconciliation.setStatus(difference.signum() == 0 ? "MATCH" : "SETTLED");
            reconciliations.add(reconciliation);
        }
        reconciliationRepository.saveAll(reconciliations);

        for (TradeRecord trade : trades) {
            trade.setSettledBatchId(batch.getId());
        }
        tradeRecordRepository.saveAll(trades);

        batch.setTotalTrades(trades.size());
        batch.setTotalAmount(Money.money(trades.stream()
                .map(TradeRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)));
        batch.setTotalFee(Money.money(trades.stream()
                .map(trade -> trade.getBuyFee().add(trade.getSellFee()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)));
        batch.setFirmCount(firmIds.size());
        batch.setStatus("COMPLETED");
        batch.setEndTime(LocalDateTime.now());
        batchRepository.save(batch);

        log.info("Clearing batch {} completed: trades={}, firms={}, amount={}",
                batch.getBatchNo(), batch.getTotalTrades(), batch.getFirmCount(), batch.getTotalAmount());
        return toDto(batch, details, reconciliations);
    }

    public List<SettlementBatchDto> listBatches(int limit) {
        return batchRepository.findAllByOrderByIdDesc(PageRequest.of(0, Math.max(1, Math.min(limit, 100))))
                .stream()
                .map(batch -> toDto(batch, List.of(), List.of()))
                .toList();
    }

    public SettlementBatchDto getBatch(Long batchId) {
        SettlementBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new BizException(ErrorCode.VALIDATION, "清算批次不存在"));
        return toDto(batch,
                detailRepository.findByBatchIdOrderByFirmIdAscCommodityIdAsc(batchId),
                reconciliationRepository.findByBatchIdOrderByFirmIdAsc(batchId));
    }

    public ClearingSummaryDto summary() {
        ClearingSummaryDto dto = new ClearingSummaryDto();
        dto.setUnsettledTrades(tradeRecordRepository.countBySettledBatchIdIsNull());
        dto.setUnsettledAmount(Money.money(tradeRecordRepository.sumUnsettledAmount()));
        batchRepository.findTopByOrderByIdDesc().ifPresent(batch -> {
            dto.setLastBatchNo(batch.getBatchNo());
            dto.setLastBatchTime(batch.getEndTime() == null ? batch.getCreateTime() : batch.getEndTime());
        });
        dto.setBankTotal(Money.money(bankAccountRepository.findAll().stream()
                .map(BankAccount::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add)));
        dto.setLedgerTotal(Money.money(fundsAccountRepository.findAll().stream()
                .map(FundsAccount::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add)));
        return dto;
    }

    private SettlementBatchDto toDto(SettlementBatch batch,
                                     List<SettlementDetail> details,
                                     List<Reconciliation> reconciliations) {
        Map<Long, Firm> firms = firmRepository.findAll().stream()
                .collect(Collectors.toMap(Firm::getId, Function.identity()));
        Map<Long, Commodity> commodities = commodityRepository.findAll().stream()
                .collect(Collectors.toMap(Commodity::getId, Function.identity()));

        SettlementBatchDto dto = new SettlementBatchDto();
        dto.setId(batch.getId());
        dto.setBatchNo(batch.getBatchNo());
        dto.setTradeDay(batch.getTradeDay());
        dto.setStatus(batch.getStatus());
        dto.setTotalTrades(batch.getTotalTrades());
        dto.setTotalAmount(batch.getTotalAmount());
        dto.setTotalFee(batch.getTotalFee());
        dto.setFirmCount(batch.getFirmCount());
        dto.setStartTime(batch.getStartTime());
        dto.setEndTime(batch.getEndTime());
        dto.setCreateTime(batch.getCreateTime());

        dto.setDetails(details.stream().map(detail -> {
            Firm firm = firms.get(detail.getFirmId());
            Commodity commodity = commodities.get(detail.getCommodityId());
            SettlementDetailDto item = new SettlementDetailDto();
            item.setFirmId(detail.getFirmId());
            item.setFirmCode(firm == null ? "" : firm.getCode());
            item.setFirmName(firm == null ? "" : firm.getName());
            item.setCommodityId(detail.getCommodityId());
            item.setCommodityCode(commodity == null ? "" : commodity.getCode());
            item.setCommodityName(commodity == null ? "" : commodity.getName());
            item.setOpeningQty(detail.getOpeningQty());
            item.setClosingQty(detail.getClosingQty());
            item.setBuyQty(detail.getBuyQty());
            item.setSellQty(detail.getSellQty());
            item.setBuyAmount(detail.getBuyAmount());
            item.setSellAmount(detail.getSellAmount());
            item.setFee(detail.getFee());
            item.setNetAmount(detail.getNetAmount());
            return item;
        }).toList());

        dto.setReconciliations(reconciliations.stream().map(reconciliation -> {
            Firm firm = firms.get(reconciliation.getFirmId());
            ReconciliationDto item = new ReconciliationDto();
            item.setFirmId(reconciliation.getFirmId());
            item.setFirmCode(firm == null ? "" : firm.getCode());
            item.setFirmName(firm == null ? "" : firm.getName());
            item.setLedgerBalance(reconciliation.getLedgerBalance());
            item.setBankBalance(reconciliation.getBankBalance());
            item.setSettledAmount(reconciliation.getSettledAmount());
            item.setDifference(reconciliation.getDifference());
            item.setStatus(reconciliation.getStatus());
            return item;
        }).toList());
        return dto;
    }

    private record Key(Long firmId, Long commodityId) {
    }

    private static final class Acc {
        private BigDecimal buyQty = BigDecimal.ZERO;
        private BigDecimal sellQty = BigDecimal.ZERO;
        private BigDecimal buyAmount = BigDecimal.ZERO;
        private BigDecimal sellAmount = BigDecimal.ZERO;
        private BigDecimal fee = BigDecimal.ZERO;

        private void addBuy(BigDecimal quantity, BigDecimal amount, BigDecimal buyFee) {
            buyQty = buyQty.add(quantity);
            buyAmount = buyAmount.add(amount);
            fee = fee.add(buyFee);
        }

        private void addSell(BigDecimal quantity, BigDecimal amount, BigDecimal sellFee) {
            sellQty = sellQty.add(quantity);
            sellAmount = sellAmount.add(amount);
            fee = fee.add(sellFee);
        }
    }
}
