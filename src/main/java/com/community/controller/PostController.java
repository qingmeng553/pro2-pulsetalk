package com.community.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.community.common.api.R;
import com.community.dto.PostDTO;
import com.community.entity.Post;
import com.community.entity.PostCategory;
import com.community.service.PostInteractionService;
import com.community.service.PostService;
import com.community.service.RankService;
import com.community.vo.InteractionVO;
import com.community.vo.PageVO;
import com.community.vo.PostDetailVO;
import com.community.vo.PostListItemVO;
import com.community.vo.RankItemVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 帖子模块接口
 *
 * <pre>
 * GET    /post/category/list   获取全部帖子分类(游客可访问)
 * GET    /post/list            帖子分页列表(游客可访问，过滤 is_deleted=1，支持分类筛选)
 * GET    /post/{id}            帖子详情(游客可访问；浏览量 Redis 自增；命中热点缓存)
 * POST   /post                 新建帖子(需登录；限流校验；markdown+emoji+多张配图)
 * PUT    /post/{id}            修改自己帖子(需登录；仅作者可改；更新DB后删除Redis缓存)
 * DELETE /post/{id}            用户删除自己帖子(需登录；仅作者，软删除，清理缓存)
 * DELETE /post/admin/{id}      管理员软删除任意帖子(需 admin 角色)
 * POST   /post/{id}/like       点赞/取消点赞(需登录，只操作 Redis)
 * POST   /post/{id}/collect    收藏/取消收藏(需登录，只操作 Redis)
 * GET    /post/hot/rank        热度榜单(游客可访问，ZSet)
 * POST   /post/image           上传帖子配图(需登录，返回可访问 URL)【资源型接口，非CRUD】
 * </pre>
 */
@RestController
@RequestMapping("/post")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final PostInteractionService interactionService;
    private final RankService rankService;

    // ==================== 分类 & 列表(游客可读) ====================

    /** 获取全部帖子分类 */
    @GetMapping("/category/list")
    public R<List<PostCategory>> categoryList() {
        return R.ok(postService.listCategories());
    }

    /** 帖子分页列表，支持分类筛选与作者筛选(游客可访问) */
    @GetMapping("/list")
    public R<PageVO<PostListItemVO>> list(@RequestParam(defaultValue = "1") long page,
                                          @RequestParam(defaultValue = "10") long size,
                                          @RequestParam(required = false) Long categoryId,
                                          @RequestParam(required = false) Long userId) {
        return R.ok(postService.pageList(page, size, categoryId, userId));
    }

    /** 帖子详情(游客可访问；浏览量 Redis 自增；命中热点缓存) */
    @GetMapping("/{id}")
    public R<PostDetailVO> detail(@PathVariable Long id) {
        return R.ok(postService.detail(id));
    }

    /** 当前置顶规则帖(游客可访问；无置顶返回 data=null) */
    @GetMapping("/pinned")
    public R<PostDetailVO> pinned() {
        return R.ok(postService.getPinnedPost());
    }

    /** 热度榜单 TopN(游客可访问，供 ECharts/D3 渲染) */
    @GetMapping("/hot/rank")
    public R<List<RankItemVO>> hotRank(@RequestParam(defaultValue = "10") int topN) {
        return R.ok(rankService.top(Math.min(Math.max(topN, 1), 50)));
    }

    // ==================== 帖子写操作(需登录) ====================

    /** 新建帖子(需登录；发帖限流) */
    @SaCheckLogin
    @PostMapping
    public R<Post> create(@Valid @RequestBody PostDTO dto) {
        return R.ok(postService.create(StpUtil.getLoginIdAsLong(), dto));
    }

    /** 修改自己的帖子(需登录，仅作者可改) */
    @SaCheckLogin
    @PutMapping("/{id}")
    public R<Post> update(@PathVariable Long id, @Valid @RequestBody PostDTO dto) {
        return R.ok(postService.update(StpUtil.getLoginIdAsLong(), id, dto));
    }

    /** 用户删除自己的帖子(需登录，仅作者，逻辑软删除) */
    @SaCheckLogin
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        postService.deleteOwn(StpUtil.getLoginIdAsLong(), id);
        return R.ok();
    }

    /** 管理员软删除任意帖子(admin 角色鉴权) */
    @SaCheckRole("admin")
    @DeleteMapping("/admin/{id}")
    public R<Void> adminDelete(@PathVariable Long id) {
        postService.adminDelete(id);
        return R.ok();
    }

    /** 管理员将某帖设为置顶规则帖(admin 角色鉴权；自动替换旧置顶，全站仅一条) */
    @SaCheckRole("admin")
    @PutMapping("/admin/pin/{id}")
    public R<Void> pin(@PathVariable Long id) {
        postService.pin(StpUtil.getLoginIdAsLong(), id);
        return R.ok();
    }

    /** 管理员取消置顶规则帖(admin 角色鉴权) */
    @SaCheckRole("admin")
    @DeleteMapping("/admin/pin")
    public R<Void> unpin() {
        postService.unpin(StpUtil.getLoginIdAsLong());
        return R.ok();
    }

    // ==================== 点赞 / 收藏(只操作 Redis) ====================

    /** 点赞/取消点赞(需登录) */
    @SaCheckLogin
    @PostMapping("/{id}/like")
    public R<InteractionVO> like(@PathVariable Long id) {
        return R.ok(interactionService.toggleLike(id, StpUtil.getLoginIdAsLong()));
    }

    /** 收藏/取消收藏(需登录) */
    @SaCheckLogin
    @PostMapping("/{id}/collect")
    public R<InteractionVO> collect(@PathVariable Long id) {
        return R.ok(interactionService.toggleCollect(id, StpUtil.getLoginIdAsLong()));
    }

    /** 上传帖子配图(需登录，返回 MinIO 可访问 URL) */
    @SaCheckLogin
    @PostMapping("/image")
    public R<String> uploadImage(@RequestParam("file") MultipartFile file) {
        return R.ok(postService.uploadImage(file));
    }
}
