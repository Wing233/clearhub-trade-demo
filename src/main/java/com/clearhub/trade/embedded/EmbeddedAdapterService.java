package com.clearhub.trade.embedded;

import com.clearhub.trade.integration.AdapterAccountDto;
import com.clearhub.trade.integration.AdapterBankRequest;
import com.clearhub.trade.integration.AdapterBankResponse;
import com.clearhub.trade.integration.AdapterSettleRequest;
import com.clearhub.trade.integration.AdapterSettleResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "integration.mode", havingValue = "embedded")
public class EmbeddedAdapterService {

    public static final String INSTITUTION = "DEMO-CLEARING";

    private final EmbeddedMockBankService mockBank;
    private final Map<String, AdapterSettleResponse> records = new ConcurrentHashMap<>();

    public AdapterBankResponse deposit(AdapterBankRequest request) {
        return mockBank.deposit(request);
    }

    public AdapterBankResponse withdraw(AdapterBankRequest request) {
        return mockBank.withdraw(request);
    }

    public AdapterSettleResponse settle(AdapterSettleRequest request) {
        String clearingRef = "CH-" + request.getBatchNo();
        AdapterSettleResponse cached = records.get(clearingRef);
        if (cached != null) {
            return cached;
        }

        AdapterSettleResponse response = new AdapterSettleResponse();
        response.setClearingRef(clearingRef);
        response.setAcceptedAt(LocalDateTime.now());

        int success = 0;
        int failed = 0;
        for (AdapterSettleRequest.Adjustment adjustment : request.getAdjustments()) {
            if (adjustment.getAmount() == null || adjustment.getAmount().signum() == 0) {
                success++;
                continue;
            }
            AdapterBankRequest bankRequest = new AdapterBankRequest(
                    clearingRef + "-" + adjustment.getAccountNo(),
                    adjustment.getFirmId(),
                    adjustment.getAccountNo(),
                    adjustment.getAmount()
            );
            AdapterBankResponse bankResponse = mockBank.adjust(bankRequest);
            if (bankResponse.isSuccess()) {
                success++;
            } else {
                failed++;
            }
        }

        if (failed == 0) {
            response.setStatus("ACCEPTED");
            response.setSuccess(true);
            response.setMessage("clearing accepted");
        } else if (success > 0) {
            response.setStatus("PARTIAL");
            response.setSuccess(false);
            response.setMessage("partial adjustments failed");
        } else {
            response.setStatus("FAILED");
            response.setSuccess(false);
            response.setMessage("all adjustments failed");
        }
        records.put(clearingRef, response);
        log.info("Embedded clearing {}: batch={}, status={}, success={}, failed={}",
                clearingRef, request.getBatchNo(), response.getStatus(), success, failed);
        return response;
    }

    public List<AdapterAccountDto> accounts() {
        return mockBank.accounts();
    }

    public Map<String, Object> health() {
        Map<String, Object> bank = new LinkedHashMap<>();
        bank.put("status", "UP");
        bank.put("accounts", mockBank.accounts().size());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "UP");
        result.put("institution", INSTITUTION);
        result.put("mockBank", bank);
        result.put("time", LocalDateTime.now());
        return result;
    }

    public boolean reset() {
        records.clear();
        mockBank.reset();
        return true;
    }
}
