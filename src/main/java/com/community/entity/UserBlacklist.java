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
 * 黑名单实体 user_blacklist
 *
 * <p>业务语义为“双向阻断”：A 拉黑 B 后，(A,B) 与 (B,A) 均视为已拉黑，
 * 双方无法互发私聊、会话列表互相隐藏，好友关系同时解除。
 */
@Data
@TableName("user_blacklist")
public class UserBlacklist implements Serializable {

    /** 主键(数据库自增) */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 拉黑发起方 */
    private Long userId;

    /** 被拉黑方 */
    private Long blackUserId;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
