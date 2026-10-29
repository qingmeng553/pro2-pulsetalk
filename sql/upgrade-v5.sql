-- ============================================================================
-- PulseTalk 1.1.0 增量脚本 v5
-- 说明：可重复执行前请先备份；本文件按版本累积，后续新增字段继续往下追加。
-- ============================================================================

-- 计数同步（每 5 分钟）只更新 view/like/collect，不应改动帖子的“编辑时间”。
-- 旧表结构的 ON UPDATE CURRENT_TIMESTAMP 会让“编辑于”变成同步时间，这里去掉自动更新，
-- 由应用在真正编辑内容时通过 MyBatis-Plus 的自动填充写入 update_time。
ALTER TABLE post
    MODIFY COLUMN update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
    COMMENT '更新时间(仅内容编辑时由应用写入)';
