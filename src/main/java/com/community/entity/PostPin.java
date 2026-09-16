package com.community.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 置顶规则帖实体 post_pin（单行记录，id 固定为 1）。 */
@Data
@TableName("post_pin")
public class PostPin implements Serializable {

    /** 固定主键：全站唯一一行 */
    public static final int PIN_ID = 1;

    /** 主键(固定为1，应用层手动赋值) */
    @TableId(type = IdType.INPUT)
    private Integer id;

    /** 置顶帖ID(post.id) */
    private Long postId;

    /** 最近操作的管理员ID(sys_user.id) */
    private Long adminId;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
