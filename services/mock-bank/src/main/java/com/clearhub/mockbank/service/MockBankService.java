package com.clearhub.mockbank.service;

import com.clearhub.mockbank.config.MockBankProperties;
import com.clearhub.mockbank.domain.MockBankAccount;
import com.clearhub.mockbank.domain.MockBankTransaction;
import com.clearhub.mockbank.dto.MockBankAccountDto;
import com.clearhub.mockbank.dto.MockBankRequest;
import com.clearhub.mockbank.dto.MockBankResponse;
import com.clearhub.mockbank.dto.MockBankTransactionDto;
import com.clearhub.mockbank.repository.MockBankAccountRepository;
import com.clearhub.mockbank.repository.MockBankTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class MockBankService implements ApplicationRunner {

    private static final DateTimeFormatter TXN_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final MockBankProperties properties;
    private final MockBankAccountRepository accountRepository;
    private final MockBankTransactionRepository transactionRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (accountRepository.count() == 0) {
            properties.getAccounts().forEach(seed -> {
                MockBankAccount account = new MockBankAccount();
                account.setFirmId(seed.getFirmId());
                account.setAccountNo(seed.getAccountNo());
                account.setBankName(properties.getName());
                account.setBalance(scale(seed.getBalance()));
                accountRepository.save(account);
            });
            log.info("Mock bank seeded with {} accounts", properties.getAccounts().size());
        }
    }

    @Transactional
    public MockBankResponse deposit(MockBankRequest request) {
        return apply(request, "DEPOSIT", positive(request.getAmount()));
    }

    @Transactional
    public MockBankResponse withdraw(MockBankRequest request) {
        return apply(request, "WITHDRAW", positive(request.getAmount()).negate());
    }

    @Transactional
    public MockBankResponse adjust(MockBankRequest request) {
        return apply(request, "SETTLE", scale(request.getAmount()));
    }

    @Transactional
    public void reset() {
        transactionRepository.deleteAllInBatch();
        accountRepository.deleteAllInBatch();
        properties.getAccounts().forEach(seed -> {
            MockBankAccount account = new MockBankAccount();
            account.setFirmId(seed.getFirmId());
            account.setAccountNo(seed.getAccountNo());
            account.setBankName(properties.getName());
            account.setBalance(scale(seed.getBalance()));
            accountRepository.save(account);
        });
        log.info("Mock bank reset");
    }

    public List<MockBankAccountDto> accounts() {
        return accountRepository.findAllByOrderByFirmIdAsc().stream().map(this::toAccountDto).toList();
    }

    public MockBankAccountDto account(String accountNo) {
        return accountRepository.findByAccountNo(accountNo)
                .map(this::toAccountDto)
                .orElseThrow(() -> new IllegalArgumentException("account not found: " + accountNo));
    }

    public List<MockBankTransactionDto> transactions(String accountNo, int limit) {
        PageRequest page = PageRequest.of(0, Math.max(1, Math.min(limit, 200)));
        List<MockBankTransaction> list = accountNo == null || accountNo.isBlank()
                ? transactionRepository.findAllByOrderByIdDesc(page)
                : transactionRepository.findByAccountNoOrderByIdDesc(accountNo, page);
        return list.stream().map(this::toTransactionDto).toList();
    }

    private MockBankResponse apply(MockBankRequest request, String type, BigDecimal delta) {
        var existing = transactionRepository.findByRequestId(request.getRequestId());
        if (existing.isPresent()) {
            MockBankTransaction txn = existing.get();
            return MockBankResponse.ok(txn.getTxnNo(), txn.getAccountNo(), txn.getBalanceAfter());
        }
        MockBankAccount account = accountRepository.findByAccountNoForUpdate(request.getAccountNo())
                .orElseThrow(() -> new IllegalArgumentException("account not found: " + request.getAccountNo()));
        BigDecimal newBalance = account.getBalance().add(delta);
        if (newBalance.signum() < 0) {
            throw new IllegalStateException("insufficient mock bank balance");
        }
        account.setBalance(scale(newBalance));
        accountRepository.save(account);

        MockBankTransaction txn = new MockBankTransaction();
        txn.setTxnNo("MB" + LocalDateTime.now().format(TXN_FORMAT)
                + String.format("%03d", ThreadLocalRandom.current().nextInt(1000)));
        txn.setRequestId(request.getRequestId());
        txn.setFirmId(request.getFirmId() == null ? account.getFirmId() : request.getFirmId());
        txn.setAccountNo(account.getAccountNo());
        txn.setType(type);
        txn.setAmount(scale(delta));
        txn.setBalanceAfter(account.getBalance());
        txn.setRefNo(request.getRefNo());
        txn.setRemark(request.getRemark());
        transactionRepository.save(txn);
        return MockBankResponse.ok(txn.getTxnNo(), account.getAccountNo(), account.getBalance());
    }

    private BigDecimal positive(BigDecimal amount) {
        BigDecimal value = scale(amount);
        if (value.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        return value;
    }

    private BigDecimal scale(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(4, RoundingMode.HALF_UP);
    }

    private MockBankAccountDto toAccountDto(MockBankAccount account) {
        MockBankAccountDto dto = new MockBankAccountDto();
        dto.setId(account.getId());
        dto.setFirmId(account.getFirmId());
        dto.setAccountNo(account.getAccountNo());
        dto.setBankName(account.getBankName());
        dto.setCurrency(account.getCurrency());
        dto.setBalance(account.getBalance());
        dto.setStatus(account.getStatus());
        return dto;
    }

    private MockBankTransactionDto toTransactionDto(MockBankTransaction txn) {
        MockBankTransactionDto dto = new MockBankTransactionDto();
        dto.setId(txn.getId());
        dto.setTxnNo(txn.getTxnNo());
        dto.setRequestId(txn.getRequestId());
        dto.setFirmId(txn.getFirmId());
        dto.setAccountNo(txn.getAccountNo());
        dto.setType(txn.getType());
        dto.setAmount(txn.getAmount());
        dto.setBalanceAfter(txn.getBalanceAfter());
        dto.setRefNo(txn.getRefNo());
        dto.setRemark(txn.getRemark());
        dto.setCreateTime(txn.getCreateTime());
        return dto;
    }
}
