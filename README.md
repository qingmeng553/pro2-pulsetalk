# PulseTalk

以 Redis 为核心场景的社区系统：帖子、评论、点赞收藏、热度推荐、私聊、好友、站内通知与后台管理。

互动类数据（点赞 / 收藏 / 浏览）只写 Redis，由定时任务批量落库；详情页采用 Cache-Aside 缓存并配合空值缓存与随机过期；热度推荐基于 ZSet 实时计算；接口层使用 Redis 计数器限流；登录会话同样托管在 Redis。前端为 Vue 3 单页应用，适配桌面与移动端浏览器。

---

## 功能

**内容社区**
- 帖子：Markdown 正文、emoji、最多 9 张配图，支持编辑与逻辑删除
- 评论：一级评论（楼层号）、分页浏览、作者与管理员删除
- 三栏首页（左导航 / 信息流 / 活动位）、分类筛选、关键词搜索、分页帖子流、大家都在看（热度 Top5）
- 浏览量、点赞、收藏计数，详情页展示实时状态

**社交互动**
- 站内消息：谁赞了 / 评论了 / 收藏了我的帖子、谁加我好友；评论通知可跳转帖子并定位楼层
- 私聊：会话列表、消息分页与增量拉取、已读未读；非好友双方各限 1 条打招呼消息
- 好友：申请、通过、拒绝、删除、好友列表、待处理列表
- 黑名单：拉黑即双向阻断（互发私信、会话隐藏、自动解除好友）
- 个人主页：资料、关系态、TA 的帖子；头像可点击进入
- 聊天记录保留 7 天，到期由定时任务清理

**账号安全**
- 修改昵称、修改密码（校验原密码）
- 个人密保问题（问题与答案均为用户自定义，答案加密存储）
- 忘记密码：输入账号取回本人密保问题，答对即可重置密码，带限流保护

**管理后台（ADMIN）**
- 软删除任意帖子 / 评论
- 全站唯一一条置顶规则帖：设置、替换、取消，管理员可共同编辑
- 向全体用户发送官方消息
- 用户管理：搜索、封禁 / 解封、删除；被封禁用户无法发帖与评论

---

## 技术栈

| 层 | 选型 |
| --- | --- |
| 后端 | JDK 17 · Spring Boot 3.2 · Sa-Token（会话存 Redis）· MyBatis-Plus |
| 存储 | MySQL 8 · Redis 7 · MinIO |
| 前端 | Vue 3 · Vite · Element Plus · ECharts · D3.js · axios |
| 部署 | Docker · Docker Compose · Nginx |

---

## 设计要点

### Redis 键

| Key | 类型 | 用途 |
| --- | --- | --- |
| `community:post:like:{postId}` | Set | 点赞用户集合 |
| `community:post:like:count:{postId}` | String | 点赞计数 |
| `community:post:collect:{postId}` | Set | 收藏用户集合 |
| `community:post:collect:count:{postId}` | String | 收藏计数 |
| `community:post:view:{postId}` | String | 浏览量（INCR） |
| `community:post:hot:zset` | ZSet | 热度榜，`score = 点赞×3 + 收藏×5 + 浏览×0.1` |
| `community:post:cache:{postId}` | String | 帖子详情缓存 |
| `community:post:cache:null:{postId}` | String | 空值缓存 |
| `community:limit:create:post:{userId}` | String | 发帖限流 |
| `community:limit:comment:{userId}` | String | 评论限流 |

### 实现要点
- **计数异步落库**：点赞 / 收藏 / 浏览只操作 Redis，脏标记集合记录变更帖子，定时任务每 5 分钟以 Redis 绝对值为准覆盖写回 MySQL，写库幂等；读取端直接取 Redis 实时值
- **缓存**：Cache-Aside 旁路缓存，更新数据库后失效缓存；空值缓存拦截不存在的 ID；TTL 加入 30~60 分钟随机偏移
- **热度推荐**：点赞 ±3、收藏 ±5、浏览 +0.1 实时增量维护 ZSet，首页左栏按分数倒序取 Top5 供「大家都在看」；Redis 榜单为空时启动自动按数据库计数重建
- **限流**：Redis 计数器固定窗口，发帖 3 次/分钟、评论 10 次/分钟；忘记密码有独立限流
- **会话**：Sa-Token 登录态持久化 Redis，支持多端与多实例共享
- **上传**：MinIO 桶自动创建并设置公共读；扩展名白名单 + 文件头魔数 + 5MB 上限校验
- **删除**：业务数据逻辑删除；删除用户时释放原用户名占用
- **响应**：HTTP 200 + 业务码（401 未登录 / 403 无权限 / 404 不存在 / 429 限流），前端拦截 401 弹出登录框

