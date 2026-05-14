package com.shikou.hannime.exception;

import com.shikou.hannime.entities.response.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionAdviser {

    @ExceptionHandler(NoHandlerFoundException.class)
    public Result handleNoHandlerFound(NoHandlerFoundException e) {
        log.warn("未找到处理器: {} {}", e.getHttpMethod(), e.getRequestURL());
        return Result.error(ErrorCode.NOT_FOUND, "接口不存在: " + e.getHttpMethod() + " " + e.getRequestURL());
    }

    @ExceptionHandler(BizException.class)
    public Result handleBizException(BizException e) {
        return Result.error(e.getErrorCode(), e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result handleException(Exception e) {
        log.error("未预期异常", e);
        return Result.error(ErrorCode.INTERNAL_SERVER_ERROR);
    }
}
