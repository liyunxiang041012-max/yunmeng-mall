package com.liyun.user.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "注册请求参数")
public class RegisterDTO {
    @Schema(description = "手机号", example = "13800138000")
    private String phone;
    @Schema(description = "验证码", example = "123456")
    private String code;
    @Schema(description = "密码", example = "123456qq")
    private String password;
    @Schema(description = "昵称", example = "张三")
    private String nickname;

}
