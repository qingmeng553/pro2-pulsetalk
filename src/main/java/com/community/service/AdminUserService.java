package com.community.service;

import com.community.vo.PageVO;
import com.community.vo.UserAdminVO;

/**
 * 管理端 - 用户管理服务(仅 ADMIN 可见，鉴权在 Controller 的 @SaCheckRole("admin"))
 *
 * <p>能力：用户表分页查询、封禁、解封、删除(逻辑删除)。
 * 约束：不允许操作管理员账号本人/其它管理员，避免误封管理员。
 */
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
}
