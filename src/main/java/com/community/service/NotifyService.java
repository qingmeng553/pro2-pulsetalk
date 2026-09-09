package com.community.service;

import com.community.vo.NotifyItemVO;
import com.community.vo.PageVO;

import java.util.List;

/**
 * 站内消息通知服务
 *
 * <p>覆盖事件：有人赞了你(LIKE)、评论了你(COMMENT)、收藏了你(COLLECT)、
 * 申请/通过好友(FRIEND)。点赞/收藏取消时同步撤回未读通知。
 */
public interface NotifyService {

    /** 赞了你的帖子时通知(自己赞自己不发) */
    void onPostLiked(long postOwnerId, long actorId, long postId);

    /** 取消赞时撤回对应未读通知 */
    void removePostLiked(long postOwnerId, long actorId, long postId);

    /** 收藏了你的帖子时通知(自己收藏自己不发) */
    void onPostCollected(long postOwnerId, long actorId, long postId);

    /** 取消收藏时撤回对应未读通知 */
    void removePostCollected(long postOwnerId, long actorId, long postId);

    /** 评论了你的帖子时通知(自己评论自己不发) */
    void onPostCommented(long postOwnerId, long actorId, long postId, long commentId, String contentPreview);

    /** 收到好友申请时通知 */
    void onFriendRequested(long targetUserId, long requesterId);

    /** 好友申请被通过时通知申请方 */
    void onFriendAccepted(long requesterId, long accepterId);

    /** 好友申请被拒绝/撤回时清理待处理通知 */
    void removeFriendRequest(long targetUserId, long requesterId);

    /**
     * 管理员向全体用户发送官方消息(SYSTEM 通知，除发送者外每人一条)
     *
     * @param adminId 发送管理员ID
     * @param content 官方消息内容
     */
    void broadcast(long adminId, String content);

    /**
     * 我的通知分页(最新在前)
     */
    PageVO<NotifyItemVO> pageList(long userId, long page, long size);

    /**
     * 未读通知数
     */
    long unreadCount(long userId);

    /**
     * 标记已读；ids 为空表示全部
     */
    void markRead(long userId, List<Long> ids);
}
