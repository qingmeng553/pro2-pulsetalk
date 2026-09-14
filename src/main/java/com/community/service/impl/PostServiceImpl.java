package com.community.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.common.api.ResultCode;
import com.community.common.constant.RedisKeys;
import com.community.common.exception.BusinessException;
import com.community.dto.PostDTO;
import com.community.entity.Post;
import com.community.entity.PostCategory;
import com.community.entity.PostPin;
import com.community.entity.SysUser;
import com.community.entity.UserRole;
import com.community.mapper.PostCategoryMapper;
import com.community.mapper.PostMapper;
import com.community.mapper.PostPinMapper;
import com.community.mapper.SysUserMapper;
import com.community.service.MinioFileService;
import com.community.service.PostInteractionService;
import com.community.service.PostService;
import com.community.service.RankService;
import com.community.service.RateLimitService;
import com.community.vo.AuthorVO;
import com.community.vo.PageVO;
import com.community.vo.PostDetailVO;
import com.community.vo.PostListItemVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 帖子服务实现
 *
 * <p>Redis 设计落点：
 * <ul>
 *   <li>Cache-Aside 旁路缓存：详情缓存 {@code community:post:cache:{id}}，更新/删除后直接删缓存，下次请求回写</li>
 *   <li>防缓存穿透：DB 无此帖时写空值缓存 {@code community:post:cache:null:{id}}</li>
 *   <li>防缓存雪崩：详情缓存 TTL 加入 30~60 分钟随机偏移</li>
 *   <li>浏览量：{@code community:post:view:{id}} INCR 自增，同时热度 ZSet +0.1，并标记脏集合等待定时落库</li>
 *   <li>点赞/收藏只操作 Redis(见 {@link PostInteractionServiceImpl})，本服务只负责读聚合</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    /** 浏览在热度分中的权重(score = 点赞*3 + 收藏*5 + 浏览*0.1) */
    private static final double VIEW_WEIGHT = 0.1d;

    private final PostMapper postMapper;
    private final PostCategoryMapper categoryMapper;
    private final SysUserMapper userMapper;
    private final PostPinMapper postPinMapper;
    private final PostInteractionService interactionService;
    private final RankService rankService;
    private final RateLimitService rateLimitService;
    private final MinioFileService minioFileService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    // ==================== 分类 ====================

    @Override
    public List<PostCategory> listCategories() {
        return categoryMapper.selectList(new LambdaQueryWrapper<PostCategory>()
                .orderByAsc(PostCategory::getSort)
                .orderByAsc(PostCategory::getId));
    }

    // ==================== 列表 ====================

    @Override
    public PageVO<PostListItemVO> pageList(long page, long size, Long categoryId, Long userId) {
        // 列表查询不加载大字段 content，减轻 IO/带宽
        QueryWrapper<Post> qw = new QueryWrapper<>();
        qw.select("id", "user_id", "category_id", "title", "img_urls",
                "view_count", "like_count", "collect_count", "create_time");
        if (categoryId != null) {
            qw.eq("category_id", categoryId);
        }
        if (userId != null) {
            // v2：个人主页“TA的帖子”按作者过滤
            qw.eq("user_id", userId);
        }
        // 最新发布在前(逻辑删除行由 @TableLogic 自动过滤)
        qw.orderByDesc("create_time").orderByDesc("id");

        Page<Post> p = postMapper.selectPage(new Page<>(page, size), qw);
        List<Post> records = p.getRecords();
        if (CollectionUtils.isEmpty(records)) {
            return emptyPage(p);
        }

        // 批量查作者与分类(避免 N+1)
        List<Long> userIds = records.stream().map(Post::getUserId).distinct().toList();
        List<Long> catIds = records.stream().map(Post::getCategoryId).distinct().toList();
        Map<Long, SysUser> userMap = userIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity(), (a, b) -> a));
        Map<Long, PostCategory> catMap = catIds.isEmpty() ? Map.of()
                : categoryMapper.selectBatchIds(catIds).stream()
                .collect(Collectors.toMap(PostCategory::getId, Function.identity(), (a, b) -> a));

        List<PostListItemVO> voList = new ArrayList<>(records.size());
        for (Post post : records) {
            PostListItemVO vo = new PostListItemVO();
            vo.setId(post.getId());
            vo.setTitle(post.getTitle());
            vo.setCategoryId(post.getCategoryId());
            vo.setCategoryName(catMap.containsKey(post.getCategoryId())
                    ? catMap.get(post.getCategoryId()).getName() : null);
            vo.setCover(firstImage(post.getImgUrls()));
            // 计数以 Redis 实时值为准(与库值基本一致，5分钟内可能的差异在此校正)
            vo.setViewCount((int) interactionService.viewCount(post.getId()));
            vo.setLikeCount((int) interactionService.likeCount(post.getId()));
            vo.setCollectCount((int) interactionService.collectCount(post.getId()));
            vo.setCreateTime(post.getCreateTime());
            vo.setAuthor(toAuthor(userMap.get(post.getUserId())));
            voList.add(vo);
        }

        PageVO<PostListItemVO> result = new PageVO<>();
        result.setRecords(voList);
        result.setTotal(p.getTotal());
        result.setCurrent(p.getCurrent());
        result.setSize(p.getSize());
        result.setPages(p.getPages());
        return result;
    }

    // ==================== 详情(Cache-Aside) ====================

    @Override
    public PostDetailVO detail(long postId) {
        // 1) 空值缓存命中 → 直接判定不存在(防缓存穿透的第二步拦截)
        if (Boolean.TRUE.equals(redisTemplate.hasKey(RedisKeys.postNullCacheKey(postId)))) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在或已被删除");
        }

        PostDetailVO vo;
        String cacheJson = redisTemplate.opsForValue().get(RedisKeys.postCacheKey(postId));
        if (StringUtils.hasText(cacheJson)) {
            // 2a) 命中热点缓存
            try {
                vo = objectMapper.readValue(cacheJson, PostDetailVO.class);
            } catch (JsonProcessingException e) {
                log.warn("[缓存] 详情缓存反序列化失败，回源数据库 postId={}", postId);
                redisTemplate.delete(RedisKeys.postCacheKey(postId));
                vo = loadAndCache(postId);
            }
        } else {
            // 2b) 未命中 → 查库并回写缓存
            vo = loadAndCache(postId);
        }

        // 3) 浏览量自增 + 热度榜更新 + 脏标记(每次访问都计数，含缓存命中)
        recordView(postId);

        // 4) 动态字段实时回填(计数/当前用户点赞收藏状态)
        fillDynamicFields(vo, postId);
        return vo;
    }

    /**
     * 查库组装详情静态部分并回写缓存；查无记录时写空值缓存并抛 404
     */
    private PostDetailVO loadAndCache(long postId) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            // 空值缓存：随机 3~5 分钟，防止对不存在ID的反复穿透查询打爆DB
            long nullTtl = randomSeconds(180, 300);
            redisTemplate.opsForValue().set(RedisKeys.postNullCacheKey(postId), "1",
                    Duration.ofSeconds(nullTtl));
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在或已被删除");
        }
        PostDetailVO vo = assembleStatic(post);
        try {
            // 热点缓存 TTL 带随机偏移(30~60分钟)，规避缓存雪崩
            long ttl = randomSeconds(1800, 3600);
            redisTemplate.opsForValue().set(RedisKeys.postCacheKey(postId),
                    objectMapper.writeValueAsString(vo), Duration.ofSeconds(ttl));
        } catch (JsonProcessingException e) {
            log.warn("[缓存] 详情缓存序列化失败 postId={}", postId, e);
        }
        return vo;
    }

    /**
     * 浏览量自增(INCR) + 热度 ZSet 增量(0.1) + 脏集合标记，Pipeline 一次往返完成
     */
    private void recordView(long postId) {
        byte[] idBytes = String.valueOf(postId).getBytes(StandardCharsets.UTF_8);
        byte[] viewKey = RedisKeys.viewKey(postId).getBytes(StandardCharsets.UTF_8);
        byte[] hotKey = RedisKeys.HOT_ZSET.getBytes(StandardCharsets.UTF_8);
        byte[] dirtyKey = RedisKeys.SYNC_DIRTY_SET.getBytes(StandardCharsets.UTF_8);

        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            connection.stringCommands().incr(viewKey);                 // 浏览量 +1
            connection.zSetCommands().zIncrBy(hotKey, VIEW_WEIGHT, idBytes); // 热度 +0.1
            connection.setCommands().sAdd(dirtyKey, idBytes);          // 标记待同步
            return null;
        });
    }

    /** 组装详情静态字段(不含动态计数)，供缓存与响应复用 */
    private PostDetailVO assembleStatic(Post post) {
        PostDetailVO vo = new PostDetailVO();
        vo.setId(post.getId());
        vo.setTitle(post.getTitle());
        vo.setContent(post.getContent());
        vo.setImgUrls(parseImgUrls(post.getImgUrls()));
        vo.setCategoryId(post.getCategoryId());
        PostCategory category = categoryMapper.selectById(post.getCategoryId());
        vo.setCategoryName(category == null ? null : category.getName());
        vo.setCreateTime(post.getCreateTime());
        vo.setUpdateTime(post.getUpdateTime());
        vo.setAuthor(toAuthor(userMapper.selectById(post.getUserId())));
        return vo;
    }

    /** 动态字段实时回填：浏览量/点赞数/收藏数/当前用户是否点赞收藏 */
    private void fillDynamicFields(PostDetailVO vo, long postId) {
        vo.setViewCount(interactionService.viewCount(postId));
        vo.setLikeCount(interactionService.likeCount(postId));
        vo.setCollectCount(interactionService.collectCount(postId));
        if (StpUtil.isLogin()) {
            long uid = StpUtil.getLoginIdAsLong();
            vo.setLiked(interactionService.isLiked(postId, uid));
            vo.setCollected(interactionService.isCollected(postId, uid));
        } else {
            vo.setLiked(false);
            vo.setCollected(false);
        }
    }

    // ==================== 发帖 / 编辑 / 删除 ====================

    @Override
    public Post create(long userId, PostDTO dto) {
        // v3 封禁校验：被封禁用户不可发帖
        requireNotBanned(userId);
        // 发帖接口限流(Redis 计数器)
        rateLimitService.checkCreatePost(userId);
        requireCategory(dto.getCategoryId());

        Post post = new Post();
        post.setUserId(userId);
        post.setCategoryId(dto.getCategoryId());
        post.setTitle(dto.getTitle().trim());
        post.setContent(dto.getContent());
        post.setImgUrls(toImgUrlsJson(dto.getImgUrls()));
        postMapper.insert(post);
        log.info("[发帖] userId={} 发布新帖 postId={}", userId, post.getId());
        return post;
    }

    @Override
    public Post update(long userId, long postId, PostDTO dto) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在或已被删除");
        }
        // 仅作者可改；若该帖为置顶规则帖，管理员可共同编辑(管理员共管规则帖)
        boolean owner = Objects.equals(post.getUserId(), userId);
        boolean adminCanEditPinned = isAdmin(userId) && isPinned(postId);
        if (!owner && !adminCanEditPinned) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能修改自己发布的帖子");
        }
        requireCategory(dto.getCategoryId());

        post.setCategoryId(dto.getCategoryId());
        post.setTitle(dto.getTitle().trim());
        post.setContent(dto.getContent());
        post.setImgUrls(toImgUrlsJson(dto.getImgUrls()));
        postMapper.updateById(post);

        // Cache-Aside：更新数据库之后直接删除缓存，下次请求自动回写
        evictPostCache(postId);
        log.info("[编辑] userId={} 修改帖子 postId={}", userId, postId);
        return post;
    }

    @Override
    public void deleteOwn(long userId, long postId) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在或已被删除");
        }
        if (!Objects.equals(post.getUserId(), userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能删除自己发布的帖子");
        }
        // 逻辑软删除(is_deleted=1) + 清理 Redis 相关数据
        postMapper.deleteById(postId);
        cleanupPostRedis(postId);
        clearPinIfMatches(postId);
        log.info("[删除] userId={} 删除自己的帖子 postId={}", userId, postId);
    }

    @Override
    public void adminDelete(long postId) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在或已被删除");
        }
        postMapper.deleteById(postId);
        cleanupPostRedis(postId);
        clearPinIfMatches(postId);
        log.info("[管理员删除] 管理员软删除帖子 postId={}", postId);
    }

    @Override
    public String uploadImage(MultipartFile file) {
        return minioFileService.uploadPostImage(file);
    }

    // ==================== v2：置顶规则帖 ====================

    @Override
    public PostDetailVO getPinnedPost() {
        PostPin pin = postPinMapper.selectById(PostPin.PIN_ID);
        if (pin == null) {
            return null;
        }
        Post post = postMapper.selectById(pin.getPostId());
        if (post == null) {
            // 置顶帖被删但记录未清理(异常兜底)：顺手清理
            postPinMapper.deleteById(PostPin.PIN_ID);
            return null;
        }
        // 复用详情装配(不累计浏览量；规则帖长期展示，不应计为浏览)
        PostDetailVO vo = assembleStatic(post);
        fillDynamicFields(vo, post.getId());
        return vo;
    }

    @Override
    public void pin(long adminId, long postId) {
        if (postMapper.selectById(postId) == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在或已被删除");
        }
        PostPin pin = postPinMapper.selectById(PostPin.PIN_ID);
        if (pin == null) {
            PostPin created = new PostPin();
            created.setId(PostPin.PIN_ID);
            created.setPostId(postId);
            created.setAdminId(adminId);
            postPinMapper.insert(created);
        } else {
            pin.setPostId(postId);
            pin.setAdminId(adminId);
            postPinMapper.updateById(pin);
        }
        log.info("[置顶] 管理员 {} 将帖子 {} 设为置顶规则帖(全站仅一条)", adminId, postId);
    }

    @Override
    public void unpin(long adminId) {
        if (postPinMapper.selectById(PostPin.PIN_ID) != null) {
            postPinMapper.deleteById(PostPin.PIN_ID);
            log.info("[置顶] 管理员 {} 取消了置顶规则帖", adminId);
        }
    }

    // ==================== 私有辅助 ====================

    /**
     * 清理帖子在 Redis 中的全部痕迹：互动键、缓存、榜单、脏标记
     */
    private void cleanupPostRedis(long postId) {
        String id = String.valueOf(postId);
        redisTemplate.delete(List.of(
                RedisKeys.likeKey(postId),
                RedisKeys.likeCountKey(postId),
                RedisKeys.collectKey(postId),
                RedisKeys.collectCountKey(postId),
                RedisKeys.viewKey(postId),
                RedisKeys.postCacheKey(postId),
                RedisKeys.postNullCacheKey(postId)
        ));
        rankService.remove(postId);
        redisTemplate.opsForSet().remove(RedisKeys.SYNC_DIRTY_SET, id);
    }

    /** 删除(失效)帖子详情缓存 */
    private void evictPostCache(long postId) {
        redisTemplate.delete(List.of(RedisKeys.postCacheKey(postId), RedisKeys.postNullCacheKey(postId)));
    }

    private void requireCategory(Long categoryId) {
        if (categoryMapper.selectById(categoryId) == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "所选分类不存在");
        }
    }

    /** 是否为管理员(用于“管理员可共同编辑置顶规则帖”判定) */
    private boolean isAdmin(long userId) {
        SysUser user = userMapper.selectById(userId);
        return user != null && UserRole.ADMIN == user.getRole();
    }

    /** v3 封禁校验：账号被封禁时禁止发帖/评论等创作行为 */
    private void requireNotBanned(long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user != null && user.getStatus() != null && user.getStatus() == SysUser.STATUS_BANNED) {
            throw new BusinessException(ResultCode.FORBIDDEN, "账号已被封禁，暂时无法发帖");
        }
    }

    /** 该帖是否为当前置顶规则帖 */
    private boolean isPinned(long postId) {
        PostPin pin = postPinMapper.selectById(PostPin.PIN_ID);
        return pin != null && Objects.equals(pin.getPostId(), postId);
    }

    /** 删除帖子后若其是置顶规则帖，则同步清除置顶记录 */
    private void clearPinIfMatches(long postId) {
        PostPin pin = postPinMapper.selectById(PostPin.PIN_ID);
        if (pin != null && Objects.equals(pin.getPostId(), postId)) {
            postPinMapper.deleteById(PostPin.PIN_ID);
        }
    }

    /** 图片URL列表 → JSON 字符串存储(img_urls 字段) */
    private String toImgUrlsJson(List<String> imgUrls) {
        if (CollectionUtils.isEmpty(imgUrls)) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(imgUrls);
        } catch (JsonProcessingException e) {
            // 字符串列表不可能序列化失败，仅做防御
            throw new BusinessException(ResultCode.ERROR, "图片数据格式错误");
        }
    }

    /** JSON 字符串 → 图片URL列表 */
    private List<String> parseImgUrls(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            log.warn("[解析] img_urls 解析失败: {}", json);
            return Collections.emptyList();
        }
    }

    /** 取第一张配图作为封面 */
    private String firstImage(String imgUrlsJson) {
        List<String> urls = parseImgUrls(imgUrlsJson);
        return urls.isEmpty() ? null : urls.get(0);
    }

    /** 用户实体 → 作者摘要 */
    private AuthorVO toAuthor(SysUser user) {
        AuthorVO vo = new AuthorVO();
        if (user == null) {
            vo.setNickname("已注销用户");
            return vo;
        }
        vo.setUserId(user.getId());
        vo.setNickname(user.getNickname());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setRole(user.getRole());
        return vo;
    }

    /** [min,max] 之间的随机秒数(缓存过期随机偏移) */
    private long randomSeconds(long min, long max) {
        return ThreadLocalRandom.current().nextLong(min, max + 1);
    }

    private PageVO<PostListItemVO> emptyPage(Page<Post> p) {
        PageVO<PostListItemVO> vo = new PageVO<>();
        vo.setRecords(Collections.emptyList());
        vo.setTotal(p.getTotal());
        vo.setCurrent(p.getCurrent());
        vo.setSize(p.getSize());
        vo.setPages(p.getPages());
        return vo;
    }
}
