package com.liyun.item.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 浏览历史 VO
 */
@Data
public class HistoryItemVO {

    /**
     * 浏览记录 ID
     */
    private Long id;

    /**
     * 商品 ID
     */
    private Long itemId;

    /**
     * 商品名称
     */
    private String name;

    /**
     * 商品名称（别名）
     */
    private String itemName;

    /**
     * 商品主图
     */
    private String mainImage;

    /**
     * 商品图片（备选）
     */
    private String image;

    /**
     * 商品价格（分）
     */
    private Long price;

    /**
     * 浏览时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 浏览时间（别名）
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime viewTime;
}
