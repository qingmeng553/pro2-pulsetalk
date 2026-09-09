package com.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发送私聊消息请求
 */
@Data
public class ChatSendDTO {

    /** 消息内容 */
    @NotBlank(message = "消息内容不能为空")
    @Size(max = 2000, message = "单条消息最长2000个字符")
    private String content;
}
