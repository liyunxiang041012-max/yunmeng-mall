package com.liyun.pay.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liyun.pay.domain.pojo.OrderItem;
import com.liyun.pay.mapper.OrderItemMapper;
import com.liyun.pay.service.IOrderItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 订单明细表 服务实现类
 * </p>
 *
 * @author liyun
 * @since 2026-05-23
 */
@Service
@RequiredArgsConstructor
public class OrderItemServiceImpl extends ServiceImpl<OrderItemMapper, OrderItem> implements IOrderItemService {

    @Override
    public void createOrderItems(String orderId, Long userId, List<Long> cartIds) {
        // TODO: 实现创建订单明细逻辑
    }

    @Override
    public List<OrderItem> getOrderItems(String orderId) {
        return list(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, orderId));
    }

    @Override
    public void saveOrderItems(List<OrderItem> orderItems) {
        if (orderItems != null && !orderItems.isEmpty()) {
            saveBatch(orderItems);
        }
    }
}
