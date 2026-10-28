package com.clearhub.adapter.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class BankTxnRequest {

    @NotBlank
    private String requestId;

    private Long firmId;

    @NotBlank
    private String accountNo;

    @NotNull
    private BigDecimal amount;

    private String refNo;

    private String remark;
}
