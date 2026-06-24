package com.liyun.ai.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemReviewRequest {

    /** 商品名称 */
    private String name;

    /** 商品图片 */
    private String image;

    /** 分类ID */
    private Long categoryId;

    /** 品牌ID */
    private Long brandId;

    /** 价格（分） */
    private Long price;

    /** 库存 */
    private Integer stock;
}
