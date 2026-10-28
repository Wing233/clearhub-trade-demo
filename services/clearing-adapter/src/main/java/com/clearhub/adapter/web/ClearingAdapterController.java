package com.clearhub.adapter.web;

import com.clearhub.adapter.dto.AdapterBankRequest;
import com.clearhub.adapter.dto.AdapterBankResponse;
import com.clearhub.adapter.dto.ClearingSettleRequest;
import com.clearhub.adapter.dto.ClearingSettleResponse;
import com.clearhub.adapter.dto.MockBankAccountDto;
import com.clearhub.adapter.service.ClearingAdapterService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/adapter")
@RequiredArgsConstructor
public class ClearingAdapterController {

    private final ClearingAdapterService clearingAdapterService;

    @GetMapping("/health")
    public Map<String, Object> health() {
        return clearingAdapterService.health();
    }

    @GetMapping("/institutions")
    public Map<String, Object> institutions() {
        return clearingAdapterService.institutions();
    }

    @GetMapping("/accounts")
    public List<MockBankAccountDto> accounts() {
        return clearingAdapterService.accounts();
    }

    @PostMapping("/bank/deposit")
    public AdapterBankResponse deposit(@Valid @RequestBody AdapterBankRequest request) {
        return clearingAdapterService.deposit(request);
    }

    @PostMapping("/bank/withdraw")
    public AdapterBankResponse withdraw(@Valid @RequestBody AdapterBankRequest request) {
        return clearingAdapterService.withdraw(request);
    }

    @PostMapping("/clearing/settle")
    public ClearingSettleResponse settle(@Valid @RequestBody ClearingSettleRequest request) {
        return clearingAdapterService.settle(request);
    }

    @PostMapping("/reset")
    public Map<String, Object> reset() {
        boolean success = clearingAdapterService.reset();
        return Map.of("success", success, "message", success ? "reset" : "mock bank reset failed");
    }

    @GetMapping("/clearing/records")
    public List<ClearingSettleResponse> records() {
        return clearingAdapterService.records();
    }
}
