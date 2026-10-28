package com.clearhub.trade.integration;

import lombok.Data;

import java.util.List;

@Data
public class IntegrationStatusDto {
    private boolean enabled;
    private String status;
    private String institution;
    private String mockBankStatus;
    private String message;
    private List<AdapterAccountDto> accounts;
}
