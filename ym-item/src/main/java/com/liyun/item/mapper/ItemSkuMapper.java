package com.liyun.item.mapper;

import com.liyun.item.domain.pojo.ItemSku;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * <p>
 * SKU Mapper 接口
 * </p>
 *
 * @author liyun
 * @since 2026-05-09
 */
public interface ItemSkuMapper extends BaseMapper<ItemSku> {

    /**
     * 乐观锁扣减库存: WHERE stock >= qty 保证原子性，防超卖
     * @param skuId SKU ID
     * @param qty   扣减数量
     * @return 影响行数（0表示库存不足）
     */
    @Update("UPDATE item_sku SET stock = stock - #{qty}, update_time = NOW() WHERE id = #{skuId} AND stock >= #{qty}")
    int deductStock(@Param("skuId") Long skuId, @Param("qty") Integer qty);

    /**
     * 恢复库存（取消/超时订单时调用）
     */
    @Update("UPDATE item_sku SET stock = stock + #{qty}, update_time = NOW() WHERE id = #{skuId}")
    int restoreStock(@Param("skuId") Long skuId, @Param("qty") Integer qty);

}
