package com.liyun.promotion.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.liyun.common.utils.PageDTO;
import com.liyun.promotion.domain.po.FlashSale;
import com.liyun.promotion.domain.po.FlashSaleItem;

import java.util.List;
import java.util.Map;

public interface IFlashSaleService extends IService<FlashSale> {

    // ===== 管理员 =====

    /** 创建秒杀活动 */
    void createFlashSale(FlashSale flashSale, List<FlashSaleItem> items);

    // ===== 用户端 =====

    /** 查询进行中的秒杀商品（start_time <= now <= end_time） */
    List<Map<String, Object>> getOngoingFlashSales();

    /** 查询预热中的秒杀商品（now < start_time 且 3天内） */
    List<Map<String, Object>> getWarmingFlashSales();

    /** 查询秒杀商品详情 */
    Map<String, Object> getFlashSaleItemDetail(Long itemId);

    /** 秒杀下单（扣库存） */
    void placeFlashOrder(Long itemId, Long userId);

    // ===== 商家端 =====

    /** 商家提交商品参与秒杀（自动关联平台常驻活动，含折扣+时间校验） */
    void merchantSubmitItems(List<FlashSaleItem> items);

    /** 商家调整未开始秒杀商品的时间 */
    void merchantUpdateTime(Long itemId, String startTime, String endTime);

    /** 商家查看自己的秒杀商品看板 */
    List<Map<String, Object>> getMerchantDashboard(Long shopId);

    /** 商家分页查询自己的秒杀商品 */
    PageDTO<Map<String, Object>> merchantQueryFlashSaleItems(Long shopId, Integer pageNo, Integer pageSize, String status);
}
