package com.shikou.hannime.exception;

import lombok.Getter;

@Getter
public class BizException extends RuntimeException {
    ErrorCode errorCode;

    public BizException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BizException(String message) {
        super(message);
        this.errorCode = ErrorCode.FAIL;
    }

    public BizException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
