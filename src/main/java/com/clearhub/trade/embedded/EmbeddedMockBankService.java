package com.clearhub.trade.embedded;

import com.clearhub.trade.config.DemoProperties;
import com.clearhub.trade.integration.AdapterAccountDto;
import com.clearhub.trade.integration.AdapterBankRequest;
import com.clearhub.trade.integration.AdapterBankResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
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
@ConditionalOnProperty(name = "integration.mode", havingValue = "embedded")
public class EmbeddedMockBankService {

    private static final DateTimeFormatter TXN_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final DemoProperties demoProperties;
    private final MockBankAccountRepository accountRepository;
    private final MockBankTransactionRepository transactionRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void seedOnStartup() {
        seedIfEmpty();
    }

    @Transactional
    public void seedIfEmpty() {
        if (accountRepository.count() > 0) {
            return;
        }
        seedAccounts();
        log.info("Embedded mock bank seeded with {} accounts", accountRepository.count());
    }

    @Transactional
    public void reset() {
        transactionRepository.deleteAllInBatch();
        accountRepository.deleteAllInBatch();
        seedAccounts();
        log.info("Embedded mock bank reset with {} accounts", accountRepository.count());
    }

    @Transactional
    public AdapterBankResponse deposit(AdapterBankRequest request) {
        if (request.getAmount() == null || request.getAmount().signum() <= 0) {
            return fail("amount must be positive");
        }
        return apply(request, "DEPOSIT", scale(request.getAmount()));
    }

    @Transactional
    public AdapterBankResponse withdraw(AdapterBankRequest request) {
        if (request.getAmount() == null || request.getAmount().signum() <= 0) {
            return fail("amount must be positive");
        }
        return apply(request, "WITHDRAW", scale(request.getAmount()).negate());
    }

    @Transactional
    public AdapterBankResponse adjust(AdapterBankRequest request) {
        return apply(request, "SETTLE", scale(request.getAmount()));
    }

    public List<AdapterAccountDto> accounts() {
        return accountRepository.findAllByOrderByFirmIdAsc().stream().map(account -> {
            AdapterAccountDto dto = new AdapterAccountDto();
            dto.setId(account.getId());
            dto.setFirmId(account.getFirmId());
            dto.setAccountNo(account.getAccountNo());
            dto.setBankName(account.getBankName());
            dto.setCurrency(account.getCurrency());
            dto.setBalance(account.getBalance());
            dto.setStatus(account.getStatus());
            return dto;
        }).toList();
    }

    private AdapterBankResponse apply(AdapterBankRequest request, String type, BigDecimal delta) {
        var existing = transactionRepository.findByRequestId(request.getRequestId());
        if (existing.isPresent()) {
            MockBankTransaction txn = existing.get();
            return ok(txn.getTxnNo(), txn.getBalanceAfter());
        }
        MockBankAccount account = accountRepository.findByAccountNoForUpdate(request.getAccountNo())
                .orElse(null);
        if (account == null) {
            return fail("mock bank account not found: " + request.getAccountNo());
        }
        BigDecimal newBalance = scale(account.getBalance().add(delta));
        if (newBalance.signum() < 0) {
            return fail("insufficient mock bank balance");
        }
        account.setBalance(newBalance);
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
        txn.setRefNo(request.getRequestId());
        txn.setRemark(type + " via embedded adapter");
        transactionRepository.save(txn);
        return ok(txn.getTxnNo(), account.getBalance());
    }

    private void seedAccounts() {
        List<DemoProperties.AccountSeed> seeds = demoProperties.getAccounts();
        if (seeds == null || seeds.isEmpty()) {
            seeds = List.of(
                    seed(1L, "MM001", new BigDecimal("100000000.00")),
                    seed(2L, "DEMO001", new BigDecimal("10000000.00")),
                    seed(3L, "DEMO002", new BigDecimal("10000000.00")),
                    seed(4L, "FLOW001", new BigDecimal("1000000000.00"))
            );
        }
        for (DemoProperties.AccountSeed seed : seeds) {
            MockBankAccount account = new MockBankAccount();
            account.setFirmId(seed.getId());
            account.setAccountNo("BANK-" + seed.getCode());
            account.setBankName("演示银行");
            account.setBalance(scale(seed.getCash()));
            account.setStatus(0);
            accountRepository.save(account);
        }
    }

    private DemoProperties.AccountSeed seed(Long id, String code, BigDecimal cash) {
        DemoProperties.AccountSeed seed = new DemoProperties.AccountSeed();
        seed.setId(id);
        seed.setCode(code);
        seed.setCash(cash);
        return seed;
    }

    private BigDecimal scale(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(4, RoundingMode.HALF_UP);
    }

    private AdapterBankResponse ok(String externalRef, BigDecimal balance) {
        AdapterBankResponse response = new AdapterBankResponse();
        response.setSuccess(true);
        response.setExternalRef(externalRef);
        response.setBalance(scale(balance));
        return response;
    }

    private AdapterBankResponse fail(String message) {
        AdapterBankResponse response = new AdapterBankResponse();
        response.setSuccess(false);
        response.setMessage(message);
        return response;
    }
}
