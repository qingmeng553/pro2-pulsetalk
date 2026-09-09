package com.community.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 帖子评论实体 post_comment（一级评论，不做楼中楼嵌套）
 */
@Data
@TableName("post_comment")
public class PostComment implements Serializable {

    /** 主键(数据库自增) */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属帖子ID(post.id) */
    private Long postId;

    /** 评论人用户ID(sys_user.id) */
    private Long userId;

    /** 评论内容 */
    private String content;

    /** 逻辑删除标记 0未删/1已删 */
    @TableLogic
    private Integer isDeleted;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
