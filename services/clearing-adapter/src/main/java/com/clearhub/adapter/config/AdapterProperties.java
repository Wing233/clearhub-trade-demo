package com.clearhub.adapter.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "adapter")
public class AdapterProperties {

    private String institution = "DEMO-CLEARING";
    private String mockBankBaseUrl = "http://127.0.0.1:8091";
    private int timeoutMs = 5000;
}
