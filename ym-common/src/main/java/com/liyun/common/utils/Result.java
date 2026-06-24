package com.liyun.common.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.liyun.common.enums.ResultCode;
import lombok.Data;

import java.io.Serializable;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL) // data为null时不返回给前端，报文更干净
public class Result<T> implements Serializable {
    private Integer code;
    private String message;
    private T data;
    private Long timestamp;

    private Result() {
        this.timestamp = System.currentTimeMillis();
    }

    public boolean isSuccess() {
        return this.code != null && this.code.equals(ResultCode.SUCCESS.getCode());
    }

    public static <T> Result<T> success() {
        Result<T> r = new Result<>();
        r.setCode(ResultCode.SUCCESS.getCode());
        r.setMessage(ResultCode.SUCCESS.getMessage());
        return r;
    }

    public static <T> Result<T> success(T data) {
        Result<T> r = success();
        r.setData(data);
        return r;
    }

    public static <T> Result<T> fail(ResultCode resultCode) {
        Result<T> r = new Result<>();
        r.setCode(resultCode.getCode());
        r.setMessage(resultCode.getMessage());
        return r;
    }

    public static <T> Result<T> fail(int code, String message) {
        Result<T> r = new Result<>();
        r.setCode(code);
        r.setMessage(message);
        return r;
    }

    public static <T> Result<T> fail(String message) {
        return fail(ResultCode.FAIL.getCode(), message);
    }
}
