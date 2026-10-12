package com.liyun.common.enums;


import lombok.Getter;

@Getter
public enum ResultCode {
    SUCCESS(200, "操作成功"),
    FAIL(500, "操作失败"),
    PARAM_ERROR(400, "参数校验失败"),
    UNAUTHORIZED(401, "未登录或Token过期"),
    FORBIDDEN(403, "权限不足"),
    NOT_FOUND(404, "资源不存在"),

    // 用户业务码（1000起步，避免与HTTP状态码冲突）
    USER_EXIST(1001, "用户已存在"),
    USER_NOT_EXIST(1002, "用户不存在"),
    PASSWORD_ERROR(1003, "密码错误"),
    ACCOUNT_DISABLED(1004, "账号已被禁用"),

    // 订单业务码（2000起步）
    ORDER_ITEMS_EMPTY(2001, "订单商品列表不能为空"),
    SKU_QUERY_FAILED(2002, "查询商品信息失败"),
    SKU_NOT_FOUND(2003, "商品不存在"),
    STOCK_NOT_ENOUGH(2004, "商品库存不足"),
    MULTI_SHOP_NOT_ALLOWED(2005, "不支持多店铺下单");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
