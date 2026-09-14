# PulseTalk

> 一个以 Redis 为核心实战场景的社区系统：帖子、评论、点赞收藏、热度榜单、私聊、好友、站内通知与后台管理。

PulseTalk 面向「高并发读写分离」这一典型场景做了完整落地：**互动类数据（点赞/收藏/浏览）只写 Redis**，由定时任务批量落库；详情页走 Cache-Aside 缓存并配合空值缓存与随机过期；热度榜基于 ZSet 实时计算；接口层用 Redis 计数器限流；登录会话同样托管在 Redis。前端为 Vue 3 单页应用，适配桌面与移动端浏览器。

---

## 功能一览

**内容社区**
- 帖子：Markdown 正文、emoji、最多 9 张配图，支持编辑与逻辑删除
- 评论：一级评论（楼层号）、分页浏览、作者/管理员删除
- 分类筛选、分页帖子流、热度排行榜（ECharts + D3 可视化）
- 浏览量、点赞、收藏计数；详情页点赞/收藏状态与实时计数

**社交互动**
- 站内消息：谁赞了我 / 评论了我 / 收藏了我 / 加我好友；评论通知可跳转帖子并定位楼层
- 私聊：会话列表、消息分页与增量拉取、已读未读；非好友双方各限 1 条打招呼消息
- 好友：申请 / 通过 / 拒绝 / 删除 / 好友列表 / 待处理列表
- 黑名单：拉黑即双向阻断（互发私信、会话隐藏、自动解除好友）
- 个人主页：用户资料、关系态、TA 的帖子；头像可点击进入
- 聊天记录保留 7 天，到期由定时任务清理

**账号安全**
- 修改昵称、修改密码（校验原密码）
- 个人密保问题（问题与答案均为用户自定义，答案加密存储）
- 忘记密码：输入账号取回本人密保问题，答对即可重置密码（带限流保护）

**管理后台（ADMIN）**
- 软删除任意帖子 / 评论
- 全站唯一一条置顶规则帖：可设置、替换、取消，管理员可共同编辑
- 向全体用户发送官方消息
- 用户管理：搜索、封禁 / 解封、删除；被封禁用户无法发帖与评论

---

## 技术栈

| 层 | 选型 |
| --- | --- |
| 后端 | JDK 17 · Spring Boot 3.2 · Sa-Token（会话存 Redis）· MyBatis-Plus |
| 存储 | MySQL 8 · Redis 7 · MinIO（对象存储） |
| 前端 | Vue 3 · Vite · Element Plus · ECharts · D3.js · axios |
| 部署 | Docker · Docker Compose · Nginx |

---

## 系统设计要点

### Redis 键设计

| Key | 类型 | 用途 |
| --- | --- | --- |
| `community:post:like:{postId}` | Set | 点赞用户集合 |
| `community:post:like:count:{postId}` | String | 点赞计数 |
| `community:post:collect:{postId}` | Set | 收藏用户集合 |
| `community:post:collect:count:{postId}` | String | 收藏计数 |
| `community:post:view:{postId}` | String | 浏览量（INCR） |
| `community:post:hot:zset` | ZSet | 热度榜，`score = 点赞×3 + 收藏×5 + 浏览×0.1` |
| `community:post:cache:{postId}` | String | 帖子详情缓存（JSON） |
| `community:post:cache:null:{postId}` | String | 空值缓存，防缓存穿透 |
| `community:limit:create:post:{userId}` | String | 发帖限流计数 |
| `community:limit:comment:{userId}` | String | 评论限流计数 |

### 关键设计
- **计数异步落库**：点赞/收藏/浏览只操作 Redis，通过脏标记集合记录变更帖子，定时任务（每 5 分钟）以 Redis 绝对值为准覆盖写回 MySQL，写库幂等；读取端直接取 Redis 实时值。
- **缓存三件套**：Cache-Aside 旁路缓存（更新数据库后失效缓存）；空值缓存拦截不存在的 ID；缓存 TTL 加入 30~60 分钟随机偏移规避雪崩。
- **热度榜**：点赞 ±3、收藏 ±5、浏览 +0.1 实时增量维护 ZSet，读取时按分数倒序取 TopN，并回表补全标题与分类。
- **接口限流**：Redis 计数器固定窗口，发帖 3 次/分钟、评论 10 次/分钟，找回密码另有独立限流。
- **会话管理**：Sa-Token 登录态持久化到 Redis，支持多端与多实例共享。
- **文件上传**：MinIO 桶自动创建并设置公共读策略；扩展名白名单 + 文件头魔数 + 5MB 上限三重校验，拒绝脚本伪装。
- **删除策略**：业务数据一律逻辑删除；用户删除额外释放原用户名占用。
- **统一响应**：HTTP 200 + 业务码（401 未登录 / 403 无权限 / 404 不存在 / 429 限流），前端拦截 401 弹登录框，避免浏览器错误页。

---

## 目录结构

