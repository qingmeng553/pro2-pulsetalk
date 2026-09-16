package com.community.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 黑名单实体 user_blacklist。 */
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
