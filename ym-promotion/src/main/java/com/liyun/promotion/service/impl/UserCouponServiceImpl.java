package com.liyun.promotion.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.liyun.common.utils.BeanUtils;
import com.liyun.common.utils.PageDTO;
import com.liyun.common.context.UserContext;
import com.liyun.promotion.domain.dto.CouponEventDTO;
import com.liyun.promotion.domain.po.Coupon;
import com.liyun.promotion.domain.po.ExchangeCode;
import com.liyun.promotion.domain.po.UserCoupon;
import com.liyun.promotion.domain.vo.UserCouponVO;
import com.liyun.promotion.enums.ExchangeCodeStatus;
import com.liyun.promotion.enums.UserCouponStatus;
import com.liyun.promotion.exception.BadRequestException;
import com.liyun.promotion.exception.BizIllegalException;
import com.liyun.promotion.mapper.CouponMapper;
import com.liyun.promotion.mapper.UserCouponMapper;
import com.liyun.promotion.mq.CouponMqSender;
import com.liyun.promotion.query.UserCouponQuery;
import com.liyun.promotion.service.IExchangeCodeService;
import com.liyun.promotion.service.IUserCouponService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liyun.promotion.utils.CodeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserCouponServiceImpl extends ServiceImpl<UserCouponMapper, UserCoupon> implements IUserCouponService {

    private final CouponMapper couponMapper;
    private final IExchangeCodeService codeService;
    private final RedissonClient redissonClient;
    private final CouponMqSender couponMqSender;

    @Override
    public void receiveCoupon(Long couponId) {
        // 1.查询优惠券
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null) {
            throw new BadRequestException("优惠券不存在");
        }

        // 2.校验发放时间
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(coupon.getIssueBeginTime()) || now.isAfter(coupon.getIssueEndTime())) {
            throw new BadRequestException("优惠券不在发放时间范围内");
        }

        // 3.校验库存
        if (coupon.getIssueNum() >= coupon.getTotalNum()) {
            throw new BadRequestException("优惠券库存不足");
        }

        Long userId = UserContext.getUserId();
        String key = "lock:coupon:uid:" + userId;
        RLock lock = redissonClient.getLock(key);
        boolean isLock = lock.tryLock();
        if (!isLock) {
            throw new BizIllegalException("请求频繁");
        }
        try {
            IUserCouponService userCouponService = (IUserCouponService) AopContext.currentProxy();
            userCouponService.checkAndCreateUserCoupon(userId, coupon);
        } finally {
            lock.unlock();
        }
    }

    @Transactional
    @Override
    public void checkAndCreateUserCoupon(Long userId, Coupon coupon) {
        // 1.校验每人限领数量
        long count = lambdaQuery()
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getCouponId, coupon.getId())
                .count();
        if (count >= coupon.getUserLimit()) {
            throw new BadRequestException("您已经领取过该优惠券");
        }

        // 2.更新优惠券已发放数量+1
        int updateResult = couponMapper.incrIssueNum(coupon.getId());
        if (updateResult == 0) {
            throw new BizIllegalException("优惠券库存不足");
        }

        // 3.新增用户券
        saveUserCoupon(coupon, userId);
    }

    @Override
    public void exchangeCoupon(String code) {
        // 1.校验并解析兑换码
        long serialNum = CodeUtil.parseCode(code);

        // 2.校验是否已兑换（bitmap CAS）
        boolean exchanged = codeService.updateExchangeMark(serialNum, true);
        if (exchanged) {
            throw new BizIllegalException("兑换码已兑换");
        }

        try {
            // 3.查询兑换码
            ExchangeCode exchangeCode = codeService.getById((int) serialNum);
            if (exchangeCode == null) {
                throw new BizIllegalException("兑换码不存在");
            }

            // 4.校验是否过期
            if (exchangeCode.getExpiredTime().isBefore(LocalDateTime.now())) {
                throw new BizIllegalException("兑换码已过期");
            }

            // 5.查询优惠券
            Coupon coupon = couponMapper.selectById(exchangeCode.getExchangeTargetId());
            if (coupon == null) {
                throw new BizIllegalException("兑换码对应的优惠券不存在");
            }

            Long userId = UserContext.getUserId();
            String key = "lock:coupon:uid:" + userId;
            RLock lock = redissonClient.getLock(key);
            boolean isLock = lock.tryLock();
            if (!isLock) {
                throw new BizIllegalException("请求太频繁，请稍后再试");
            }
            try {
                IUserCouponService userCouponService = (IUserCouponService) AopContext.currentProxy();
                // 6.校验限领、扣库存、生成用户券
                userCouponService.checkAndCreateUserCoupon(userId, coupon);
                // 7.更新兑换码状态
                codeService.lambdaUpdate()
                        .set(ExchangeCode::getUserId, userId)
                        .set(ExchangeCode::getStatus, ExchangeCodeStatus.USED)
                        .eq(ExchangeCode::getId, (int) serialNum)
                        .update();
            } finally {
                lock.unlock();
            }
        } catch (Exception e) {
            codeService.updateExchangeMark(serialNum, false);
            throw e;
        }
    }

    @Override
    public PageDTO<UserCouponVO> pageQueryUserCoupons(UserCouponQuery query) {
        Long userId = UserContext.getUserId();
        Page<UserCoupon> page = lambdaQuery()
                .eq(query.getStatus() != null, UserCoupon::getStatus, query.getStatus())
                .eq(UserCoupon::getUserId, userId)
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        List<UserCoupon> records = page.getRecords();
        if (records.isEmpty()) {
            return PageDTO.empty(page);
        }
        List<Long> couponIds = records.stream().map(UserCoupon::getCouponId).collect(Collectors.toList());
        Map<Long, Coupon> couponMap = couponMapper.selectBatchIds(couponIds)
                .stream().collect(Collectors.toMap(Coupon::getId, c -> c));
        List<UserCouponVO> voList = records.stream().map(uc -> {
            Coupon coupon = couponMap.get(uc.getCouponId());
            UserCouponVO vo = BeanUtils.copyBean(uc, UserCouponVO.class);
            vo.setSpecific(coupon.getSpecific());
            vo.setName(coupon.getName());
            vo.setDiscountType(coupon.getDiscountType());
            vo.setThresholdAmount(coupon.getThresholdAmount());
            vo.setDiscountValue(coupon.getDiscountValue());
            vo.setMaxDiscountAmount(coupon.getMaxDiscountAmount());
            vo.setTermDays(coupon.getTermDays());
            return vo;
        }).collect(Collectors.toList());

        return PageDTO.of(page, voList);
    }

    @Override
    public void handleExpiredCoupons() {
        LocalDateTime now = LocalDateTime.now();
        // 查询已过期但未标记的用户券
        List<UserCoupon> expiredList = lambdaQuery()
                .eq(UserCoupon::getStatus, UserCouponStatus.UNUSED)
                .le(UserCoupon::getTermEndTime, now)
                .list();

        if (expiredList.isEmpty()) {
            log.debug("没有需要标记过期的用户券");
            return;
        }

        log.info("扫描到 {} 张过期用户券，开始批量标记", expiredList.size());
        for (UserCoupon uc : expiredList) {
            uc.setStatus(UserCouponStatus.EXPIRED);
        }
        updateBatchById(expiredList);

        // 发送MQ过期事件
        for (UserCoupon uc : expiredList) {
            couponMqSender.sendCouponIssueEvent(
                    new CouponEventDTO(uc.getUserId(), uc.getCouponId(), uc.getId(), "EXPIRE"));
        }
        log.info("批量标记过期用户券完成，共 {} 张", expiredList.size());
    }

    private void saveUserCoupon(Coupon coupon, Long userId) {
        UserCoupon uc = new UserCoupon();
        uc.setUserId(userId);
        uc.setCouponId(coupon.getId());

        LocalDateTime termBeginTime = coupon.getTermBeginTime();
        LocalDateTime termEndTime = coupon.getTermEndTime();
        if (termBeginTime == null) {
            termBeginTime = LocalDateTime.now();
            termEndTime = termBeginTime.plusDays(coupon.getTermDays());
        }
        uc.setTermBeginTime(termBeginTime);
        uc.setTermEndTime(termEndTime);
        uc.setStatus(UserCouponStatus.UNUSED);
        save(uc);

        // 发送MQ消息
        couponMqSender.sendCouponIssueEvent(
                new CouponEventDTO(userId, coupon.getId(), uc.getId(), "ISSUE"));
    }
}
