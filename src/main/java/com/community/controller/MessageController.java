package com.community.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.community.common.api.R;
import com.community.dto.BroadcastDTO;
import com.community.dto.NotifyReadDTO;
import com.community.service.NotifyService;
import com.community.vo.NotifyItemVO;
import com.community.vo.PageVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 站内消息(消息板块)接口 —— 看谁赞了我/评论了我/收藏了我/加我好友
 *
 * <pre>
 * GET  /notify/list          通知分页(需登录)
 * GET  /notify/unread-count  未读数(顶栏角标)
 * POST /notify/read          标记已读(ids 为空=全部)
 * POST /notify/broadcast     管理员向全体用户发送官方消息(SYSTEM，admin 角色)
 * </pre>
 */
@RestController
@RequestMapping("/notify")
@RequiredArgsConstructor
public class MessageController {

    private final NotifyService notifyService;

    /** 通知分页(最新在前) */
    @SaCheckLogin
    @GetMapping("/list")
    public R<PageVO<NotifyItemVO>> list(@RequestParam(defaultValue = "1") long page,
                                        @RequestParam(defaultValue = "20") long size) {
        return R.ok(notifyService.pageList(StpUtil.getLoginIdAsLong(), page, size));
    }

    /** 未读通知数 */
    @SaCheckLogin
    @GetMapping("/unread-count")
    public R<Map<String, Long>> unreadCount() {
        Map<String, Long> data = new HashMap<>();
        data.put("count", notifyService.unreadCount(StpUtil.getLoginIdAsLong()));
        return R.ok(data);
    }

    /** 标记已读(不传 ids 则全部已读) */
    @SaCheckLogin
    @PostMapping("/read")
    public R<Void> markRead(@RequestBody(required = false) @Valid NotifyReadDTO dto) {
        notifyService.markRead(StpUtil.getLoginIdAsLong(),
                dto == null ? null : dto.getIds());
        return R.ok();
    }

    /** 管理员向全体用户发送官方消息(SYSTEM 通知) */
    @SaCheckRole("admin")
    @PostMapping("/broadcast")
    public R<Void> broadcast(@Valid @RequestBody BroadcastDTO dto) {
        notifyService.broadcast(StpUtil.getLoginIdAsLong(), dto.getContent());
        return R.ok();
    }
}
