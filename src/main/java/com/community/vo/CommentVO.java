package com.community.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评论 VO
 */
@Data
public class CommentVO {

    /** 评论ID */
    private Long id;

    /** 所属帖子ID */
    private Long postId;

    /** 评论人用户ID */
    private Long userId;

    /** 评论内容 */
    private String content;

    /** 楼层号(从1递增，按分页动态计算) */
    private Integer floor;

    /** 评论人信息(含角色，便于渲染管理员徽章) */
    private AuthorVO author;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
