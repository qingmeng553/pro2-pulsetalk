package com.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改昵称(改名)请求
 */
@Data
public class UpdateProfileDTO {

    /** 新昵称 */
    @NotBlank(message = "昵称不能为空")
    @Size(max = 32, message = "昵称最长32个字符")
    private String nickname;
}
