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

-- ---------------------------------------------------------------------------
-- 历史图片地址收口：绝对地址 → 相对路径 /files
-- 1) 先备份数据库；2) 先跑下面注释的 SELECT 看影响行数；3) 再执行 UPDATE。
-- 生产如果用的是别的域名/IP，把 REPLACE 与 WHERE 里的前缀换成你的旧地址即可。
-- ---------------------------------------------------------------------------
-- SELECT COUNT(*) AS post_rows FROM post
--   WHERE img_urls LIKE '%http://127.0.0.1:9000%' OR img_urls LIKE '%http://localhost:9000%'
--      OR img_urls LIKE '%http://121.43.252.96/files%' OR img_urls LIKE '%https://qingmeng553.com/files%';
-- SELECT COUNT(*) AS user_rows FROM sys_user WHERE avatar_url LIKE 'http%';
-- SELECT COUNT(*) AS content_rows FROM post WHERE content LIKE '%http://127.0.0.1:9000%';

UPDATE post
SET img_urls = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(img_urls,
        'http://127.0.0.1:9000', '/files'),
        'http://localhost:9000', '/files'),
        'http://121.43.252.96/files', '/files'),
        'http://qingmeng553.com/files', '/files'),
        'https://qingmeng553.com/files', '/files')
WHERE img_urls LIKE '%http://127.0.0.1:9000%'
   OR img_urls LIKE '%http://localhost:9000%'
   OR img_urls LIKE '%http://121.43.252.96/files%'
   OR img_urls LIKE '%http://qingmeng553.com/files%'
   OR img_urls LIKE '%https://qingmeng553.com/files%';

UPDATE post
SET content = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(content,
        'http://127.0.0.1:9000', '/files'),
        'http://localhost:9000', '/files'),
        'http://121.43.252.96/files', '/files'),
        'http://qingmeng553.com/files', '/files'),
        'https://qingmeng553.com/files', '/files')
WHERE content LIKE '%http://127.0.0.1:9000%'
   OR content LIKE '%http://localhost:9000%'
   OR content LIKE '%http://121.43.252.96/files%'
   OR content LIKE '%http://qingmeng553.com/files%'
   OR content LIKE '%https://qingmeng553.com/files%';

UPDATE sys_user
SET avatar_url = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(avatar_url,
        'http://127.0.0.1:9000', '/files'),
        'http://localhost:9000', '/files'),
        'http://121.43.252.96/files', '/files'),
        'http://qingmeng553.com/files', '/files'),
        'https://qingmeng553.com/files', '/files')
WHERE avatar_url LIKE '%http://127.0.0.1:9000%'
   OR avatar_url LIKE '%http://localhost:9000%'
   OR avatar_url LIKE '%http://121.43.252.96/files%'
   OR avatar_url LIKE '%http://qingmeng553.com/files%'
   OR avatar_url LIKE '%https://qingmeng553.com/files%';

-- ---------------------------------------------------------------------------
-- alpha2：邮箱绑定与找回
-- 老用户 email 为 NULL；MySQL 唯一索引允许多个 NULL
-- ---------------------------------------------------------------------------
ALTER TABLE sys_user
    ADD COLUMN email VARCHAR(128) DEFAULT NULL COMMENT '绑定邮箱',
    ADD COLUMN email_verified TINYINT NOT NULL DEFAULT 0 COMMENT '0未验证/1已验证',
    ADD UNIQUE KEY uk_email (email);

-- ---------------------------------------------------------------------------
-- beta1：可配置个人主页
-- ---------------------------------------------------------------------------
ALTER TABLE sys_user
    ADD COLUMN profile_layout JSON DEFAULT NULL COMMENT '主页布局 schema',
    ADD COLUMN profile_version INT NOT NULL DEFAULT 0 COMMENT '主页布局乐观锁版本';

CREATE TABLE IF NOT EXISTS user_guestbook (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    owner_id     BIGINT       NOT NULL COMMENT '主页主人',
    from_user_id BIGINT       NOT NULL COMMENT '留言人',
    content      VARCHAR(200) NOT NULL COMMENT '留言内容',
    is_deleted   TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_owner (owner_id, create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '主页留言板';

CREATE TABLE IF NOT EXISTS profile_report (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    target_user_id BIGINT       NOT NULL COMMENT '被举报主页',
    reporter_id    BIGINT       NOT NULL COMMENT '举报人',
    module_type    VARCHAR(20)  NOT NULL COMMENT '模块类型(profile=整页)',
    reason         VARCHAR(200) NOT NULL COMMENT '举报原因',
    status         TINYINT      NOT NULL DEFAULT 0 COMMENT '0待处理/1已处理',
    handle_by      BIGINT       DEFAULT NULL COMMENT '处理管理员',
    handle_time    DATETIME     DEFAULT NULL COMMENT '处理时间',
    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_status (status, create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '主页举报';

CREATE TABLE IF NOT EXISTS profile_hidden_module (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id     BIGINT       NOT NULL COMMENT '主页主人',
    module_type VARCHAR(20)  NOT NULL COMMENT '被隐藏模块',
    hidden_by   BIGINT       NOT NULL COMMENT '操作管理员',
    reason      VARCHAR(200) DEFAULT NULL COMMENT '原因',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_module (user_id, module_type)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '管理员隐藏的主页模块';
