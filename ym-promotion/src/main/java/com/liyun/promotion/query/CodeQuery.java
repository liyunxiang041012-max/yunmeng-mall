package com.liyun.promotion.query;

import com.liyun.common.query.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "兑换码查询参数")
public class CodeQuery extends PageQuery {
    @Schema(description = "兑换码对应的优惠券id")
    @NotNull
    private Long couponId;

    @Schema(description = "兑换码状态，1：未兑换，2：已兑换")
    private Integer status;
}
