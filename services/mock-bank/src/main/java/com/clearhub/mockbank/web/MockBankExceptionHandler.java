package com.clearhub.mockbank.web;

import com.clearhub.mockbank.dto.MockBankResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class MockBankExceptionHandler {

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public MockBankResponse handleBusiness(RuntimeException ex) {
        log.warn("Mock bank request rejected: {}", ex.getMessage());
        return MockBankResponse.fail(ex.getMessage());
    }
}
