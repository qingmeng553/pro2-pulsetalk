package com.community.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.community.common.api.ResultCode;
import com.community.common.constant.RedisKeys;
import com.community.common.exception.BusinessException;
import com.community.common.util.PasswordUtil;
import com.community.dto.BindEmailDTO;
import com.community.dto.RebindEmailDTO;
import com.community.dto.ResetByCodeDTO;
import com.community.dto.SendCodeDTO;
import com.community.entity.SysUser;
import com.community.mapper.SysUserMapper;
import com.community.service.CaptchaService;
import com.community.service.EmailAuthService;
import com.community.service.MailService;
import com.community.service.RateLimitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** 邮箱绑定 / 换绑 / 邮箱找回实现。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailAuthServiceImpl implements EmailAuthService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final int DAILY_MAX = 10;
    private static final int IP_HOURLY_MAX = 20;
    private static final int FAIL_MAX = 5;
    private static final Duration FAIL_TTL = Duration.ofMinutes(5);

    private final SysUserMapper userMapper;
    private final StringRedisTemplate redisTemplate;
    private final RateLimitService rateLimitService;
    private final CaptchaService captchaService;
    private final MailService mailService;

    @Override
    public void sendCode(SendCodeDTO dto, String ip) {
        String scene = dto.getScene();
        String email = normalizeEmail(dto.getEmail());

        if (!captchaService.verify(dto.getCaptchaId(), dto.getCaptchaCode())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "图形验证码错误或已过期");
        }

        // 双限流：同邮箱每日、同 IP 每小时、同场景 60 秒
        String day = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String hour = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHH"));
        rateLimitService.check(RedisKeys.verifyQuotaKey(email, day), DAILY_MAX, 86400);
        rateLimitService.check(RedisKeys.verifyIpKey(ip, hour), IP_HOURLY_MAX, 3600);
        rateLimitService.check(RedisKeys.verifySendKey(scene, email), 1, 60);

        if (isLocked(scene, email)) {
            throw new BusinessException(ResultCode.TOO_MANY_REQUESTS, "验证码错误次数过多，请稍后再试");
        }

        if ("bind".equals(scene) || "rebind".equals(scene)) {
            SysUser user = requireLoginUser();
            if ("bind".equals(scene) && StringUtils.hasText(user.getEmail())) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "已绑定邮箱，请使用换绑");
            }
            if ("rebind".equals(scene)) {
                if (!StringUtils.hasText(user.getEmail())) {
                    throw new BusinessException(ResultCode.BAD_REQUEST, "尚未绑定邮箱，请先绑定");
                }
                if (email.equalsIgnoreCase(user.getEmail())) {
                    throw new BusinessException(ResultCode.BAD_REQUEST, "新邮箱不能与当前邮箱相同");
                }
            }
            if (emailTaken(email, user.getId())) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "该邮箱已被其他账号绑定");
            }
        } else if (!"reset".equals(scene)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "场景不合法");
        }

        // 防枚举：找回场景下，未绑定邮箱也返回成功，但不发信
        if ("reset".equals(scene)) {
            SysUser target = findByEmail(email);
            if (target == null || target.getEmailVerified() == null || target.getEmailVerified() != 1) {
                log.info("[邮箱] 找回验证码请求：邮箱未绑定，静默返回");
                return;
            }
        }

        String code = randomCode();
        String codeKey = RedisKeys.verifyCodeKey(scene, email);
        redisTemplate.opsForValue().set(codeKey, code, CODE_TTL);
        try {
            mailService.sendCode(email, code, scene);
        } catch (RuntimeException e) {
            // 发信失败就不要留下可用验证码，避免“接口报错但码已生成”
            redisTemplate.delete(codeKey);
            throw e;
        }
    }

    @Override
    public void bind(BindEmailDTO dto) {
        SysUser user = requireLoginUser();
        String email = normalizeEmail(dto.getEmail());

        if (StringUtils.hasText(user.getEmail())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "已绑定邮箱，请使用换绑");
        }
        if (!PasswordUtil.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "当前密码不正确");
        }
        verifyCode("bind", email, dto.getCode());
        if (emailTaken(email, user.getId())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "该邮箱已被其他账号绑定");
        }

        user.setEmail(email);
        user.setEmailVerified(1);
        try {
            userMapper.updateById(user);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "该邮箱已被其他账号绑定");
        }
        notify(email, "PulseTalk 邮箱绑定成功",
                "你的 PulseTalk 账号已绑定该邮箱，可用于找回密码。\n如非本人操作，请立即修改密码。");
        log.info("[邮箱] 用户 {} 绑定邮箱 {}", user.getId(), mask(email));    }

    @Override
    public void rebind(RebindEmailDTO dto) {
        SysUser user = requireLoginUser();
        String newEmail = normalizeEmail(dto.getNewEmail());

        if (!StringUtils.hasText(user.getEmail())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "尚未绑定邮箱，请先绑定");
        }
        if (newEmail.equalsIgnoreCase(user.getEmail())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "新邮箱不能与当前邮箱相同");
        }
        if (!PasswordUtil.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "当前密码不正确");
        }
        verifyCode("rebind", newEmail, dto.getCode());
        if (emailTaken(newEmail, user.getId())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "该邮箱已被其他账号绑定");
        }

        String oldEmail = user.getEmail();
        user.setEmail(newEmail);
        user.setEmailVerified(1);
        try {
            userMapper.updateById(user);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "该邮箱已被其他账号绑定");
        }
        notify(oldEmail, "PulseTalk 邮箱已更换",
                "你的 PulseTalk 账号绑定邮箱已被更换。如非本人操作，请立即联系管理员。");
        notify(newEmail, "PulseTalk 新邮箱绑定成功",
                "该邮箱已绑定到你的 PulseTalk 账号，可用于找回密码。");
        log.info("[邮箱] 用户 {} 换绑邮箱 {} -> {}", user.getId(), mask(oldEmail), mask(newEmail));
    }

    @Override
    public void resetByCode(ResetByCodeDTO dto) {
        String email = normalizeEmail(dto.getEmail());
        verifyCode("reset", email, dto.getCode());

        SysUser user = findByEmail(email);
        if (user == null) {
            // 验证码已消费，统一提示，不暴露账号是否存在
            throw new BusinessException(ResultCode.BAD_REQUEST, "验证码无效或已过期");
        }
        if (PasswordUtil.matches(dto.getNewPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "新密码不能与原密码相同");
        }

        user.setPassword(PasswordUtil.encode(dto.getNewPassword()));
        userMapper.updateById(user);
        // 踢掉该用户全部会话
        StpUtil.logout(user.getId());
        notify(email, "PulseTalk 密码已重置",
                "你的 PulseTalk 账号密码已通过邮箱验证码重置。\n如非本人操作，请立即联系管理员。");
        log.info("[邮箱] 用户 {} 通过邮箱验证码重置了密码", user.getId());
    }

    /** 校验验证码：一次性；失败累计 5 次锁定 5 分钟 */
    private void verifyCode(String scene, String email, String input) {
        if (isLocked(scene, email)) {
            throw new BusinessException(ResultCode.TOO_MANY_REQUESTS, "验证码错误次数过多，请稍后再试");
        }
        String key = RedisKeys.verifyCodeKey(scene, email);
        String saved = redisTemplate.opsForValue().get(key);
        if (saved == null || !saved.equals(input == null ? "" : input.trim())) {
            recordFail(scene, email);
            throw new BusinessException(ResultCode.BAD_REQUEST, "验证码无效或已过期");
        }
        redisTemplate.delete(key);
        redisTemplate.delete(RedisKeys.verifyFailKey(scene, email));
    }

    private boolean isLocked(String scene, String email) {
        String value = redisTemplate.opsForValue().get(RedisKeys.verifyFailKey(scene, email));
        if (value == null) {
            return false;
        }
        try {
            return Integer.parseInt(value) >= FAIL_MAX;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void recordFail(String scene, String email) {
        String key = RedisKeys.verifyFailKey(scene, email);
        Long fails = redisTemplate.opsForValue().increment(key);
        if (fails != null && fails == 1L) {
            redisTemplate.expire(key, FAIL_TTL);
        }
    }

    private String randomCode() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    private SysUser requireLoginUser() {
        if (!StpUtil.isLogin()) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        }
        SysUser user = userMapper.selectById(StpUtil.getLoginIdAsLong());
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户不存在，请重新登录");
        }
        return user;
    }

    private SysUser findByEmail(String email) {
        return userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getEmail, email));
    }

    private boolean emailTaken(String email, long excludeUserId) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getEmail, email)
                .ne(SysUser::getId, excludeUserId));
        return count != null && count > 0;
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private String mask(String email) {
        if (email == null) {
            return "";
        }
        int at = email.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        return email.charAt(0) + "***" + email.substring(at);
    }

    /** 安全通知不阻塞主流程（邮件失败只记日志） */
    private void notify(String to, String subject, String content) {
        try {
            mailService.sendNotice(to, subject, content);
        } catch (Exception e) {
            log.warn("[邮箱] 安全通知发送失败 to={} err={}", mask(to), e.getMessage());
        }
    }
}
