package com.community.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.community.common.api.ResultCode;
import com.community.common.constant.RedisKeys;
import com.community.common.exception.BusinessException;
import com.community.common.util.PasswordUtil;
import com.community.dto.ForgotQueryDTO;
import com.community.dto.ForgotResetDTO;
import com.community.dto.LoginDTO;
import com.community.dto.RegisterDTO;
import com.community.dto.SecurityQuestionDTO;
import com.community.dto.UpdatePasswordDTO;
import com.community.dto.UpdateProfileDTO;
import com.community.entity.SysUser;
import com.community.entity.UserRole;
import com.community.mapper.SysUserMapper;
import com.community.service.AuthService;
import com.community.service.MinioFileService;
import com.community.service.RateLimitService;
import com.community.vo.CurrentUserVO;
import com.community.vo.LoginVO;
import com.community.vo.SecurityQuestionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;

/**
 * 用户认证服务实现
 *
 * <p>v3：新增改名、修改密码、密保问题(设置/查询/找回密码)，找回流程带 Redis 限流防爆破。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper userMapper;
    private final MinioFileService minioFileService;
    private final RateLimitService rateLimitService;

    @Override
    public void register(RegisterDTO dto) {
        String username = dto.getUsername().trim();

        // 用户名唯一性校验
        Long count = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));
        if (count != null && count > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "用户名已被注册");
        }

        SysUser user = new SysUser();
        user.setUsername(username);
        // 密码加密存储(salt$sha256hex)
        user.setPassword(PasswordUtil.encode(dto.getPassword()));
        user.setNickname(StringUtils.hasText(dto.getNickname()) ? dto.getNickname().trim() : username);
        user.setRole(UserRole.USER);
        userMapper.insert(user);
        log.info("[注册] 新用户注册成功 userId={}, username={}", user.getId(), username);
    }

    @Override
    public LoginVO login(LoginDTO dto) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, dto.getUsername().trim()));
        if (user == null || !PasswordUtil.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "用户名或密码错误");
        }

        // Sa-Token 登录：会话 token 由 sa-token-redis-jackson 持久化到 Redis
        StpUtil.login(user.getId());
        String tokenValue = StpUtil.getTokenValue();

        LoginVO vo = new LoginVO();
        vo.setTokenName(StpUtil.getTokenName());
        vo.setTokenValue(tokenValue);
        vo.setUser(toCurrentUserVO(user));
        log.info("[登录] 用户登录成功 userId={}", user.getId());
        return vo;
    }

    @Override
    public void logout() {
        // 注销当前会话(同时删除 Redis 中的 token 与 Session)
        StpUtil.logout();
    }

    @Override
    public CurrentUserVO currentUser() {
        long userId = StpUtil.getLoginIdAsLong();
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户不存在，请重新登录");
        }
        return toCurrentUserVO(user);
    }

    @Override
    public String uploadAvatar(MultipartFile file) {
        long userId = StpUtil.getLoginIdAsLong();
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户不存在，请重新登录");
        }
        // 上传到 MinIO 并更新头像字段(旧头像对象暂不删除，可自行扩展清理)
        String url = minioFileService.uploadAvatar(file);
        user.setAvatarUrl(url);
        userMapper.updateById(user);
        log.info("[头像] 用户 {} 更新头像: {}", userId, url);
        return url;
    }

    /** 实体 → 当前用户 VO(注意不回传 password) */
    public static CurrentUserVO toCurrentUserVO(SysUser user) {
        CurrentUserVO vo = new CurrentUserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setRole(user.getRole());
        // v3：密保问题状态 + 封禁状态(前端据此隐藏设置入口 / 禁用发帖评论)
        vo.setHasSecurityQuestion(user.getSecurityQuestion() != null && !user.getSecurityQuestion().isBlank());
        vo.setBanned(user.getStatus() != null && user.getStatus() == SysUser.STATUS_BANNED);
        return vo;
    }

    // ==================== v3：账号自助(改名/改密/密保) ====================

    @Override
    public CurrentUserVO updateProfile(UpdateProfileDTO dto) {
        long userId = StpUtil.getLoginIdAsLong();
        SysUser user = requireLoginUser(userId);
        user.setNickname(dto.getNickname().trim());
        userMapper.updateById(user);
        log.info("[改名] 用户 {} 昵称更新为 {}", userId, user.getNickname());
        return toCurrentUserVO(user);
    }

    @Override
    public void updatePassword(UpdatePasswordDTO dto) {
        long userId = StpUtil.getLoginIdAsLong();
        SysUser user = requireLoginUser(userId);
        if (!PasswordUtil.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "原密码不正确");
        }
        if (PasswordUtil.matches(dto.getNewPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "新密码不能与原密码相同");
        }
        user.setPassword(PasswordUtil.encode(dto.getNewPassword()));
        userMapper.updateById(user);
        log.info("[改密] 用户 {} 修改了登录密码", userId);
    }

    @Override
    public void setSecurityQuestion(SecurityQuestionDTO dto) {
        long userId = StpUtil.getLoginIdAsLong();
        SysUser user = requireLoginUser(userId);
        if (user.getSecurityQuestion() != null && !user.getSecurityQuestion().isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "已设置过密保问题");
        }
        user.setSecurityQuestion(dto.getQuestion().trim());
        user.setSecurityAnswer(PasswordUtil.encode(normalizeAnswer(dto.getAnswer())));
        userMapper.updateById(user);
        log.info("[密保] 用户 {} 设置了密保问题", userId);
    }

    @Override
    public SecurityQuestionVO securityQuestionStatus() {
        SysUser user = requireLoginUser(StpUtil.getLoginIdAsLong());
        SecurityQuestionVO vo = new SecurityQuestionVO();
        boolean has = user.getSecurityQuestion() != null && !user.getSecurityQuestion().isBlank();
        vo.setHasQuestion(has);
        vo.setQuestion(has ? user.getSecurityQuestion() : null);
        return vo;
    }

    @Override
    public SecurityQuestionVO forgotQuestion(ForgotQueryDTO dto) {
        String username = dto.getUsername().trim();
        // 限流：同一用户名 10 分钟内最多查询 10 次，防账号枚举
        rateLimitService.check(RedisKeys.forgotLimitKey(username), 10, 600);

        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));
        SecurityQuestionVO vo = new SecurityQuestionVO();
        if (user == null || user.getSecurityQuestion() == null || user.getSecurityQuestion().isBlank()) {
            // 不暴露账号是否存在，统一提示
            vo.setHasQuestion(false);
            return vo;
        }
        vo.setHasQuestion(true);
        vo.setQuestion(user.getSecurityQuestion());
        return vo;
    }

    @Override
    public void forgotReset(ForgotResetDTO dto) {
        String username = dto.getUsername().trim();
        // 限流：同一用户名 10 分钟内最多尝试 5 次，防密保答案暴力破解
        rateLimitService.check(RedisKeys.forgotResetLimitKey(username), 5, 600);

        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));
        if (user == null || user.getSecurityAnswer() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "该账号未设置密保问题，请联系管理员");
        }
        if (!PasswordUtil.matches(normalizeAnswer(dto.getAnswer()), user.getSecurityAnswer())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "密保答案不正确");
        }
        user.setPassword(PasswordUtil.encode(dto.getNewPassword()));
        userMapper.updateById(user);
        log.info("[找回密码] 用户 {} 通过密保答案重置了密码", user.getId());
    }

    /** 密保答案归一化：去空白 + 转小写(大小写不敏感，提升可用性) */
    private String normalizeAnswer(String answer) {
        return answer == null ? "" : answer.trim().toLowerCase(Locale.ROOT);
    }

    /** 取当前登录用户实体(逻辑删除/不存在时提示重新登录) */
    private SysUser requireLoginUser(long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户不存在，请重新登录");
        }
        return user;
    }
}
