package com.community.service;

import com.community.dto.BindEmailDTO;
import com.community.dto.RebindEmailDTO;
import com.community.dto.ResetByCodeDTO;
import com.community.dto.SendCodeDTO;

/** 邮箱绑定 / 换绑 / 邮箱找回（alpha2）。 */
public interface EmailAuthService {

    /**
     * 发送验证码（所有场景都先过图形码 + 双限流 + 失败锁定）
     *
     * @param dto 邮箱 + 场景 + 图形码
     * @param ip  客户端 IP
     */
    void sendCode(SendCodeDTO dto, String ip);

    /**
     * 首次绑定邮箱（登录 + 当前密码 + 验证码）
     */
    void bind(BindEmailDTO dto);

    /**
     * 换绑邮箱（登录 + 当前密码 + 新邮箱验证码；旧邮箱收通知）
     */
    void rebind(RebindEmailDTO dto);

    /**
     * 邮箱验证码重置密码（公开；成功后踢掉该用户全部会话并通知）
     */
    void resetByCode(ResetByCodeDTO dto);
}
