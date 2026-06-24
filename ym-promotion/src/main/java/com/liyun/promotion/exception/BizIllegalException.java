package com.liyun.promotion.exception;

import lombok.Getter;

@Getter
public class BizIllegalException extends RuntimeException {
    private final int code;

    public BizIllegalException(String message) {
        super(message);
        this.code = 500;
    }

    public BizIllegalException(int code, String message) {
        super(message);
        this.code = code;
    }
}
