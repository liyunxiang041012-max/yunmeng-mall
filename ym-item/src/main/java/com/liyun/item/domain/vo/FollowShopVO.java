package com.liyun.item.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 关注店铺 VO
 */
@Data
public class FollowShopVO {

    /** 关注记录 ID */
    private Long id;

    /** 店铺 ID */
    private Long shopId;

    /** 店铺名称 */
    private String shopName;

    /** 店铺 logo */
    private String logo;

    /** 店铺描述 */
    private String description;

    /** 关注时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
