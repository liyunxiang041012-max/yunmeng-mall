package com.liyun.item.domain.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;

@Data
public class ShopDTO {


    @Schema(description = "logo")
    private String logo;
    @Size(max = 512)
    private String description;
    @Schema(description = "nickname", example = "l")
    private String shopName;
    @Schema(description = "手机号", example = "13800138000")
    private String phone;
    @Schema(description = "验证码", example = "123456")
    private String code;
    @Schema(description = "密码", example = "123456qq")
    private String password;
    @Schema(description = "昵称", example = "张三")
    private String nickname;


}
