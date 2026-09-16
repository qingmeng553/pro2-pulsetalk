package com.community.vo;

import lombok.Data;

/** 密保问题状态 VO。 */
@Data
public class SecurityQuestionVO {

    /** 是否已设置密保问题 */
    private Boolean hasQuestion;

    /** 密保问题文本(未设置时为 null) */
    private String question;
}
