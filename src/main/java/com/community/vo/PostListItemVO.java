package com.community.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 帖子列表项 VO(首页帖子流)
 */
@Data
public class PostListItemVO {

    /** 帖子ID */
    private Long id;

    /** 标题 */
    private String title;

    /** 分类ID */
    private Long categoryId;

    /** 分类名称 */
    private String categoryName;

    /** 封面图(取配图第一张) */
    private String cover;

    /** 浏览量 */
    private Integer viewCount;

    /** 点赞数 */
    private Integer likeCount;

    /** 收藏数 */
    private Integer collectCount;

    /** 作者信息 */
    private AuthorVO author;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
