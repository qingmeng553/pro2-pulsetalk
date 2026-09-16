package com.community.config;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.dao.SaTokenDaoRedisJackson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/** 强制将 Sa-Token 持久层绑定为 Redis 实现。 */
@Slf4j
@Configuration
public class SaTokenDaoBinder {

    @Bean
    public ApplicationRunner bindRedisSaTokenDao(ApplicationContext context) {
        return args -> {
            Map<String, SaTokenDao> daoBeans = context.getBeansOfType(SaTokenDao.class);
            SaTokenDao redisDao = daoBeans.values().stream()
                    .filter(dao -> dao instanceof SaTokenDaoRedisJackson)
                    .findFirst()
                    .orElse(null);
            if (redisDao == null) {
                log.warn("[Sa-Token] 未找到 Redis 持久层实现(SaTokenDaoRedisJackson)，会话将退化为内存存储！");
                return;
            }
            if (SaManager.getSaTokenDao() != redisDao) {
                SaManager.setSaTokenDao(redisDao);
                log.info("[Sa-Token] 会话持久层已切换为 Redis(Jackson序列化)");
            } else {
                log.info("[Sa-Token] 会话持久层已确认使用 Redis");
            }
        };
    }
}