---

## 目录结构

```
.
├── pom.xml
├── Dockerfile
├── docker-compose.yml         # 开发编排：mysql / redis / minio / app
├── deploy/
│   ├── docker-compose.prod.yml
│   ├── nginx.prod.conf
│   ├── .env.prod.example
│   └── www/                   # 前端构建产物
├── sql/
│   ├── init.sql
│   └── upgrade-v*.sql
├── src/main/java/com/community/
│   ├── common/                # 统一响应、异常、Redis Key、工具
│   ├── config/                # Sa-Token、MyBatis-Plus、MinIO、CORS
│   ├── controller/            # 认证、帖子、评论、消息、私聊、关系、管理端
│   ├── service/ (impl)
│   ├── task/                  # 计数同步、聊天记录清理
│   └── entity/ dto/ vo/ mapper/
├── src/main/resources/
│   ├── application.yml
│   ├── application-dev.yml
│   └── application-docker.yml
└── pro2-frontend/
    ├── src/api/ components/ views/ store/ utils/ styles/
    └── vite.config.js
```

---

## 本地部署

### 环境要求

| 依赖 | 版本 | 用途 |
| --- | --- | --- |
| JDK | 17 | 后端运行与编译 |
| Maven | 3.8+ | 依赖管理与打包 |
| Node.js | 18+ | 前端构建 |
| Docker | 20.10+（含 Compose v2） | 启动 MySQL 与 MinIO |
| Redis | 7 | 可用本机实例或容器 |

### 1. 启动依赖服务

```bash
# MySQL（宿主机 13306）与 MinIO（9000 / 9001）
docker compose up -d mysql minio

# Redis：本机已有 7.x 可跳过；否则用容器
docker run -d --name pulsetalk-redis -p 6379:6379 redis:7-alpine
```

MySQL 容器首次启动时会自动执行 `sql/init.sql`：创建 `community_db`、9 张业务表，并预置账号与分类，首次初始化约 30~60 秒，可通过 `docker compose ps` 查看健康状态。

对应连接参数写在 `src/main/resources/application-dev.yml`：

| 项 | 值 |
| --- | --- |
| MySQL | `127.0.0.1:13306` · 库 `community_db` · root / root2297752516 |
| Redis | `127.0.0.1:6379`（无密码） |
| MinIO | `127.0.0.1:9000` · minioadmin / minioadmin123 · 桶 `community-upload` |

> 图片统一存相对路径 `/files/...`：开发环境由 Vite 把 `/files` 代理到 MinIO 9000，生产由 Nginx 反代，不再需要配置公网图片地址。

> 使用本机自建 MySQL 时，改该文件的 `spring.datasource.url` 为对应端口，并手动执行 `sql/init.sql`。

### 2. 启动后端（8085）

```bash
mvn spring-boot:run
# 或先打包再运行
mvn clean install -DskipTests
java -jar target/pro2-1.1.0.jar
```

启动成功日志包含：Sa-Token 会话持久层使用 Redis、MinIO 桶初始化完成、`PulseTalk 社区后端启动成功`。存活探测：`curl http://localhost:8085/demo/ping`。

### 3. 启动前端（5173）

```bash
cd pro2-frontend
npm install
npm run dev            # 浏览器访问 http://localhost:5173
```

前端请求统一走 `/api`，由 Vite 代理转发到 `http://localhost:8085`（剥离前缀）并自动携带 `satoken` 头，无跨域配置。

### 4. 登录账号

| 账号 | 密码 | 角色 | 权限 |
| --- | --- | --- | --- |
| admin | 123456 | ADMIN | 全部功能 + 软删除任意内容 + 置顶规则帖 + 用户管理 |
| test | 123456 | USER | 发帖、评论、点赞收藏、私聊、好友 |

### 5. 自检路径

1. 首页可浏览帖子流与置顶公告，未登录点点赞会弹出登录框
2. 登录后顶栏立即显示昵称（admin 带管理员徽章），无需刷新
3. 发帖支持 Markdown、emoji 与多图上传，详情页可预览图片
4. 点赞/收藏即时生效，左栏「大家都在看」分数同步变化
5. 消息中心可看到点赞/评论/收藏/好友通知，评论通知可跳转帖子定位楼层
6. 私聊：非好友双方各限 1 条，加好友后可继续
7. 个人中心可改昵称、改密码、设置密保问题；退出后用「忘记密码」可自助重置
8. admin 进入「用户管理」可封禁/解封/删除用户

---

## 接口

