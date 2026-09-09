package com.community.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.community.common.api.R;
import com.community.service.RelationService;
import com.community.vo.AuthorVO;
import com.community.vo.UserProfileVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 社交关系接口：用户主页(点头像了解彼此) + 好友 + 黑名单
 *
 * <pre>
 * GET  /user/{id}                  用户公开主页(游客可看；含 relation 关系)
 * POST /friend/request/{userId}    发送好友申请
 * POST /friend/accept/{userId}     通过好友申请
 * POST /friend/reject/{userId}     拒绝好友申请
 * POST /friend/remove/{userId}     删除好友
 * GET  /friend/list                我的好友
 * GET  /friend/pending/list        收到的好友申请(待处理)
 * POST /blacklist/add/{userId}     拉黑(双向阻断互动，自动解除好友)
 * POST /blacklist/remove/{userId}  解除拉黑
 * GET  /blacklist/list             我的黑名单
 * </pre>
 */
@RestController
@RequiredArgsConstructor
public class RelationController {

    private final RelationService relationService;

    /** 用户公开主页(游客可访问，未登录时 relation=NONE) */
    @GetMapping("/user/{id}")
    public R<UserProfileVO> profile(@PathVariable Long id) {
        Long viewer = StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : null;
        return R.ok(relationService.profile(viewer, id));
    }

    // ==================== 好友 ====================

    /** 发送好友申请 */
    @SaCheckLogin
    @PostMapping("/friend/request/{userId}")
    public R<Void> requestFriend(@PathVariable Long userId) {
        relationService.requestFriend(StpUtil.getLoginIdAsLong(), userId);
        return R.ok();
    }

    /** 通过好友申请 */
    @SaCheckLogin
    @PostMapping("/friend/accept/{userId}")
    public R<Void> acceptFriend(@PathVariable Long userId) {
        relationService.acceptFriend(StpUtil.getLoginIdAsLong(), userId);
        return R.ok();
    }

    /** 拒绝好友申请 */
    @SaCheckLogin
    @PostMapping("/friend/reject/{userId}")
    public R<Void> rejectFriend(@PathVariable Long userId) {
        relationService.rejectFriend(StpUtil.getLoginIdAsLong(), userId);
        return R.ok();
    }

    /** 删除好友 */
    @SaCheckLogin
    @PostMapping("/friend/remove/{userId}")
    public R<Void> removeFriend(@PathVariable Long userId) {
        relationService.removeFriend(StpUtil.getLoginIdAsLong(), userId);
        return R.ok();
    }

    /** 我的好友列表 */
    @SaCheckLogin
    @GetMapping("/friend/list")
    public R<List<AuthorVO>> friendList() {
        return R.ok(relationService.friendList(StpUtil.getLoginIdAsLong()));
    }

    /** 收到的好友申请 */
    @SaCheckLogin
    @GetMapping("/friend/pending/list")
    public R<List<AuthorVO>> pendingList() {
        return R.ok(relationService.pendingFriendList(StpUtil.getLoginIdAsLong()));
    }

    // ==================== 黑名单 ====================

    /** 拉黑(双向阻断互动) */
    @SaCheckLogin
    @PostMapping("/blacklist/add/{userId}")
    public R<Void> addBlacklist(@PathVariable Long userId) {
        relationService.addBlacklist(StpUtil.getLoginIdAsLong(), userId);
        return R.ok();
    }

    /** 解除拉黑 */
    @SaCheckLogin
    @PostMapping("/blacklist/remove/{userId}")
    public R<Void> removeBlacklist(@PathVariable Long userId) {
        relationService.removeBlacklist(StpUtil.getLoginIdAsLong(), userId);
        return R.ok();
    }

    /** 我的黑名单列表 */
    @SaCheckLogin
    @GetMapping("/blacklist/list")
    public R<List<AuthorVO>> blacklistList() {
        return R.ok(relationService.blacklistList(StpUtil.getLoginIdAsLong()));
    }
}
