package com.liyun.item.domain.pojo;

import java.math.BigDecimal;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 
 * </p>
 *
 * @author liyun
 * @since 2026-05-14
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("shop_rating")
public class ShopRating implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 商家ID
     */
    private Long shopId;

    /**
     * 总成交数
     */
    private Integer totalOrders;

    /**
     * 好评数
     */
    private Integer goodReviews;

    /**
     * 差评数
     */
    private Integer badReviews;

    /**
     * 综合评分（如 4.9）
     */
    private BigDecimal averageScore;

    /**
     * 好评率（%）
     */
    private BigDecimal goodRate;

    private LocalDateTime lastUpdated;


}
