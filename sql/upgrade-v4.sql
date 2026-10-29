-- =====================================================
-- PulseTalk v4：索引优化（增量脚本，可重复执行前请先确认索引是否已存在）
-- 背景：对照真实查询发现三处索引缺口
--   1) 帖子列表按分类/作者 + 时间倒序 → 现有单列索引只能过滤，排序要 filesort
--   2) 「我收到的好友申请」按 friend_id + status 查 → 现有索引首列是 user_id，吃不上，全表扫描
--   3) 评论列表按 post_id + 时间 → 现有 (post_id, is_deleted) 可过滤，排序要 filesort
-- 执行方式：
--   docker exec -i pro2-mysql mysql -uroot -p'口令' community_db < sql/upgrade-v4.sql
-- 前置检查（避免重复建索引报 Duplicate key name）：
--   SHOW INDEX FROM post; SHOW INDEX FROM user_friend; SHOW INDEX FROM post_comment;
-- =====================================================

-- 1) 帖子：分类页列表（where category_id=? and is_deleted=0 order by create_time desc）
ALTER TABLE post ADD INDEX idx_category_time (category_id, is_deleted, create_time);

-- 2) 帖子：个人主页「TA 的帖子」（where user_id=? and is_deleted=0 order by create_time desc）
ALTER TABLE post ADD INDEX idx_user_time (user_id, is_deleted, create_time);

-- 3) 好友：收到的申请 / 待处理列表（where friend_id=? and status=?）
ALTER TABLE user_friend ADD INDEX idx_friend_status (friend_id, status);

-- 4) 评论：某帖评论按时间排序（where post_id=? and is_deleted=0 order by create_time asc）
ALTER TABLE post_comment ADD INDEX idx_post_time (post_id, is_deleted, create_time);

-- 5) 可选（确认无用后再删）：post_comment.idx_user 目前没有按用户查评论的查询，属于冗余索引，
--    会增加写入成本。确认后可执行：
-- ALTER TABLE post_comment DROP INDEX idx_user;
