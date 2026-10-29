package com.community.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/** 图形验证码：captchaId 用于校验，image 为 data:image/png;base64。 */
@Data
@AllArgsConstructor
public class CaptchaVO {

    private String captchaId;

    private String image;
}
