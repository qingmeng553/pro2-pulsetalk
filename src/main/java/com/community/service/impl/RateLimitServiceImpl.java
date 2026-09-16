package com.community.service.impl;

import com.community.common.api.ResultCode;
import com.community.common.exception.BusinessException;
import com.community.service.RateLimitService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/** Redis 固定窗口限流实现。 */
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
