package com.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户注册请求
 */
@Data
public class RegisterDTO {

    /** 登录账号：3-20位字母数字下划线 */
    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_]{3,20}$", message = "用户名需为3-20位字母/数字/下划线")
    private String username;

    /** 密码：6-32位 */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度需在6-32位之间")
    private String password;

    /** 昵称(可选，缺省时使用用户名) */
    @Size(max = 32, message = "昵称最长32个字符")
    private String nickname;
}
