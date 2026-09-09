package com.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员向全体用户发送官方消息请求
 */
@Data
public class BroadcastDTO {

    /** 官方消息内容 */
    @NotBlank(message = "消息内容不能为空")
    @Size(max = 500, message = "官方消息最长500个字符")
    private String content;
}
