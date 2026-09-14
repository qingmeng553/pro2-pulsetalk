package com.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.common.api.ResultCode;
import com.community.common.exception.BusinessException;
import com.community.entity.SysUser;
import com.community.entity.UserRole;
import com.community.mapper.SysUserMapper;
import com.community.service.AdminUserService;
import com.community.vo.PageVO;
import com.community.vo.UserAdminVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 管理端 - 用户管理服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final SysUserMapper userMapper;

    @Override
    public PageVO<UserAdminVO> pageList(long page, long size, String keyword) {
        LambdaQueryWrapper<SysUser> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            qw.and(w -> w.like(SysUser::getUsername, kw).or().like(SysUser::getNickname, kw));
        }
        qw.orderByDesc(SysUser::getId);
        // 注意：不回传 password / securityAnswer
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
        // 逻辑删除(MyBatis-Plus @TableLogic)；同时改名释放用户名占用，便于同名重新注册
        String freed = target.getUsername() + "_deleted_" + target.getId();
        target.setUsername(freed.length() > 50 ? freed.substring(0, 50) : freed);
        userMapper.updateById(target);
        userMapper.deleteById(targetId);
        log.info("[用户管理] 管理员 {} 删除用户 {}(原账号 {})", adminId, targetId, freed);
    }

    /** 校验目标用户可被操作：存在、非管理员、非自己 */
    private SysUser requireOperable(long adminId, long targetId) {
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
