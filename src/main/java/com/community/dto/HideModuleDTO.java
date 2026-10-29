package com.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 管理员隐藏/恢复主页模块。 */
@Data
public class HideModuleDTO {

    @NotBlank(message = "缺少模块类型")
    @Size(max = 20)
    private String moduleType;

    /** true=隐藏，false=恢复 */
    private Boolean hidden;

    @Size(max = 200, message = "原因最多 200 字")
    private String reason;
}
