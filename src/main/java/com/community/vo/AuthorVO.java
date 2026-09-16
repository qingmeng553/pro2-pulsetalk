package com.community.vo;

import com.community.entity.UserRole;
import lombok.Data;

/** 作者(发帖人/评论人)摘要信息。 */
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
