package com.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 举报主页/模块。 */
@Data
public class ProfileReportDTO {

    /** profile=整页，或具体模块类型 */
    @NotBlank(message = "缺少举报对象")
    @Size(max = 20)
    private String moduleType;

    @NotBlank(message = "请填写举报原因")
    @Size(max = 200, message = "举报原因最多 200 字")
    private String reason;
}
