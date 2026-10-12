package com.liyun.pay.domain.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CartDTO {

    @NotNull(message = "商品ID不能为空")
    private Long skuId;        // 商品 SKU ID

    @Min(value = 1, message = "数量至少为1")
    private Integer quantity;  // 购买数量

}
