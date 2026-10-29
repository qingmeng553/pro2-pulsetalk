package com.community.task;

import com.community.service.RankService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** 启动时预热热度榜：Redis 数据丢失(换容器/清库)后按数据库计数恢复。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RankWarmUpRunner implements ApplicationRunner {

    private final RankService rankService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            rankService.rebuildFromDbIfEmpty();
        } catch (RuntimeException e) {
            log.warn("[热度榜] 预热失败: {}", e.getMessage());
        }
    }
}
