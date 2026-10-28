package com.clearhub.trade.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TradeDto {
    private Long id;
    private String tradeNo;
    private Long commodityId;
    private String commodityCode;
    private String commodityName;
    private BigDecimal price;
    private BigDecimal quantity;
    private BigDecimal amount;
    private BigDecimal buyFee;
    private BigDecimal sellFee;
    private Long buyFirmId;
    private Long sellFirmId;
    private String side;
    private String sideLabel;
    private LocalDateTime createTime;
}
