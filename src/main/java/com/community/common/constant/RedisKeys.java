package com.community.common.constant;

/**
 * Redis Key 规范常量（统一前缀 community:）
 *
 * <pre>
 * community:post:like:{postId}            Set   帖子点赞用户id集合
 * community:post:like:count:{postId}      String 帖子点赞计数
 * community:post:collect:{postId}         Set   帖子收藏用户id集合
 * community:post:collect:count:{postId}   String 帖子收藏计数
 * community:post:view:{postId}            String 帖子浏览量
 * community:post:hot:zset                 ZSet  帖子热度榜单 score=点赞*3+收藏*5+浏览*0.1
 * community:post:cache:{postId}           热点帖子详情缓存JSON
 * community:post:cache:null:{postId}      空值缓存(防缓存穿透)
 * community:limit:create:post:{userId}    发帖接口限流计数器
 * community:limit:comment:{userId}        评论接口限流计数器
 * </pre>
 *
 * <p>额外辅助键（同样 community: 前缀，由程序内部使用）：
 * <pre>
 * community:post:sync:dirty               Set 待同步计数帖子id集合(定时任务扫描后批量落库)
 * </pre>
 */
public final class RedisKeys {

    private RedisKeys() {
    }

    /** 点赞用户集合前缀 */
    private static final String POST_LIKE = "community:post:like:";
    /** 点赞计数键前缀 */
    private static final String POST_LIKE_COUNT = "community:post:like:count:";
    /** 收藏用户集合前缀 */
    private static final String POST_COLLECT = "community:post:collect:";
    /** 收藏计数键前缀 */
    private static final String POST_COLLECT_COUNT = "community:post:collect:count:";
    /** 浏览量键前缀 */
    private static final String POST_VIEW = "community:post:view:";
    /** 热点帖子详情缓存前缀 */
    private static final String POST_CACHE = "community:post:cache:";
    /** 热点帖子空值缓存前缀(防穿透) */
    private static final String POST_CACHE_NULL = "community:post:cache:null:";

    /** 发帖限流键前缀 */
    private static final String LIMIT_CREATE_POST = "community:limit:create:post:";
    /** 评论限流键前缀 */
    private static final String LIMIT_COMMENT = "community:limit:comment:";

    /** ZSet 帖子热度榜单 */
    public static final String HOT_ZSET = "community:post:hot:zset";

    /** 待同步计数脏标记集合(内部辅助) */
    public static final String SYNC_DIRTY_SET = "community:post:sync:dirty";

    public static String likeKey(long postId) {
        return POST_LIKE + postId;
    }

    public static String likeCountKey(long postId) {
        return POST_LIKE_COUNT + postId;
    }

    public static String collectKey(long postId) {
        return POST_COLLECT + postId;
    }

    public static String collectCountKey(long postId) {
        return POST_COLLECT_COUNT + postId;
    }

    public static String viewKey(long postId) {
        return POST_VIEW + postId;
    }

    public static String postCacheKey(long postId) {
        return POST_CACHE + postId;
    }

    public static String postNullCacheKey(long postId) {
        return POST_CACHE_NULL + postId;
    }

    public static String createPostLimitKey(long userId) {
        return LIMIT_CREATE_POST + userId;
    }

    public static String commentLimitKey(long userId) {
        return LIMIT_COMMENT + userId;
    }

    /** 忘记密码-查询密保问题限流键(按用户名，防账号枚举) */
    public static String forgotLimitKey(String username) {
        return "community:limit:forgot:query:" + username;
    }

    /** 忘记密码-校验密保答案限流键(按用户名，防答案暴力破解) */
    public static String forgotResetLimitKey(String username) {
        return "community:limit:forgot:reset:" + username;
    }
}
