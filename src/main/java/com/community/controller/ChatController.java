package com.community.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.community.common.api.R;
import com.community.dto.ChatSendDTO;
import com.community.service.ChatService;
import com.community.vo.ChatMessageVO;
import com.community.vo.ChatThreadVO;
import com.community.vo.PageVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 私聊接口(需登录)
 *
 * <pre>
 * GET  /chat/threads           会话列表(最近联系人；拉黑会话隐藏)
 * GET  /chat/messages/{userId} 与某人消息(分页正序；afterId 增量拉取供轮询)
 * POST /chat/send/{userId}     发送消息(黑名单校验/非好友限1条)
 * POST /chat/read/{userId}     会话标记已读
 * GET  /chat/unread-count      全部未读私信数(顶栏角标)
 * </pre>
 */
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /** 会话列表 */
    @SaCheckLogin
    @GetMapping("/threads")
    public R<List<ChatThreadVO>> threads() {
        return R.ok(chatService.threadList(StpUtil.getLoginIdAsLong()));
    }

    /** 消息记录(正序分页；afterId 非空时返回该ID之后的新消息) */
    @SaCheckLogin
    @GetMapping("/messages/{userId}")
    public R<PageVO<ChatMessageVO>> messages(@PathVariable Long userId,
                                             @RequestParam(defaultValue = "1") long page,
                                             @RequestParam(defaultValue = "20") long size,
                                             @RequestParam(required = false) Long afterId) {
        return R.ok(chatService.messages(StpUtil.getLoginIdAsLong(), userId, page, size, afterId));
    }

    /** 发送私聊消息 */
    @SaCheckLogin
    @PostMapping("/send/{userId}")
    public R<ChatMessageVO> send(@PathVariable Long userId, @Valid @RequestBody ChatSendDTO dto) {
        return R.ok(chatService.send(StpUtil.getLoginIdAsLong(), userId, dto));
    }

    /** 会话标记已读 */
    @SaCheckLogin
    @PostMapping("/read/{userId}")
    public R<Void> markRead(@PathVariable Long userId) {
        chatService.markRead(StpUtil.getLoginIdAsLong(), userId);
        return R.ok();
    }

    /** 全部未读私信数 */
    @SaCheckLogin
    @GetMapping("/unread-count")
    public R<Map<String, Long>> unreadCount() {
        Map<String, Long> data = new HashMap<>();
        data.put("count", chatService.totalUnread(StpUtil.getLoginIdAsLong()));
        return R.ok(data);
    }
}
