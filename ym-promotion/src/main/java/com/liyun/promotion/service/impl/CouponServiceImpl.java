package com.liyun.promotion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.liyun.common.utils.BeanUtils;
import com.liyun.common.utils.CollUtils;
import com.liyun.common.utils.PageDTO;
import com.liyun.common.utils.StringUtils;
import com.liyun.common.context.UserContext;
import com.liyun.promotion.domain.dto.CouponFormDTO;
import com.liyun.promotion.domain.dto.CouponIssueFormDTO;
import com.liyun.promotion.domain.po.Coupon;
import com.liyun.promotion.domain.po.CouponScope;
import com.liyun.promotion.domain.po.UserCoupon;
import com.liyun.promotion.domain.vo.*;
import com.liyun.promotion.enums.CouponStatus;
import com.liyun.promotion.enums.ObtainType;
import com.liyun.promotion.enums.UserCouponStatus;
import com.liyun.promotion.exception.BadRequestException;
import com.liyun.promotion.exception.BizIllegalException;
import com.liyun.promotion.mapper.CouponMapper;
import com.liyun.promotion.query.CouponQuery;
import com.liyun.promotion.service.ICouponScopeService;
import com.liyun.promotion.service.ICouponService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liyun.promotion.service.IExchangeCodeService;
import com.liyun.promotion.service.IUserCouponService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxl.job.core.context.XxlJobHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.liyun.promotion.enums.CouponStatus.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CouponServiceImpl extends ServiceImpl<CouponMapper, Coupon> implements ICouponService {

    private final ICouponScopeService scopeService;
    private final IExchangeCodeService codeService;
    private final IUserCouponService userCouponService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /** 清理已结束优惠券的保留天数（默认7天） */
    @Value("${coupon.cleanup.days:7}")
    private int cleanupDays;

    private static final String COUPON_ISSUING_CACHE_KEY = "cache:coupon:issuing";
    private static final long COUPON_CACHE_TTL = 5; // 分钟

    @Override
    @Transactional
    public void saveCoupon(CouponFormDTO dto) {
        // 1.保存优惠券信息
        Coupon coupon = BeanUtils.copyBean(dto, Coupon.class);
        save(coupon);

        // 2.保存限定范围
        if (!dto.getSpecific()) {
            return;
        }
        Long couponId = coupon.getId();
        List<Long> scopes = dto.getScopes();
        if (CollUtils.isEmpty(scopes)) {
            throw new RuntimeException("限定范围不能为空");
        }
        List<CouponScope> list = scopes.stream()
                .map(bizId -> new CouponScope().setBizId(bizId).setCouponId(couponId))
                .collect(Collectors.toList());
        scopeService.saveBatch(list);
    }

    @Override
    public PageDTO<CouponPageVO> pageQueryCoupon(CouponQuery query) {
        String name = query.getName();
        Integer status = query.getStatus();
        Integer type = query.getType();

        Page<Coupon> page = lambdaQuery()
                .eq(type != null, Coupon::getDiscountType, type)
                .eq(status != null, Coupon::getStatus, status)
                .like(StringUtils.isNotBlank(name), Coupon::getName, name)
                .page(query.toMpPageDefaultSortByCreateTimeDesc());

        List<Coupon> records = page.getRecords();
        if (CollUtils.isEmpty(records)) {
            return PageDTO.empty(page);
        }
        List<CouponPageVO> vo = BeanUtils.copyList(records, CouponPageVO.class);
        return PageDTO.of(page, vo);
    }

    @Transactional
    @Override
    public void beginIssue(CouponIssueFormDTO dto) {
        // 1.查询优惠券
        Coupon coupon = getById(dto.getId());
        if (coupon == null) {
            throw new BadRequestException("优惠券不存在！");
        }
        // 2.判断优惠券状态
        if (coupon.getStatus() != CouponStatus.DRAFT && coupon.getStatus() != PAUSE) {
            throw new BizIllegalException("优惠券状态错误！");
        }
        // 3.判断是否立刻发放
        LocalDateTime issueBeginTime = dto.getIssueBeginTime();
        LocalDateTime now = LocalDateTime.now();
        boolean isBegin = issueBeginTime == null || !issueBeginTime.isAfter(now);
        // 4.更新优惠券
        Coupon c = BeanUtils.copyBean(dto, Coupon.class);
        if (isBegin) {
            c.setStatus(ISSUING);
            c.setIssueBeginTime(now);
        } else {
            c.setStatus(UN_ISSUE);
        }
        updateById(c);

        // 5.判断是否需要生成兑换码
        if (coupon.getObtainWay() == ObtainType.ISSUE && coupon.getStatus() == CouponStatus.DRAFT) {
            coupon.setIssueEndTime(c.getIssueEndTime());
            codeService.asynGenerateCodes(coupon);
        }
        // 清除缓存
        clearCouponCache();
    }

    @Override
    @Transactional
    public void updateCoupon(Long id, CouponFormDTO dto) {
        Coupon coupon = getById(id);
        if (coupon == null) {
            throw new RuntimeException("优惠券不存在");
        }
        if (coupon.getStatus() == CouponStatus.DRAFT) {
            Coupon result = BeanUtils.copyBean(dto, Coupon.class);
            result.setId(id);
            updateById(result);
            // 先删除旧的 scope
            scopeService.remove(new LambdaQueryWrapper<CouponScope>()
                    .eq(CouponScope::getCouponId, id));
            // 再根据 dto 重新保存
            if (dto.getSpecific() && CollUtils.isNotEmpty(dto.getScopes())) {
                List<CouponScope> list = dto.getScopes().stream()
                        .map(bizId -> new CouponScope().setBizId(bizId).setCouponId(id))
                        .collect(Collectors.toList());
                scopeService.saveBatch(list);
            }
        } else {
            throw new RuntimeException("优惠券状态错误");
        }
    }

    @Override
    @Transactional
    public void removeByIdAndCouponScope(Long id) {
        Coupon c = getById(id);
        if (c == null) {
            throw new RuntimeException("优惠券不存在");
        }
        if (c.getStatus() != CouponStatus.DRAFT) {
            throw new RuntimeException("优惠券状态错误");
        }
        removeById(id);
        scopeService.remove(new LambdaQueryWrapper<CouponScope>().eq(CouponScope::getCouponId, id));
    }

    @Override
    public CouponDetailVO getCouponById(Long id) {
        Coupon coupon = getById(id);
        if (coupon == null) {
            throw new RuntimeException("优惠券不存在");
        }
        CouponDetailVO vo = BeanUtils.copyBean(coupon, CouponDetailVO.class);
        List<CouponScope> scopeList = scopeService.lambdaQuery().eq(CouponScope::getCouponId, id).list();
        if (CollUtils.isNotEmpty(scopeList)) {
            List<CouponScopeVO> scopeVO = BeanUtils.copyList(scopeList, CouponScopeVO.class);
            vo.setScopes(scopeVO);
        }
        return vo;
    }

    @Override
    public void onTimeBeginIssue() {
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();

        LocalDateTime now = LocalDateTime.now();
        List<Coupon> list = lambdaQuery()
                .eq(Coupon::getStatus, UN_ISSUE)
                .le(Coupon::getIssueBeginTime, now)
                .apply("id % {0} = {1}", shardTotal, shardIndex)
                .list();

        if (CollUtils.isEmpty(list)) {
            return;
        }
        list.forEach(coupon -> coupon.setStatus(ISSUING));
        updateBatchById(list);
        // 清除缓存，下次查询时重新加载
        clearCouponCache();
    }

    @Override
    @Transactional
    public void onTimeEndIssue() {
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();

        LocalDateTime now = LocalDateTime.now();
        List<Coupon> list = lambdaQuery()
                .eq(Coupon::getStatus, ISSUING)
                .le(Coupon::getIssueEndTime, now)
                .apply("id % {0} = {1}", shardTotal, shardIndex)
                .list();

        if (CollUtils.isEmpty(list)) {
            return;
        }
        list.forEach(coupon -> coupon.setStatus(CouponStatus.FINISHED));
        updateBatchById(list);
        // 清除缓存
        clearCouponCache();
    }

    private void clearCouponCache() {
        redisTemplate.delete(COUPON_ISSUING_CACHE_KEY);
        log.debug("清除发放中优惠券缓存");
    }

    @Override
    public void pauseIssue(Long id) {
        Coupon c = getById(id);
        if (c == null) {
            throw new RuntimeException("优惠券不存在");
        }
        if (c.getStatus() != ISSUING) {
            throw new RuntimeException("优惠券状态错误");
        }
        c.setStatus(PAUSE);
        updateById(c);
        clearCouponCache();
    }

    @Override
    public List<CouponVO> queryIssuingCoupon() {
        // 1. 查询发放中的优惠券（尝试从缓存获取 coupon 列表）
        List<Coupon> coupons = getIssuingCouponsFromCache();
        if (CollUtils.isEmpty(coupons)) {
            return CollUtils.emptyList();
        }

        // 2. 查询当前用户已领取的券（用户相关，不缓存）
        List<Long> couponIds = coupons.stream().map(Coupon::getId).collect(Collectors.toList());
        List<UserCoupon> userCoupons = userCouponService.lambdaQuery()
                .eq(UserCoupon::getUserId, UserContext.getUserId())
                .in(UserCoupon::getCouponId, couponIds)
                .list();

        Map<Long, Long> issuedMap = userCoupons.stream()
                .collect(Collectors.groupingBy(UserCoupon::getCouponId, Collectors.counting()));
        Map<Long, Long> unusedMap = userCoupons.stream()
                .filter(uc -> uc.getStatus() == UserCouponStatus.UNUSED)
                .collect(Collectors.groupingBy(UserCoupon::getCouponId, Collectors.counting()));

        List<CouponVO> list = new ArrayList<>(coupons.size());
        for (Coupon c : coupons) {
            CouponVO vo = BeanUtils.copyBean(c, CouponVO.class);
            list.add(vo);
            vo.setAvailable(c.getTotalNum() > c.getIssueNum()
                    && issuedMap.getOrDefault(c.getId(), 0L) < c.getUserLimit());
            vo.setReceived(unusedMap.getOrDefault(c.getId(), 0L) > 0);
        }

        return list;
    }

    /** 从缓存或DB获取发放中的优惠券列表 */
    private List<Coupon> getIssuingCouponsFromCache() {
        String cached = redisTemplate.opsForValue().get(COUPON_ISSUING_CACHE_KEY);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, new TypeReference<List<Coupon>>() {});
            } catch (Exception e) {
                log.warn("发放中优惠券缓存反序列化失败", e);
            }
        }
        // DB查询
        List<Coupon> coupons = lambdaQuery()
                .eq(Coupon::getStatus, ISSUING)
                .eq(Coupon::getObtainWay, ObtainType.PUBLIC)
                .list();
        // 写入缓存
        try {
            redisTemplate.opsForValue().set(COUPON_ISSUING_CACHE_KEY,
                    objectMapper.writeValueAsString(coupons), COUPON_CACHE_TTL, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("发放中优惠券缓存写入失败", e);
        }
        return coupons;
    }

    @Override
    @Transactional
    public void deleteFinishedCoupons() {
        LocalDateTime deadline = LocalDateTime.now().minusDays(cleanupDays);
        log.info("【优惠券清除】扫描条件: status=FINISHED, issueEndTime <= {} (保留{}天)", deadline, cleanupDays);

        // 1.先统计总数
        long total = lambdaQuery()
                .eq(Coupon::getStatus, FINISHED)
                .le(Coupon::getIssueEndTime, deadline)
                .count();
        log.info("【优惠券清除】符合条件的优惠券总数: {}", total);

        if (total == 0) {
            log.info("【优惠券清除】无符合条件的优惠券，跳过");
            return;
        }

        // 2.查询待删除列表
        List<Coupon> finishedList = lambdaQuery()
                .eq(Coupon::getStatus, FINISHED)
                .le(Coupon::getIssueEndTime, deadline)
                .list();

        List<Long> couponIds = finishedList.stream().map(Coupon::getId).collect(Collectors.toList());
        log.info("【优惠券清除】开始批量删除 {} 张优惠券: {}", couponIds.size(), couponIds);

        // 3.删除作用范围
        boolean scopeResult = scopeService.remove(new LambdaQueryWrapper<CouponScope>()
                .in(CouponScope::getCouponId, couponIds));
        log.info("【优惠券清除】作用范围删除结果: {}", scopeResult);

        // 4.删除用户券
        boolean userCouponResult = userCouponService.remove(new LambdaQueryWrapper<UserCoupon>()
                .in(UserCoupon::getCouponId, couponIds));
        log.info("【优惠券清除】用户券删除结果: {}", userCouponResult);

        // 5.删除兑换码
        boolean codeResult = codeService.remove(new LambdaQueryWrapper<com.liyun.promotion.domain.po.ExchangeCode>()
                .in(com.liyun.promotion.domain.po.ExchangeCode::getExchangeTargetId, couponIds));
        log.info("【优惠券清除】兑换码删除结果: {}", codeResult);

        // 6.删除优惠券
        boolean couponResult = removeBatchByIds(couponIds);
        log.info("【优惠券清除】优惠券删除结果: {}", couponResult);
        log.info("【优惠券清除】本次共清除 {} 张优惠券", couponIds.size());
    }
}
