package com.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 主页留言。 */
@Data
public class GuestbookDTO {

    @NotBlank(message = "留言内容不能为空")
    @Size(max = 200, message = "留言最多 200 字")
    private String content;
}
