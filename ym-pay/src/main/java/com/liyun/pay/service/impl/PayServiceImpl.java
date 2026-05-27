package com.liyun.pay.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liyun.common.context.UserContext;
import com.liyun.common.enums.ResultCode;
import com.liyun.common.exception.BizException;
import com.liyun.pay.domain.dto.PayDTO;
import com.liyun.pay.domain.pojo.Pay;
import com.liyun.pay.domain.pojo.OrderItem;
import com.liyun.pay.domain.vo.PayVO;
import com.liyun.pay.enums.PayStatus;
import com.liyun.pay.mapper.PayMapper;
import com.liyun.pay.service.IOrderService;
import com.liyun.pay.service.IPayService;
import com.liyun.pay.service.ICartService;
import com.liyun.pay.service.IOrderItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayServiceImpl extends ServiceImpl<PayMapper, Pay> implements IPayService {

    private final IOrderService orderService;
    private final ICartService cartService;
    private final IOrderItemService orderItemService;

    @Override
    @Transactional
    public PayVO createPay(PayDTO dto) {
        // 1. 获取当前用户ID
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }

        // 2. 幂等性检查：同一订单是否已有待支付的支付记录
        Pay existingPay = this.getOne(new LambdaQueryWrapper<Pay>()
                .eq(Pay::getOrderId, dto.getOrderId())
                .eq(Pay::getStatus, PayStatus.PENDING));
        if (existingPay != null) {
            return toVO(existingPay);
        }

        // 3. 生成支付单号：PAY + 时间戳 + 6位随机数
        String payNo = generatePayNo();

        // 4. 创建支付记录
        Pay pay = new Pay();
        pay.setUserId(userId);
        pay.setOrderId(dto.getOrderId());
        pay.setPayNo(payNo);
        pay.setPayChannel(dto.getPayChannel());
        pay.setAmount(dto.getAmount());
        pay.setStatus(PayStatus.PENDING);
        pay.setAddressId(dto.getAddressId());
        pay.setNote(dto.getNote());
        pay.setCouponId(dto.getCouponId());
        pay.setCreateTime(LocalDateTime.now());
        pay.setUpdateTime(LocalDateTime.now());
        this.save(pay);

        // 5. 生成支付链接（实际对接第三方支付时需替换）
        PayVO vo = toVO(pay);
        vo.setPayUrl("https://pay.yunmengmall.com/pay?payNo=" + payNo);
        return vo;
    }

    /**
     * 生成支付单号：PAY + 时间戳 + 6位随机数
     */
    private String generatePayNo() {
        long timestamp = System.currentTimeMillis();
        int random = new Random().nextInt(900000) + 100000;
        return "PAY" + timestamp + random;
    }

    @Override
    public List<PayVO> payList() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
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
            throw new BizException(ResultCode.NOT_FOUND, "支付记录不存在");
        }
        return toVO(pay);
    }

    @Override
    @Transactional
    public void cancelPay(Long id) {
        Pay pay = this.getById(id);
        if (pay == null) {
            throw new BizException(ResultCode.NOT_FOUND, "支付记录不存在");
        }
        if (pay.getStatus() != PayStatus.PENDING) {
            throw new BizException(ResultCode.FAIL, "只能取消待支付状态的订单");
        }
        pay.setStatus(PayStatus.CANCELLED);
        pay.setUpdateTime(LocalDateTime.now());
        this.updateById(pay);
    }

    @Override
    @Transactional
    public void paySuccess(String payNo) {
        log.info("支付成功回调开始，payNo: {}", payNo);
        
        // 1. 查询支付记录
        Pay pay = this.getOne(new LambdaQueryWrapper<Pay>()
                .eq(Pay::getPayNo, payNo));
        if (pay == null) {
            throw new BizException(ResultCode.NOT_FOUND, "支付记录不存在");
        }
        if (pay.getStatus() != PayStatus.PENDING) {
            throw new BizException(ResultCode.FAIL, "该支付单已处理");
        }

        log.info("找到支付记录，orderId: {}, userId: {}", pay.getOrderId(), pay.getUserId());

        // 2. 更新支付状态为已支付
        pay.setStatus(PayStatus.PAID);
        pay.setPayTime(LocalDateTime.now());
        pay.setUpdateTime(LocalDateTime.now());
        this.updateById(pay);

        // 3. 更新订单状态为已支付
        orderService.updateOrderStatus(pay.getOrderId(), 1);
        log.info("订单状态已更新为已支付");

        // 4. 查询订单项，获取已购买的skuId列表
        List<OrderItem> orderItems = orderItemService.list(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, pay.getOrderId()));
        
        log.info("查询到订单项数量: {}", orderItems != null ? orderItems.size() : 0);
        
        if (orderItems != null && !orderItems.isEmpty()) {
            // 5. 提取skuId列表
            List<Long> skuIds = orderItems.stream()
                    .map(OrderItem::getSkuId)
                    .collect(Collectors.toList());
            
            log.info("准备清理购物车，skuIds: {}", skuIds);
            
            // 6. 清理购物车中已购买的商品
            cartService.deleteCartBySkuIds(skuIds);
            
            log.info("购物车清理完成");
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
