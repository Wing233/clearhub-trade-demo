package com.clearhub.trade.integration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "integration")
public class IntegrationProperties {

    private boolean enabled = true;
    private String adapterBaseUrl = "http://127.0.0.1:8090";
    private int timeoutMs = 5000;
}
