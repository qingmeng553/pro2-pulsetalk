package com.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 管理员重置用户密码。 */
@Data
public class AdminResetPasswordDTO {

    @NotBlank(message = "请输入新密码")
    @Size(min = 6, max = 32, message = "新密码长度需在6-32位之间")
    private String newPassword;
}
