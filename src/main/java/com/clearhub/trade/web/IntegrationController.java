package com.clearhub.trade.web;

import com.clearhub.trade.common.ApiResponse;
import com.clearhub.trade.integration.AdapterAccountDto;
import com.clearhub.trade.integration.AdapterClient;
import com.clearhub.trade.integration.IntegrationProperties;
import com.clearhub.trade.integration.IntegrationStatusDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/integration")
@RequiredArgsConstructor
public class IntegrationController {

    private final AdapterClient adapterClient;
    private final IntegrationProperties integrationProperties;

    @GetMapping("/status")
    public ApiResponse<IntegrationStatusDto> status() {
        IntegrationStatusDto dto = new IntegrationStatusDto();
        dto.setEnabled(integrationProperties.isEnabled());
        if (!integrationProperties.isEnabled()) {
            dto.setStatus("DISABLED");
            dto.setMessage("外部通道未启用");
            return ApiResponse.ok(dto);
        }
        Map<String, Object> health = adapterClient.health();
        dto.setStatus(String.valueOf(health.getOrDefault("status", "DOWN")));
        dto.setInstitution(String.valueOf(health.getOrDefault("institution", "DEMO-CLEARING")));
        Object mockBank = health.get("mockBank");
        if (mockBank instanceof Map<?, ?> map) {
            Object status = map.get("status");
            dto.setMockBankStatus(status == null ? "DOWN" : String.valueOf(status));
        } else {
            dto.setMockBankStatus("DOWN");
        }
        if (health.containsKey("message")) {
            dto.setMessage(String.valueOf(health.get("message")));
        }
        dto.setAccounts(adapterClient.accounts());
        return ApiResponse.ok(dto);
    }

    @GetMapping("/accounts")
    public ApiResponse<List<AdapterAccountDto>> accounts() {
        return ApiResponse.ok(adapterClient.accounts());
    }
}
