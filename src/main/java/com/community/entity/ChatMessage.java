package com.community.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 私聊消息实体 chat_message。 */
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
