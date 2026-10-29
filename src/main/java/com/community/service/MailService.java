package com.community.service;

/** 邮件发送：验证码 + 安全通知。 */
public interface MailService {

    /**
     * 发送邮箱验证码
     *
     * @param to    收件邮箱
     * @param code  6 位验证码
     * @param scene bind / rebind / reset
     */
    void sendCode(String to, String code, String scene);

    /**
     * 发送安全通知（改密/换绑等）
     */
    void sendNotice(String to, String subject, String content);
}
