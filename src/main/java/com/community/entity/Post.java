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
 * 帖子实体 post
 *
 * <p>计数说明：view/like/collect 三个计数由 Redis 记录，定时任务每 5 分钟批量同步回本表，
 * 点赞/收藏等写操作不实时写数据库。
 */
@Data
@TableName("post")
public class Post implements Serializable {

    /** 主键(数据库自增) */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 作者用户ID(system_user.id) */
    private Long userId;

    /** 分类ID(post_category.id) */
    private Long categoryId;

    /** 标题 */
    private String title;

    /** 正文(Markdown，支持 emoji) */
    private String content;

    /** 配图URL，JSON 数组字符串，如 ["http://x:9000/bucket/a.jpg"] */
    private String imgUrls;

    /** 浏览量(Redis 增量 + 定时同步) */
    private Integer viewCount;

    /** 点赞数(Redis Set size + 定时同步) */
    private Integer likeCount;

    /** 收藏数(Redis Set size + 定时同步) */
    private Integer collectCount;

    /** 逻辑删除标记 0未删/1已删(MyBatis-Plus @TableLogic 自动过滤) */
    @TableLogic
    private Integer isDeleted;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
