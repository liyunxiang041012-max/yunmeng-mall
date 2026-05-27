package com.liyun.pay.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.liyun.pay.domain.pojo.OrderItem;

import java.util.List;

/**
 * <p>
 * 订单明细表 服务类
 * </p>
 *
 * @author liyun
 * @since 2026-05-23
 */
public interface IOrderItemService extends IService<OrderItem> {

    /**
     * 创建订单明细
     */
    void createOrderItems(String orderId, Long userId, List<Long> cartIds);

    /**
     * 查询订单明细列表
     */
    List<OrderItem> getOrderItems(String orderId);

    /**
     * 批量保存订单项
     */
    void saveOrderItems(List<OrderItem> orderItems);
}
