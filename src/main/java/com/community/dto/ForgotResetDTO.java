package com.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 忘记密码-通过密保答案重置密码请求
 */
@Data
public class ForgotResetDTO {

    /** 登录账号 */
    @NotBlank(message = "请输入用户名")
    private String username;

    /** 密保答案 */
    @NotBlank(message = "请输入密保答案")
    private String answer;

    /** 新密码 */
    @NotBlank(message = "请输入新密码")
    @Size(min = 6, max = 32, message = "新密码长度需在6-32位之间")
    private String newPassword;
}