| 模块 | 接口 |
| --- | --- |
| 认证 | `POST /auth/register`、`POST /auth/login`、`POST /auth/logout`、`GET /auth/current-user`、`POST /auth/upload-avatar` |
| 账号安全 | `PUT /auth/profile`、`PUT /auth/password`、`GET/POST /auth/security-question`、`POST /auth/forgot/question`、`POST /auth/forgot/reset` |
| 帖子 | `GET /post/category/list`、`GET /post/list`（`keyword` 可选，标题/正文模糊搜索）、`GET /post/{id}`、`GET /post/pinned`、`POST /post`、`PUT /post/{id}`、`DELETE /post/{id}`、`DELETE /post/admin/{id}`、`PUT /post/admin/pin/{id}`、`DELETE /post/admin/pin`、`POST /post/image` |
| 互动 | `POST /post/{id}/like`、`POST /post/{id}/collect`、`GET /post/hot/rank` |
| 评论 | `GET /post/{postId}/comment/list`、`POST /post/{postId}/comment`、`DELETE /comment/{id}`、`DELETE /comment/admin/{id}` |
| 消息 | `GET /notify/list`、`GET /notify/unread-count`、`POST /notify/read`、`POST /notify/broadcast` |
| 私聊 | `GET /chat/threads`、`GET /chat/messages/{userId}`、`POST /chat/send/{userId}`、`POST /chat/read/{userId}`、`GET /chat/unread-count` |
| 关系 | `GET /user/{id}`、`POST /friend/request|accept|reject|remove/{userId}`、`GET /friend/list`、`GET /friend/pending/list`、`POST /blacklist/add|remove/{userId}`、`GET /blacklist/list` |
| 管理端 | `GET /admin/user/list`、`POST /admin/user/{id}/ban`、`POST /admin/user/{id}/unban`、`DELETE /admin/user/{id}` |
| 探测 | `GET /demo/ping` |

---

## 服务器部署

### 架构

生产环境只对外暴露 Nginx 的 `80 / 443`：`/api` 反向代理到后端，`/files` 反向代理到 MinIO。MySQL、Redis、MinIO 与后端仅在内网（Docker 网络）互通，宿主机不映射端口。

```
浏览器 ──► Nginx(80/443)
             ├── /            前端静态资源
             ├── /api/*       后端（8085，剥离 /api 前缀）
             └── /files/*     MinIO（9000，图片）
                    └── 内网：mysql:3306 · redis:6379 · minio:9000 · pro2:8085
```

### 1. 构建

```bash
mvn clean install -DskipTests                 # 产物 target/pro2-1.1.0.jar
cd pro2-frontend && npm run build && cd ..    # 产物 pro2-frontend/dist
cp -r pro2-frontend/dist/* deploy/www/        # 前端产物放入挂载目录
```

### 2. 配置

```bash
cp deploy/.env.prod.example deploy/.env.prod
```

| 变量 | 说明 |
| --- | --- |
| `MYSQL_ROOT_PASSWORD` | 数据库 root 密码 |
| `MINIO_ACCESS_KEY` / `MINIO_SECRET_KEY` | MinIO 账号与密钥 |
| `REDIS_PASSWORD` | Redis 密码，留空表示不启用 |
| `MINIO_PUBLIC_ENDPOINT` | 图片地址前缀，默认 `/files`，一般不用改；只有图片要直连其他对象存储域名时才覆盖 |

### 3. 上传与启动

```bash
# 上传项目（Dockerfile、compose、sql、src、deploy 均为必需；jar 与 dist 为构建产物）
scp -r pom.xml Dockerfile docker-compose.yml sql src deploy root@<服务器IP>:/opt/pulsetalk/
scp target/pro2-1.1.0.jar root@<服务器IP>:/opt/pulsetalk/target/

# 启动
docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env.prod up -d --build
docker compose -f deploy/docker-compose.prod.yml ps      # 期望 mysql/redis/minio/pro2/web 均 healthy
```

### 4. 防火墙（云服务器安全组）

| 端口 | 说明 |
| --- | --- |
| 80 / 443 | 站点入口，必开 |
| 22 | SSH 部署 |
| 8085 / 9000 / 9001 / 3306 / 6379 | 不需要开放，全部走内网与反向代理 |

### 5. 验证

```bash
curl http://<服务器IP>/api/demo/ping                 # 经 Nginx 探测后端
curl -X POST http://<服务器IP>/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username":"admin","password":"123456"}'
```

浏览器访问 `http://<服务器IP>/`，发帖并上传一张图片，确认图片地址形如 `/files/community-upload/...`（相对路径，浏览器按当前域名解析）。

### 6. HTTPS

