package com.community.vo;

import com.community.entity.UserRole;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户公开主页 VO(点头像了解彼此)
 *
 * <p>relation 表示当前登录用户与该用户的关系(游客恒为 NONE)：
 * SELF / FRIEND / PENDING_SENT(我申请的待通过) / PENDING_RECEIVED(对方申请待我处理)
 * / BLACKED(任意一方已拉黑，双向阻断) / NONE
 */
@Data
public class UserProfileVO {

    /** 用户ID */
    private Long userId;

    /** 昵称 */
    private String nickname;

    /** 头像URL */
    private String avatarUrl;

    /** 角色 USER/ADMIN */
    private UserRole role;

    /** 注册时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 帖子数 */
    private Long postCount;

    /** 与当前用户的关系(见类注释) */
    private String relation;
}
