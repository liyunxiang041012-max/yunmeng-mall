package com.liyun.pay.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liyun.api.client.ItemFeign;
import com.liyun.api.client.ShopFeign;
import com.liyun.api.client.UserFeign;
import com.liyun.api.dto.ItemInfoDTO;
import com.liyun.api.dto.ShopInfoDTO;
import com.liyun.api.dto.SkuInfoDTO;
import com.liyun.common.context.UserContext;
import com.liyun.common.utils.PageDTO;
import com.liyun.common.utils.Result;
import com.liyun.pay.domain.dto.OrderDTO;
import com.liyun.pay.domain.pojo.Order;
import com.liyun.pay.domain.pojo.OrderItem;
import com.liyun.pay.domain.vo.CreateOrderVO;
import com.liyun.pay.enums.OrderStatus;
import com.liyun.pay.mapper.OrderMapper;
import com.liyun.pay.service.IOrderItemService;
import com.liyun.pay.service.IOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 订单表 服务实现类
 * </p>
 *
 * @author liyun
 * @since 2026-05-23
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements IOrderService {

    private final ItemFeign itemFeign;
    private final ShopFeign shopFeign;
    private final IOrderItemService orderItemService;
    private final UserFeign userFeign;
    /** 超时时间：1分钟（测试用，上线改回30） */
    private static final long ORDER_TIMEOUT_MINUTES = 1;

    // ==================== 用户端 ====================

    @Override
    public CreateOrderVO createOrder(OrderDTO dto) {
        if (dto == null || dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new RuntimeException("订单商品不能为空");
        }

        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new RuntimeException("用户未登录");
        }

        for (OrderDTO.OrderItemDTO item : dto.getItems()) {
            if (item.getSkuId() == null) {
                throw new RuntimeException("商品SKU ID不能为空");
            }
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new RuntimeException("购买数量必须大于0");
            }
        }

        List<Long> skuIds = dto.getItems().stream()
                .map(OrderDTO.OrderItemDTO::getSkuId)
                .collect(Collectors.toList());

        List<SkuInfoDTO> skuList = itemFeign.batchGetSkuInfo(skuIds);
        if (skuList == null || skuList.isEmpty()) {
            throw new RuntimeException("查询商品信息失败");
        }

        Map<Long, SkuInfoDTO> skuMap = skuList.stream()
                .collect(Collectors.toMap(SkuInfoDTO::getId, sku -> sku));

        long totalAmount = 0L;
        List<OrderItem> orderItems = new ArrayList<>();
        String orderId = generateOrderId();

        for (OrderDTO.OrderItemDTO item : dto.getItems()) {
            SkuInfoDTO sku = skuMap.get(item.getSkuId());

            if (sku == null) {
                throw new RuntimeException("商品不存在，skuId：" + item.getSkuId());
            }
            if (sku.getStock() == null || sku.getStock() < item.getQuantity()) {
                throw new RuntimeException("商品库存不足，skuId：" + item.getSkuId());
            }
            if (sku.getPrice() == null || sku.getPrice() <= 0) {
                throw new RuntimeException("商品价格异常，skuId：" + item.getSkuId());
            }

            long itemAmount = (long) sku.getPrice() * item.getQuantity();
            totalAmount += itemAmount;

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

        Order order = new Order();
        order.setId(orderId);
        order.setUserId(userId);
        order.setShopId(orderItems.get(0).getShopId());
        order.setTotalAmount(totalAmount);
        order.setPayAmount(0L);
        order.setDiscountAmount(0L);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        order.setExpireTime(LocalDateTime.now().plusMinutes(ORDER_TIMEOUT_MINUTES));

        save(order);
        orderItemService.saveOrderItems(orderItems);

        return new CreateOrderVO(orderId, order.getExpireTime());
    }

    public static String generateOrderId() {
        long timestamp = System.currentTimeMillis();
        int random = new Random().nextInt(900000) + 100000;
        return timestamp + String.valueOf(random);
    }

    @Override
    public List<Order> orderList(Long userId) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getUserId, userId)
                .orderByDesc(Order::getCreateTime);
        return list(wrapper);
    }

    @Override
    public Order getOrderDetail(String orderId) {
        return getById(orderId);
    }

    @Override
    public void cancelOrder(String orderId) {
        Order order = getById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!OrderStatus.PENDING_PAYMENT.equals(order.getStatus())) {
            throw new RuntimeException("只能取消待付款的订单");
        }
        order.setStatus(OrderStatus.CANCELLED);
        order.setUpdateTime(LocalDateTime.now());
        updateById(order);
    }

    @Override
    public void updateOrderStatus(String orderId, Integer status) {
        Order order = getById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        OrderStatus newStatus = OrderStatus.of(status);
        order.setStatus(newStatus);
        order.setUpdateTime(LocalDateTime.now());
        if (newStatus == OrderStatus.PAID) {
            order.setPayTime(LocalDateTime.now());
        }
        updateById(order);
    }

    @Override
    public void handleTimeoutOrders() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(ORDER_TIMEOUT_MINUTES);
        List<Order> timeoutOrders = lambdaQuery()
                .eq(Order::getStatus, OrderStatus.PENDING_PAYMENT)
                .le(Order::getCreateTime, deadline)
                .list();

        if (timeoutOrders.isEmpty()) return;

        LocalDateTime now = LocalDateTime.now();
        for (Order order : timeoutOrders) {
            order.setStatus(OrderStatus.CANCELLED);
            order.setUpdateTime(now);
            updateById(order);
            log.info("订单超时自动取消: {}", order.getId());
        }
        log.info("本次取消超时订单 {} 笔", timeoutOrders.size());
    }

    // ==================== 商家端 ====================

    @Override
    public PageDTO<Map<String, Object>> shopOrderList(Long shopId, Integer page, Integer size, String status, String keyword) {
        Page<Order> p = Page.of(page, size);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(Order::getShopId, shopId);

        // 状态筛选
        if (status != null && !status.isBlank()) {
            try {
                OrderStatus os = OrderStatus.valueOf(status);
                wrapper.eq(Order::getStatus, os);
            } catch (IllegalArgumentException ignored) {
                // 非法状态值忽略
            }
        }

        // 关键词搜索（订单号）
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(Order::getId, keyword));
        }

        wrapper.orderByDesc(Order::getCreateTime);
        page(p, wrapper);

        List<Order> orders = p.getRecords();
        if (orders.isEmpty()) {
            return PageDTO.of(p, Collections.emptyList());
        }

        // 批量收集 userId → 查用户昵称
        Set<Long> userIds = orders.stream().map(Order::getUserId).collect(Collectors.toSet());
        Map<Long, String> userNicknameMap = new HashMap<>();
        for (Long uid : userIds) {
            try {
                Map<String, Object> userMap = userFeign.getUserById(uid);
                if (userMap != null) {
                    String nickname = (String) userMap.getOrDefault("nickname", "");
                    if (nickname == null || nickname.isEmpty()) {
                        nickname = (String) userMap.getOrDefault("phone", "");
                    }
                    userNicknameMap.put(uid, nickname);
                }
            } catch (Exception e) {
                log.warn("查询用户信息失败 userId={}", uid, e);
                userNicknameMap.put(uid, "");
            }
        }

        // 为每个订单补充商品名、商品图、买家名
        List<Map<String, Object>> enrichedList = new ArrayList<>();
        for (Order order : orders) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", order.getId());
            map.put("shopId", order.getShopId());
            map.put("userId", order.getUserId());
            map.put("totalAmount", order.getTotalAmount());
            map.put("payAmount", order.getPayAmount());
            map.put("discountAmount", order.getDiscountAmount());
            map.put("status", order.getStatus() != null ? order.getStatus().name() : null);
            map.put("createTime", order.getCreateTime());
            map.put("updateTime", order.getUpdateTime());
            map.put("payTime", order.getPayTime());
            map.put("expireTime", order.getExpireTime());
            map.put("address", order.getAddress());
            map.put("phone", order.getPhone());
            map.put("trackingNo", order.getTrackingNo());
            map.put("remark", order.getRemark());

            // 查第一个订单项
            List<OrderItem> items = orderItemService.getOrderItems(order.getId());
            if (items != null && !items.isEmpty()) {
                OrderItem first = items.get(0);
                map.put("productName", first.getName());
                map.put("productImage", first.getImage());
            } else {
                map.put("productName", "");
                map.put("productImage", "");
            }

            map.put("buyerName", userNicknameMap.getOrDefault(order.getUserId(), ""));
            enrichedList.add(map);
        }

        return PageDTO.of(p, enrichedList);
    }

    @Override
    public Map<String, Object> getShopOrderDetail(String orderId) {
        Order order = getById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", order.getId());
        map.put("shopId", order.getShopId());
        map.put("userId", order.getUserId());
        map.put("totalAmount", order.getTotalAmount());
        map.put("payAmount", order.getPayAmount());
        map.put("discountAmount", order.getDiscountAmount());
        map.put("status", order.getStatus() != null ? order.getStatus().name() : null);
        map.put("createTime", order.getCreateTime());
        map.put("updateTime", order.getUpdateTime());
        map.put("payTime", order.getPayTime());
        map.put("expireTime", order.getExpireTime());
        map.put("address", order.getAddress());
        map.put("phone", order.getPhone());
        map.put("trackingNo", order.getTrackingNo());
        map.put("remark", order.getRemark());
        map.put("orderNo", order.getId());

        // 商品明细列表
        List<OrderItem> items = orderItemService.getOrderItems(orderId);
        List<Map<String, Object>> itemList = new ArrayList<>();
        if (items != null) {
            for (OrderItem item : items) {
                Map<String, Object> im = new LinkedHashMap<>();
                im.put("name", item.getName());
                im.put("image", item.getImage());
                im.put("price", item.getPrice());
                im.put("quantity", item.getQuantity());
                itemList.add(im);
            }
        }
        map.put("items", itemList);

        // 买家信息
        try {
            Map<String, Object> userMap = userFeign.getUserById(order.getUserId());
            if (userMap != null) {
                map.put("buyerName", userMap.getOrDefault("nickname", ""));
                map.put("buyerPhone", userMap.getOrDefault("phone", ""));
            }
        } catch (Exception e) {
            log.warn("查询买家信息失败 userId={}", order.getUserId(), e);
            map.put("buyerName", "");
            map.put("buyerPhone", "");
        }

        return map;
    }

    @Override
    public Map<String, Object> getShopStats(Long shopId) {
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.plusDays(1).atStartOfDay();

        // 今日订单
        List<Order> todayOrders = lambdaQuery()
                .eq(Order::getShopId, shopId)
                .ge(Order::getCreateTime, todayStart)
                .lt(Order::getCreateTime, todayEnd)
                .list();

        long todaySales = todayOrders.stream()
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED && o.getStatus() != OrderStatus.REFUNDED)
                .mapToLong(o -> o.getPayAmount() != null && o.getPayAmount() > 0 ? o.getPayAmount() : o.getTotalAmount())
                .sum();

        long todayOrderCount = todayOrders.size();

        // 累计统计
        List<Order> allOrders = lambdaQuery()
                .eq(Order::getShopId, shopId)
                .list();

        long totalRevenue = allOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.PAID || o.getStatus() == OrderStatus.SHIPPED || o.getStatus() == OrderStatus.COMPLETED)
                .mapToLong(o -> o.getPayAmount() != null && o.getPayAmount() > 0 ? o.getPayAmount() : o.getTotalAmount())
                .sum();

        long totalOrderCount = allOrders.size();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("todaySales", todaySales);
        stats.put("todayOrders", (int) todayOrderCount);
        stats.put("totalRevenue", totalRevenue);
        stats.put("totalOrders", (int) totalOrderCount);
        stats.put("views", 0); // 暂不支持浏览量
        return stats;
    }

    @Override
    public List<Map<String, Object>> getRevenueTrend(Long shopId, String period) {
        int days = "30d".equals(period) ? 30 : ("90d".equals(period) ? 90 : 7);
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(days - 1);

        // 查改时间段内所有已支付/已发货/已完成的订单
        List<Order> orders = lambdaQuery()
                .eq(Order::getShopId, shopId)
                .in(Order::getStatus, OrderStatus.PAID, OrderStatus.SHIPPED, OrderStatus.COMPLETED)
                .ge(Order::getCreateTime, startDate.atStartOfDay())
                .lt(Order::getCreateTime, today.plusDays(1).atStartOfDay())
                .list();

        // 按日期分组汇总
        Map<LocalDate, Long> dailyAmount = new LinkedHashMap<>();
        for (LocalDate d = startDate; !d.isAfter(today); d = d.plusDays(1)) {
            dailyAmount.put(d, 0L);
        }
        for (Order o : orders) {
            LocalDate orderDate = o.getCreateTime().toLocalDate();
            long amount = o.getPayAmount() != null && o.getPayAmount() > 0 ? o.getPayAmount() : o.getTotalAmount();
            dailyAmount.merge(orderDate, amount, Long::sum);
        }

        List<Map<String, Object>> trend = new ArrayList<>();
        for (Map.Entry<LocalDate, Long> entry : dailyAmount.entrySet()) {
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", entry.getKey().toString());
            point.put("amount", entry.getValue());
            trend.add(point);
        }
        return trend;
    }

    @Override
    public void shipOrder(String orderId, String trackingNo) {
        Order order = getById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!OrderStatus.PAID.equals(order.getStatus())) {
            throw new RuntimeException("只能对已付款的订单发货");
        }
        order.setStatus(OrderStatus.SHIPPED);
        order.setTrackingNo(trackingNo);
        order.setUpdateTime(LocalDateTime.now());
        updateById(order);
        log.info("订单发货成功: {}, 物流单号: {}", orderId, trackingNo);
    }
}
