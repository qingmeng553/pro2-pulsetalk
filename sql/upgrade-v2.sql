-- ============================================================================
-- PulseTalk v2 增量升级脚本 upgrade-v2.sql
-- 适用：已在运行的 community_db（docker mysql 数据卷已初始化过 v1 四张表，
--       不会自动重跑 init.sql），本脚本仅新增 5 张业务表，不影响原有数据。
--
-- 执行方式(在项目根目录)：
--   docker compose exec -T mysql mysql -uroot -proot2297752516 community_db < sql/upgrade-v2.sql
-- 或使用任意 MySQL 客户端选中 community_db 后执行本文件。
-- ============================================================================
SET NAMES utf8mb4;

DROP TABLE IF EXISTS post_pin;
DROP TABLE IF EXISTS user_notify;
DROP TABLE IF EXISTS chat_message;
DROP TABLE IF EXISTS user_blacklist;
DROP TABLE IF EXISTS user_friend;

-- ============================================================================
-- 好友关系表 user_friend
-- ============================================================================
CREATE TABLE user_friend (
    id          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id     BIGINT   NOT NULL COMMENT '用户A(申请方或好友关系持有方)',
    friend_id   BIGINT   NOT NULL COMMENT '用户B',
    status      TINYINT  NOT NULL DEFAULT 0 COMMENT '0=申请中 1=已通过',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_pair (user_id, friend_id),
    KEY idx_friend (user_id, status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '好友关系表';

-- ============================================================================
-- 黑名单表 user_blacklist
-- ============================================================================
CREATE TABLE user_blacklist (
    id            BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id       BIGINT   NOT NULL COMMENT '拉黑发起方',
    black_user_id BIGINT   NOT NULL COMMENT '被拉黑方',
    create_time   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_block (user_id, black_user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '黑名单表';

-- ============================================================================
-- 私聊消息表 chat_message（7 天自动清理）
-- ============================================================================
CREATE TABLE chat_message (
    id           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    from_user_id BIGINT        NOT NULL COMMENT '发送方',
    to_user_id   BIGINT        NOT NULL COMMENT '接收方',
    content      VARCHAR(2000) NOT NULL COMMENT '消息内容',
    is_read      TINYINT       NOT NULL DEFAULT 0 COMMENT '是否已读: 0未读/1已读',
    create_time  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_pair (from_user_id, to_user_id, id),
    KEY idx_to   (to_user_id, is_read, id),
    KEY idx_time (create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '私聊消息(7天自动清理)';

-- ============================================================================
-- 站内消息通知表 user_notify
-- ============================================================================
CREATE TABLE user_notify (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id     BIGINT       NOT NULL COMMENT '接收人',
    actor_id    BIGINT       NOT NULL COMMENT '触发人',
    type        VARCHAR(16)  NOT NULL COMMENT '事件类型: LIKE/COMMENT/COLLECT/FRIEND',
    post_id     BIGINT       DEFAULT NULL COMMENT '关联帖子',
    comment_id  BIGINT       DEFAULT NULL COMMENT '关联评论',
    content     VARCHAR(500) DEFAULT NULL COMMENT '内容快照',
    is_read     TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已读: 0未读/1已读',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_user_time (user_id, id),
    KEY idx_user_read (user_id, is_read)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '站内消息通知表';

-- ============================================================================
-- 置顶规则帖表 post_pin（单行 id=1）
-- ============================================================================
CREATE TABLE post_pin (
    id          INT      NOT NULL COMMENT '固定为1(仅一条置顶)',
    post_id     BIGINT   NOT NULL COMMENT '置顶帖ID(post.id)',
    admin_id    BIGINT   NOT NULL COMMENT '最近操作管理员(sys_user.id)',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '置顶规则帖(单行)';
