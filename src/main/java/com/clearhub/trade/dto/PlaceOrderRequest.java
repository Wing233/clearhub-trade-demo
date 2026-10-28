package com.clearhub.trade.dto;

import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class PlaceOrderRequest {

    @NotNull(message = "商品不能为空")
    private Long commodityId;

    @NotNull(message = "买卖方向不能为空")
    private String side;

    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.0001", message = "价格必须大于 0")
    private BigDecimal price;

    @NotNull(message = "数量不能为空")
    @DecimalMin(value = "0.0001", message = "数量必须大于 0")
    private BigDecimal quantity;
}
