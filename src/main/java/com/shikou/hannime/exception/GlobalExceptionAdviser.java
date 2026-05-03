package com.shikou.hannime.exception;

import com.shikou.hannime.entities.response.Result;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionAdviser {
    @ExceptionHandler(Exception.class)
    public Result handleException(Exception e) {
        return Result.error(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(BizException.class)
    public Result handleException(BizException e) {
        return Result.error(e.getErrorCode(), e.getMessage());
    }
}
