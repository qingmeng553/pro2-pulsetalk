package com.community.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.community.common.api.R;
import com.community.service.AdminUserService;
import com.community.vo.PageVO;
import com.community.vo.UserAdminVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端 - 用户管理接口(仅 ADMIN 可见/可用)
 *
 * <pre>
 * GET    /admin/user/list         用户表分页(支持 keyword 账号/昵称搜索)
 * POST   /admin/user/{id}/ban     封禁用户(封禁后不可发帖、不可评论)
 * POST   /admin/user/{id}/unban   解封用户
 * DELETE /admin/user/{id}         删除用户(逻辑删除)
 * </pre>
 */
@RestController
@RequestMapping("/admin/user")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    /** 用户表分页 */
    @SaCheckRole("admin")
    @GetMapping("/list")
    public R<PageVO<UserAdminVO>> list(@RequestParam(defaultValue = "1") long page,
                                       @RequestParam(defaultValue = "10") long size,
                                       @RequestParam(required = false) String keyword) {
        return R.ok(adminUserService.pageList(page, size, keyword));
    }

    /** 封禁用户 */
    @SaCheckRole("admin")
    @PostMapping("/{id}/ban")
    public R<Void> ban(@PathVariable Long id) {
        adminUserService.ban(StpUtil.getLoginIdAsLong(), id);
        return R.ok();
    }

    /** 解封用户 */
    @SaCheckRole("admin")
    @PostMapping("/{id}/unban")
    public R<Void> unban(@PathVariable Long id) {
        adminUserService.unban(StpUtil.getLoginIdAsLong(), id);
        return R.ok();
    }

    /** 删除用户(逻辑删除) */
    @SaCheckRole("admin")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        adminUserService.delete(StpUtil.getLoginIdAsLong(), id);
        return R.ok();
    }
}
