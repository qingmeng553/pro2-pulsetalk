package com.community.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.community.common.api.R;
import com.community.dto.CommentDTO;
import com.community.service.CommentService;
import com.community.vo.CommentVO;
import com.community.vo.PageVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 评论模块接口(一级评论)
 *
 * <pre>
 * GET    /post/{postId}/comment/list   评论分页列表(游客可访问，过滤软删除)
 * POST   /post/{postId}/comment        发表评论(需登录；限流校验)
 * DELETE /comment/{id}                 用户删除自己评论(需登录，软删除)
 * DELETE /comment/admin/{id}           管理员软删除任意评论(需 admin 角色)
 * </pre>
 */
@RestController
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    /** 评论分页列表(游客可访问) */
    @GetMapping("/post/{postId}/comment/list")
    public R<PageVO<CommentVO>> commentList(@PathVariable Long postId,
                                            @RequestParam(defaultValue = "1") long page,
                                            @RequestParam(defaultValue = "10") long size) {
        return R.ok(commentService.pageList(postId, page, size));
    }

    /** 发表评论(需登录；评论接口限流) */
    @SaCheckLogin
    @PostMapping("/post/{postId}/comment")
    public R<Void> createComment(@PathVariable Long postId, @Valid @RequestBody CommentDTO dto) {
        commentService.create(StpUtil.getLoginIdAsLong(), postId, dto);
        return R.ok();
    }

    /** 用户删除自己的评论(需登录，逻辑软删除) */
    @SaCheckLogin
    @DeleteMapping("/comment/{id}")
    public R<Void> deleteComment(@PathVariable Long id) {
        commentService.deleteOwn(StpUtil.getLoginIdAsLong(), id);
        return R.ok();
    }

    /** 管理员软删除任意评论(admin 角色鉴权) */
    @SaCheckRole("admin")
    @DeleteMapping("/comment/admin/{id}")
    public R<Void> adminDeleteComment(@PathVariable Long id) {
        commentService.adminDelete(id);
        return R.ok();
    }
}