```
.
├── pom.xml                    # 单模块后端工程
├── Dockerfile                 # 仅复制运行构建产物
├── docker-compose.yml         # 本地开发编排：mysql / redis / minio / app
├── deploy/                    # 生产编排与网关配置
│   ├── docker-compose.prod.yml
│   ├── nginx.prod.conf
│   ├── .env.prod.example
│   └── www/                   # 前端构建产物挂载目录
├── sql/
│   ├── init.sql               # 初始化：建库建表 + 预置账号与分类
│   └── upgrade-v*.sql         # 历史版本增量脚本
├── src/main/java/com/community/
│   ├── common/                # 统一响应、异常、Redis Key、工具类
│   ├── config/                # Sa-Token、MyBatis-Plus、MinIO、CORS 等配置
│   ├── controller/            # 认证、帖子、评论、消息、私聊、关系、管理端
│   ├── service/ (impl)        # 业务服务
│   ├── task/                  # 计数同步、聊天记录清理
│   └── entity/ dto/ vo/ mapper/
├── src/main/resources/
│   ├── application.yml
│   ├── application-dev.yml    # 本机开发
│   └── application-docker.yml # 容器环境（支持环境变量注入）
└── pro2-frontend/             # Vue 3 前端
    ├── src/api/ components/ views/ store/ utils/ styles/
    └── vite.config.js         # /api 代理到后端并携带 sa-token
```

---

## 快速开始

### 1. 依赖服务
```bash
docker compose up -d mysql minio     # MySQL(宿主 13306) + MinIO(9000/9001)
# 本机若无 Redis：docker run -d --name pulsetalk-redis -p 6379:6379 redis:7-alpine
```
MySQL 首次启动会自动执行 `sql/init.sql`（建表 + 预置账号与分类）。

### 2. 启动后端（端口 8085）
```bash
mvn spring-boot:run
# 或
mvn clean install -DskipTests && java -jar target/pro2-1.0.1.jar
```
接口探测：`GET http://localhost:8085/demo/ping`

### 3. 启动前端（端口 5173）
```bash
cd pro2-frontend
npm install
npm run dev        # 打开 http://localhost:5173
```

### 4. 默认账号
| 账号 | 密码 | 角色 |
| --- | --- | --- |
| admin | 123456 | 管理员 |
| test | 123456 | 普通用户 |

---

## 接口概览

| 模块 | 主要接口 |
| --- | --- |
| 认证 | `POST /auth/register`、`POST /auth/login`、`POST /auth/logout`、`GET /auth/current-user`、`POST /auth/upload-avatar` |
| 账号安全 | `PUT /auth/profile`、`PUT /auth/password`、`GET/POST /auth/security-question`、`POST /auth/forgot/question`、`POST /auth/forgot/reset` |
| 帖子 | `GET /post/category/list`、`GET /post/list`、`GET /post/{id}`、`GET /post/pinned`、`POST /post`、`PUT /post/{id}`、`DELETE /post/{id}`、`DELETE /post/admin/{id}`、`PUT /post/admin/pin/{id}`、`DELETE /post/admin/pin`、`POST /post/image` |
| 互动 | `POST /post/{id}/like`、`POST /post/{id}/collect`、`GET /post/hot/rank` |
| 评论 | `GET /post/{postId}/comment/list`、`POST /post/{postId}/comment`、`DELETE /comment/{id}`、`DELETE /comment/admin/{id}` |
| 消息 | `GET /notify/list`、`GET /notify/unread-count`、`POST /notify/read`、`POST /notify/broadcast` |
| 私聊 | `GET /chat/threads`、`GET /chat/messages/{userId}`、`POST /chat/send/{userId}`、`POST /chat/read/{userId}`、`GET /chat/unread-count` |
| 关系 | `GET /user/{id}`、`POST /friend/request|accept|reject|remove/{userId}`、`GET /friend/list`、`GET /friend/pending/list`、`POST /blacklist/add|remove/{userId}`、`GET /blacklist/list` |
| 管理端 | `GET /admin/user/list`、`POST /admin/user/{id}/ban`、`POST /admin/user/{id}/unban`、`DELETE /admin/user/{id}` |
| 探测 | `GET /demo/ping` |

---

## 部署

开发环境使用根目录 `docker-compose.yml`（mysql / redis / minio / app 四服务，含健康检查与数据卷）。
生产环境使用 `deploy/docker-compose.prod.yml`：**唯一对外入口为 Nginx 的 80/443**，`/api` 反向代理到后端、`/files` 反向代理到 MinIO 图片，数据库/缓存/对象存储与后端均不暴露宿主机端口；密钥与图片公网地址通过 `deploy/.env.prod.example` 复制为 `.env.prod` 后注入。

```bash
# 本地构建
mvn clean install -DskipTests
cd pro2-frontend && npm run build && cd ..

# 生产启动/更新
docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env.prod up -d --build
```

---

## 更新记录

### v1.0.1
- 账号安全：修改昵称、修改密码；个人密保问题与「忘记密码」自助重置（带限流）
- 管理端用户表：搜索、封禁 / 解封 / 删除；被封禁用户不可发帖与评论
- 社交能力：消息通知、私聊、好友、黑名单、置顶规则帖、官方消息广播
- Redis 设计完善：计数异步落库、详情缓存三件套、热度榜、接口限流
- 前端：消息中心 / 聊天窗 / 他人主页 / 好友管理 / 用户管理页面，移动端适配
- 修复：聊天消息重复渲染、登录后状态不刷新、未读角标计数、若干编译与依赖问题

### v1.0.0
- 首个可运行版本：单模块后端 + Vue 3 前端 + Docker 编排，完成帖子、评论、点赞收藏、热度榜与对象存储上传
