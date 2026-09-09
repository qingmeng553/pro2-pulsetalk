package com.community.service;

import com.community.dto.ChatSendDTO;
import com.community.vo.ChatMessageVO;
import com.community.vo.ChatThreadVO;
import com.community.vo.PageVO;

import java.util.List;

/**
 * 私聊服务
 *
 * <p>规则：
 * <ul>
 *   <li>非好友双方各自最多发送 1 条消息，成为好友后可畅聊</li>
 *   <li>拉黑(双向)后无法发送消息，会话列表互相隐藏</li>
 *   <li>聊天记录仅保留 7 天，由定时任务清理</li>
 * </ul>
 */
public interface ChatService {

    /**
     * 会话列表(最近联系人，按最后消息时间倒序；已拉黑会话隐藏)
     */
    List<ChatThreadVO> threadList(long myUserId);

    /**
     * 与某用户的聊天消息。
     * <p>afterId 为空时按页正序分页；afterId 非空时返回大于该 ID 的新消息(前端轮询增量拉取)
     */
    PageVO<ChatMessageVO> messages(long myUserId, long otherUserId, long page, long size, Long afterId);

    /**
     * 发送私聊消息(校验黑名单/好友/陌生人一条限制)
     */
    ChatMessageVO send(long fromUserId, long toUserId, ChatSendDTO dto);

    /**
     * 将某会话标记为已读
     */
    void markRead(long myUserId, long otherUserId);

    /**
     * 全部未读私信数(用于顶栏角标)
     */
    long totalUnread(long myUserId);
}
