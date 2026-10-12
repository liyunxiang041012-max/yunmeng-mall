package com.liyun.promotion.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.liyun.promotion.domain.po.FlashSale;
import org.apache.ibatis.annotations.Update;

public interface FlashSaleMapper extends BaseMapper<FlashSale> {

    /** 扣减秒杀库存 */
    @Update("UPDATE flash_sale_item SET stock = stock - 1, sold = sold + 1 WHERE id = #{itemId} AND stock > 0")
    int deductStock(Long itemId);
}
