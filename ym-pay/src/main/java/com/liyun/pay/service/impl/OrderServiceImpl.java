package com.liyun.pay.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liyun.api.client.ItemFeign;
import com.liyun.api.client.PromotionFeign;
import com.liyun.api.client.ShopFeign;
import com.liyun.api.client.UserFeign;
import com.liyun.api.dto.ItemInfoDTO;
import com.liyun.api.dto.ShopInfoDTO;
import com.liyun.api.dto.SkuInfoDTO;
import com.liyun.common.context.UserContext;
import com.liyun.common.utils.PageDTO;
import com.liyun.common.utils.Result;
import com.liyun.pay.domain.dto.OrderDTO;
import com.liyun.pay.domain.pojo.Cart;
import com.liyun.pay.domain.pojo.Order;
import com.liyun.pay.domain.pojo.OrderItem;
import com.liyun.pay.domain.vo.CreateOrderVO;
import com.liyun.pay.enums.OrderStatus;
import com.liyun.pay.feign.ItemInternalFeign;
import com.liyun.pay.mapper.CartMapper;
import com.liyun.pay.mapper.OrderMapper;
import com.liyun.pay.service.ICartService;
import com.liyun.pay.service.IOrderItemService;
import com.liyun.pay.service.IOrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final PromotionFeign promotionFeign;
    private final ICartService cartService;
    private final CartMapper cartMapper;
    private final ItemInternalFeign itemInternalFeign;
    private final ObjectMapper objectMapper;
    /** 超时时间：30分钟 */
    private static final long ORDER_TIMEOUT_MINUTES = 30;

    @SuppressWarnings("unchecked")
    private <T> List<T> extractDataList(Map<String, Object> result, Class<T> clazz) {
        Object data = result.get("data");
        if (!(data instanceof List)) return Collections.emptyList();
        return ((List<Object>) data).stream()
                .map(item -> objectMapper.convertValue(item, clazz))
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractDataMap(Map<String, Object> result) {
        Object data = result.get("data");
        if (!(data instanceof Map)) return Collections.emptyMap();
        return (Map<String, Object>) data;
    }

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

        List<SkuInfoDTO> skuList = extractDataList(itemFeign.batchGetSkuInfo(skuIds), SkuInfoDTO.class);
        if (skuList == null || skuList.isEmpty()) {
            throw new RuntimeException("查询商品信息失败");
        }

        Map<Long, SkuInfoDTO> skuMap = skuList.stream()
                .collect(Collectors.toMap(SkuInfoDTO::getId, sku -> sku));

        long totalAmount = 0L;
        List<OrderItem> orderItems = new ArrayList<>();
        String orderId = generateOrderId();
        Long shopId = null;

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

            // 记录第一个商品的店铺ID
            if (shopId == null && sku.getShopId() != null) {
                shopId = sku.getShopId();
            }
        }

        // 计算优惠券折扣
        long discountAmount = 0L;
        String userCouponIdStr = dto.getUserCouponId();
        log.info("【优惠券】前端传入userCouponId: {}", userCouponIdStr);
        Long userCouponId = null;
        try {
            userCouponId = (userCouponIdStr != null && !userCouponIdStr.isBlank())
                    ? Long.parseLong(userCouponIdStr) : null;
        } catch (NumberFormatException e) {
            log.warn("【优惠券】userCouponId格式错误: {}", userCouponIdStr);
        }
        Long appliedCouponId = null;
        if (userCouponId != null && userCouponId > 0) {
            try {
                Map<String, Object> rawResult = promotionFeign.useCoupon(userCouponId, totalAmount);
                log.info("【优惠券】Feign原始返回: {}", rawResult);

                // 检查Feign返回是否成功
                Object code = rawResult.get("code");
                if (code instanceof Integer && (Integer) code != 200) {
                    log.error("【优惠券】核销失败，code={}, 原因={}", code, rawResult.get("message"));
                } else {
                    Map<String, Object> useResult = extractDataMap(rawResult);
                    log.info("【优惠券】extractData后: {}", useResult);
                    discountAmount = calculateDiscount(useResult, totalAmount);
                    if (discountAmount > 0) {
                        appliedCouponId = userCouponId;
                        log.info("【优惠券】核销成功: userCouponId={}, discountAmount={}, payAmount={}",
                                userCouponId, discountAmount, totalAmount - discountAmount);
                    } else {
                        log.warn("【优惠券】折扣为0，未生效: userCouponId={}", userCouponId);
                    }
                }
            } catch (Exception e) {
                log.error("【优惠券】Feign调用异常: userCouponId={}, totalAmount={}",
                        userCouponId, totalAmount, e);
            }
        }

        long payAmount = totalAmount - discountAmount;
        if (payAmount < 0) payAmount = 0;

        Order order = new Order();
        order.setId(orderId);
        order.setUserId(userId);
        order.setShopId(shopId);
        order.setTotalAmount(totalAmount);
        order.setPayAmount(payAmount);
        order.setDiscountAmount(discountAmount);
        order.setCouponId(appliedCouponId);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        order.setExpireTime(LocalDateTime.now().plusMinutes(ORDER_TIMEOUT_MINUTES));

        save(order);
        orderItemService.saveOrderItems(orderItems);

        // 下单后按购买数量扣减购物车
        log.info("【购物车清除】dto.items原始数据: {}",
                dto.getItems().stream()
                        .map(i -> "skuId=" + i.getSkuId() + ",qty=" + i.getQuantity())
                        .collect(Collectors.toList()));
        Map<Long, Integer> skuQtyMap = dto.getItems().stream()
                .collect(Collectors.toMap(
                        OrderDTO.OrderItemDTO::getSkuId,
                        OrderDTO.OrderItemDTO::getQuantity,
                        Integer::sum));
        log.info("【购物车清除】skuQtyMap={}", skuQtyMap);
        cartService.reduceCartAfterOrder(userId, skuQtyMap);

        // 扣减库存
        try {
            Map<String, Object> result = itemInternalFeign.deductStock(skuQtyMap);
            log.info("[STOCK] 扣减库存结果: {}", result);
            Object success = result.get("success");
            if (!Boolean.TRUE.equals(success)) {
                log.error("[STOCK] 扣减库存失败: {}", result.get("error"));
            }
        } catch (Exception e) {
            log.error("[STOCK] 扣减库存异常: {}", e.getMessage(), e);
        }

        return new CreateOrderVO(orderId, order.getExpireTime(), totalAmount, payAmount, discountAmount);
    }

    public static String generateOrderId() {
        long timestamp = System.currentTimeMillis();
        int random = new Random().nextInt(900000) + 100000;
        return timestamp + String.valueOf(random);
    }

    /**
     * 根据优惠券信息计算折扣金额
     * discountType: 1=每满减, 2=折扣, 3=无门槛, 4=满减
     */
    private long calculateDiscount(Map<String, Object> couponResult, long totalAmount) {
        if (couponResult == null || couponResult.isEmpty()) return 0;
        Integer discountType = (Integer) couponResult.get("discountType");
        Integer discountValue = (Integer) couponResult.get("discountValue");
        Integer thresholdAmount = (Integer) couponResult.get("thresholdAmount");
        Integer maxDiscountAmount = (Integer) couponResult.get("maxDiscountAmount");
        if (discountType == null || discountValue == null) return 0;

        long discount = 0;
        switch (discountType) {
            case 1: // 每满减：每满thresholdAmount减discountValue
                if (thresholdAmount != null && thresholdAmount > 0) {
                    discount = (totalAmount / thresholdAmount) * discountValue;
                } else {
                    discount = discountValue;
                }
                break;
            case 2: // 折扣：discountValue为折扣率，如80表示8折
                discount = totalAmount - (totalAmount * discountValue / 100);
                break;
            case 3: // 无门槛：直接减discountValue
                discount = discountValue;
                break;
            case 4: // 满减：满thresholdAmount减discountValue
                discount = discountValue;
                break;
        }
        // 不超过最大优惠金额
        if (maxDiscountAmount != null && maxDiscountAmount > 0 && discount > maxDiscountAmount) {
            discount = maxDiscountAmount;
        }
        // 不超过订单金额
        if (discount > totalAmount) discount = totalAmount;
        return discount;
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
        boolean isPaying = (order.getStatus() != OrderStatus.PAID && newStatus == OrderStatus.PAID);
        order.setStatus(newStatus);
        order.setUpdateTime(LocalDateTime.now());
        if (newStatus == OrderStatus.PAID) {
            order.setPayTime(LocalDateTime.now());
        }
        updateById(order);

        // 支付成功时增加销量
        if (isPaying) {
            addSoldForOrder(orderId);
        }
    }

    @Override
    public void handleTimeoutOrders() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(ORDER_TIMEOUT_MINUTES);
        List<String> timeoutOrderIds = lambdaQuery()
                .eq(Order::getStatus, OrderStatus.PENDING_PAYMENT)
                .le(Order::getCreateTime, deadline)
                .list()
                .stream()
                .map(Order::getId)
                .collect(Collectors.toList());

        if (timeoutOrderIds.isEmpty()) {
            log.info("【订单清除】无超时订单（阈值: {} 分钟），跳过", ORDER_TIMEOUT_MINUTES);
            return;
        }

        log.info("【订单清除】发现超时订单 {} 笔，开始批量删除...", timeoutOrderIds.size());

        // 先删除关联的订单项
        orderItemService.remove(
                new LambdaQueryWrapper<OrderItem>()
                        .in(OrderItem::getOrderId, timeoutOrderIds));
        log.info("【订单清除】已清除关联订单项");

        // 再批量删除订单
        removeBatchByIds(timeoutOrderIds);
        log.info("【订单清除】已清除订单: {}", timeoutOrderIds);
        log.info("【订单清除】本次共清除超时订单 {} 笔", timeoutOrderIds.size());
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
                Map<String, Object> userMap = extractDataMap(userFeign.getUserById(uid));
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
            Map<String, Object> userMap = extractDataMap(userFeign.getUserById(order.getUserId()));
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

    /** 支付成功后增加商品销量 */
    private void addSoldForOrder(String orderId) {
        try {
            List<OrderItem> items = orderItemService.getOrderItems(orderId);
            if (items == null || items.isEmpty()) return;

            Map<Long, Integer> itemQtyMap = new HashMap<>();
            for (OrderItem item : items) {
                if (item.getSpuId() != null) {
                    itemQtyMap.merge(item.getSpuId(), item.getQuantity() != null ? item.getQuantity() : 0, Integer::sum);
                }
            }
            log.info("[SOLD] 增加销量 orderId={}, itemQtyMap={}", orderId, itemQtyMap);
            itemInternalFeign.addSold(itemQtyMap);
        } catch (Exception e) {
            log.error("[SOLD] 增加销量失败 orderId={}: {}", orderId, e.getMessage(), e);
        }
    }

    // ==================== 管理员端 ====================

    @Override
    public Map<String, Object> getAdminStats() {
        List<Order> all = lambdaQuery()
                .in(Order::getStatus, OrderStatus.PAID, OrderStatus.SHIPPED, OrderStatus.COMPLETED)
                .list();
        long totalOrders = all.size();
        long totalGmv = all.stream()
                .mapToLong(o -> o.getPayAmount() != null && o.getPayAmount() > 0 ? o.getPayAmount() : o.getTotalAmount())
                .sum();

        // 今日订单
        LocalDate today = LocalDate.now();
        long todayOrders = lambdaQuery()
                .ge(Order::getCreateTime, today.atStartOfDay())
                .lt(Order::getCreateTime, today.plusDays(1).atStartOfDay())
                .count();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalOrders", totalOrders);
        result.put("totalGmv", totalGmv);
        result.put("todayOrders", todayOrders);
        return result;
    }

    @Override
    public List<Map<String, Object>> getAdminRecentOrders() {
        List<Order> recent = lambdaQuery()
                .orderByDesc(Order::getCreateTime)
                .last("LIMIT 5")
                .list();

        log.info("[ADMIN] getAdminRecentOrders: 查到 {} 条订单", recent.size());

        // 批量查买家昵称
        List<Long> userIds = recent.stream().map(Order::getUserId).distinct().collect(Collectors.toList());
        log.info("[ADMIN] 涉及 userIds: {}", userIds);
        Map<Long, String> nicknameMap = new HashMap<>();
        for (Long uid : userIds) {
            try {
                Map<String, Object> raw = userFeign.getUserById(uid);
                log.info("[ADMIN] userFeign.getUserById({}) raw: {}", uid, raw);
                if (raw != null && raw.get("data") instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = (Map<String, Object>) raw.get("data");
                    String nickname = (String) data.getOrDefault("nickname", "");
                    // 脱敏：张*明
                    String masked = maskNickname(nickname, uid);
                    nicknameMap.put(uid, masked);
                }
            } catch (Exception e) {
                log.warn("[ADMIN] 查用户昵称失败 uid={}: {}", uid, e.getMessage());
                nicknameMap.put(uid, "用户" + uid);
            }
        }

        // 时间判断基准
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        List<Map<String, Object>> list = new ArrayList<>();
        for (Order order : recent) {
            List<OrderItem> items = orderItemService.getOrderItems(order.getId());
            String productName = (items != null && !items.isEmpty()) ? items.get(0).getName() : "";
            String statusClass = switch (order.getStatus()) {
                case PAID -> "paid";
                case SHIPPED -> "shipped";
                case PENDING_PAYMENT -> "pending";
                case COMPLETED -> "done";
                case CANCELLED, REFUNDED -> "cancel";
                default -> "pending";
            };
            // time 格式化：今天→HH:mm:ss，昨天→昨天，更早→MM-dd
            String timeStr;
            if (order.getCreateTime() != null) {
                LocalDate orderDate = order.getCreateTime().toLocalDate();
                if (orderDate.equals(today)) {
                    timeStr = order.getCreateTime().toLocalTime().toString();
                } else if (orderDate.equals(yesterday)) {
                    timeStr = "昨天";
                } else {
                    timeStr = order.getCreateTime().format(java.time.format.DateTimeFormatter.ofPattern("MM-dd"));
                }
            } else {
                timeStr = "";
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("orderNo", order.getId());
            m.put("user", nicknameMap.getOrDefault(order.getUserId(), "用户" + order.getUserId()));
            m.put("product", productName);
            m.put("amount", String.format("%.2f", (order.getPayAmount() != null && order.getPayAmount() > 0
                    ? order.getPayAmount() : order.getTotalAmount()) / 100.0));
            m.put("status", order.getStatus() != null ? order.getStatus().getDesc() : "");
            m.put("statusClass", statusClass);
            m.put("time", timeStr);
            list.add(m);
        }
        return list;
    }

    /** 昵称脱敏：取首字 + * + 取末字，如 "张三明" → "张*明" */
    private String maskNickname(String nickname, Long uid) {
        if (nickname == null || nickname.isBlank()) {
            return "用户" + uid;
        }
        String trimmed = nickname.trim();
        if (trimmed.length() == 1) {
            return trimmed;
        }
        if (trimmed.length() == 2) {
            return trimmed.charAt(0) + "*";
        }
        return trimmed.charAt(0) + "*" + trimmed.charAt(trimmed.length() - 1);
    }

    @Override
    public List<Map<String, Object>> getAdminTopProducts() {
        // 查所有已支付/已发货/已完成的订单ID
        List<String> validOrderIds = lambdaQuery()
                .select(Order::getId)
                .in(Order::getStatus, OrderStatus.PAID, OrderStatus.SHIPPED, OrderStatus.COMPLETED)
                .list().stream().map(Order::getId).collect(Collectors.toList());

        log.info("[ADMIN] getAdminTopProducts: 有效订单数={}, ids={}", validOrderIds.size(), validOrderIds);

        if (validOrderIds.isEmpty()) {
            log.info("[ADMIN] 无有效订单，topProducts 返回空");
            return List.of();
        }

        // 查这些订单的商品项，按名称聚合
        List<OrderItem> allItems = orderItemService.list(
                new LambdaQueryWrapper<OrderItem>().in(OrderItem::getOrderId, validOrderIds));

        // 按商品名分组统计销量和销售额
        Map<String, Long> salesMap = new LinkedHashMap<>();   // 销量
        Map<String, Long> revenueMap = new LinkedHashMap<>(); // 销售额(分)
        for (OrderItem item : allItems) {
            String name = item.getName() != null && !item.getName().isBlank() ? item.getName() : "未知商品";
            salesMap.merge(name, (long) (item.getQuantity() != null ? item.getQuantity() : 0), Long::sum);
            long rev = (item.getPrice() != null ? item.getPrice() : 0) * (item.getQuantity() != null ? item.getQuantity() : 0);
            revenueMap.merge(name, rev, Long::sum);
        }

        // 按销售额降序取 TOP5
        return revenueMap.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(e -> {
                    String name = e.getKey();
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name", name);
                    // sales 千分位格式
                    m.put("sales", String.format("%,d", salesMap.getOrDefault(name, 0L)));
                    long revFen = revenueMap.getOrDefault(name, 0L);
                    // revenue 纯数字不带¥，前端会加
                    m.put("revenue", String.format("%.2f", revFen / 100.0));
                    return m;
                })
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getAdminRevenue(int period) {
        int days = period == 90 ? 12 : period;
        LocalDate today = LocalDate.now();
        List<Integer> values = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(period == 90 ? (long) i * 7 : i);
            LocalDate next = period == 90 ? date.plusDays(7) : date.plusDays(1);
            long dailySum = lambdaQuery()
                    .in(Order::getStatus, OrderStatus.PAID, OrderStatus.SHIPPED, OrderStatus.COMPLETED)
                    .ge(Order::getCreateTime, date.atStartOfDay())
                    .lt(Order::getCreateTime, next.atStartOfDay())
                    .list().stream()
                    .mapToLong(o -> o.getPayAmount() != null && o.getPayAmount() > 0
                            ? o.getPayAmount() : o.getTotalAmount())
                    .sum() / 100;
            values.add((int) dailySum);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("values", values);
        return result;
    }
}
