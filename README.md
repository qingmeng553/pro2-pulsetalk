# PulseTalk 开发日志（Release Notes）

> 本文件是**版本开发日志**，记录每个版本的改动、影响范围与升级动作，用于版本上传与回溯。
> - 项目介绍 / 本次更新解读 / 启动指南 → [START-HERE.md](./START-HERE.md)
> - 线上部署（阿里云轻量服务器）→ [GO-LIVE.md](./GO-LIVE.md)

---

## 版本总览

| 版本 | 状态 | 说明 | 涉及 SQL | 需重建 |
| --- | --- | --- | --- | --- |
| **v1.0.1** | **当前版本 / 首次正式发布** | PulseTalk 完整功能：社区帖子 + Redis 计数与缓存 + 私聊/好友/黑名单 + 消息通知 + 置顶规则帖 + 账号安全 + 管理端用户表 + 移动端适配 | 全新部署执行 `sql/init.sql`；若早期跑过内测库，见「数据库升级」 | 后端 + 前端 |
| v1.0.0 | 内测（未对外发布） | 首个可运行版本：单模块后端 + Vue3 前端 + Docker 编排，仅基础发帖/评论/点赞收藏 | `init.sql` | 全部 |

> 当前工程版本号：`1.0.1`（`pom.xml` / 构建产物 `target/pro2-1.0.1.jar` / 生产镜像 `pulsetalk-server:1.0.1`）

---

## v1.0.1 —— 首次正式发布

### 一、后端（Spring Boot 3.2.5 / JDK 17 / 端口 8085）

**基础社区与 Redis 实战**
- 4 张基础表：`sys_user`、`post_category`、`post`、`post_comment`；预置管理员 `admin` 与测试号 `test`
- 身份模式：游客只读（帖子/评论/榜单）；发帖、点赞、收藏、评论、上传均需登录；ADMIN 可软删除任意帖子/评论
- 点赞/收藏/浏览**只写 Redis**：Set 记用户、计数 String 快读、脏集合 + 每 5 分钟定时任务批量覆盖落库（幂等）
- 详情缓存 Cache-Aside：命中回填、更新/删除后失效；**空值缓存防穿透**；TTL 30~60 分钟随机偏移防雪崩
- 热度榜 ZSet：`score = 点赞×3 + 收藏×5 + 浏览×0.1`，写事件实时增量（±3 / ±5 / +0.1）
- 接口限流（Redis 计数器固定窗口）：发帖 3 次/分钟、评论 10 次/分钟
- 统一响应体与业务码：401 未登录 / 403 无权限 / 404 不存在 / 429 限流，配合全局异常处理
- MinIO：桶自动创建 + 公共读策略；扩展名 + **文件头魔数** + 5MB 三重校验，拒绝脚本伪装图片
- Sa-Token 会话持久化 Redis，登录态可跨实例共享；启动时兜底绑定 Redis Dao

**社交能力**
- **消息通知**：谁赞了我 / 评论了我 / 收藏了我 / 加我好友；点赞收藏取消会撤回未读；评论通知带内容预览
- **管理员官方消息**：`POST /notify/broadcast`（`@SaCheckRole("admin")`）向全体用户下发 `SYSTEM` 通知
- **私聊**：会话列表、分页消息、`afterId` 增量轮询、已读、未读总数；**非好友双方各限 1 条**打招呼消息
- **好友**：申请 / 通过 / 拒绝 / 删除 / 好友列表 / 待处理列表；互相申请自动成为好友
- **黑名单**：拉黑即**双向阻断**（互发私信、会话隐藏、自动解除好友），拉黑时清空对方未读
- **聊天记录 7 天清理**：`ChatHistoryCleanTask` 每天 04:10 物理清理过期消息
- **置顶规则帖**：全站唯一一条（`post_pin` 单行），任意管理员可共同编辑、可替换/取消置顶；帖子被删自动解绑

