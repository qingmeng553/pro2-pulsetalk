package com.community.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.community.entity.ChatMessage;
import com.community.mapper.ChatMessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 私聊记录过期清理任务
 *
 * <p>产品规则：聊天记录仅保留 7 天。
 * 每天凌晨执行一次，物理删除 7 天前(不含7天前当天？此处取“严格早于7天前0点”以上一天为界)
 * 为便于理解：删除 create_time &lt; now-7days 的消息。
 * 说明：私聊数据属于临时会话数据，过期即物理清除，不违反“业务主数据逻辑删除”的约束。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatHistoryCleanTask {

    /** 保留天数 */
    private static final int KEEP_DAYS = 7;

    private final ChatMessageMapper chatMessageMapper;

    /** 每天 04:10 执行 */
    @Scheduled(cron = "0 10 4 * * *")
    public void cleanExpiredMessages() {
        LocalDateTime deadline = LocalDateTime.now().minusDays(KEEP_DAYS);
        try {
            int removed = chatMessageMapper.delete(new LambdaQueryWrapper<ChatMessage>()
                    .lt(ChatMessage::getCreateTime, deadline));
            if (removed > 0) {
                log.info("[聊天清理] 已清理 {} 天前的私聊消息 {} 条", KEEP_DAYS, removed);
            }
        } catch (RuntimeException e) {
            log.warn("[聊天清理] 执行失败: {}", e.getMessage());
        }
    }
}
