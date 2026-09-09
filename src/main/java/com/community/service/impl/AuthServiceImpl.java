package com.community.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.community.common.api.ResultCode;
import com.community.common.exception.BusinessException;
import com.community.common.util.PasswordUtil;
import com.community.dto.LoginDTO;
import com.community.dto.RegisterDTO;
import com.community.entity.SysUser;
import com.community.entity.UserRole;
import com.community.mapper.SysUserMapper;
import com.community.service.AuthService;
import com.community.service.MinioFileService;
import com.community.vo.CurrentUserVO;
import com.community.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户认证服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper userMapper;
    private final MinioFileService minioFileService;

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
        return vo;
    }
}
