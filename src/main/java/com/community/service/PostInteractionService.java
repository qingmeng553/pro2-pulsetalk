package com.community.service;

import com.community.vo.InteractionVO;

/** 点赞/收藏服务。 */
public interface PostInteractionService {

    /**
     * 点赞/取消点赞(切换式)：Redis Set 记录用户id
     *
     * @param postId 帖子ID
     * @param userId 当前用户ID
     * @return 最新状态与计数
     */
    InteractionVO toggleLike(long postId, long userId);

    /**
     * 收藏/取消收藏(切换式)：Redis Set 记录用户id
     *
     * @param postId 帖子ID
     * @param userId 当前用户ID
     * @return 最新状态与计数
     */
    InteractionVO toggleCollect(long postId, long userId);

    /** 实时点赞数(Redis Set size) */
    long likeCount(long postId);

    /** 实时收藏数(Redis Set size) */
    long collectCount(long postId);

    /** 实时浏览量(Redis String，无则0) */
    long viewCount(long postId);

    /** 用户是否已点赞 */
    boolean isLiked(long postId, long userId);

    /** 用户是否已收藏 */
    boolean isCollected(long postId, long userId);
}
