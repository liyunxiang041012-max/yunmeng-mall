package com.liyun.promotion.constants;

public interface PromotionConstants {

    String COUPON_CODE_SERIAL_KEY = "coupon:code:serial";
    String COUPON_CODE_MAP_KEY = "coupon:code:map";
    String COUPON_CACHE_KEY_PREFIX = "prs:coupon:";
    String USER_COUPON_CACHE_KEY_PREFIX = "prs:user:coupon:";

    /** 兑换码格式正则 */
    String COUPON_CODE_PATTERN = "^[6CSB7H8DAKXZF3N95RTMVUQG2YE4JWPL]{10}$";
}
