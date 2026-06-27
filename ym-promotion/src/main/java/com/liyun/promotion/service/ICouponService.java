package com.liyun.promotion.service;

import com.liyun.common.utils.PageDTO;
import com.liyun.promotion.domain.dto.CouponFormDTO;
import com.liyun.promotion.domain.dto.CouponIssueFormDTO;
import com.liyun.promotion.domain.po.Coupon;
import com.baomidou.mybatisplus.extension.service.IService;
import com.liyun.promotion.domain.vo.CouponDetailVO;
import com.liyun.promotion.domain.vo.CouponPageVO;
import com.liyun.promotion.domain.vo.CouponVO;
import com.liyun.promotion.query.CouponQuery;

import java.util.List;

public interface ICouponService extends IService<Coupon> {

    void saveCoupon(CouponFormDTO dto);

    PageDTO<CouponPageVO> pageQueryCoupon(CouponQuery query);

    void beginIssue(CouponIssueFormDTO dto);

    void updateCoupon(Long id, CouponFormDTO dto);

    void removeByIdAndCouponScope(Long id);

    CouponDetailVO getCouponById(Long id);

    void onTimeBeginIssue();

    void onTimeEndIssue();

    void pauseIssue(Long id);

    List<CouponVO> queryIssuingCoupon();

    /** 定时删除已结束的优惠券及其关联数据 */
    void deleteFinishedCoupons();
}
