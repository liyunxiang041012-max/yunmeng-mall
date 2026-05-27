package com.liyun.pay.domain.query;

import feign.template.Literal;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class OrderQuery {

    @NotBlank(message = "订单id不能为空")
    private List<String>  orderIds;
}
