package com.community.service;

import com.community.vo.AuthorVO;
import com.community.vo.UserProfileVO;

import java.util.List;

/**
 * 用户社交关系服务：好友 + 黑名单 + 用户主页
 *
 * <p>黑名单语义：拉黑即双向阻断互动(双方不可互发私聊、会话互相隐藏、好友关系解除)。
 */
public interface RelationService {

    /**
     * 用户公开主页(游客可访问)，relation 反映当前登录用户与该用户关系
     *
     * @param viewerId 当前登录用户ID(游客为 null)
     * @param targetId 被查看用户ID
     */
    UserProfileVO profile(Long viewerId, Long targetId);

    // ==================== 好友 ====================

    /**
     * 发送好友申请(重复/已为好友/黑名单等情况会校验并提示)
     */
    void requestFriend(long fromUserId, long toUserId);

    /**
     * 通过好友申请(形成双向好友关系)
     */
    void acceptFriend(long myUserId, long requesterId);

    /**
     * 拒绝好友申请
     */
    void rejectFriend(long myUserId, long requesterId);

    /**
     * 删除好友(解除双向关系)
     */
    void removeFriend(long myUserId, long targetUserId);

    /**
     * 我的好友列表
     */
    List<AuthorVO> friendList(long myUserId);

    /**
     * 收到的好友申请列表(待我处理)
     */
    List<AuthorVO> pendingFriendList(long myUserId);

    // ==================== 黑名单 ====================

    /**
     * 拉黑用户(同时解除好友关系；双向阻断互动)
     */
    void addBlacklist(long myUserId, long targetUserId);

    /**
     * 解除拉黑
     */
    void removeBlacklist(long myUserId, long targetUserId);

    /**
     * 我的黑名单列表
     */
    List<AuthorVO> blacklistList(long myUserId);

    // ==================== 供聊天模块复用的关系判断 ====================

    /** 是否互为好友(存在任意方向 ACCEPTED) */
    boolean isFriend(long userIdA, long userIdB);

    /** 是否处于黑名单阻断状态(任意方向存在黑名单记录) */
    boolean isBlocked(long userIdA, long userIdB);
}
