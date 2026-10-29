package com.community.service;

import com.community.vo.CaptchaVO;

/** 图形验证码：服务端生成 PNG，Redis 存 2 分钟，一次性。 */
public interface CaptchaService {

    /**
     * 生成图形码（同 IP 有频率限制）
     */
    CaptchaVO generate(String ip);

    /**
     * 校验图形码（无论成功失败都销毁，避免复用）
     */
    boolean verify(String captchaId, String code);
}
