package com.community.service.impl;

import com.community.common.api.ResultCode;
import com.community.common.constant.RedisKeys;
import com.community.common.exception.BusinessException;
import com.community.service.CaptchaService;
import com.community.service.RateLimitService;
import com.community.vo.CaptchaVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;

/** 图形验证码实现：4 位字母数字，PNG base64，Redis 2 分钟一次性。 */
@Service
@RequiredArgsConstructor
public class CaptchaServiceImpl implements CaptchaService {

    /** 去掉 0/O/1/I 等易混淆字符 */
    private static final char[] CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate redisTemplate;
    private final RateLimitService rateLimitService;

    @Override
    public CaptchaVO generate(String ip) {
        // 同 IP 每分钟最多生成 30 次，防刷图
        rateLimitService.check(RedisKeys.captchaIpLimitKey(ip), 30, 60);

        String code = randomCode();
        String captchaId = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(
                RedisKeys.captchaKey(captchaId),
                code.toLowerCase(Locale.ROOT),
                Duration.ofMinutes(2));
        return new CaptchaVO(captchaId, render(code));
    }

    @Override
    public boolean verify(String captchaId, String code) {
        if (!StringUtils.hasText(captchaId) || !StringUtils.hasText(code)) {
            return false;
        }
        String key = RedisKeys.captchaKey(captchaId);
        String saved = redisTemplate.opsForValue().get(key);
        // 一次性：无论成功失败都删除，避免复用
        redisTemplate.delete(key);
        return saved != null && saved.equalsIgnoreCase(code.trim());
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder(4);
        for (int i = 0; i < 4; i++) {
            sb.append(CHARS[RANDOM.nextInt(CHARS.length)]);
        }
        return sb.toString();
    }

    private String render(String code) {
        int width = 120;
        int height = 40;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, width, height);

            // 干扰线
            g.setStroke(new BasicStroke(1f));
            for (int i = 0; i < 6; i++) {
                g.setColor(randomColor(180, 230));
                g.drawLine(RANDOM.nextInt(width), RANDOM.nextInt(height),
                        RANDOM.nextInt(width), RANDOM.nextInt(height));
            }

            // 字符
            g.setFont(new Font("SansSerif", Font.BOLD, 26));
            for (int i = 0; i < code.length(); i++) {
                g.setColor(randomColor(20, 120));
                int x = 14 + i * 26;
                int y = 30 + RANDOM.nextInt(8) - 4;
                g.drawString(String.valueOf(code.charAt(i)), x, y);
            }

            // 噪点
            for (int i = 0; i < 60; i++) {
                g.setColor(randomColor(120, 220));
                g.fillRect(RANDOM.nextInt(width), RANDOM.nextInt(height), 1, 1);
            }
        } finally {
            g.dispose();
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (IOException e) {
            throw new BusinessException(ResultCode.ERROR, "图形验证码生成失败");
        }
    }

    private Color randomColor(int min, int max) {
        int range = Math.max(1, max - min);
        return new Color(min + RANDOM.nextInt(range), min + RANDOM.nextInt(range), min + RANDOM.nextInt(range));
    }
}
