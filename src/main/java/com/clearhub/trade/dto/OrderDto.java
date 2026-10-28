package com.clearhub.trade.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderDto {
    private Long id;
    private String orderNo;
    private Long firmId;
    private Long commodityId;
    private String commodityCode;
    private String commodityName;
    private String side;
    private String sideLabel;
    private BigDecimal price;
    private BigDecimal quantity;
    private BigDecimal filledQuantity;
    private BigDecimal remainingQuantity;
    private Integer status;
    private String statusLabel;
    private BigDecimal frozenFunds;
    private BigDecimal frozenQuantity;
    private BigDecimal fee;
    private LocalDateTime createTime;
}
