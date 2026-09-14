package com.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 设置密保问题请求(问题与答案均由用户自定义)
 */
@Data
public class SecurityQuestionDTO {

    /** 密保问题 */
    @NotBlank(message = "请输入密保问题")
    @Size(max = 100, message = "密保问题最长100个字符")
    private String question;

    /** 密保答案(加密存储，不区分大小写) */
    @NotBlank(message = "请输入密保答案")
    @Size(max = 64, message = "密保答案最长64个字符")
    private String answer;
}
