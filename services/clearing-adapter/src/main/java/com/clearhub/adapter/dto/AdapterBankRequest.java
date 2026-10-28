package com.clearhub.adapter.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class AdapterBankRequest {

    @NotBlank(message = "requestId 不能为空")
    private String requestId;

    private Long firmId;

    @NotBlank(message = "accountNo 不能为空")
    private String accountNo;

    @NotNull(message = "amount 不能为空")
    private BigDecimal amount;
}
