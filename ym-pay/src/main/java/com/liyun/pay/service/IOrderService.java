package com.liyun.pay.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.liyun.pay.domain.dto.OrderDTO;
import com.liyun.pay.domain.pojo.Order;

import java.util.List;

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
    String createOrder(OrderDTO dto);

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
     * 扫描超时未支付订单并取消
     */
    void handleTimeoutOrders();
}
