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

/** 热度榜单服务实现。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RankServiceImpl implements RankService {

    /** 重建榜单时最多回填的帖子数 */
    private static final int REBUILD_LIMIT = 500;
    private static final double LIKE_WEIGHT = 3d;
    private static final double COLLECT_WEIGHT = 5d;
    private static final double VIEW_WEIGHT = 0.1d;

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
    public void rebuildFromDbIfEmpty() {
        Long size = redisTemplate.opsForZSet().size(RedisKeys.HOT_ZSET);
        if (size != null && size > 0) {
            return;
        }
        // 取互动过的帖子(浏览量>0 或有点赞/收藏)，按库中计数算分回填榜单
        List<Post> posts = postMapper.selectList(new LambdaQueryWrapper<Post>()
                .select(Post::getId, Post::getLikeCount, Post::getCollectCount, Post::getViewCount)
                .gt(Post::getViewCount, 0)
                .orderByDesc(Post::getId)
                .last("LIMIT " + REBUILD_LIMIT));
        int count = 0;
        for (Post p : posts) {
            int like = p.getLikeCount() == null ? 0 : p.getLikeCount();
            int collect = p.getCollectCount() == null ? 0 : p.getCollectCount();
            int view = p.getViewCount() == null ? 0 : p.getViewCount();
            double score = like * LIKE_WEIGHT + collect * COLLECT_WEIGHT + view * VIEW_WEIGHT;
            if (score <= 0) {
                continue;
            }
            redisTemplate.opsForZSet().add(RedisKeys.HOT_ZSET, String.valueOf(p.getId()), score);
            count++;
        }
        log.info("[热度榜] 榜单为空，已按数据库计数重建 {} 条", count);
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
    // ---- Redis 实时计数读取(直接读，避免服务间循环依赖) ----
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
