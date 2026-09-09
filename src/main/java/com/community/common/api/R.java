package com.community.common.api;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应体
 *
 * @param <T> 业务数据类型
 */
@Data
public class R<T> implements Serializable {

    /** 业务状态码：200 成功 / 401 未登录 / 403 无权限 / 404 不存在 / 429 限流 / 500 异常 */
    private Integer code;

    /** 提示信息 */
    private String message;

    /** 业务数据 */
    private T data;

    public static <T> R<T> ok() {
        return build(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }

    public static <T> R<T> ok(T data) {
        return build(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    public static <T> R<T> fail(ResultCode resultCode) {
        return build(resultCode.getCode(), resultCode.getMessage(), null);
    }

    public static <T> R<T> fail(ResultCode resultCode, String message) {
        return build(resultCode.getCode(), message, null);
    }

    public static <T> R<T> fail(int code, String message) {
        return build(code, message, null);
    }

    private static <T> R<T> build(int code, String message, T data) {
        R<T> r = new R<>();
        r.setCode(code);
        r.setMessage(message);
        r.setData(data);
        return r;
    }
}
