package com.liyun.user.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户完善个人资料请求
 */
@Data
@Schema(description = "完善个人资料")
public class UpdateProfileDTO {

    @Schema(description = "生日，格式 yyyy-MM-dd", example = "1995-06-15")
    private String birthday;

    @Schema(description = "性别：male / female", example = "male")
    private String gender;

    @Schema(description = "所在省份", example = "广东省")
    private String province;

    @Schema(description = "所在城市", example = "深圳市")
    private String city;
}
