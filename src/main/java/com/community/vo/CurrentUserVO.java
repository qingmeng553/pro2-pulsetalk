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

    /** 是否已设置密保问题(v3)：已设置则个人中心不再展示设置入口 */
    private Boolean hasSecurityQuestion;

    /** 是否被封禁(v3)：封禁用户不可发帖/评论 */
    private Boolean banned;
}
