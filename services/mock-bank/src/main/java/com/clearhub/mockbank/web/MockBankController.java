package com.clearhub.mockbank.web;

import com.clearhub.mockbank.config.MockBankProperties;
import com.clearhub.mockbank.dto.MockBankAccountDto;
import com.clearhub.mockbank.dto.MockBankRequest;
import com.clearhub.mockbank.dto.MockBankResponse;
import com.clearhub.mockbank.dto.MockBankTransactionDto;
import com.clearhub.mockbank.service.MockBankService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/mock-bank")
@RequiredArgsConstructor
public class MockBankController {

    private final MockBankService mockBankService;
    private final MockBankProperties properties;

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "UP");
        result.put("bank", properties.getName());
        result.put("time", LocalDateTime.now());
        return result;
    }

    @GetMapping("/accounts")
    public List<MockBankAccountDto> accounts() {
        return mockBankService.accounts();
    }

    @GetMapping("/accounts/{accountNo}")
    public MockBankAccountDto account(@PathVariable String accountNo) {
        return mockBankService.account(accountNo);
    }

    @PostMapping("/deposit")
    public MockBankResponse deposit(@Valid @RequestBody MockBankRequest request) {
        return mockBankService.deposit(request);
    }

    @PostMapping("/withdraw")
    public MockBankResponse withdraw(@Valid @RequestBody MockBankRequest request) {
        return mockBankService.withdraw(request);
    }

    @PostMapping("/adjust")
    public MockBankResponse adjust(@Valid @RequestBody MockBankRequest request) {
        return mockBankService.adjust(request);
    }

    @PostMapping("/reset")
    public Map<String, Object> reset() {
        mockBankService.reset();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("message", "reset");
        return result;
    }

    @GetMapping("/transactions")
    public List<MockBankTransactionDto> transactions(
            @RequestParam(required = false) String accountNo,
            @RequestParam(defaultValue = "100") int limit) {
        return mockBankService.transactions(accountNo, limit);
    }
}
