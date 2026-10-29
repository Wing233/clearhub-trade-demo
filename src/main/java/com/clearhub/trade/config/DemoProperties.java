package com.clearhub.trade.config;

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
@ConfigurationProperties(prefix = "demo")
public class DemoProperties {

    private boolean seedOnStart = true;
    private boolean resetEnabled = true;
    private MarketMaker marketMaker = new MarketMaker();
    private FlowTrader flowTrader = new FlowTrader();
    private List<AccountSeed> accounts = new ArrayList<>();

    @Getter
    @Setter
    public static class MarketMaker {
        private boolean enabled = true;
        private Long firmId = 1L;
        private long intervalMs = 3000;
        private int spreadTicks = 2;
        private int levels = 3;
        private int minQuantity = 10;
        private int maxQuantity = 60;
        private double maxPriceDrift = 0.004;
    }

    @Getter
    @Setter
    public static class FlowTrader {
        private boolean enabled = true;
        private Long firmId = 4L;
        private long intervalMs = 2500;
        private int minQuantity = 5;
        private int maxQuantity = 30;
    }

    @Getter
    @Setter
    public static class AccountSeed {
        private Long id;
        private String code;
        private String name;
        private BigDecimal cash = BigDecimal.ZERO;
        private BigDecimal position = BigDecimal.ZERO;
    }
}
