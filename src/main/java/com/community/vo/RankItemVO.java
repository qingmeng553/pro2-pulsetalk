package com.community.vo;

import lombok.Data;

/**
 * 热度榜单条目 VO(ZSet 数据，供 ECharts/D3 渲染)
 */
@Data
public class RankItemVO {

    /** 帖子ID */
    private Long postId;

    /** 帖子标题 */
    private String title;

    /** 封面图 */
    private String cover;

    /** 分类名称 */
    private String categoryName;

    /** 热度分 score = 点赞*3 + 收藏*5 + 浏览*0.1 */
    private Double score;

    /** 点赞数 */
    private Long likeCount;

    /** 收藏数 */
    private Long collectCount;

    /** 浏览量 */
    private Long viewCount;
}
