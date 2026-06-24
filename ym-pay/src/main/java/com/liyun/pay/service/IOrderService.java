package com.liyun.pay.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.liyun.common.utils.PageDTO;
import com.liyun.pay.domain.dto.OrderDTO;
import com.liyun.pay.domain.pojo.Order;
import com.liyun.pay.domain.vo.CreateOrderVO;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 订单表 服务类
 * </p>
 *
 * @author liyun
 * @since 2026-05-23
 */
public interface IOrderService extends IService<Order> {

    /**
     * 创建订单
     */
    CreateOrderVO createOrder(OrderDTO dto);

    /**
     * 查询订单列表
     */
    List<Order> orderList(Long userId);

    /**
     * 查询订单详情
     */
    Order getOrderDetail(String orderId);

    /**
     * 取消订单
     */
    void cancelOrder(String orderId);

    /**
     * 更新订单状态
     */
    void updateOrderStatus(String orderId, Integer status);

    /**
     * 扫描超时未支付订单并自动取消
     */
    void handleTimeoutOrders();

    /**
     * 查询某店铺的订单列表（分页，含商品/买家等信息）
     */
    PageDTO<Map<String, Object>> shopOrderList(Long shopId, Integer page, Integer size, String status, String keyword);

    /**
     * 查询店铺订单详情（含商品、买家信息）
     */
    Map<String, Object> getShopOrderDetail(String orderId);

    /**
     * 店铺仪表盘统计
     */
    Map<String, Object> getShopStats(Long shopId);

    /**
     * 收入趋势
     */
    List<Map<String, Object>> getRevenueTrend(Long shopId, String period);

    /**
     * 发货
     */
    void shipOrder(String orderId, String trackingNo);
}
