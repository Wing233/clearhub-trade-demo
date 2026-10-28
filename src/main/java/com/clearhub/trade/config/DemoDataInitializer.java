package com.clearhub.trade.config;

import com.clearhub.trade.service.DemoDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class DemoDataInitializer implements ApplicationRunner {

    private final DemoProperties properties;
    private final DemoDataService demoDataService;

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isSeedOnStart()) {
            log.info("Demo seeding disabled");
            return;
        }
        demoDataService.seedIfEmpty();
    }
}
