package com.liyun.promotion.mapper;

import com.liyun.promotion.domain.po.Coupon;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface CouponMapper extends BaseMapper<Coupon> {

    @Update("update coupon set issue_num = issue_num + 1 where id = #{couponId} and issue_num < total_num")
    int incrIssueNum(@Param("couponId") Long couponId);
}
