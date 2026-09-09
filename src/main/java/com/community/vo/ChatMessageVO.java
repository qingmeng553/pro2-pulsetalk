package com.community.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 私聊消息 VO
 */
@Data
public class ChatMessageVO {

    /** 消息ID */
    private Long id;

    /** 发送方 */
    private Long fromUserId;

    /** 接收方 */
    private Long toUserId;

    /** 消息内容 */
    private String content;

    /** 发送时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
