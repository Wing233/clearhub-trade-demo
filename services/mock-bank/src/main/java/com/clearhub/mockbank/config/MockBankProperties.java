package com.clearhub.mockbank.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "mock.bank")
public class MockBankProperties {

    private String name = "演示银行";
    private List<AccountSeed> accounts = new ArrayList<>();

    @Getter
    @Setter
    public static class AccountSeed {
        private String accountNo;
        private Long firmId;
        private BigDecimal balance = BigDecimal.ZERO;
    }
}
