package com.clearhub.trade.service;

import com.clearhub.trade.config.DemoProperties;
import com.clearhub.trade.integration.AdapterClient;
import com.clearhub.trade.integration.IntegrationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DemoResetService {

    private final DemoProperties properties;
    private final IntegrationProperties integrationProperties;
    private final MatcherService matcherService;
    private final DemoDataService demoDataService;
    private final MarketMakerService marketMakerService;
    private final AdapterClient adapterClient;

    public synchronized void reset() {
        if (!properties.isResetEnabled()) {
            throw new IllegalStateException("demo reset is disabled");
        }
        log.info("Resetting demo data ...");
        matcherService.stopAll();
        marketMakerService.clearQuotes();
        demoDataService.resetTradingData();
        if (integrationProperties.isEnabled()) {
            boolean ok = adapterClient.reset();
            if (!ok) {
                log.warn("Mock bank reset failed; external balances may be out of sync");
            }
        }
        matcherService.startAll();
        log.info("Demo data reset complete");
    }
}
