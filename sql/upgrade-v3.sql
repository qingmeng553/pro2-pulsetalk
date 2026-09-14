-- ============================================================================
-- PulseTalk v3 增量升级脚本 upgrade-v3.sql
-- 适用：已在运行的 community_db（新增“账号安全 / 封禁 / 逻辑删除”相关字段）
--
-- 执行方式(项目根目录)：
--   docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env.prod \
--       exec -T mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" community_db < sql/upgrade-v3.sql
-- 或任意 MySQL 客户端选中 community_db 后执行。
-- ============================================================================
SET NAMES utf8mb4;

-- sys_user 扩展字段：
--   security_question/security_answer  密保问题与答案(答案加密存储，用于“忘记密码”)
--   status                             0=正常 1=封禁(封禁后不可发帖/评论)
--   is_deleted                         0=未删 1=已删(管理员删除用户走逻辑删除)
--   update_time                        更新时间
ALTER TABLE sys_user
    ADD COLUMN security_question VARCHAR(200) DEFAULT NULL COMMENT '密保问题' AFTER avatar_url,
    ADD COLUMN security_answer   VARCHAR(128) DEFAULT NULL COMMENT '密保答案(加密存储)' AFTER security_question,
    ADD COLUMN status            TINYINT      NOT NULL DEFAULT 0 COMMENT '0正常 1封禁' AFTER role,
    ADD COLUMN is_deleted        TINYINT      NOT NULL DEFAULT 0 COMMENT '0未删 1已删' AFTER status,
    ADD COLUMN update_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER create_time;
