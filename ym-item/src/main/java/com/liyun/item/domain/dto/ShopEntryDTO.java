package com.liyun.item.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * 商家入驻请求参数
 */
@Data
@Schema(description = "商家入驻请求参数")
public class ShopEntryDTO {

    @NotBlank(message = "店铺名称不能为空")
    @Schema(description = "店铺名称", example = "我的小店")
    private String shopName;

    @Schema(description = "店铺头像URL（如果通过上传接口上传后填入）")
    private String logo;

    @Schema(description = "店铺描述")
    private String description;
}
