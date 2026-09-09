-- ============================================================================
-- PulseTalk(原 pro2 点评贴吧) —— Docker 容器 MySQL 初始化脚本 v2
-- 用途：仅作为 docker-compose 中 mysql 服务 /docker-entrypoint-initdb.d 的初始化脚本
--       （也支持在本地 MySQL 手动执行，会 DROP 重建，请谨慎！）
-- 说明：包含 DROP TABLE IF EXISTS；预置管理员 admin / 测试用户 test（密码均 123456）
--       v2 新增：好友/黑名单/私聊/站内消息/置顶帖 五张业务表
-- ============================================================================
SET NAMES utf8mb4;

-- 建库（docker 中由 MYSQL_DATABASE 自动创建，此处幂等保障手动执行场景）
CREATE DATABASE IF NOT EXISTS community_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE community_db;

-- ----------------------------------------------------------------------------
-- 清理旧表（新表先于业务表清理）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS post_pin;
DROP TABLE IF EXISTS user_notify;
DROP TABLE IF EXISTS chat_message;
DROP TABLE IF EXISTS user_blacklist;
DROP TABLE IF EXISTS user_friend;
DROP TABLE IF EXISTS post_comment;
DROP TABLE IF EXISTS post;
DROP TABLE IF EXISTS post_category;
DROP TABLE IF EXISTS sys_user;

-- ============================================================================
-- 1. 用户表 sys_user
-- ============================================================================
CREATE TABLE sys_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username    VARCHAR(50)  NOT NULL COMMENT '登录账号',
    password    VARCHAR(128) NOT NULL COMMENT '密码(salt$sha256hex, 加密存储)',
    nickname    VARCHAR(64)  DEFAULT NULL COMMENT '昵称',
    avatar_url  VARCHAR(512) DEFAULT NULL COMMENT '头像URL(MinIO)',
    role        VARCHAR(16)  NOT NULL DEFAULT 'USER' COMMENT '角色: USER普通 / ADMIN管理员',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户表';

-- 预置账号：admin / 123456 (ADMIN)，test / 123456 (USER)
INSERT INTO sys_user (id, username, password, nickname, role, create_time) VALUES
(1, 'admin', 'a3f9c1e7d5b2408e6f0a1234567890ab$6c7035cd6cb2e4f6c6cae3496e531b47f713572a1925c728e9d92d260875cbd5', '管理员', 'ADMIN', NOW()),
(2, 'test',  '7c2d8e4f1a9b3c5d7e6f8a0b1c2d3e4f$570e7f7d3e89d30bf654b7af23f7a6e106ed22b725e8a590b7e5a277ede21a2a', '测试用户', 'USER', NOW());

-- ============================================================================
-- 2. 帖子分类表 post_category
-- ============================================================================
CREATE TABLE post_category (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    name        VARCHAR(32) NOT NULL COMMENT '分类名称',
    sort        INT         NOT NULL DEFAULT 0 COMMENT '排序(越小越靠前)',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '帖子分类表';

INSERT INTO post_category (id, name, sort) VALUES
(1, '技术交流', 1),
(2, '编程问答', 2),
(3, '生活分享', 3),
(4, '游戏电竞', 4),
(5, '资源干货', 5),
(6, '随便聊聊', 6);

-- ============================================================================
-- 3. 帖子表 post
-- ============================================================================
CREATE TABLE post (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id       BIGINT       NOT NULL COMMENT '作者用户ID(sys_user.id)',
    category_id   BIGINT       NOT NULL COMMENT '分类ID(post_category.id)',
    title         VARCHAR(200) NOT NULL COMMENT '标题',
    content       MEDIUMTEXT   NOT NULL COMMENT '正文(Markdown)',
    img_urls      VARCHAR(2048) DEFAULT NULL COMMENT '配图URL(JSON数组字符串)',
    view_count    INT          NOT NULL DEFAULT 0 COMMENT '浏览量(每5分钟由Redis同步)',
    like_count    INT          NOT NULL DEFAULT 0 COMMENT '点赞数(每5分钟由Redis同步)',
    collect_count INT          NOT NULL DEFAULT 0 COMMENT '收藏数(每5分钟由Redis同步)',
    is_deleted    TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0未删/1已删',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_user (user_id),
    KEY idx_category (category_id),
    KEY idx_create_time (create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '帖子表';

-- ============================================================================
-- 4. 评论表 post_comment
-- ============================================================================
CREATE TABLE post_comment (
    id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    post_id     BIGINT        NOT NULL COMMENT '帖子ID(post.id)',
    user_id     BIGINT        NOT NULL COMMENT '评论人用户ID(sys_user.id)',
    content     VARCHAR(2000) NOT NULL COMMENT '评论内容',
    is_deleted  TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0未删/1已删',
    create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_post (post_id, is_deleted),
    KEY idx_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '帖子评论表';

-- ============================================================================
-- 5. 好友关系表 user_friend
--    双向往来：申请 = 一条 status=0(PENDING) 记录(user_id=申请方, friend_id=被申请方)
--    通过后 = 两条 status=1(ACCEPTED) 记录(互为好友，各自一行)
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
-- 6. 黑名单表 user_blacklist（拉黑即双向阻断互动；查询时双向判断）
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
-- 7. 私聊消息表 chat_message（聊天记录 7 天内有效，超出由定时任务物理清理）
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
-- 8. 站内消息通知表 user_notify
--    type: LIKE 点赞 / COMMENT 评论 / COLLECT 收藏 / FRIEND 好友申请或通过
-- ============================================================================
CREATE TABLE user_notify (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id     BIGINT       NOT NULL COMMENT '接收人',
    actor_id    BIGINT       NOT NULL COMMENT '触发人(点赞/评论/收藏/好友操作方)',
    type        VARCHAR(16)  NOT NULL COMMENT '事件类型: LIKE/COMMENT/COLLECT/FRIEND',
    post_id     BIGINT       DEFAULT NULL COMMENT '关联帖子(点赞/评论/收藏)',
    comment_id  BIGINT       DEFAULT NULL COMMENT '关联评论(评论通知)',
    content     VARCHAR(500) DEFAULT NULL COMMENT '内容快照(评论预览/好友提示语等)',
    is_read     TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已读: 0未读/1已读',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_user_time (user_id, id),
    KEY idx_user_read (user_id, is_read)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '站内消息通知表';

-- ============================================================================
-- 9. 置顶规则帖表 post_pin（单行 id=1；由管理员共同维护的唯一置顶帖=社区规则）
-- ============================================================================
CREATE TABLE post_pin (
    id          INT      NOT NULL COMMENT '固定为1(仅一条置顶)',
    post_id     BIGINT   NOT NULL COMMENT '置顶帖ID(post.id)',
    admin_id    BIGINT   NOT NULL COMMENT '最近操作管理员(sys_user.id)',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '置顶规则帖(单行)';
