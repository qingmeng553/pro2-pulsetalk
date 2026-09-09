package com.community.service.impl;

import com.community.common.api.ResultCode;
import com.community.common.exception.BusinessException;
import com.community.service.RateLimitService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis 固定窗口限流实现
 *
 * <p>原理：对限流键执行 INCR，首次 +1 时同时设置过期时间(窗口)；窗口内计数超过阈值即拒绝。
 * 固定窗口在窗口临界处可能有 2 倍流量突刺，胜在实现简单、无锁；如需更平滑可演进为滑动窗口/令牌桶(面试可扩展点)。
 */
@Service
@RequiredArgsConstructor
public class RateLimitServiceImpl implements RateLimitService {

    private final StringRedisTemplate redisTemplate;

    @Override
    public void check(String key, int max, int seconds) {
        Long count = redisTemplate.opsForValue().increment(key);
        // 第一次访问：设置窗口过期时间
        if (count != null && count == 1L) {
            redisTemplate.expire(key, Duration.ofSeconds(seconds));
        }
        if (count != null && count > max) {
            throw new BusinessException(ResultCode.TOO_MANY_REQUESTS, "操作太频繁啦，请稍后再试 ~");
        }
    }

    @Override
    public ResultCode tooManyCode() {
        return ResultCode.TOO_MANY_REQUESTS;
    }
}
