package com.community.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话列表条目 VO(私信)
 */
@Data
public class ChatThreadVO {

    /** 会话对象用户信息 */
    private AuthorVO user;

    /** 最后一条消息内容 */
    private String lastMessage;

    /** 最后消息时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastTime;

    /** 我方未读数 */
    private Long unread;

    /** 是否处于拉黑阻断状态(双向) */
    private Boolean blocked;
}
