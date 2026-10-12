package com.liyun.promotion.exception;

import lombok.Getter;

@Getter
public class BadRequestException extends RuntimeException {
    private final int code;

    public BadRequestException(String message) {
        super(message);
        this.code = 400;
    }

    public BadRequestException(int code, String message) {
        super(message);
        this.code = code;
    }
}
