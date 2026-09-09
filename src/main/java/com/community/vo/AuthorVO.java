package com.community.vo;

import com.community.entity.UserRole;
import lombok.Data;

/**
 * 作者(发帖人/评论人)摘要信息
 *
 * <p>role 用于前端在昵称旁渲染 ADMIN 盾牌管理员徽章。
 */
@Data
public class AuthorVO {

    /** 用户ID */
    private Long userId;

    /** 昵称 */
    private String nickname;

    /** 头像URL */
    private String avatarUrl;

    /** 角色 USER / ADMIN */
    private UserRole role;
}
