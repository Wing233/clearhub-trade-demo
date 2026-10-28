package com.clearhub.trade.web;

import com.clearhub.trade.common.ApiResponse;
import com.clearhub.trade.domain.BankTransaction;
import com.clearhub.trade.dto.BankAccountDto;
import com.clearhub.trade.dto.BankRequest;
import com.clearhub.trade.dto.BankTransactionDto;
import com.clearhub.trade.service.BankService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/bank")
@RequiredArgsConstructor
public class BankController {

    private final BankService bankService;

    @GetMapping("/accounts")
    public ApiResponse<List<BankAccountDto>> accounts() {
        return ApiResponse.ok(bankService.listAccounts());
    }

    @GetMapping("/transactions")
    public ApiResponse<List<BankTransactionDto>> transactions(
            @RequestParam(required = false) Long firmId,
            @RequestParam(defaultValue = "100") int limit) {
        return ApiResponse.ok(bankService.listTransactions(firmId, limit));
    }

    @PostMapping("/deposit")
    public ApiResponse<String> deposit(@Valid @RequestBody BankRequest request) {
        BankTransaction txn = bankService.deposit(request);
        return ApiResponse.ok(txn.getTxnNo());
    }

    @PostMapping("/withdraw")
    public ApiResponse<String> withdraw(@Valid @RequestBody BankRequest request) {
        BankTransaction txn = bankService.withdraw(request);
        return ApiResponse.ok(txn.getTxnNo());
    }
}
