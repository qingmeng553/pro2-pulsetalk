package com.community.vo;

import com.community.entity.UserRole;
import lombok.Data;

/**
 * 当前登录用户信息(含角色)
 */
@Data
public class CurrentUserVO {

    /** 用户ID */
    private Long id;

    /** 登录账号 */
    private String username;

    /** 昵称 */
    private String nickname;

    /** 头像URL */
    private String avatarUrl;

    /** 角色 USER / ADMIN(前端据此渲染管理员盾牌徽章、管理员删除按钮) */
    private UserRole role;
}
