package com.community.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 换绑邮箱（需登录 + 当前密码 + 新邮箱验证码）。 */
@Data
public class RebindEmailDTO {

    @NotBlank(message = "请输入新邮箱")
    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱过长")
    private String newEmail;

    @NotBlank(message = "请输入验证码")
    @Pattern(regexp = "\\d{6}", message = "验证码为 6 位数字")
    private String code;

    @NotBlank(message = "请输入当前密码")
    @Size(min = 6, max = 32, message = "密码长度需在6-32位之间")
    private String password;
}
