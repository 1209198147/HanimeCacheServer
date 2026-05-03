package com.shikou.hannime.entities.response;

import com.shikou.hannime.exception.ErrorCode;
import lombok.Data;

@Data
public class Result {
    private int code;
    private String message;
    private Object data;

    public Result(int code, String message, Object data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public Result(ErrorCode code, String message, Object data) {
        this.code = code.getCode();
        this.message = message;
        this.data = data;
    }

    public static Result success() {
        return new Result(ErrorCode.SUCCESS, "success", null);
    }

    public static Result success(Object data) {
        return new Result(ErrorCode.SUCCESS, "success", data);
    }

    public static Result success(String message, Object data) {
        return new Result(ErrorCode.SUCCESS, message, data);
    }

    public static Result fail() {
        return new Result(ErrorCode.FAIL, ErrorCode.FAIL.getMessage(), null);
    }

    public static Result fail(String message) {
        return new Result(ErrorCode.FAIL, message, null);
    }

    public static Result error(ErrorCode code) {
        return new Result(code, code.getMessage(), null);
    }
    public static Result error(ErrorCode code, String message) {
        return new Result(code, message, null);
    }
}
