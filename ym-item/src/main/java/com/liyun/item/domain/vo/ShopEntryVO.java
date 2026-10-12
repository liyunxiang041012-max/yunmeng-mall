package com.liyun.item.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商家入驻返回结果
 */
@Data
@Schema(description = "商家入驻返回结果")
public class ShopEntryVO {

    @Schema(description = "店铺ID")
    private Long shopId;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "店铺名称")
    private String shopName;

    @Schema(description = "店铺头像")
    private String logo;

    @Schema(description = "店铺描述")
    private String description;

    @Schema(description = "营业状态 1营业 0关闭")
    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "是否新建（true=新入驻 false=更新已有店铺）")
    private Boolean isNew;
}
