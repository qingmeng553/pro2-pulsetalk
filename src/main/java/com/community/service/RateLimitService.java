package com.community.service;

import com.community.common.api.ResultCode;
import com.community.common.constant.RedisKeys;
import com.community.common.exception.BusinessException;

/**
 * 接口限流服务(基于 Redis 计数器 + 过期时间实现固定窗口限流)
 *
 * <p>用于发帖、发表评论接口，抵御刷帖/刷屏。
 */
public interface RateLimitService {

    /** 发帖限流：同一用户每分钟最多发帖数 */
    int CREATE_POST_MAX = 3;
    /** 评论限流：同一用户每分钟最多评论数 */
    int COMMENT_MAX = 10;
    /** 固定窗口时长(秒) */
    int WINDOW_SECONDS = 60;

    /**
     * 发帖限流校验：超限抛出 {@link BusinessException}(code=429)
     *
     * @param userId 用户ID
     */
    default void checkCreatePost(long userId) {
        check(RedisKeys.createPostLimitKey(userId), CREATE_POST_MAX, WINDOW_SECONDS);
    }

    /**
     * 评论限流校验：超限抛出 {@link BusinessException}(code=429)
     *
     * @param userId 用户ID
     */
    default void checkComment(long userId) {
        check(RedisKeys.commentLimitKey(userId), COMMENT_MAX, WINDOW_SECONDS);
    }

    /**
     * 固定窗口限流核心逻辑
     *
     * @param key     计数器键(community:limit:*)
     * @param max     窗口内最大次数
     * @param seconds 窗口时长(秒)
     */
    void check(String key, int max, int seconds);

    /** 统一超限提示(带 ResultCode 供文档参考) */
    ResultCode tooManyCode();
}
