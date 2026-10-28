package com.clearhub.trade.service;

import com.clearhub.trade.config.DemoProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DemoResetService {

    private final DemoProperties properties;
    private final MatcherService matcherService;
    private final DemoDataService demoDataService;
    private final MarketMakerService marketMakerService;

    public synchronized void reset() {
        if (!properties.isResetEnabled()) {
            throw new IllegalStateException("demo reset is disabled");
        }
        log.info("Resetting demo data ...");
        matcherService.stopAll();
        marketMakerService.clearQuotes();
        demoDataService.resetTradingData();
        matcherService.startAll();
        log.info("Demo data reset complete");
    }
}
