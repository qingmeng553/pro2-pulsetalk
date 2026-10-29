package com.community.service.impl;

import com.community.common.api.ResultCode;
import com.community.common.exception.BusinessException;
import com.community.service.MailService;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/** 邮件发送实现：验证码 + 安全通知。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:PulseTalk <no-reply@pulsetalk.local>}")
    private String from;

    @Override
    public void sendCode(String to, String code, String scene) {
        String sceneText = switch (scene) {
            case "bind" -> "绑定邮箱";
            case "rebind" -> "更换邮箱";
            default -> "找回密码";
        };
        String body = "你的 PulseTalk " + sceneText + "验证码：" + code + "\n\n"
                + "5 分钟内有效，仅可使用一次，请勿泄露给他人。\n"
                + "如非本人操作，请忽略本邮件。\n\n"
                + "PulseTalk";
        send(to, "PulseTalk 验证码", body);
        log.info("[邮件] 验证码已发送 scene={} to={}", scene, mask(to));
    }

    @Override
    public void sendNotice(String to, String subject, String content) {
        send(to, subject, content + "\n\nPulseTalk");
        log.info("[邮件] 安全通知已发送 to={} subject={}", mask(to), subject);
    }

    private void send(String to, String subject, String text) {
        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, false);
            mailSender.send(message);
        } catch (MessagingException | MailException e) {
            log.error("[邮件] 发送失败 to={} subject={} err={}", mask(to), subject, e.getMessage());
            throw new BusinessException(ResultCode.ERROR, "邮件发送失败，请稍后再试");
        }
    }

    /** 日志脱敏：q***@163.com */
    private String mask(String email) {
        if (email == null) {
            return "";
        }
        int at = email.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        String local = email.substring(0, at);
        String domain = email.substring(at);
        if (local.length() <= 2) {
            return local.charAt(0) + "***" + domain;
        }
        return local.charAt(0) + "***" + domain;
    }
}
