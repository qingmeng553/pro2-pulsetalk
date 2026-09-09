package com.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.community.common.constant.RedisKeys;
import com.community.entity.Post;
import com.community.entity.PostCategory;
import com.community.mapper.PostCategoryMapper;
import com.community.mapper.PostMapper;
import com.community.service.RankService;
import com.community.vo.RankItemVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 热度榜单服务实现
 *
 * <p>注意：为避免与 {@code PostInteractionServiceImpl} 形成循环依赖
 * (互动服务需要调用本服务 bump 更新热度分)，本服务内部的实时计数(点赞/收藏/浏览)
 * 直接读取 Redis，不再注入互动服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RankServiceImpl implements RankService {

    private final StringRedisTemplate redisTemplate;
    private final PostMapper postMapper;
    private final PostCategoryMapper categoryMapper;
    private final ObjectMapper objectMapper;

    @Override
    public void bump(long postId, double delta) {
        if (delta == 0d) {
            return;
        }
        redisTemplate.opsForZSet().incrementScore(RedisKeys.HOT_ZSET, String.valueOf(postId), delta);
    }

    @Override
    public void remove(long postId) {
        redisTemplate.opsForZSet().remove(RedisKeys.HOT_ZSET, String.valueOf(postId));
    }

    @Override
    public List<RankItemVO> top(int topN) {
        // 1. 取热度榜 TopN(降序带分数)；member 为帖子ID
        Set<ZSetOperations.TypedTuple<String>> tuples =
                redisTemplate.opsForZSet().reverseRangeWithScores(RedisKeys.HOT_ZSET, 0, Math.max(topN - 1, 0));
        if (CollectionUtils.isEmpty(tuples)) {
            return new ArrayList<>();
        }

        List<String> postIds = tuples.stream()
                .filter(t -> t.getValue() != null && t.getScore() != null && t.getScore() > 0)
                .map(ZSetOperations.TypedTuple::getValue)
                .toList();
        if (postIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. 批量回表补全帖子信息(逻辑删除的帖子自动被过滤)
        List<Long> ids = postIds.stream().map(Long::parseLong).toList();
        Map<Long, Post> postMap = postMapper.selectList(
                        new LambdaQueryWrapper<Post>().in(Post::getId, ids))
                .stream()
                .collect(Collectors.toMap(Post::getId, Function.identity(), (a, b) -> a));

        // 3. 分类名映射
        Set<Long> categoryIds = postMap.values().stream()
                .map(Post::getCategoryId)
                .collect(Collectors.toSet());
        Map<Long, String> categoryNameMap = categoryIds.isEmpty() ? Map.of()
                : categoryMapper.selectList(new LambdaQueryWrapper<PostCategory>().in(PostCategory::getId, categoryIds))
                .stream()
                .collect(Collectors.toMap(PostCategory::getId, PostCategory::getName, (a, b) -> a));

        // 4. 按榜单顺序组装(忽略已删除/分数异常条目)
        List<RankItemVO> result = new ArrayList<>();
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            if (tuple.getValue() == null || tuple.getScore() == null || tuple.getScore() <= 0) {
                continue;
            }
            long postId = Long.parseLong(tuple.getValue());
            Post post = postMap.get(postId);
            if (post == null) {
                continue;
            }
            RankItemVO item = new RankItemVO();
            item.setPostId(post.getId());
            item.setTitle(post.getTitle());
            item.setCover(firstImage(post.getImgUrls()));
            item.setCategoryName(categoryNameMap.getOrDefault(post.getCategoryId(), ""));
            item.setScore(tuple.getScore());
            item.setLikeCount(likeCount(postId));
            item.setCollectCount(collectCount(postId));
            item.setViewCount(viewCount(postId));
            result.add(item);
        }
        return result;
    }

    /** 解析 img_urls JSON，返回第一张作为封面 */
    private String firstImage(String imgUrlsJson) {
        if (imgUrlsJson == null || imgUrlsJson.isBlank()) {
            return null;
        }
        try {
            List<String> urls = objectMapper.readValue(imgUrlsJson, new TypeReference<List<String>>() {
            });
            return CollectionUtils.isEmpty(urls) ? null : urls.get(0);
        } catch (Exception e) {
            return null;
        }
    }

    // ==================== Redis 实时计数读取(直接读，避免服务间循环依赖) ====================

    /** 实时点赞数：Set size */
    private long likeCount(long postId) {
        Long size = redisTemplate.opsForSet().size(RedisKeys.likeKey(postId));
        return size == null ? 0L : size;
    }

    /** 实时收藏数：Set size */
    private long collectCount(long postId) {
        Long size = redisTemplate.opsForSet().size(RedisKeys.collectKey(postId));
        return size == null ? 0L : size;
    }

    /** 实时浏览量：String 计数 */
    private long viewCount(long postId) {
        String v = redisTemplate.opsForValue().get(RedisKeys.viewKey(postId));
        if (v == null) {
            return 0L;
        }
        try {
            return Long.parseLong(v);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
