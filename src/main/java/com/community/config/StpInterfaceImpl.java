package com.community.config;

import cn.dev33.satoken.stp.StpInterface;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.community.entity.SysUser;
import com.community.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Sa-Token 权限数据源(角色加载)
 *
 * <p>Sa-Token 通过 StpInterface 获取当前登录用户的角色集合。
 * 数据库 role 存储为大写枚举(USER/ADMIN)，此处统一转为小写，
 * 与 @SaCheckRole("admin") 的声明保持一致(ADMIN → admin)。
 */
@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    private final SysUserMapper userMapper;

    /** 返回用户拥有的角色列表(小写)，如 ["user"] / ["admin"] */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getId, Long.parseLong(loginId.toString())));
        if (user == null || user.getRole() == null) {
            return Collections.emptyList();
        }
        return Collections.singletonList(user.getRole().name().toLowerCase(Locale.ROOT));
    }

    /** 本项目不引入细粒度权限码，返回空集合 */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return Collections.emptyList();
    }
}
