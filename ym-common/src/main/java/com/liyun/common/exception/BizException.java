package com.liyun.common.exception;

import com.liyun.common.enums.ResultCode;
import lombok.Getter;

@Getter
public class BizException extends RuntimeException {

    private final int code;
    private final String message;

    public BizException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
        this.message = resultCode.getMessage();
    }

}