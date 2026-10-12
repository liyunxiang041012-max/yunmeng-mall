package com.liyun.pay.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 更新订单状态 DTO
 */
@Data
public class UpdateOrderStatusDTO {

    @NotBlank(message = "订单号不能为空")
    private String orderId;

    @NotNull(message = "订单状态不能为空")
    private Integer status;
}
