package com.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.community.common.api.ResultCode;
import com.community.common.exception.BusinessException;
import com.community.common.util.AuthorVOs;
import com.community.entity.SysUser;
import com.community.entity.UserBlacklist;
import com.community.entity.UserFriend;
import com.community.mapper.PostMapper;
import com.community.mapper.SysUserMapper;
import com.community.mapper.UserBlacklistMapper;
import com.community.mapper.UserFriendMapper;
import com.community.service.NotifyService;
import com.community.service.RelationService;
import com.community.vo.AuthorVO;
import com.community.vo.UserProfileVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 好友 + 黑名单 + 用户主页服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RelationServiceImpl implements RelationService {

    /** 关系态常量 */
    private static final String REL_SELF = "SELF";
    private static final String REL_FRIEND = "FRIEND";
    private static final String REL_PENDING_SENT = "PENDING_SENT";
    private static final String REL_PENDING_RECEIVED = "PENDING_RECEIVED";
    private static final String REL_BLACKED = "BLACKED";
    private static final String REL_NONE = "NONE";

    private final SysUserMapper userMapper;
    private final UserFriendMapper friendMapper;
    private final UserBlacklistMapper blacklistMapper;
    private final PostMapper postMapper;
    private final NotifyService notifyService;

    @Override
    public UserProfileVO profile(Long viewerId, Long targetId) {
        SysUser target = userMapper.selectById(targetId);
        if (target == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }

        UserProfileVO vo = new UserProfileVO();
        vo.setUserId(target.getId());
        vo.setNickname(target.getNickname());
        vo.setAvatarUrl(target.getAvatarUrl());
        vo.setRole(target.getRole());
        vo.setCreateTime(target.getCreateTime());
        Long count = postMapper.selectCount(new LambdaQueryWrapper<com.community.entity.Post>()
                .eq(com.community.entity.Post::getUserId, targetId));
        vo.setPostCount(count == null ? 0L : count);
        vo.setRelation(resolveRelation(viewerId, targetId));
        return vo;
    }

    /** 解析当前登录用户与目标用户的关系 */
    private String resolveRelation(Long viewerId, Long targetId) {
        if (viewerId == null) {
            return REL_NONE;
        }
        if (Objects.equals(viewerId, targetId)) {
            return REL_SELF;
        }
        if (isBlocked(viewerId, targetId)) {
            return REL_BLACKED;
        }
        // 好友(任一方向 ACCEPTED)
        Long friend = friendMapper.selectCount(new LambdaQueryWrapper<UserFriend>()
                .eq(UserFriend::getStatus, UserFriend.STATUS_ACCEPTED)
                .and(w -> w.eq(UserFriend::getUserId, viewerId).eq(UserFriend::getFriendId, targetId)
                        .or().eq(UserFriend::getUserId, targetId).eq(UserFriend::getFriendId, viewerId)));
        if (friend != null && friend > 0) {
            return REL_FRIEND;
        }
        // 我发起的申请
        Long sent = friendMapper.selectCount(new LambdaQueryWrapper<UserFriend>()
                .eq(UserFriend::getUserId, viewerId)
                .eq(UserFriend::getFriendId, targetId)
                .eq(UserFriend::getStatus, UserFriend.STATUS_PENDING));
        if (sent != null && sent > 0) {
            return REL_PENDING_SENT;
        }
        // 对方发起的申请
        Long received = friendMapper.selectCount(new LambdaQueryWrapper<UserFriend>()
                .eq(UserFriend::getUserId, targetId)
                .eq(UserFriend::getFriendId, viewerId)
                .eq(UserFriend::getStatus, UserFriend.STATUS_PENDING));
        if (received != null && received > 0) {
            return REL_PENDING_RECEIVED;
        }
        return REL_NONE;
    }

    // ==================== 好友 ====================

    @Override
    public void requestFriend(long fromUserId, long toUserId) {
        if (fromUserId == toUserId) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不能添加自己为好友");
        }
        requireUser(toUserId);
        if (isBlocked(fromUserId, toUserId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "由于拉黑/被拉黑关系，无法添加好友");
        }
        if (isFriend(fromUserId, toUserId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "你们已经是好友啦");
        }

        // 若对方已向我发起过申请 → 直接互相通过
        UserFriend reversePending = friendMapper.selectOne(new LambdaQueryWrapper<UserFriend>()
                .eq(UserFriend::getUserId, toUserId)
                .eq(UserFriend::getFriendId, fromUserId)
                .eq(UserFriend::getStatus, UserFriend.STATUS_PENDING)
                .last("LIMIT 1"));
        if (reversePending != null) {
            friendMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<UserFriend>()
                    .eq(UserFriend::getId, reversePending.getId())
                    .set(UserFriend::getStatus, UserFriend.STATUS_ACCEPTED));
            insertAccepted(fromUserId, toUserId);
            notifyService.onFriendAccepted(toUserId, fromUserId);
            log.info("[好友] {} 与 {} 互为申请，自动成为好友", fromUserId, toUserId);
            return;
        }

        // 我方是否已发过申请
        Long sent = friendMapper.selectCount(new LambdaQueryWrapper<UserFriend>()
                .eq(UserFriend::getUserId, fromUserId)
                .eq(UserFriend::getFriendId, toUserId)
                .eq(UserFriend::getStatus, UserFriend.STATUS_PENDING));
        if (sent != null && sent > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "已发送过好友申请，请等待对方处理");
        }

        UserFriend pending = new UserFriend();
        pending.setUserId(fromUserId);
        pending.setFriendId(toUserId);
        pending.setStatus(UserFriend.STATUS_PENDING);
        friendMapper.insert(pending);
        notifyService.onFriendRequested(toUserId, fromUserId);
        log.info("[好友] {} 向 {} 发起好友申请", fromUserId, toUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void acceptFriend(long myUserId, long requesterId) {
        UserFriend pending = friendMapper.selectOne(new LambdaQueryWrapper<UserFriend>()
                .eq(UserFriend::getUserId, requesterId)
                .eq(UserFriend::getFriendId, myUserId)
                .eq(UserFriend::getStatus, UserFriend.STATUS_PENDING)
                .last("LIMIT 1"));
        if (pending == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "没有待处理的好友申请");
        }
        // 对方行置为通过
        pending.setStatus(UserFriend.STATUS_ACCEPTED);
        friendMapper.updateById(pending);
        // 我方补一行反向关系
        insertAccepted(myUserId, requesterId);
        // 顺带把我方早先发给对方的申请也置为通过(如双向申请场景)
        friendMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<UserFriend>()
                .eq(UserFriend::getUserId, myUserId)
                .eq(UserFriend::getFriendId, requesterId)
                .eq(UserFriend::getStatus, UserFriend.STATUS_PENDING)
                .set(UserFriend::getStatus, UserFriend.STATUS_ACCEPTED));

        notifyService.onFriendAccepted(requesterId, myUserId);
        log.info("[好友] {} 通过了 {} 的好友申请", myUserId, requesterId);
    }

    @Override
    public void rejectFriend(long myUserId, long requesterId) {
        friendMapper.delete(new LambdaQueryWrapper<UserFriend>()
                .eq(UserFriend::getUserId, requesterId)
                .eq(UserFriend::getFriendId, myUserId)
                .eq(UserFriend::getStatus, UserFriend.STATUS_PENDING));
        notifyService.removeFriendRequest(myUserId, requesterId);
        log.info("[好友] {} 拒绝了 {} 的好友申请", myUserId, requesterId);
    }

    @Override
    public void removeFriend(long myUserId, long targetUserId) {
        friendMapper.delete(new LambdaQueryWrapper<UserFriend>()
                .and(w -> w.eq(UserFriend::getUserId, myUserId).eq(UserFriend::getFriendId, targetUserId)
                        .or().eq(UserFriend::getUserId, targetUserId).eq(UserFriend::getFriendId, myUserId)));
        log.info("[好友] {} 与 {} 解除好友关系", myUserId, targetUserId);
    }

    @Override
    public List<AuthorVO> friendList(long myUserId) {
        Set<Long> ids = new LinkedHashSet<>();
        friendMapper.selectList(new LambdaQueryWrapper<UserFriend>()
                        .eq(UserFriend::getUserId, myUserId)
                        .eq(UserFriend::getStatus, UserFriend.STATUS_ACCEPTED))
                .forEach(f -> ids.add(f.getFriendId()));
        friendMapper.selectList(new LambdaQueryWrapper<UserFriend>()
                        .eq(UserFriend::getFriendId, myUserId)
                        .eq(UserFriend::getStatus, UserFriend.STATUS_ACCEPTED))
                .forEach(f -> ids.add(f.getUserId()));
        return toAuthorList(ids);
    }

    @Override
    public List<AuthorVO> pendingFriendList(long myUserId) {
        Set<Long> ids = friendMapper.selectList(new LambdaQueryWrapper<UserFriend>()
                        .eq(UserFriend::getFriendId, myUserId)
                        .eq(UserFriend::getStatus, UserFriend.STATUS_PENDING))
                .stream().map(UserFriend::getUserId).collect(Collectors.toCollection(LinkedHashSet::new));
        return toAuthorList(ids);
    }

    // ==================== 黑名单 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addBlacklist(long myUserId, long targetUserId) {
        if (myUserId == targetUserId) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不能拉黑自己");
        }
        requireUser(targetUserId);
        Long exists = blacklistMapper.selectCount(new LambdaQueryWrapper<UserBlacklist>()
                .eq(UserBlacklist::getUserId, myUserId)
                .eq(UserBlacklist::getBlackUserId, targetUserId));
        if (exists == null || exists == 0) {
            UserBlacklist block = new UserBlacklist();
            block.setUserId(myUserId);
            block.setBlackUserId(targetUserId);
            blacklistMapper.insert(block);
        }
        // 拉黑即解除好友关系(含申请中记录)
        friendMapper.delete(new LambdaQueryWrapper<UserFriend>()
                .and(w -> w.eq(UserFriend::getUserId, myUserId).eq(UserFriend::getFriendId, targetUserId)
                        .or().eq(UserFriend::getUserId, targetUserId).eq(UserFriend::getFriendId, myUserId)));
        log.info("[黑名单] {} 拉黑了 {}（双向阻断）", myUserId, targetUserId);
    }

    @Override
    public void removeBlacklist(long myUserId, long targetUserId) {
        blacklistMapper.delete(new LambdaQueryWrapper<UserBlacklist>()
                .eq(UserBlacklist::getUserId, myUserId)
                .eq(UserBlacklist::getBlackUserId, targetUserId));
        log.info("[黑名单] {} 解除了对 {} 的拉黑", myUserId, targetUserId);
    }

    @Override
    public List<AuthorVO> blacklistList(long myUserId) {
        Set<Long> ids = blacklistMapper.selectList(new LambdaQueryWrapper<UserBlacklist>()
                        .eq(UserBlacklist::getUserId, myUserId))
                .stream().map(UserBlacklist::getBlackUserId).collect(Collectors.toCollection(LinkedHashSet::new));
        return toAuthorList(ids);
    }

    // ==================== 关系判断(供聊天等复用) ====================

    @Override
    public boolean isFriend(long userIdA, long userIdB) {
        Long count = friendMapper.selectCount(new LambdaQueryWrapper<UserFriend>()
                .eq(UserFriend::getStatus, UserFriend.STATUS_ACCEPTED)
                .and(w -> w.eq(UserFriend::getUserId, userIdA).eq(UserFriend::getFriendId, userIdB)
                        .or().eq(UserFriend::getUserId, userIdB).eq(UserFriend::getFriendId, userIdA)));
        return count != null && count > 0;
    }

    @Override
    public boolean isBlocked(long userIdA, long userIdB) {
        Long count = blacklistMapper.selectCount(new LambdaQueryWrapper<UserBlacklist>()
                .and(w -> w.eq(UserBlacklist::getUserId, userIdA).eq(UserBlacklist::getBlackUserId, userIdB)
                        .or().eq(UserBlacklist::getUserId, userIdB).eq(UserBlacklist::getBlackUserId, userIdA)));
        return count != null && count > 0;
    }

    // ==================== 私有辅助 ====================

    private void requireUser(long userId) {
        if (userMapper.selectById(userId) == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
    }

    /** 插入一条 ACCEPTED 关系(防止与既有行冲突，先查再插) */
    private void insertAccepted(long userId, long friendId) {
        Long exists = friendMapper.selectCount(new LambdaQueryWrapper<UserFriend>()
                .eq(UserFriend::getUserId, userId)
                .eq(UserFriend::getFriendId, friendId));
        if (exists == null || exists == 0) {
            UserFriend f = new UserFriend();
            f.setUserId(userId);
            f.setFriendId(friendId);
            f.setStatus(UserFriend.STATUS_ACCEPTED);
            friendMapper.insert(f);
        }
    }

    private List<AuthorVO> toAuthorList(Set<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return new ArrayList<>();
        }
        Map<Long, SysUser> userMap = userMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity(), (a, b) -> a));
        List<AuthorVO> list = new ArrayList<>(ids.size());
        for (Long id : ids) {
            SysUser u = userMap.get(id);
            if (u != null) {
                list.add(AuthorVOs.of(u));
            }
        }
        return list;
    }
}
