package com.liyun.promotion.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("flash_sale_item")
public class FlashSaleItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long flashSaleId;

    private Long skuId;

    private Long spuId;

    /** 秒杀价格（分） */
    private Long flashPrice;

    /** 秒杀库存 */
    private Integer stock;

    /** 已售数量 */
    private Integer sold;

    /** 每人限购 */
    private Integer limitPerUser;

    /** 排序 */
    private Integer sort;
}
