package com.community.vo;

import lombok.Data;

/**
 * 密保问题状态 VO
 *
 * <p>hasQuestion=false 表示该账号尚未设置密保问题（忘记密码不可用）。
 * 出于安全考虑，登录后查询自身状态时会返回问题文本；游客找回流程需先提交用户名。
 */
@Data
public class SecurityQuestionVO {

    /** 是否已设置密保问题 */
    private Boolean hasQuestion;

    /** 密保问题文本(未设置时为 null) */
    private String question;
}
