package com.community.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 私聊消息实体 chat_message
 *
 * <p>规则：
 * <ul>
 *   <li>非好友双方各自最多发送 1 条打招呼消息，成为好友后畅聊</li>
 *   <li>拉黑双方阻断互动(发送/会话均不可)</li>
 *   <li>聊天记录保留 7 天，由定时任务 ChatHistoryCleanTask 每天清理过期数据</li>
 * </ul>
 */
@Data
@TableName("chat_message")
public class ChatMessage implements Serializable {

    /** 主键(数据库自增) */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 发送方用户ID */
    private Long fromUserId;

    /** 接收方用户ID */
    private Long toUserId;

    /** 消息内容 */
    private String content;

    /** 是否已读：0未读 1已读 */
    private Integer isRead;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
