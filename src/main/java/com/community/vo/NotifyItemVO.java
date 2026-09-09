package com.community.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站内消息通知 VO(谁赞了我/评论了我/收藏了我/加我好友)
 */
@Data
public class NotifyItemVO {

    /** 通知ID */
    private Long id;

    /** 事件类型 LIKE/COMMENT/COLLECT/FRIEND */
    private String type;

    /** 触发人(点赞/评论/收藏/好友操作方) */
    private AuthorVO actor;

    /** 关联帖子ID(点赞/评论/收藏；点击跳转帖子) */
    private Long postId;

    /** 关联帖子标题 */
    private String postTitle;

    /** 关联评论ID(评论通知可定位到楼层) */
    private Long commentId;

    /** 内容快照(评论预览等) */
    private String content;

    /** 是否已读 */
    private Boolean read;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
