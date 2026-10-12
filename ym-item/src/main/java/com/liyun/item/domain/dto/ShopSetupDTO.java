package com.liyun.item.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "店铺设置请求参数")
public class ShopSetupDTO {

    @NotBlank(message = "店铺名称不能为空")
    @Schema(description = "店铺名称", example = "我的小店")
    private String shopName;

    @Schema(description = "店铺logo URL")
    private String logo;

    @Size(max = 512, message = "店铺描述不能超过512字")
    @Schema(description = "店铺描述")
    private String description;
}
