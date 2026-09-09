package com.community.entity;

/**
 * 用户角色枚举
 *
 * <p>USER 普通用户：可发帖/评论/点赞/收藏/上传，仅可删除自己发布的内容；
 * ADMIN 管理员：额外拥有软删除任意帖子、任意评论的权限(接口 @SaCheckRole("admin"))。
 */
public enum UserRole {

    /** 普通用户 */
    USER,

    /** 管理员 */
    ADMIN
}
