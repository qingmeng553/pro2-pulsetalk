package com.community.common.api;

import lombok.Getter;

/**
 * 统一响应码枚举
 *
 * <p>约定：HTTP 状态码固定 200，业务结果通过响应体中的 {@code code} 表达，
 * 避免游客访问受限接口时浏览器出现原生 403 空白页；前端 axios 拦截器根据
 * code 弹出“去登录/取消”确认弹窗。
 */
@Getter
public enum ResultCode {

    /** 成功 */
    SUCCESS(200, "success"),

    /** 参数错误(校验失败/格式错误) */
    BAD_REQUEST(400, "请求参数错误"),

    /** 未登录或登录态已过期(前端据此弹出登录确认弹窗) */
    UNAUTHORIZED(401, "未登录或登录已过期，请先登录"),

    /** 已登录但无权限(如普通用户访问管理员接口) */
    FORBIDDEN(403, "没有权限执行该操作"),

    /** 资源不存在或已被删除 */
    NOT_FOUND(404, "资源不存在"),

    /** 接口限流触发 */
    TOO_MANY_REQUESTS(429, "操作过于频繁，请稍后再试"),

    /** 服务器内部错误 */
    ERROR(500, "服务器内部错误");

    /** 业务状态码 */
    private final int code;

    /** 默认提示信息 */
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
