package com.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.community.common.api.ResultCode;
import com.community.common.constant.RedisKeys;
import com.community.common.exception.BusinessException;
import com.community.entity.Post;
import com.community.mapper.PostMapper;
import com.community.service.NotifyService;
import com.community.service.PostInteractionService;
import com.community.service.RankService;
import com.community.vo.InteractionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 点赞/收藏服务实现
 *
 * <p>点赞与收藏采用同一套 Redis 模式：
 * <ul>
 *   <li>{@code community:post:like:{postId}} / {@code collect}：Set 存用户id集合(判重+计数+幂等)</li>
 *   <li>{@code community:post:like:count:{postId}}：String 快速计数</li>
 *   <li>热度 ZSet 同步 ±delta：点赞±3、收藏±5，保证榜单实时性</li>
 *   <li>写入脏标记集合，供每 5 分钟定时任务批量同步 MySQL</li>
 * </ul>
 * 全程不直接 UPDATE post 表，符合“不要每次点赞直接写数据库”的约束。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostInteractionServiceImpl implements PostInteractionService {

    /** 点赞在热度分中的权重 */
    private static final double LIKE_WEIGHT = 3d;
    /** 收藏在热度分中的权重 */
    private static final double COLLECT_WEIGHT = 5d;

    private final StringRedisTemplate redisTemplate;
    private final PostMapper postMapper;
    private final RankService rankService;
    private final NotifyService notifyService;

    @Override
    public InteractionVO toggleLike(long postId, long userId) {
        Post post = requirePost(postId);
        String member = String.valueOf(userId);
        String setKey = RedisKeys.likeKey(postId);

        // SADD：返回1表示新增(本次为点赞)，返回0表示已存在(本次为取消点赞)
        Long added = redisTemplate.opsForSet().add(setKey, member);
        boolean active = added != null && added > 0L;
        if (!active) {
            redisTemplate.opsForSet().remove(setKey, member);
        }
        long count = refreshCountKey(RedisKeys.likeCountKey(postId), setKey);

        // 实时同步热度榜 score：点赞 +3 / 取消 -3
        rankService.bump(postId, active ? LIKE_WEIGHT : -LIKE_WEIGHT);
        markDirty(postId);

        // v2 消息通知：赞了你的帖子 / 取消赞撤回未读通知(自己赞自己不通知)
        if (active) {
            notifyService.onPostLiked(post.getUserId(), userId, postId);
        } else {
            notifyService.removePostLiked(post.getUserId(), userId, postId);
        }
        return vo(active, count);
    }

    @Override
    public InteractionVO toggleCollect(long postId, long userId) {
        Post post = requirePost(postId);
        String member = String.valueOf(userId);
        String setKey = RedisKeys.collectKey(postId);

        Long added = redisTemplate.opsForSet().add(setKey, member);
        boolean active = added != null && added > 0L;
        if (!active) {
            redisTemplate.opsForSet().remove(setKey, member);
        }
        long count = refreshCountKey(RedisKeys.collectCountKey(postId), setKey);

        // 实时同步热度榜 score：收藏 +5 / 取消 -5
        rankService.bump(postId, active ? COLLECT_WEIGHT : -COLLECT_WEIGHT);
        markDirty(postId);

        // v2 消息通知：收藏了你的帖子 / 取消收藏撤回未读通知
        if (active) {
            notifyService.onPostCollected(post.getUserId(), userId, postId);
        } else {
            notifyService.removePostCollected(post.getUserId(), userId, postId);
        }
        return vo(active, count);
    }

    @Override
    public long likeCount(long postId) {
        return sizeOrZero(RedisKeys.likeKey(postId));
    }

    @Override
    public long collectCount(long postId) {
        return sizeOrZero(RedisKeys.collectKey(postId));
    }

    @Override
    public long viewCount(long postId) {
        String v = redisTemplate.opsForValue().get(RedisKeys.viewKey(postId));
        return v == null ? 0L : parseLong(v);
    }

    @Override
    public boolean isLiked(long postId, long userId) {
        Boolean member = redisTemplate.opsForSet().isMember(RedisKeys.likeKey(postId), String.valueOf(userId));
        return Boolean.TRUE.equals(member);
    }

    @Override
    public boolean isCollected(long postId, long userId) {
        Boolean member = redisTemplate.opsForSet().isMember(RedisKeys.collectKey(postId), String.valueOf(userId));
        return Boolean.TRUE.equals(member);
    }

    /**
     * 以 Set size 为准刷新计数 String 键(两键保持一致)
     */
    private long refreshCountKey(String countKey, String setKey) {
        long size = sizeOrZero(setKey);
        redisTemplate.opsForValue().set(countKey, String.valueOf(size));
        return size;
    }

    /** 写入脏标记：告知定时任务该帖子计数需要同步 */
    private void markDirty(long postId) {
        redisTemplate.opsForSet().add(RedisKeys.SYNC_DIRTY_SET, String.valueOf(postId));
    }

    /** 帖子必须存在(未被逻辑删除)，返回帖子实体供通知使用 */
    private Post requirePost(long postId) {
        Post post = postMapper.selectOne(new LambdaQueryWrapper<Post>().eq(Post::getId, postId));
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在或已被删除");
        }
        return post;
    }

    private long sizeOrZero(String key) {
        Long size = redisTemplate.opsForSet().size(key);
        return size == null ? 0L : size;
    }

    private long parseLong(String v) {
        try {
            return Long.parseLong(v);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private InteractionVO vo(boolean active, long count) {
        InteractionVO vo = new InteractionVO();
        vo.setActive(active);
        vo.setCount(count);
        return vo;
    }
}
