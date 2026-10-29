package com.community.service;

import com.community.vo.PageVO;
import com.community.vo.UserAdminVO;

/** 管理端 - 用户管理服务(仅 ADMIN 可见，鉴权在 Controller 的 @SaCheckRole("admin…。 */
public interface AdminUserService {

    /**
     * 用户分页列表(支持账号/昵称关键字模糊搜索)
     *
     * @param page    页码
     * @param size    每页条数
     * @param keyword 关键字(可空)
     */
    PageVO<UserAdminVO> pageList(long page, long size, String keyword);

    /**
     * 封禁用户(封禁后不可发帖、不可评论)
     *
     * @param adminId  操作管理员ID
     * @param targetId 目标用户ID
     */
    void ban(long adminId, long targetId);

    /**
     * 解封用户
     */
    void unban(long adminId, long targetId);

    /**
     * 删除用户(逻辑删除；同时释放用户名占用)
     */
    void delete(long adminId, long targetId);

    /**
     * 管理员重置用户密码（未绑定邮箱用户的唯一找回方式；踢掉该用户全部会话）
     */
    void resetPassword(long adminId, long targetId, String newPassword);
}
