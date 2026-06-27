package com.liyun.promotion.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.liyun.promotion.domain.po.FlashSale;
import com.liyun.promotion.domain.po.FlashSaleItem;

import java.util.List;
import java.util.Map;

public interface IFlashSaleService extends IService<FlashSale> {

    /** 创建秒杀活动 */
    void createFlashSale(FlashSale flashSale, List<FlashSaleItem> items);

    /** 查询当前进行中的秒杀活动及商品 */
    List<Map<String, Object>> getCurrentFlashSales();

    /** 查询秒杀商品详情 */
    Map<String, Object> getFlashSaleItemDetail(Long itemId);

    /** 秒杀下单（扣库存） */
    void placeFlashOrder(Long itemId, Long userId);
}
