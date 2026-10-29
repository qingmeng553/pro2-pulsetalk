package com.community.service;

import com.community.vo.RankItemVO;

import java.util.List;

/** 热度榜单服务(Redis ZSet)。 */
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

    /**
     * 榜单为空时用数据库计数重建(Redis 换容器/数据丢失后自动恢复，避免榜单只剩新帖)
     */
    void rebuildFromDbIfEmpty();
}
