package com.community.vo;

import com.community.entity.UserRole;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端用户表条目 VO(管理员可见的用户管理模块)
 */
@Data
public class UserAdminVO {

    /** 用户ID */
    private Long id;

    /** 登录账号 */
    private String username;

    /** 昵称 */
    private String nickname;

    /** 头像URL */
    private String avatarUrl;

    /** 角色 USER / ADMIN */
    private UserRole role;

    /** 状态 0正常 / 1封禁 */
    private Integer status;

    /** 是否封禁(便于前端绑定) */
    private Boolean banned;

    /** 注册时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
