package com.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.common.api.ResultCode;
import com.community.common.exception.BusinessException;
import com.community.common.util.AuthorVOs;
import com.community.dto.ChatSendDTO;
import com.community.entity.ChatMessage;
import com.community.entity.SysUser;
import com.community.mapper.ChatMessageMapper;
import com.community.mapper.SysUserMapper;
import com.community.service.ChatService;
import com.community.service.RelationService;
import com.community.vo.ChatMessageVO;
import com.community.vo.ChatThreadVO;
import com.community.vo.PageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 私聊服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatMessageMapper chatMessageMapper;
    private final SysUserMapper userMapper;
    private final RelationService relationService;

    @Override
    public List<ChatThreadVO> threadList(long myUserId) {
        // 拉最近若干条与我相关的消息(时间倒序)，再在内存按会话对象聚合出“最后一条”
        List<ChatMessage> recent = chatMessageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getFromUserId, myUserId)
                .or()
                .eq(ChatMessage::getToUserId, myUserId)
                .orderByDesc(ChatMessage::getId)
                .last("LIMIT 300"));
        if (CollectionUtils.isEmpty(recent)) {
            return new ArrayList<>();
        }

        // 保持“最后活跃优先”的顺序
        LinkedHashMap<Long, ChatMessage> lastByUser = new LinkedHashMap<>();
        for (ChatMessage m : recent) {
            Long other = Objects.equals(m.getFromUserId(), myUserId) ? m.getToUserId() : m.getFromUserId();
            lastByUser.putIfAbsent(other, m);
        }

        // 未读数聚合
        Map<Long, Long> unreadMap = new LinkedHashMap<>();
        List<ChatMessage> unread = chatMessageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getToUserId, myUserId)
                .eq(ChatMessage::getIsRead, 0));
        for (ChatMessage m : unread) {
            unreadMap.merge(m.getFromUserId(), 1L, Long::sum);
        }

        // 会话对象信息
        Map<Long, SysUser> userMap = batchUsers(lastByUser.keySet());

        List<ChatThreadVO> threads = new ArrayList<>();
        for (Map.Entry<Long, ChatMessage> e : lastByUser.entrySet()) {
            Long otherId = e.getKey();
            // 拉黑双向阻断：会话互相隐藏
            if (relationService.isBlocked(myUserId, otherId)) {
                continue;
            }
            SysUser other = userMap.get(otherId);
            if (other == null) {
                continue;
            }
            ChatMessage last = e.getValue();
            ChatThreadVO vo = new ChatThreadVO();
            vo.setUser(AuthorVOs.of(other));
            vo.setLastMessage(preview(last.getContent()));
            vo.setLastTime(last.getCreateTime());
            vo.setUnread(unreadMap.getOrDefault(otherId, 0L));
            vo.setBlocked(false);
            threads.add(vo);
        }
        return threads;
    }

    @Override
    public PageVO<ChatMessageVO> messages(long myUserId, long otherUserId, long page, long size, Long afterId) {
        if (relationService.isBlocked(myUserId, otherUserId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "你们处于拉黑状态，无法查看会话");
        }
        // 会话消息 = (我→他) ∪ (他→我)
        LambdaQueryWrapper<ChatMessage> qw = new LambdaQueryWrapper<ChatMessage>()
                .and(w -> w.eq(ChatMessage::getFromUserId, myUserId).eq(ChatMessage::getToUserId, otherUserId))
                .or(w -> w.eq(ChatMessage::getFromUserId, otherUserId).eq(ChatMessage::getToUserId, myUserId));

        // 增量拉取模式(afterId)：返回该ID之后的新消息(轮询用)
        if (afterId != null) {
            qw.gt(ChatMessage::getId, afterId).orderByAsc(ChatMessage::getId).last("LIMIT 200");
            List<ChatMessage> rows = chatMessageMapper.selectList(qw);
            PageVO<ChatMessageVO> vo = new PageVO<>();
            vo.setRecords(rows.stream().map(this::toVO).toList());
            vo.setTotal((long) rows.size());
            vo.setCurrent(1L);
            vo.setSize((long) rows.size());
            vo.setPages(1L);
            return vo;
        }

        // 普通分页(时间正序)
        qw.orderByAsc(ChatMessage::getId);
        Page<ChatMessage> p = chatMessageMapper.selectPage(new Page<>(page, size), qw);
        return PageVO.of(p, this::toVO);
    }

    @Override
    public ChatMessageVO send(long fromUserId, long toUserId, ChatSendDTO dto) {
        String content = dto.getContent() == null ? "" : dto.getContent().trim();
        if (content.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "消息内容不能为空");
        }
        if (fromUserId == toUserId) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不能给自己发消息");
        }
        if (userMapper.selectById(toUserId) == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "对方不存在");
        }
        // 黑名单双向阻断
        if (relationService.isBlocked(fromUserId, toUserId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "你们处于拉黑状态，无法发送消息");
        }
        // 非好友：各自仅可发送 1 条打招呼消息
        if (!relationService.isFriend(fromUserId, toUserId)) {
            Long sent = chatMessageMapper.selectCount(new LambdaQueryWrapper<ChatMessage>()
                    .eq(ChatMessage::getFromUserId, fromUserId)
                    .eq(ChatMessage::getToUserId, toUserId));
            if (sent != null && sent > 0) {
                throw new BusinessException(ResultCode.FORBIDDEN,
                        "你们还不是好友：非好友双方各自只能发送一条打招呼消息，成为好友后可畅聊");
            }
        }

        ChatMessage msg = new ChatMessage();
        msg.setFromUserId(fromUserId);
        msg.setToUserId(toUserId);
        msg.setContent(content);
        msg.setIsRead(0);
        chatMessageMapper.insert(msg);
        log.info("[私聊] {} → {} 发送消息 messageId={}", fromUserId, toUserId, msg.getId());
        return toVO(msg);
    }

    @Override
    public void markRead(long myUserId, long otherUserId) {
        chatMessageMapper.update(null, new LambdaUpdateWrapper<ChatMessage>()
                .eq(ChatMessage::getToUserId, myUserId)
                .eq(ChatMessage::getFromUserId, otherUserId)
                .eq(ChatMessage::getIsRead, 0)
                .set(ChatMessage::getIsRead, 1));
    }

    @Override
    public long totalUnread(long myUserId) {
        Long count = chatMessageMapper.selectCount(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getToUserId, myUserId)
                .eq(ChatMessage::getIsRead, 0));
        return count == null ? 0L : count;
    }

    // ==================== 私有辅助 ====================

    private Map<Long, SysUser> batchUsers(java.util.Collection<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Map.of();
        }
        return userMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity(), (a, b) -> a));
    }

    private ChatMessageVO toVO(ChatMessage m) {
        ChatMessageVO vo = new ChatMessageVO();
        vo.setId(m.getId());
        vo.setFromUserId(m.getFromUserId());
        vo.setToUserId(m.getToUserId());
        vo.setContent(m.getContent());
        vo.setCreateTime(m.getCreateTime());
        return vo;
    }

    private String preview(String content) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        return content.length() > 60 ? content.substring(0, 60) + "…" : content;
    }
}
