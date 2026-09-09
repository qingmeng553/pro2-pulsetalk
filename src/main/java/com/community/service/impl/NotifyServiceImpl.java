package com.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.common.util.AuthorVOs;
import com.community.entity.Post;
import com.community.entity.SysUser;
import com.community.entity.UserNotify;
import com.community.mapper.PostMapper;
import com.community.mapper.SysUserMapper;
import com.community.mapper.UserNotifyMapper;
import com.community.service.NotifyService;
import com.community.vo.AuthorVO;
import com.community.vo.NotifyItemVO;
import com.community.vo.PageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 站内消息通知服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotifyServiceImpl implements NotifyService {

    private final UserNotifyMapper notifyMapper;
    private final SysUserMapper userMapper;
    private final PostMapper postMapper;

    @Override
    public void onPostLiked(long postOwnerId, long actorId, long postId) {
        if (postOwnerId == actorId) {
            return;
        }
        save(postOwnerId, actorId, UserNotify.TYPE_LIKE, postId, null, null);
    }

    @Override
    public void removePostLiked(long postOwnerId, long actorId, long postId) {
        notifyMapper.delete(new LambdaQueryWrapper<UserNotify>()
                .eq(UserNotify::getUserId, postOwnerId)
                .eq(UserNotify::getActorId, actorId)
                .eq(UserNotify::getType, UserNotify.TYPE_LIKE)
                .eq(UserNotify::getPostId, postId)
                .eq(UserNotify::getIsRead, 0));
    }

    @Override
    public void onPostCollected(long postOwnerId, long actorId, long postId) {
        if (postOwnerId == actorId) {
            return;
        }
        save(postOwnerId, actorId, UserNotify.TYPE_COLLECT, postId, null, null);
    }

    @Override
    public void removePostCollected(long postOwnerId, long actorId, long postId) {
        notifyMapper.delete(new LambdaQueryWrapper<UserNotify>()
                .eq(UserNotify::getUserId, postOwnerId)
                .eq(UserNotify::getActorId, actorId)
                .eq(UserNotify::getType, UserNotify.TYPE_COLLECT)
                .eq(UserNotify::getPostId, postId)
                .eq(UserNotify::getIsRead, 0));
    }

    @Override
    public void onPostCommented(long postOwnerId, long actorId, long postId, long commentId, String contentPreview) {
        if (postOwnerId == actorId) {
            return;
        }
        save(postOwnerId, actorId, UserNotify.TYPE_COMMENT, postId, commentId, contentPreview);
    }

    @Override
    public void onFriendRequested(long targetUserId, long requesterId) {
        save(targetUserId, requesterId, UserNotify.TYPE_FRIEND, null, null, "请求添加你为好友");
    }

    @Override
    public void onFriendAccepted(long requesterId, long accepterId) {
        // 删除申请时的待处理通知(若仍未读)
        notifyMapper.delete(new LambdaQueryWrapper<UserNotify>()
                .eq(UserNotify::getUserId, accepterId)
                .eq(UserNotify::getActorId, requesterId)
                .eq(UserNotify::getType, UserNotify.TYPE_FRIEND)
                .eq(UserNotify::getIsRead, 0));
        // 通知申请方：已通过
        save(requesterId, accepterId, UserNotify.TYPE_FRIEND, null, null, "通过了你的好友申请，可以开始畅聊啦");
    }

    @Override
    public void removeFriendRequest(long targetUserId, long requesterId) {
        notifyMapper.delete(new LambdaQueryWrapper<UserNotify>()
                .eq(UserNotify::getUserId, targetUserId)
                .eq(UserNotify::getActorId, requesterId)
                .eq(UserNotify::getType, UserNotify.TYPE_FRIEND)
                .eq(UserNotify::getIsRead, 0));
    }

    @Override
    public void broadcast(long adminId, String content) {
        String text = content == null ? "" : content.trim();
        if (text.isEmpty()) {
            return;
        }
        // 全体用户(除发送者本人)各写一条 SYSTEM 官方消息
        List<SysUser> receivers = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .select(SysUser::getId)
                .ne(SysUser::getId, adminId));
        int sent = 0;
        for (SysUser receiver : receivers) {
            save(receiver.getId(), adminId, UserNotify.TYPE_SYSTEM, null, null, text);
            sent++;
        }
        log.info("[官方消息] 管理员 {} 向 {} 位用户广播官方消息", adminId, sent);
    }

    @Override
    public PageVO<NotifyItemVO> pageList(long userId, long page, long size) {
        Page<UserNotify> p = notifyMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<UserNotify>()
                        .eq(UserNotify::getUserId, userId)
                        .orderByDesc(UserNotify::getId));

        List<UserNotify> records = p.getRecords();
        if (CollectionUtils.isEmpty(records)) {
            return emptyPage(p);
        }

        // 批量取触发人与帖子信息，避免 N+1
        Set<Long> actorIds = records.stream().map(UserNotify::getActorId).collect(Collectors.toSet());
        Set<Long> postIds = records.stream().map(UserNotify::getPostId)
                .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Map<Long, SysUser> userMap = actorIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(actorIds).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity(), (a, b) -> a));
        Map<Long, String> postTitleMap = postIds.isEmpty() ? Map.of()
                : postMapper.selectBatchIds(postIds).stream()
                .collect(Collectors.toMap(Post::getId, Post::getTitle, (a, b) -> a));

        List<NotifyItemVO> voList = new ArrayList<>(records.size());
        for (UserNotify n : records) {
            NotifyItemVO vo = new NotifyItemVO();
            vo.setId(n.getId());
            vo.setType(n.getType());
            vo.setActor(AuthorVOs.of(userMap.get(n.getActorId())));
            vo.setPostId(n.getPostId());
            vo.setCommentId(n.getCommentId());
            String title = n.getPostId() == null ? null : postTitleMap.get(n.getPostId());
            vo.setPostTitle(title == null ? "帖子已删除" : title);
            vo.setContent(n.getContent());
            vo.setRead(n.getIsRead() != null && n.getIsRead() == 1);
            vo.setCreateTime(n.getCreateTime());
            voList.add(vo);
        }

        PageVO<NotifyItemVO> result = new PageVO<>();
        result.setRecords(voList);
        result.setTotal(p.getTotal());
        result.setCurrent(p.getCurrent());
        result.setSize(p.getSize());
        result.setPages(p.getPages());
        return result;
    }

    @Override
    public long unreadCount(long userId) {
        Long count = notifyMapper.selectCount(new LambdaQueryWrapper<UserNotify>()
                .eq(UserNotify::getUserId, userId)
                .eq(UserNotify::getIsRead, 0));
        return count == null ? 0L : count;
    }

    @Override
    public void markRead(long userId, List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            // 全部已读
            notifyMapper.update(null, new LambdaUpdateWrapper<UserNotify>()
                    .eq(UserNotify::getUserId, userId)
                    .eq(UserNotify::getIsRead, 0)
                    .set(UserNotify::getIsRead, 1));
        } else {
            notifyMapper.update(null, new LambdaUpdateWrapper<UserNotify>()
                    .eq(UserNotify::getUserId, userId)
                    .in(UserNotify::getId, ids)
                    .eq(UserNotify::getIsRead, 0)
                    .set(UserNotify::getIsRead, 1));
        }
    }

    /** 写一条通知 */
    private void save(long userId, long actorId, String type, Long postId, Long commentId, String content) {
        UserNotify notify = new UserNotify();
        notify.setUserId(userId);
        notify.setActorId(actorId);
        notify.setType(type);
        notify.setPostId(postId);
        notify.setCommentId(commentId);
        notify.setContent(content);
        notify.setIsRead(0);
        notifyMapper.insert(notify);
    }

    private PageVO<NotifyItemVO> emptyPage(Page<UserNotify> p) {
        PageVO<NotifyItemVO> vo = new PageVO<>();
        vo.setRecords(Collections.emptyList());
        vo.setTotal(p.getTotal());
        vo.setCurrent(p.getCurrent());
        vo.setSize(p.getSize());
        vo.setPages(p.getPages());
        return vo;
    }
}
