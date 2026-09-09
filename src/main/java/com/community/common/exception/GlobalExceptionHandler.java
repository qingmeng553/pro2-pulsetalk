package com.community.common.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotRoleException;
import cn.dev33.satoken.exception.SaTokenException;
import com.community.common.api.R;
import com.community.common.api.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理器
 *
 * <p>Sa-Token 未登录(NotLoginException)/无权限(NotRoleException)在注解鉴权阶段抛出，
 * 统一在这里兜底，转换为 401/403 业务码，前端据此弹出“去登录/取消”弹窗，而不是 403 空白页。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常 */
    @ExceptionHandler(BusinessException.class)
    public R<Void> handleBusiness(BusinessException e) {
        return R.fail(e.getCode(), e.getMessage());
    }

    /** 未登录 / 登录态失效 */
    @ExceptionHandler(NotLoginException.class)
    public R<Void> handleNotLogin(NotLoginException e) {
        log.warn("[未登录] {}", e.getMessage());
        return R.fail(ResultCode.UNAUTHORIZED, ResultCode.UNAUTHORIZED.getMessage());
    }

    /** 已登录但角色权限不足 */
    @ExceptionHandler(NotRoleException.class)
    public R<Void> handleNotRole(NotRoleException e) {
        log.warn("[无角色权限] {}", e.getMessage());
        return R.fail(ResultCode.FORBIDDEN, "需要管理员权限才能执行该操作");
    }

    /** 其它 Sa-Token 异常 */
    @ExceptionHandler(SaTokenException.class)
    public R<Void> handleSaToken(SaTokenException e) {
        log.warn("[Sa-Token异常] {}", e.getMessage());
        return R.fail(ResultCode.UNAUTHORIZED);
    }

    /** @RequestBody 参数校验失败 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<Void> handleValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getField() + " " + err.getDefaultMessage())
                .orElse(ResultCode.BAD_REQUEST.getMessage());
        return R.fail(ResultCode.BAD_REQUEST, msg);
    }

    /** 表单绑定校验失败 */
    @ExceptionHandler(BindException.class)
    public R<Void> handleBind(BindException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getField() + " " + err.getDefaultMessage())
                .orElse(ResultCode.BAD_REQUEST.getMessage());
        return R.fail(ResultCode.BAD_REQUEST, msg);
    }

    /** 缺少请求参数 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public R<Void> handleMissingParam(MissingServletRequestParameterException e) {
        return R.fail(ResultCode.BAD_REQUEST, "缺少必要参数: " + e.getParameterName());
    }

    /** 请求体不可读(JSON 格式错误等) */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public R<Void> handleNotReadable(HttpMessageNotReadableException e) {
        return R.fail(ResultCode.BAD_REQUEST, "请求体格式错误");
    }

    /** 上传文件超过限制 */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public R<Void> handleMaxUpload(MaxUploadSizeExceededException e) {
        return R.fail(ResultCode.BAD_REQUEST, "上传文件过大，单文件请控制在 5MB 以内");
    }

    /** 资源不存在(Spring6 静态资源) */
    @ExceptionHandler(NoResourceFoundException.class)
    public R<Void> handleNoResource(NoResourceFoundException e) {
        return R.fail(ResultCode.NOT_FOUND);
    }

    /** 请求方式不支持 */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public R<Void> handleMethodNotSupport(HttpRequestMethodNotSupportedException e) {
        return R.fail(ResultCode.BAD_REQUEST, "不支持的请求方式: " + e.getMethod());
    }

    /** 其它未知异常兜底 */
    @ExceptionHandler(Exception.class)
    public R<Void> handleException(Exception e) {
        log.error("[系统异常]", e);
        return R.fail(ResultCode.ERROR, "系统繁忙，请稍后再试");
    }
}
