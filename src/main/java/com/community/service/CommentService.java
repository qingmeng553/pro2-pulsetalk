package com.community.service;

import com.community.dto.CommentDTO;
import com.community.vo.CommentVO;
import com.community.vo.PageVO;

/**
 * 评论服务(一级评论，不做楼中楼嵌套)
 */
public interface CommentService {

    /**
     * 帖子评论分页列表(游客可访问，自动过滤逻辑删除)
     *
     * @param postId 帖子ID
     * @param page   页码(从1开始)
     * @param size   每页条数
     * @return 分页评论(含楼层号、作者信息)
     */
    PageVO<CommentVO> pageList(long postId, long page, long size);

    /**
     * 发表评论(需登录 + 接口限流)
     *
     * @param userId 评论人ID
     * @param postId 帖子ID
     * @param dto    评论内容
     */
    void create(long userId, long postId, CommentDTO dto);

    /**
     * 用户删除自己的评论(逻辑软删除)
     *
     * @param userId    当前用户ID
     * @param commentId 评论ID
     */
    void deleteOwn(long userId, long commentId);

    /**
     * 管理员软删除任意评论(@SaCheckRole("admin") 鉴权在 Controller 层)
     *
     * @param commentId 评论ID
     */
    void adminDelete(long commentId);
}
