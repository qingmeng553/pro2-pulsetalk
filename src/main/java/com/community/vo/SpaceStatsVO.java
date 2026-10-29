package com.community.vo;

import lombok.Data;

/** 主页统计：发帖 / 获赞 / 好友 / 访问量。 */
@Data
public class SpaceStatsVO {

    private Long postCount = 0L;

    private Long likeCount = 0L;

    private Long friendCount = 0L;

    private Long viewCount = 0L;
}
