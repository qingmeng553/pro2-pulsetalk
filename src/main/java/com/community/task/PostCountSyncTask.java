package com.community.task;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.community.common.constant.RedisKeys;
import com.community.entity.Post;
import com.community.mapper.PostMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Redis 计数 → MySQL 批量同步定时任务
 *
 * <p>策略：点赞/收藏/浏览量全程只操作 Redis(不实时写库)，本任务每 5 分钟执行一次：
 * <ol>
 *   <li>读取脏标记集合 {@code community:post:sync:dirty} 中待同步的帖子ID(谁被互动过同步谁)</li>
 *   <li>以 Redis 权威数据为准：点赞数=Set size、收藏数=Set size、浏览量=INCR 累计值</li>
 *   <li>批量 UPDATE post 表对应计数(幂等：直接覆盖为 Redis 绝对值)</li>
 *   <li>成功后移除脏标记，未成功(如帖子刚好被删)下轮重试或自动清理</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PostCountSyncTask {

    private final PostMapper postMapper;
    private final StringRedisTemplate redisTemplate;

    /** 每 5 分钟执行一次 */
    @Scheduled(cron = "0 */5 * * * *")
    public void syncCountsToDb() {
        Set<String> dirtyIds = redisTemplate.opsForSet().members(RedisKeys.SYNC_DIRTY_SET);
        if (dirtyIds == null || dirtyIds.isEmpty()) {
            return;
        }

        int updated = 0;
        for (String sid : dirtyIds) {
            try {
                long postId = Long.parseLong(sid);
                // 从 Redis 读取权威计数(键不存在视为 0)；post 表计数为 INT 列，先解箱为 long 再安全收敛
                int like = Math.toIntExact(sizeOrZero(RedisKeys.likeKey(postId)));
                int collect = Math.toIntExact(sizeOrZero(RedisKeys.collectKey(postId)));
                int view = Math.toIntExact(viewOrZero(postId));

                // 覆盖写库(逻辑删除的帖子会被 @TableLogic 条件过滤，相当于跳过)
                int rows = postMapper.update(null, new LambdaUpdateWrapper<Post>()
                        .eq(Post::getId, postId)
                        .set(Post::getLikeCount, like)
                        .set(Post::getCollectCount, collect)
                        .set(Post::getViewCount, view));
                if (rows > 0) {
                    redisTemplate.opsForSet().remove(RedisKeys.SYNC_DIRTY_SET, sid);
                    updated++;
                } else {
                    // 帖子已删除：清理脏标记
                    redisTemplate.opsForSet().remove(RedisKeys.SYNC_DIRTY_SET, sid);
                }
            } catch (RuntimeException e) {
                // Long.parseLong(NumberFormatException) 与 Redis/MyBatis 运行时异常统一兜底
                log.warn("[同步] 帖子 {} 计数同步失败: {}", sid, e.getMessage());
            }
        }
        log.info("[同步] 本轮同步帖子计数完成, 成功 {} 条", updated);
    }

    private Long sizeOrZero(String key) {
        Long size = redisTemplate.opsForSet().size(key);
        return size == null ? 0L : size;
    }

    private long viewOrZero(long postId) {
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