**账号安全**
- `PUT /auth/profile` 修改昵称；`PUT /auth/password` 修改密码（校验原密码）
- 密保问题：`POST /auth/security-question` 自定义「问题 + 答案」（答案加密、大小写不敏感、仅可设置一次）；`GET /auth/security-question` 查状态
- 忘记密码：`POST /auth/forgot/question`（按账号取回本人问题，限流 10 次/10 分钟防枚举）+ `POST /auth/forgot/reset`（答对即重置，限流 5 次/10 分钟防爆破）

**管理端**
- `GET /admin/user/list` 用户分页（账号/昵称搜索）
- `POST /admin/user/{id}/ban` / `unban` / `DELETE /admin/user/{id}`
- 约束：不能操作自己、不能封禁/删除管理员；删除为逻辑删除并**释放原用户名占用**
- **封禁效果**：被封禁用户不可发帖、不可评论（浏览与点赞等不受影响）

### 二、数据库（`sql/`）
- `init.sql`：全新部署一键建库建表——共 **9 张表**
  `sys_user`（含密保问题/封禁/逻辑删除/更新时间）、`post_category`、`post`、`post_comment`、`user_friend`、`user_blacklist`、`chat_message`、`user_notify`、`post_pin`
- 增量脚本（仅当你早期跑过内测库时需要，按顺序执行）：
  - `sql/upgrade-v2.sql`：新增五张社交表（好友/黑名单/私聊/通知/置顶）
  - `sql/upgrade-v3.sql`：`sys_user` 增量字段（密保问题、封禁、逻辑删除、更新时间）

### 三、前端（`pro2-frontend`，Vue3 + Vite）
- 页面：首页帖子流、帖子详情、发帖/编辑、热度排行榜、消息中心、私聊窗口、他人主页、好友管理、个人中心、用户管理
- 组件：Emoji 选择器、Markdown 渲染（marked + DOMPurify 消毒）、多图上传预览、用户小卡片（点头像进主页）、统一图标标签 `MiniTag`
- 交互：游客点击写操作 → 弹「去登录 / 取消」；未读一律**红点**（顶栏/会话/通知）不显示数字；管理员徽章与管理员删除按钮
- 动效：D3 卡片错落入场 / hover 上浮 / 榜单动态渲染；ECharts 热度条形图
- 账号安全：登录页「忘记密码？」两步找回；个人中心改名/改密/密保设置（**设置成功后入口不再显示**）；封禁态禁用发帖与评论
- 管理端：`/admin/users` 用户表 + 搜索 + 封禁/解封/删除（路由 `adminOnly` 守卫）
- **移动端适配**：全局断点（768/420）+ 导航、首页、详情、发帖、聊天、消息、他人主页、好友、榜单、个人中心、后台表格逐页适配

### 四、本次一并修复（内测期问题）
| 问题 | 处理 |
| --- | --- |
| 后端编译失败：包装类型 `Long` 强转 `int`、`multi-catch` 父子类互斥 | 改为解箱后收敛 `Math.toIntExact`；合并异常捕获 |
| Bean 循环依赖：互动服务 ↔ 榜单服务 | `RankServiceImpl` 改为直接读 Redis 取计数，依赖链单向化 |
| 聊天消息重复渲染（发送方一条渲染多条） | 基于 messageId 去重合并（历史/轮询/发送统一入口）+ 轮询防重入 + 切会话作废旧在途请求 |
| 登录成功后页面状态不刷新（需 F5） | 登录回调链路：存 token → 写响应式仓库 → 拉 `/auth/current-user` 校准 → 再跳转/执行待办 |
| 私信未读角标虚高（隐藏会话仍计数） | 未读总数与会话列表同口径剔除黑名单；拉黑时清空对方未读 |
| 直接访问 8085 根路径返回 404 | 新增 `GET /` 服务信息与接口引导 |
| 前端模块加载失败（图标导出名不存在导致整页挂掉） | 校正图标引用，改用实际存在的导出 |
| 开发环境数据库/对象存储连不上 | 数据源指向容器 MySQL `13306`，MinIO 凭据与 compose 对齐 |
| 登录框暴露测试账号提示 | 移除提示（正式站点不暴露测试凭据） |

