package com.clearhub.adapter.client;

import com.clearhub.adapter.config.AdapterProperties;
import com.clearhub.adapter.dto.BankTxnRequest;
import com.clearhub.adapter.dto.BankTxnResponse;
import com.clearhub.adapter.dto.MockBankAccountDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class MockBankClient {

    private final RestTemplate restTemplate;
    private final AdapterProperties properties;

    public BankTxnResponse deposit(BankTxnRequest request) {
        return post("/mock-bank/deposit", request);
    }

    public BankTxnResponse withdraw(BankTxnRequest request) {
        return post("/mock-bank/withdraw", request);
    }

    public BankTxnResponse adjust(BankTxnRequest request) {
        return post("/mock-bank/adjust", request);
    }

    public Map<String, Object> health() {
        try {
            return restTemplate.getForObject(url("/mock-bank/health"), Map.class);
        } catch (RestClientException ex) {
            log.warn("Mock bank health check failed: {}", ex.getMessage());
            return Map.of("status", "DOWN", "message", ex.getMessage());
        }
    }

    public List<MockBankAccountDto> accounts() {
        try {
            ResponseEntity<List<MockBankAccountDto>> response = restTemplate.exchange(
                    url("/mock-bank/accounts"),
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {
                    });
            return response.getBody() == null ? List.of() : response.getBody();
        } catch (RestClientException ex) {
            log.warn("Mock bank accounts query failed: {}", ex.getMessage());
            return List.of();
        }
    }

    public boolean reset() {
        try {
            restTemplate.postForObject(url("/mock-bank/reset"), null, Map.class);
            return true;
        } catch (RestClientException ex) {
            log.warn("Mock bank reset failed: {}", ex.getMessage());
            return false;
        }
    }

    private BankTxnResponse post(String path, BankTxnRequest request) {
        try {
            BankTxnResponse response = restTemplate.postForObject(url(path), request, BankTxnResponse.class);
            return response == null ? failure("empty response from mock bank") : response;
        } catch (RestClientException ex) {
            log.warn("Mock bank call {} failed: {}", path, ex.getMessage());
            return failure(ex.getMessage());
        }
    }

    private BankTxnResponse failure(String message) {
        BankTxnResponse response = new BankTxnResponse();
        response.setSuccess(false);
        response.setMessage(message);
        return response;
    }

    private String url(String path) {
        return properties.getMockBankBaseUrl().replaceAll("/+$", "") + path;
    }
}
