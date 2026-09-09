package com.community.service;

import com.community.dto.PostDTO;
import com.community.entity.Post;
import com.community.entity.PostCategory;
import com.community.vo.PageVO;
import com.community.vo.PostDetailVO;
import com.community.vo.PostListItemVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 帖子服务：分类、列表、详情(Cache-Aside + 浏览计数)、发帖、编辑、删除(作者/管理员)、置顶规则帖
 */
public interface PostService {

    /**
     * 获取全部帖子分类(游客可访问，按 sort 升序)
     */
    List<PostCategory> listCategories();

    /**
     * 帖子分页列表(游客可访问，过滤逻辑删除)
     *
     * @param page       页码
     * @param size       每页条数
     * @param categoryId 分类ID(为空查全部)
     * @param userId     作者ID(为空查全部；个人主页“TA的帖子”使用)
     */
    PageVO<PostListItemVO> pageList(long page, long size, Long categoryId, Long userId);

    /**
     * 帖子详情(游客可访问)：命中热点缓存；每次访问 Redis 浏览量自增 + 热度榜更新
     */
    PostDetailVO detail(long postId);

    /**
     * 新建帖子(需登录；发帖接口限流；markdown + emoji + 多张配图)
     */
    Post create(long userId, PostDTO dto);

    /**
     * 修改帖子(需登录)：仅作者可改；若该帖为当前置顶规则帖，管理员可共同编辑
     */
    Post update(long userId, long postId, PostDTO dto);

    /**
     * 用户删除自己的帖子(需登录且仅作者，逻辑软删除 + 清理 Redis)
     */
    void deleteOwn(long userId, long postId);

    /**
     * 管理员软删除任意帖子(@SaCheckRole("admin") 鉴权在 Controller)，删除同时清理 Redis 缓存
     */
    void adminDelete(long postId);

    /**
     * 上传帖子配图(需登录)
     */
    String uploadImage(MultipartFile file);

    // ==================== v2：置顶规则帖 ====================

    /**
     * 获取当前置顶规则帖(游客可访问；无置顶时返回 null)
     */
    PostDetailVO getPinnedPost();

    /**
     * 管理员置顶某帖为规则帖(替换旧置顶，全站仅一条；已在 Controller 做 admin 角色校验)
     *
     * @param adminId 操作管理员ID
     * @param postId  帖子ID
     */
    void pin(long adminId, long postId);

    /**
     * 管理员取消置顶(已在 Controller 做 admin 角色校验)
     */
    void unpin(long adminId);
}
