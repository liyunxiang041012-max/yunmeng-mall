package com.liyun.pay.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liyun.pay.domain.dto.PayDTO;
import com.liyun.pay.domain.pojo.Order;
import com.liyun.pay.domain.pojo.Pay;
import com.liyun.pay.domain.vo.PayVO;
import com.liyun.pay.enums.OrderStatus;
import com.liyun.pay.enums.PayStatus;
import com.liyun.pay.mapper.PayMapper;
import com.liyun.pay.service.IOrderService;
import com.liyun.pay.service.IPayService;
import com.liyun.common.context.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayServiceImpl extends ServiceImpl<PayMapper, Pay> implements IPayService {

    private final IOrderService orderService;
    private final RedissonClient redissonClient;

    private Long getCurrentUserId() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new RuntimeException("用户未登录");
        }
        return userId;
    }

    @Override
    @Transactional
    public PayVO createPay(PayDTO dto) {
        Long userId = getCurrentUserId();

        Pay pay = new Pay();
        pay.setUserId(userId);
        pay.setOrderId(dto.getOrderId());
        pay.setPayNo(UUID.randomUUID().toString().replace("-", ""));
        pay.setPayChannel(dto.getPayChannel());
        pay.setAmount(dto.getAmount());
        pay.setStatus(PayStatus.PENDING); // 待支付
        pay.setCreateTime(LocalDateTime.now());
        pay.setUpdateTime(LocalDateTime.now());
        this.save(pay);

        return toVO(pay);
    }

    @Override
    public List<PayVO> payList() {
        Long userId = getCurrentUserId();
        List<Pay> pays = this.list(new LambdaQueryWrapper<Pay>()
                .eq(Pay::getUserId, userId)
                .orderByDesc(Pay::getCreateTime));

        if (pays.isEmpty()) {
            return Collections.emptyList();
        }

        return pays.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public PayVO getPayDetail(Long id) {
        Pay pay = this.getById(id);
        if (pay == null) {
            throw new RuntimeException("支付记录不存在");
        }
        return toVO(pay);
    }

    @Override
    @Transactional
    public void cancelPay(Long id) {
        Pay pay = this.getById(id);
        if (pay == null) {
            throw new RuntimeException("支付记录不存在");
        }
        if (pay.getStatus() != PayStatus.PENDING) {
            throw new RuntimeException("只能取消待支付状态的订单");
        }
        pay.setStatus(PayStatus.CLOSED); // 已关闭
        pay.setUpdateTime(LocalDateTime.now());
        this.updateById(pay);
    }

    @Override
    @Transactional
    public void handlePayCallback(String payNo) {
        // 1.查询支付记录
        Pay pay = this.getOne(new LambdaQueryWrapper<Pay>()
                .eq(Pay::getPayNo, payNo));
        if (pay == null) {
            throw new RuntimeException("支付记录不存在");
        }

        // 2.限流：同一用户每秒最多1次回调
        String rateKey = "rate:pay:callback:uid:" + pay.getUserId();
        RRateLimiter limiter = redissonClient.getRateLimiter(rateKey);
        limiter.trySetRate(RateType.OVERALL, 1, 1, RateIntervalUnit.SECONDS);
        if (!limiter.tryAcquire()) {
            log.warn("[PAY] 用户{}支付回调过于频繁，被限流", pay.getUserId());
            throw new RuntimeException("操作过于频繁，请稍后重试");
        }

        // 3.当前支付记录已支付则跳过
        if (pay.getStatus() == PayStatus.PAID) {
            return;
        }

        // 4.检查订单是否已支付（防止不同支付单重复支付同一订单）
        Order order = orderService.getById(pay.getOrderId());
        if (order != null && order.getStatus() == OrderStatus.PAID) {
            log.warn("[PAY] 订单已支付，忽略重复回调 orderId={}", pay.getOrderId());
            return;
        }

        // 5.更新支付状态
        pay.setStatus(PayStatus.PAID);
        pay.setPayTime(LocalDateTime.now());
        pay.setUpdateTime(LocalDateTime.now());
        this.updateById(pay);

        // 6.联动更新订单状态为已支付（会触发销量增加）
        try {
            orderService.updateOrderStatus(pay.getOrderId(), OrderStatus.PAID.getCode());
            log.info("[PAY] 支付回调成功，订单{}已更新为已支付", pay.getOrderId());
        } catch (Exception e) {
            // 回滚支付状态，保持数据一致
            pay.setStatus(PayStatus.PENDING);
            pay.setPayTime(null);
            this.updateById(pay);
            log.error("[PAY] 更新订单状态失败，已回滚支付状态 orderId={}: {}", pay.getOrderId(), e.getMessage(), e);
            throw new RuntimeException("支付回调处理失败", e);
        }
    }

    private PayVO toVO(Pay pay) {
        PayVO vo = new PayVO();
        vo.setId(pay.getId());
        vo.setUserId(pay.getUserId());
        vo.setOrderId(pay.getOrderId());
        vo.setPayNo(pay.getPayNo());
        vo.setPayChannel(pay.getPayChannel());
        vo.setAmount(pay.getAmount());
        vo.setStatus(pay.getStatus());
        vo.setPayTime(pay.getPayTime());
        vo.setCreateTime(pay.getCreateTime());
        return vo;
    }
}
