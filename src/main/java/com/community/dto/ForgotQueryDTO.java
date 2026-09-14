package com.community.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 忘记密码-查询密保问题请求
 */
@Data
public class ForgotQueryDTO {

    /** 登录账号 */
    @NotBlank(message = "请输入用户名")
    private String username;
}
