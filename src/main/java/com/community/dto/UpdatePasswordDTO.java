package com.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改密码请求(需校验原密码)
 */
@Data
public class UpdatePasswordDTO {

    /** 原密码 */
    @NotBlank(message = "请输入原密码")
    private String oldPassword;

    /** 新密码 */
    @NotBlank(message = "请输入新密码")
    @Size(min = 6, max = 32, message = "新密码长度需在6-32位之间")
    private String newPassword;
}
