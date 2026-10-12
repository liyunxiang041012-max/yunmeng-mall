package com.liyun.user.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "地址信息")
public class AddressDTO {



    @Schema(description = "收货人")
    private String receiver;
    @Schema(description = "手机号")
    private String phone;
    @Schema(description = "地址名称")
    private String address;
     @Schema(description = "是否默认地址")
    private Integer isDefault;

}
