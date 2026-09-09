package com.community.common.exception;

import com.community.common.api.ResultCode;
import lombok.Getter;

/**
 * 业务异常：携带业务状态码，由全局异常处理器统一转为响应体
 */
@Getter
public class BusinessException extends RuntimeException {

    /** 业务状态码(默认 500) */
    private final int code;

    public BusinessException(String message) {
        super(message);
        this.code = ResultCode.ERROR.getCode();
    }

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
