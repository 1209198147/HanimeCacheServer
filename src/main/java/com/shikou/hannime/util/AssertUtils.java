package com.shikou.hannime.util;

import com.shikou.hannime.exception.BizException;
import com.shikou.hannime.exception.ErrorCode;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.Objects;

public class AssertUtils {
    public static void isTrue(boolean condition, String message) {
        if (condition) {
            throw new BizException(message);
        }
    }
    public static void isFalse(boolean condition, String message) {
        if (!condition) {
            throw new BizException(message);
        }
    }

    public static void isNull(Object object, String message) {
        if (Objects.nonNull(object)) {
            throw new BizException(message);
        }
    }

    public static void nonNull(Object object, String message) {
        if (Objects.isNull(object)) {
            throw new BizException(message);
        }
    }
    public static void nonNull(Object object, ErrorCode code, String message) {
        if (Objects.isNull(object)) {
            throw new BizException(code, message);
        }
    }


    public static void isNotEmpty(String str, String message) {
        if (StringUtils.isEmpty(str)) {
            throw new BizException(message);
        }
    }

    public static void isNotEmpty(Collection<?> collection, String message) {
        if (CollectionUtils.isEmpty(collection)) {
            throw new BizException(message);
        }
    }

    public static void isNotBlank(String str, String message) {
        if (StringUtils.isBlank(str)) {
            throw new BizException(message);
        }
    }

    public static void fail(ErrorCode code) {
        throw new BizException(code);
    }

    public static void fail(String message) {
        throw new BizException(message);
    }

    public static void fail(ErrorCode code, String message) {
        throw new BizException(code, message);
    }
}
