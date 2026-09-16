package com.community.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 好友关系实体 user_friend。 */
@Data
@TableName("user_friend")
public class UserFriend implements Serializable {

    /** 关系状态：申请中 */
    public static final int STATUS_PENDING = 0;
    /** 关系状态：已通过(好友) */
    public static final int STATUS_ACCEPTED = 1;

    /** 主键(数据库自增) */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户A(申请方或好友关系持有方) */
    private Long userId;

    /** 用户B */
    private Long friendId;

    /** 0=申请中 1=已通过 */
    private Integer status;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
