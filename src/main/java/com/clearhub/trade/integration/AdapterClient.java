package com.clearhub.trade.integration;

import com.clearhub.trade.embedded.EmbeddedAdapterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
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
public class AdapterClient {

    private final RestTemplate integrationRestTemplate;
    private final IntegrationProperties properties;
    private final ObjectProvider<EmbeddedAdapterService> embeddedAdapterProvider;

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    public AdapterBankResponse deposit(AdapterBankRequest request) {
        if (isEmbedded()) {
            return embedded().deposit(request);
        }
        return post("/adapter/bank/deposit", request, AdapterBankResponse.class, "deposit");
    }

    public AdapterBankResponse withdraw(AdapterBankRequest request) {
        if (isEmbedded()) {
            return embedded().withdraw(request);
        }
        return post("/adapter/bank/withdraw", request, AdapterBankResponse.class, "withdraw");
    }

    public AdapterSettleResponse settle(AdapterSettleRequest request) {
        if (isEmbedded()) {
            return embedded().settle(request);
        }
        try {
            AdapterSettleResponse response = integrationRestTemplate.postForObject(
                    url("/adapter/clearing/settle"), request, AdapterSettleResponse.class);
            if (response == null) {
                return failureSettle("empty response from adapter");
            }
            return response;
        } catch (RestClientException ex) {
            log.warn("Adapter clearing settle failed: {}", ex.getMessage());
            return failureSettle(ex.getMessage());
        }
    }

    public Map<String, Object> health() {
        if (isEmbedded()) {
            return embedded().health();
        }
        try {
            return integrationRestTemplate.getForObject(url("/adapter/health"), Map.class);
        } catch (RestClientException ex) {
            log.warn("Adapter health check failed: {}", ex.getMessage());
            return Map.of("status", "DOWN", "message", ex.getMessage());
        }
    }

    public List<AdapterAccountDto> accounts() {
        if (isEmbedded()) {
            return embedded().accounts();
        }
        try {
            ResponseEntity<List<AdapterAccountDto>> response = integrationRestTemplate.exchange(
                    url("/adapter/accounts"),
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {
                    });
            return response.getBody() == null ? List.of() : response.getBody();
        } catch (RestClientException ex) {
            log.warn("Adapter accounts query failed: {}", ex.getMessage());
            return List.of();
        }
    }

    public boolean reset() {
        if (isEmbedded()) {
            return embedded().reset();
        }
        try {
            integrationRestTemplate.postForObject(url("/adapter/reset"), null, Map.class);
            return true;
        } catch (RestClientException ex) {
            log.warn("Adapter reset failed: {}", ex.getMessage());
            return false;
        }
    }

    private boolean isEmbedded() {
        return "embedded".equalsIgnoreCase(properties.getMode());
    }

    private EmbeddedAdapterService embedded() {
        EmbeddedAdapterService adapter = embeddedAdapterProvider.getIfAvailable();
        if (adapter == null) {
            throw new IllegalStateException("integration.mode=embedded but embedded adapter is not available");
        }
        return adapter;
    }

    private <T> T post(String path, Object body, Class<T> type, String action) {
        try {
            T response = integrationRestTemplate.postForObject(url(path), body, type);
            if (response == null) {
                throw new IllegalStateException("empty response");
            }
            return response;
        } catch (RestClientException | IllegalStateException ex) {
            log.warn("Adapter {} failed: {}", action, ex.getMessage());
            throw new IllegalStateException("适配器调用失败：" + ex.getMessage(), ex);
        }
    }

    private AdapterSettleResponse failureSettle(String message) {
        AdapterSettleResponse response = new AdapterSettleResponse();
        response.setSuccess(false);
        response.setStatus("FAILED");
        response.setMessage(message);
        return response;
    }

    private String url(String path) {
        return properties.getAdapterBaseUrl().replaceAll("/+$", "") + path;
    }
}
