package com.community.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 发送邮箱验证码：scene = bind | rebind | reset。 */
@Data
public class SendCodeDTO {

    @NotBlank(message = "请输入邮箱")
    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱过长")
    private String email;

    /** bind 绑定 / rebind 换绑 / reset 找回 */
    @NotBlank(message = "缺少场景")
    @Pattern(regexp = "bind|rebind|reset", message = "场景不合法")
    private String scene;

    @NotBlank(message = "图形验证码不能为空")
    private String captchaId;

    @NotBlank(message = "请输入图形验证码")
    private String captchaCode;
}
