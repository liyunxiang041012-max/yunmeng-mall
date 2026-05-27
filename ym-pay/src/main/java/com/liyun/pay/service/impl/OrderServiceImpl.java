package com.liyun.pay.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liyun.api.client.ItemFeign;
import com.liyun.api.dto.SkuInfoDTO;
import com.liyun.common.context.UserContext;
import com.liyun.common.enums.ResultCode;
import com.liyun.common.exception.BizException;
import com.liyun.pay.domain.dto.OrderDTO;
import com.liyun.pay.domain.pojo.Order;
import com.liyun.pay.domain.pojo.OrderItem;
import com.liyun.pay.enums.OrderStatus;
import com.liyun.pay.mapper.OrderMapper;
import com.liyun.pay.service.IOrderItemService;
import com.liyun.pay.service.IOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * <p>
 * 订单表 服务实现类
 * </p>
 *
 * @author liyun
 * @since 2026-05-23
 */
@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements IOrderService {

    private final ItemFeign itemFeign;
    private final IOrderItemService orderItemService;
    @Override
    @Transactional
    public String createOrder(OrderDTO dto) {
        // 1. 参数校验
        if (dto == null || dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new BizException(ResultCode.PARAM_ERROR, "订单商品不能为空");
        }

        // 2. 获取用户ID
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }

        // 3. 校验订单项
        for (OrderDTO.OrderItemDTO item : dto.getItems()) {
            if (item.getSkuId() == null) {
                throw new BizException(ResultCode.PARAM_ERROR, "商品SKU ID不能为空");
            }
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new BizException(ResultCode.PARAM_ERROR, "购买数量必须大于0");
            }
        }

        // 4. 批量查询SKU信息
        List<Long> skuIds = dto.getItems().stream()
                .map(OrderDTO.OrderItemDTO::getSkuId)
                .collect(Collectors.toList());

        List<SkuInfoDTO> skuList = itemFeign.batchGetSkuInfo(skuIds);
        if (skuList == null || skuList.isEmpty()) {
            throw new BizException(ResultCode.FAIL, "查询商品信息失败");
        }

        // 5. 转成Map便于查找
        Map<Long, SkuInfoDTO> skuMap = skuList.stream()
                .collect(Collectors.toMap(SkuInfoDTO::getId, sku -> sku));

        // 6. 验证SKU + 组装OrderItem + 计算金额
        long totalAmount = 0L;
        List<OrderItem> orderItems = new ArrayList<>();
        String orderId = generateOrderId();

        for (OrderDTO.OrderItemDTO item : dto.getItems()) {
            SkuInfoDTO sku = skuMap.get(item.getSkuId());

            if (sku == null) {
                throw new BizException(ResultCode.NOT_FOUND, "商品不存在，skuId：" + item.getSkuId());
            }
            if (sku.getStock() == null || sku.getStock() < item.getQuantity()) {
                throw new BizException(ResultCode.FAIL, "商品库存不足，skuId：" + item.getSkuId());
            }
            if (sku.getPrice() == null || sku.getPrice() <= 0) {
                throw new BizException(ResultCode.FAIL, "商品价格异常，skuId：" + item.getSkuId());
            }

            // 计算小计
            long itemAmount = (long) sku.getPrice() * item.getQuantity();
            totalAmount += itemAmount;

            // 组装订单项
            OrderItem orderItem = new OrderItem()
                    .setOrderId(orderId)
                    .setUserId(userId)
                    .setSkuId(sku.getId())
                    .setSpuId(sku.getItemId())
                    .setShopId(sku.getShopId())
                    .setName(sku.getName())
                    .setImage(sku.getImage())
                    .setPrice((long) sku.getPrice())
                    .setQuantity(item.getQuantity());
            orderItems.add(orderItem);
        }

        // 7. 计算优惠金额
        long discountAmount = 0L;
        // TODO: 优惠券逻辑，根据 dto.getCouponId() 查询优惠券并计算 discountAmount

        long payAmount = totalAmount - discountAmount;
        if (payAmount < 0) {
            payAmount = 0L;
        }

        // 8. 创建订单
        Order order = new Order();
        order.setId(orderId);
        order.setUserId(userId);
        order.setShopId(orderItems.get(0).getShopId());
        order.setTotalAmount(totalAmount);
        order.setPayAmount(payAmount);
        order.setCouponId(dto.getCouponId());
        order.setDiscountAmount(discountAmount);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());

        // 9. 保存订单和订单项
        save(order);
        orderItemService.saveOrderItems(orderItems);

        // 10. 扣减库存
        for (OrderDTO.OrderItemDTO item : dto.getItems()) {
            itemFeign.deductStock(item.getSkuId(), item.getQuantity());
        }

        return orderId;
    }

    public static String generateOrderId() {
        long timestamp = System.currentTimeMillis();
        int random = new Random().nextInt(900000) + 100000; // 6位随机数
        return timestamp + String.valueOf(random);
    }

    @Override
    public List<Order> orderList(Long userId) {
        return list(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .orderByDesc(Order::getCreateTime));
    }

    @Override
    public Order getOrderDetail(String orderId) {
        Order order = getById(orderId);
        if (order == null) {
            throw new BizException(ResultCode.NOT_FOUND, "订单不存在");
        }
        return order;
    }

    @Override
    @Transactional
    public void cancelOrder(String orderId) {
        Order order = getById(orderId);
        if (order == null) {
            throw new BizException(ResultCode.NOT_FOUND, "订单不存在");
        }
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new BizException(ResultCode.FAIL, "只能取消待付款订单");
        }
        order.setStatus(OrderStatus.CANCELLED);
        order.setUpdateTime(LocalDateTime.now());
        updateById(order);
    }

    @Override
    @Transactional
    public void updateOrderStatus(String orderId, Integer status) {
        Order order = getById(orderId);
        if (order == null) {
            throw new BizException(ResultCode.NOT_FOUND, "订单不存在");
        }
        order.setStatus(OrderStatus.of(status));
        order.setUpdateTime(LocalDateTime.now());
        if (status == OrderStatus.PAID.getCode()) {
            order.setPayTime(LocalDateTime.now());
        }
        updateById(order);
    }
}
