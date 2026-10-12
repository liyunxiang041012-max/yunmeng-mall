package com.liyun.promotion.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

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

    /** 商家店铺ID */
    private Long shopId;

    /** 秒杀价格（分） */
    private Long flashPrice;

    /** 秒杀库存 */
    private Integer stock;

    /** 已售数量 */
    private Integer sold;

    /** 每人限购 */
    private Integer limitPerUser;

    /** 秒杀开始时间（商家设定） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    /** 秒杀结束时间（商家设定） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    /** 排序 */
    private Integer sort;
}
