package com.liyun.common.exception;

import com.liyun.common.enums.ResultCode;
import com.liyun.common.utils.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 业务异常
    @ExceptionHandler(BizException.class)
    public Result<?> handleBizException(BizException e) {
        log.warn("业务异常：{}", e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    // RuntimeException 异常（含业务层 throw new RuntimeException("xxx")）
    @ExceptionHandler(RuntimeException.class)
    public Result<?> handleRuntimeException(RuntimeException e) {
        // 404 静默处理，不打印任何日志
        if (e instanceof ResponseStatusException responseStatusException) {
            if (responseStatusException.getStatusCode().value() == 404) {
                return Result.fail(404, "资源不存在");
            }
        }
        
        log.warn("运行异常：{}", e.getMessage());
        return Result.fail(500, e.getMessage());
    }

    // 参数校验异常
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleValidException(MethodArgumentNotValidException e) {
        BindingResult bindingResult = e.getBindingResult();
        String message = bindingResult.getFieldErrors().stream()
                .map(f -> f.getField() + ":" + f.getDefaultMessage())
                .findFirst()
                .orElse("参数校验失败");
        log.warn("参数异常：{}", message);
        return Result.fail(ResultCode.PARAM_ERROR.getCode(), message);
    }

    // 兜底异常
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("系统异常：", e);
        return Result.fail(ResultCode.FAIL.getCode(), "服务器内部错误");
    }
}
