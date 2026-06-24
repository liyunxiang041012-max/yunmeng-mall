package com.liyun.pay.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liyun.pay.domain.dto.PayDTO;
import com.liyun.pay.domain.pojo.Pay;
import com.liyun.pay.domain.vo.PayVO;
import com.liyun.pay.enums.PayStatus;
import com.liyun.pay.mapper.PayMapper;
import com.liyun.pay.service.IPayService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PayServiceImpl extends ServiceImpl<PayMapper, Pay> implements IPayService {

    private Long getCurrentUserId() {
        return 1L;
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
