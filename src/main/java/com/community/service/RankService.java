package com.community.service;

import com.community.vo.RankItemVO;

import java.util.List;

/**
 * 热度榜单服务(Redis ZSet)
 *
 * <p>榜单键：{@code community:post:hot:zset}，score = 点赞*3 + 收藏*5 + 浏览*0.1。
 * 各写事件实时增量维护(点赞±3 / 收藏±5 / 浏览+0.1)，读取时 ZREVRANGE 取 TopN。
 */
public interface RankService {

    /**
     * 对某帖子热度分做增量调整
     *
     * @param postId 帖子ID
     * @param delta  增量(可为负，如取消点赞)
     */
    void bump(long postId, double delta);

    /**
     * 帖子被删除时从榜单移除
     *
     * @param postId 帖子ID
     */
    void remove(long postId);

    /**
     * 取热度榜 TopN(结合数据库补全标题/分类，剔除已被删除帖子)
     *
     * @param topN 取前 N 名
     * @return 榜单条目(热度从高到低)
     */
    List<RankItemVO> top(int topN);
}
