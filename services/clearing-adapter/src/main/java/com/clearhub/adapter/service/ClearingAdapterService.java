package com.clearhub.adapter.service;

import com.clearhub.adapter.client.MockBankClient;
import com.clearhub.adapter.config.AdapterProperties;
import com.clearhub.adapter.dto.AdapterBankRequest;
import com.clearhub.adapter.dto.AdapterBankResponse;
import com.clearhub.adapter.dto.BankTxnRequest;
import com.clearhub.adapter.dto.BankTxnResponse;
import com.clearhub.adapter.dto.ClearingSettleRequest;
import com.clearhub.adapter.dto.ClearingSettleResponse;
import com.clearhub.adapter.dto.MockBankAccountDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class ClearingAdapterService {

    private final AdapterProperties properties;
    private final MockBankClient mockBankClient;
    private final Map<String, ClearingSettleResponse> records = new ConcurrentHashMap<>();

    public AdapterBankResponse deposit(AdapterBankRequest request) {
        BankTxnRequest bankRequest = bankRequest(request, "BANK-DEPOSIT", "适配器入金");
        BankTxnResponse response = mockBankClient.deposit(bankRequest);
        return map(response);
    }

    public AdapterBankResponse withdraw(AdapterBankRequest request) {
        BankTxnRequest bankRequest = bankRequest(request, "BANK-WITHDRAW", "适配器出金");
        BankTxnResponse response = mockBankClient.withdraw(bankRequest);
        return map(response);
    }

    public ClearingSettleResponse settle(ClearingSettleRequest request) {
        String clearingRef = "CH-" + request.getBatchNo();

        ClearingSettleResponse response = new ClearingSettleResponse();
        response.setClearingRef(clearingRef);
        response.setAcceptedAt(LocalDateTime.now());

        int success = 0;
        int failed = 0;
        List<ClearingSettleResponse.AdjustmentResult> results = new ArrayList<>();
        for (ClearingSettleRequest.Adjustment adjustment : request.getAdjustments()) {
            ClearingSettleResponse.AdjustmentResult result = new ClearingSettleResponse.AdjustmentResult();
            result.setFirmId(adjustment.getFirmId());
            result.setAccountNo(adjustment.getAccountNo());
            if (adjustment.getAmount() == null || adjustment.getAmount().signum() == 0) {
                result.setSuccess(true);
                result.setMessage("no adjustment");
                success++;
                results.add(result);
                continue;
            }
            BankTxnRequest bankRequest = new BankTxnRequest();
            bankRequest.setRequestId(clearingRef + "-" + adjustment.getAccountNo());
            bankRequest.setFirmId(adjustment.getFirmId());
            bankRequest.setAccountNo(adjustment.getAccountNo());
            bankRequest.setAmount(adjustment.getAmount());
            bankRequest.setRefNo(clearingRef);
            bankRequest.setRemark("清算批次 " + request.getBatchNo());
            BankTxnResponse bankResponse = mockBankClient.adjust(bankRequest);
            result.setSuccess(bankResponse.isSuccess());
            result.setExternalRef(bankResponse.getTxnNo());
            result.setMessage(bankResponse.getMessage());
            if (bankResponse.isSuccess()) {
                success++;
            } else {
                failed++;
            }
            results.add(result);
        }
        response.setAdjustments(results);
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
        log.info("Clearing settle {}: batch={}, status={}, success={}, failed={}",
                clearingRef, request.getBatchNo(), response.getStatus(), success, failed);
        return response;
    }

    public List<MockBankAccountDto> accounts() {
        return mockBankClient.accounts();
    }

    public List<ClearingSettleResponse> records() {
        return List.copyOf(records.values());
    }

    public Map<String, Object> health() {
        Map<String, Object> bank = mockBankClient.health();
        Map<String, Object> result = new LinkedHashMap<>();
        boolean bankUp = "UP".equalsIgnoreCase(String.valueOf(bank.get("status")));
        result.put("status", bankUp ? "UP" : "DEGRADED");
        result.put("institution", properties.getInstitution());
        result.put("mockBank", bank);
        result.put("time", LocalDateTime.now());
        return result;
    }

    public Map<String, Object> institutions() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("clearingHouse", properties.getInstitution());
        result.put("bank", "DEMO-BANK");
        result.put("protocol", "JSON/HTTP (demo)");
        return result;
    }

    public boolean reset() {
        records.clear();
        return mockBankClient.reset();
    }

    private BankTxnRequest bankRequest(AdapterBankRequest request, String refNo, String remark) {
        BankTxnRequest bankRequest = new BankTxnRequest();
        bankRequest.setRequestId(request.getRequestId());
        bankRequest.setFirmId(request.getFirmId());
        bankRequest.setAccountNo(request.getAccountNo());
        bankRequest.setAmount(request.getAmount());
        bankRequest.setRefNo(refNo);
        bankRequest.setRemark(remark);
        return bankRequest;
    }

    private AdapterBankResponse map(BankTxnResponse response) {
        if (response == null) {
            return AdapterBankResponse.fail("empty response");
        }
        if (!response.isSuccess()) {
            return AdapterBankResponse.fail(response.getMessage());
        }
        return AdapterBankResponse.ok(response.getTxnNo(), response.getBalance());
    }
}
