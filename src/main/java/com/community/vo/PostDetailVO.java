package com.community.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 帖子详情 VO
 *
 * <p>说明：本对象整体可作为热点详情缓存 JSON 序列化；
 * 动态字段(viewCount/likeCount/collectCount/liked/collected)在缓存回填后每次读取实时覆盖。
 * 未登录/未知字段反序列化兼容：{@link JsonIgnoreProperties} 忽略未知键。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PostDetailVO {

    /** 帖子ID */
    private Long id;

    /** 标题 */
    private String title;

    /** Markdown 正文 */
    private String content;

    /** 配图URL列表 */
    private List<String> imgUrls;

    /** 分类ID */
    private Long categoryId;

    /** 分类名称 */
    private String categoryName;

    /** 浏览量(实时读取 Redis) */
    private Long viewCount;

    /** 点赞数(实时读取 Redis) */
    private Long likeCount;

    /** 收藏数(实时读取 Redis) */
    private Long collectCount;

    /** 当前登录用户是否已点赞 */
    private Boolean liked;

    /** 当前登录用户是否已收藏 */
    private Boolean collected;

    /** 作者信息 */
    private AuthorVO author;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
