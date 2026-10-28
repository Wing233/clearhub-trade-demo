package com.clearhub.trade.service;

import com.clearhub.trade.common.BizException;
import com.clearhub.trade.common.ErrorCode;
import com.clearhub.trade.common.Money;
import com.clearhub.trade.domain.BankAccount;
import com.clearhub.trade.domain.BankTransaction;
import com.clearhub.trade.domain.Firm;
import com.clearhub.trade.dto.BankAccountDto;
import com.clearhub.trade.dto.BankRequest;
import com.clearhub.trade.dto.BankSettlementResult;
import com.clearhub.trade.dto.BankTransactionDto;
import com.clearhub.trade.repository.BankAccountRepository;
import com.clearhub.trade.repository.BankTransactionRepository;
import com.clearhub.trade.repository.FirmRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BankService {

    private static final DateTimeFormatter TXN_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final BankAccountRepository bankAccountRepository;
    private final BankTransactionRepository bankTransactionRepository;
    private final FirmRepository firmRepository;
    private final MarginLedger marginLedger;

    @Transactional
    public BankTransaction deposit(BankRequest request) {
        BankAccount account = lockAccount(request.getFirmId());
        marginLedger.credit(request.getFirmId(), Money.money(request.getAmount()),
                "BANK_DEPOSIT", "BANK", account.getId(), request.getRemark());
        account.setBalance(Money.money(account.getBalance().add(request.getAmount())));
        bankAccountRepository.save(account);
        return record(account, "DEPOSIT", Money.money(request.getAmount()), request.getRemark());
    }

    @Transactional
    public BankTransaction withdraw(BankRequest request) {
        BankAccount account = lockAccount(request.getFirmId());
        BigDecimal amount = Money.money(request.getAmount());
        marginLedger.debit(request.getFirmId(), amount,
                "BANK_WITHDRAW", "BANK", account.getId(), request.getRemark());
        if (account.getBalance().compareTo(amount) < 0) {
            throw new BizException(ErrorCode.INSUFFICIENT_FUNDS, "银行账户余额不足");
        }
        account.setBalance(Money.money(account.getBalance().subtract(amount)));
        bankAccountRepository.save(account);
        return record(account, "WITHDRAW", amount.negate(), request.getRemark());
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public BankSettlementResult settle(Long firmId, BigDecimal ledgerBalance, String batchNo) {
        BankAccount account = lockAccount(firmId);
        BigDecimal before = account.getBalance();
        BigDecimal settled = Money.money(ledgerBalance.subtract(before));
        account.setBalance(Money.money(ledgerBalance));
        bankAccountRepository.save(account);
        if (settled.signum() != 0) {
            record(account, "SETTLE", settled, "清算批次 " + batchNo);
        }
        return new BankSettlementResult(before, settled, account.getBalance());
    }

    public List<BankAccountDto> listAccounts() {
        Map<Long, Firm> firms = firmRepository.findAll().stream()
                .collect(Collectors.toMap(Firm::getId, Function.identity()));
        return bankAccountRepository.findAllByOrderByFirmIdAsc().stream()
                .map(account -> {
                    Firm firm = firms.get(account.getFirmId());
                    BankAccountDto dto = new BankAccountDto();
                    dto.setId(account.getId());
                    dto.setFirmId(account.getFirmId());
                    dto.setFirmCode(firm == null ? "" : firm.getCode());
                    dto.setFirmName(firm == null ? "" : firm.getName());
                    dto.setAccountNo(account.getAccountNo());
                    dto.setBankName(account.getBankName());
                    dto.setBalance(account.getBalance());
                    dto.setStatus(account.getStatus());
                    return dto;
                })
                .toList();
    }

    public List<BankTransactionDto> listTransactions(Long firmId, int limit) {
        Map<Long, Firm> firms = firmRepository.findAll().stream()
                .collect(Collectors.toMap(Firm::getId, Function.identity()));
        PageRequest page = PageRequest.of(0, Math.max(1, Math.min(limit, 200)));
        List<BankTransaction> transactions = firmId == null
                ? bankTransactionRepository.findAllByOrderByIdDesc(page)
                : bankTransactionRepository.findByFirmIdOrderByIdDesc(firmId, page);
        return transactions.stream()
                .map(txn -> {
                    Firm firm = firms.get(txn.getFirmId());
                    BankTransactionDto dto = new BankTransactionDto();
                    dto.setId(txn.getId());
                    dto.setTxnNo(txn.getTxnNo());
                    dto.setFirmId(txn.getFirmId());
                    dto.setFirmName(firm == null ? "" : firm.getName());
                    dto.setType(txn.getType());
                    dto.setTypeLabel(typeLabel(txn.getType()));
                    dto.setAmount(txn.getAmount());
                    dto.setBalanceAfter(txn.getBalanceAfter());
                    dto.setRemark(txn.getRemark());
                    dto.setCreateTime(txn.getCreateTime());
                    return dto;
                })
                .toList();
    }

    private BankAccount lockAccount(Long firmId) {
        firmRepository.findById(firmId)
                .orElseThrow(() -> new BizException(ErrorCode.ACCOUNT_NOT_FOUND, "firmId=" + firmId));
        return bankAccountRepository.findByFirmIdForUpdate(firmId)
                .orElseThrow(() -> new BizException(ErrorCode.ACCOUNT_NOT_FOUND, "银行账户不存在"));
    }

    private BankTransaction record(BankAccount account, String type, BigDecimal amount, String remark) {
        BankTransaction txn = new BankTransaction();
        txn.setTxnNo("B" + LocalDateTime.now().format(TXN_FORMAT)
                + String.format("%03d", ThreadLocalRandom.current().nextInt(1000)));
        txn.setFirmId(account.getFirmId());
        txn.setAccountId(account.getId());
        txn.setType(type);
        txn.setAmount(Money.money(amount));
        txn.setBalanceAfter(Money.money(account.getBalance()));
        txn.setRemark(remark);
        return bankTransactionRepository.save(txn);
    }

    private String typeLabel(String type) {
        return switch (type) {
            case "DEPOSIT" -> "入金";
            case "WITHDRAW" -> "出金";
            case "SETTLE" -> "清算结算";
            default -> type;
        };
    }
}