### 五、部署（本版含生产编排）
- `Dockerfile`（eclipse-temurin:17-jre-alpine，仅复制运行 jar）、`docker-compose.yml`（开发编排：mysql/redis/minio/pro2）
- 生产专用：`deploy/docker-compose.prod.yml`（独立编排，**仅 Nginx 80 对外**）、`deploy/nginx.prod.conf`（`/api` 反代后端、`/files` 反代 MinIO 图片）、`deploy/.env.prod.example`（密钥与图片公网地址）、`GO-LIVE.md`（上线清单）

### 六、升级 / 发布步骤
```bash
# 1) 打包（工程版本号已统一为 1.0.1）
mvn clean install -DskipTests                 # 产物 target/pro2-1.0.1.jar
cd pro2-frontend && npm run build && cd ..    # 产物 pro2-frontend/dist

# 2) 上传（本地 → 服务器）
scp target/pro2-1.0.1.jar root@qingmeng553.xyz:/opt/pulsetalk/target/
scp -r pro2-frontend/dist/* root@qingmeng553.xyz:/opt/pulsetalk/deploy/www/

# 3) 数据库（仅早期内测库需要增量脚本；全新部署用 sql/init.sql）
docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env.prod \
    exec -T mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" community_db < sql/upgrade-v3.sql

# 4) 重建后端 + 重启网关
ssh root@qingmeng553.xyz "cd /opt/pulsetalk && \
  docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env.prod up -d --build pro2 && \
  docker compose -f deploy/docker-compose.prod.yml restart web"
```

### 七、验证结果（发布前自测）
1. 游客可浏览首页/详情/榜单；点赞评论触发登录弹窗
2. 登录后无需刷新即显示昵称与管理员徽章
3. 发帖（Markdown + emoji + 多图）→ 详情展示 → 编辑后缓存立即更新
4. 点赞/收藏实时变化，热度榜同步更新；消息中心收到通知，评论通知可跳帖定位楼层
5. 私聊非好友限 1 条、好友畅聊；聊天窗刷新不重复、切会话不串台
6. 好友申请/通过/删除、黑名单双向阻断与解除
7. 改名生效、改密后旧密码失效、密保设置后入口隐藏、忘记密码答对可重置且答错触发限流
8. admin 封禁用户 → 发帖/评论被拦；解封恢复；删除后无法登录且同名可注册
9. 手机浏览器下各页面无横向溢出、可正常操作

---

## 后续版本记录模板（新增版本时复制本节到上方）

```markdown
## vX.Y.Z —— 一句话标题（YYYY-MM-DD）

### 新增
- …

### 修复
- …

### 影响范围
- 数据库：无 / sql/upgrade-vX.sql
- 后端：无 / 需重新打 jar
- 前端：无 / 需重新 build

### 升级步骤
（命令）

### 验证结果
- …
```

---

## 版本发布规范（建议）

1. **版本号**：`主版本.次版本.修订号`。新功能 +1 次版本；仅修 bug +1 修订号；涉及不兼容的表结构/接口变更才 +1 主版本。
2. **上传前确认三件事**：
   - 是否含 SQL → 有则随版本携带对应的 `sql/upgrade-*.sql` 并在服务器执行；
   - 是否含后端 → `mvn clean install -DskipTests` 上传 `target/pro2-1.0.1.jar` 并 `up -d --build pro2`；
   - 是否含前端 → `npm run build` 覆盖 `deploy/www` 并 `restart web`。
3. **每次发版在本文件顶部追加一节**（版本号 / 改动清单 / 影响范围 / 升级步骤 / 验证结果），并同步更新顶部「版本总览」表。
4. 建议 `git tag v1.0.1` 打标签，并在服务器保留上一版 jar 与 `www` 备份，便于快速回滚。
