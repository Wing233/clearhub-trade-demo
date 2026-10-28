package com.clearhub.trade.service;

import com.clearhub.trade.common.BizException;
import com.clearhub.trade.common.ErrorCode;
import com.clearhub.trade.common.Money;
import com.clearhub.trade.domain.FundsAccount;
import com.clearhub.trade.domain.FundsFlow;
import com.clearhub.trade.repository.FundsAccountRepository;
import com.clearhub.trade.repository.FundsFlowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Minimal cash ledger: freeze, release, buy settlement and sell credit.
 * It mirrors the semantics of the original MarginLib for the demo paths only.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MarginLedger {

    private final FundsAccountRepository fundsRepository;
    private final FundsFlowRepository flowRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public FundsAccount freeze(Long firmId, BigDecimal amount, String bizCode,
                               String refType, Long refId, String note) {
        FundsAccount account = lock(firmId);
        if (account.available().compareTo(amount) < 0) {
            throw new BizException(ErrorCode.INSUFFICIENT_FUNDS,
                    "需要 " + Money.money(amount) + "，可用 " + Money.money(account.available()));
        }
        account.setFrozenFunds(account.getFrozenFunds().add(amount));
        fundsRepository.save(account);
        writeFlow(account, bizCode, BigDecimal.ZERO, amount, refType, refId, note);
        return account;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void release(Long firmId, BigDecimal amount, String bizCode,
                        String refType, Long refId, String note) {
        if (amount.signum() <= 0) {
            return;
        }
        FundsAccount account = lock(firmId);
        BigDecimal newFrozen = Money.zeroIfNegative(account.getFrozenFunds().subtract(amount));
        account.setFrozenFunds(newFrozen);
        fundsRepository.save(account);
        writeFlow(account, bizCode, BigDecimal.ZERO, amount.negate(), refType, refId, note);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void settleBuy(Long firmId, BigDecimal deduct, BigDecimal releaseExtra,
                          String refType, Long refId, String note) {
        FundsAccount account = lock(firmId);
        BigDecimal frozenReduction = deduct.add(releaseExtra);
        BigDecimal newFrozen = Money.zeroIfNegative(account.getFrozenFunds().subtract(frozenReduction));
        BigDecimal newBalance = Money.zeroIfNegative(account.getBalance().subtract(deduct));
        if (newBalance.signum() == 0 && account.getBalance().compareTo(deduct) < 0) {
            log.warn("Buyer {} balance would go negative, clamped to zero", firmId);
        }
        account.setFrozenFunds(newFrozen);
        account.setBalance(newBalance);
        fundsRepository.save(account);
        writeFlow(account, "TRADE_BUY", deduct.negate(), frozenReduction.negate(), refType, refId, note);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void creditSell(Long firmId, BigDecimal proceeds, String refType, Long refId, String note) {
        FundsAccount account = lock(firmId);
        account.setBalance(account.getBalance().add(proceeds));
        fundsRepository.save(account);
        writeFlow(account, "TRADE_SELL", proceeds, BigDecimal.ZERO, refType, refId, note);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void credit(Long firmId, BigDecimal amount, String bizCode,
                       String refType, Long refId, String note) {
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        FundsAccount account = lock(firmId);
        account.setBalance(account.getBalance().add(amount));
        fundsRepository.save(account);
        writeFlow(account, bizCode, amount, BigDecimal.ZERO, refType, refId, note);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void debit(Long firmId, BigDecimal amount, String bizCode,
                      String refType, Long refId, String note) {
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        FundsAccount account = lock(firmId);
        if (account.available().compareTo(amount) < 0) {
            throw new BizException(ErrorCode.INSUFFICIENT_FUNDS,
                    "需要 " + Money.money(amount) + "，可用 " + Money.money(account.available()));
        }
        account.setBalance(account.getBalance().subtract(amount));
        fundsRepository.save(account);
        writeFlow(account, bizCode, amount.negate(), BigDecimal.ZERO, refType, refId, note);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public BigDecimal available(Long firmId) {
        return lock(firmId).available();
    }

    private FundsAccount lock(Long firmId) {
        return fundsRepository.findForUpdate(firmId, 1L)
                .orElseThrow(() -> new BizException(ErrorCode.ACCOUNT_NOT_FOUND, "firmId=" + firmId));
    }

    private void writeFlow(FundsAccount account, String bizCode, BigDecimal amount, BigDecimal frozenAmount,
                           String refType, Long refId, String note) {
        FundsFlow flow = new FundsFlow();
        flow.setAccountId(account.getId());
        flow.setFirmId(account.getFirmId());
        flow.setBizCode(bizCode);
        flow.setAmount(Money.money(amount));
        flow.setFrozenAmount(Money.money(frozenAmount));
        flow.setBalanceAfter(Money.money(account.getBalance()));
        flow.setFrozenAfter(Money.money(account.getFrozenFunds()));
        flow.setRefType(refType);
        flow.setRefId(refId);
        flow.setNote(note);
        flowRepository.save(flow);
    }
}
