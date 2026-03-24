package com.veingraph.common.exception;

import com.veingraph.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 * 统一捕获并返回友好错误信息，避免暴露 Whitelabel Error Page
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public Result<String> handleException(Exception e) {
        log.error("请求处理异常：", e);
        return Result.fail(500, e.getClass().getSimpleName() + ": " + e.getMessage());
    }
}
