package com.shikou.hannime.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
    SUCCESS(200, "Success"),
    BAD_REQUEST(400, "Bad Request"),
    UNAUTHORIZED(401, "Unauthorized"),
    FORBIDDEN(403, "Forbidden"),
    NOT_FOUND(404, "Not Found"),
    FAIL(500, "Fail"),
    INTERNAL_SERVER_ERROR(501, "Internal Server Error"),
    TASK_NOT_FOUND(604, "Task Not Found"),
    TASK_ENGINE_STOPPED(605, "Task Engine Stopped");

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
    private final int code;
    private final String message;
}
