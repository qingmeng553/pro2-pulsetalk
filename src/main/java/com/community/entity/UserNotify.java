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
 * 站内消息通知实体 user_notify
 *
 * <p>type 取值：
 * <ul>
 *   <li>LIKE    有人点赞了你的帖子</li>
 *   <li>COMMENT 有人评论了你的帖子</li>
 *   <li>COLLECT 有人收藏了你的帖子</li>
 *   <li>FRIEND  好友申请 / 通过了好友申请</li>
 * </ul>
 * 点赞/收藏取消时会同步删除对应的未读通知，保证消息真实反映当前状态。
 */
@Data
@TableName("user_notify")
public class UserNotify implements Serializable {

    /** 通知类型：点赞 */
    public static final String TYPE_LIKE = "LIKE";
    /** 通知类型：评论 */
    public static final String TYPE_COMMENT = "COMMENT";
    /** 通知类型：收藏 */
    public static final String TYPE_COLLECT = "COLLECT";
    /** 通知类型：好友 */
    public static final String TYPE_FRIEND = "FRIEND";
    /** 通知类型：官方消息(管理员广播给全体用户) */
    public static final String TYPE_SYSTEM = "SYSTEM";

    /** 主键(数据库自增) */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 接收人用户ID */
    private Long userId;

    /** 触发人用户ID */
    private Long actorId;

    /** 事件类型 LIKE/COMMENT/COLLECT/FRIEND */
    private String type;

    /** 关联帖子ID(点赞/评论/收藏时) */
    private Long postId;

    /** 关联评论ID(评论时) */
    private Long commentId;

    /** 内容快照(评论预览/好友提示语等) */
    private String content;

    /** 是否已读：0未读 1已读 */
    private Integer isRead;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
