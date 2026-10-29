package com.community.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.common.api.ResultCode;
import com.community.common.exception.BusinessException;
import com.community.common.util.PasswordUtil;
import com.community.entity.SysUser;
import com.community.entity.UserRole;
import com.community.mapper.SysUserMapper;
import com.community.service.AdminUserService;
import com.community.service.MailService;
import com.community.vo.PageVO;
import com.community.vo.UserAdminVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** 管理端 - 用户管理服务实现。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final SysUserMapper userMapper;
    private final MailService mailService;

    @Override
    public PageVO<UserAdminVO> pageList(long page, long size, String keyword) {
        LambdaQueryWrapper<SysUser> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            qw.and(w -> w.like(SysUser::getUsername, kw).or().like(SysUser::getNickname, kw));
        }
        qw.orderByDesc(SysUser::getId);
        // 不回传 password / securityAnswer
        qw.select(SysUser::getId, SysUser::getUsername, SysUser::getNickname, SysUser::getAvatarUrl,
                SysUser::getRole, SysUser::getStatus, SysUser::getCreateTime);

        Page<SysUser> p = userMapper.selectPage(new Page<>(page, size), qw);
        return PageVO.of(p, this::toVO);
    }

    @Override
    public void ban(long adminId, long targetId) {
        SysUser target = requireOperable(adminId, targetId);
        target.setStatus(SysUser.STATUS_BANNED);
        userMapper.updateById(target);
        log.info("[用户管理] 管理员 {} 封禁用户 {}({})", adminId, targetId, target.getUsername());
    }

    @Override
    public void unban(long adminId, long targetId) {
        SysUser target = requireOperable(adminId, targetId);
        target.setStatus(SysUser.STATUS_NORMAL);
        userMapper.updateById(target);
        log.info("[用户管理] 管理员 {} 解封用户 {}({})", adminId, targetId, target.getUsername());
    }

    @Override
    public void delete(long adminId, long targetId) {
        SysUser target = requireOperable(adminId, targetId);
        // 逻辑删除，并释放原用户名
        String freed = target.getUsername() + "_deleted_" + target.getId();
        target.setUsername(freed.length() > 50 ? freed.substring(0, 50) : freed);
        userMapper.updateById(target);
        userMapper.deleteById(targetId);
        log.info("[用户管理] 管理员 {} 删除用户 {}(原账号 {})", adminId, targetId, freed);
    }

    @Override
    public void resetPassword(long adminId, long targetId, String newPassword) {
        SysUser target = requireOperable(adminId, targetId);
        target.setPassword(PasswordUtil.encode(newPassword));
        userMapper.updateById(target);
        // 未绑定邮箱用户无法自助找回，管理员重置是唯一通道；重置后踢会话
        StpUtil.logout(targetId);
        if (StringUtils.hasText(target.getEmail())) {
            try {
                mailService.sendNotice(target.getEmail(), "PulseTalk 密码已被管理员重置",
                        "你的 PulseTalk 账号密码已被管理员重置，请使用新密码登录。\n如非本人操作，请立即联系管理员。");
            } catch (Exception e) {
                log.warn("[用户管理] 重置密码通知发送失败 userId={} err={}", targetId, e.getMessage());
            }
        }
        log.info("[用户管理] 管理员 {} 重置了用户 {}({}) 的密码", adminId, targetId, target.getUsername());
    }

    /** 校验目标用户可被操作：存在、非管理员、非自己 */    private SysUser requireOperable(long adminId, long targetId) {
        if (adminId == targetId) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不能对自己执行该操作");
        }
        SysUser target = userMapper.selectById(targetId);
        if (target == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在或已被删除");
        }
        if (target.getRole() == UserRole.ADMIN) {
            throw new BusinessException(ResultCode.FORBIDDEN, "不能封禁/删除管理员账号");
        }
        return target;
    }

    /** 实体 → 管理端 VO */
    private UserAdminVO toVO(SysUser user) {
        UserAdminVO vo = new UserAdminVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setRole(user.getRole());
        vo.setStatus(user.getStatus() == null ? SysUser.STATUS_NORMAL : user.getStatus());
        vo.setBanned(vo.getStatus() == SysUser.STATUS_BANNED);
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }
}