在云控制台申请免费证书并下载 Nginx 版本，将证书挂载到 web 容器，按 `deploy/nginx.prod.conf` 中的注释启用 443 段；图片是相对路径，换 HTTPS 不需要改数据库或 `MINIO_PUBLIC_ENDPOINT`。

### 7. 版本升级

```bash
# 数据库增量脚本按需执行（v2：社交表；v3：用户密保与封禁字段）
docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env.prod \
    exec -T mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" community_db < sql/upgrade-v3.sql

# 替换 jar 与前端资源后重建
docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env.prod up -d --build pro2
docker compose -f deploy/docker-compose.prod.yml restart web
```

### 8. 运维

```bash
# 日志
docker compose -f deploy/docker-compose.prod.yml logs -f pro2 web minio

# 数据卷备份（MySQL）
docker run --rm -v pulsetalk_mysql-data:/data -v /opt/pulsetalk/backup:/backup \
    alpine tar czf /backup/mysql-$(date +%F).tgz -C /data .

# 全新初始化（清空数据，慎用）
docker compose -f deploy/docker-compose.prod.yml down -v
docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env.prod up -d --build
```

### 常见问题

| 现象 | 原因与处理 |
| --- | --- |
| 图片打不开 | 检查 Nginx `location /files/` 是否反代到 `minio:9000`；本地开发检查 Vite 的 `/files` 代理与 MinIO 是否启动 |
| 页面能开、接口 404 | 访问地址应带 `/api`（经 Nginx）；后端未暴露 8085 |
| 经 `/api` 仍返回「资源不存在」 | Nginx `proxy_pass http://后端:8085/` 结尾缺少 `/`，`/api` 前缀未被剥离 |
| 容器一直 unhealthy | 查看 `logs mysql`，首次初始化需要 30~60 秒 |
| 点赞计数未落库 | 数据库计数每 5 分钟同步一次，详情页读取的是 Redis 实时值 |
| 忘记密码提示未设置密保 | 需先在个人中心设置密保问题，未设置时由管理员重置 |
| 主键显示为字符串 | 后端将 Long 序列化为字符串，避免前端精度丢失 |

---

## 更新记录

### v1.1.0（进行中）
- 界面体系重构：统一设计 Token（颜色 / 字号 / 间距 / 圆角 / 阴影 / 动效），去除渐变与彩色阴影
- 布局重构：三栏骨架（左侧导航 + 中间信息流 + 右侧活动位），帖子详情改为「左正文 + 右互动」
- 交互调整：以滚动浏览为主，取消榜单排名页，改为「大家都在看」
- 首页与登录已落地：三栏骨架 + 紧凑信息流 + 关键词搜索 + 移动端底部 Tab；登录/注册弹窗改分段切换（S5/S6）
- 帖子详情已落地：左正文 + 右互动/评论（960~1279 左栏进抽屉、评论仍在右侧），相关推荐，移动端吸底操作条；点赞红心常显（未点亮描边、点亮实心）（S7）
- 地址收口：图片统一存相对路径 `/files`（后端 `public-endpoint=/files`，Vite/Nginx 反代），历史绝对地址批量迁移（`sql/upgrade-v5.sql`）（S1）
- 修复：计数同步每 5 分钟会刷新帖子「编辑于」时间（`sql/upgrade-v5.sql` 去掉 `post.update_time` 的 `ON UPDATE`）
- 账号体系：邮箱绑定 + 验证码找回密码（与密保双通道）
- 个人主页：模块开关 / 排序 + 主题装扮（参考 B 站空间与 GitHub Profile）
- 多端：Capacitor 壳的移动 App（1.2.0 起全功能对齐）

### v1.0.3
- 注册成功后自动登录；登录状态即时生效，无需刷新页面
- 前端交互与动效优化：导航滚动投影、卡片入场与悬浮、点赞弹跳、消息气泡滑入、未读红点脉冲
- 代码注释精简为短说明
- 临时修改了图片上传失败的问题

### v1.0.1
- 账号安全：修改昵称、修改密码、个人密保问题与忘记密码自助重置
- 管理后台：用户管理（搜索、封禁 / 解封、删除），封禁用户不可发帖与评论
- 社交：站内通知、私聊、好友、黑名单、置顶规则帖、官方消息
- Redis：计数异步落库、详情缓存三件套、热度榜、接口限流
- 前端：消息中心、聊天窗、他人主页、好友管理、用户管理；移动端适配
- 修复：聊天消息重复渲染、登录后状态不刷新、未读计数、编译与依赖问题

### v1.0.0
- 首个可运行版本：单模块后端 + Vue 3 前端 + Docker 编排，完成帖子、评论、点赞收藏、热度榜与对象存储上传
