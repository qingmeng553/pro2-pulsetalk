# PulseTalk 社区（原名：点评贴吧 / pro2）

> Redis 综合实战项目 · 仿贴吧社区 · 兼顾面试技术深度与适度娱乐化动效

单模块 SpringBoot3 后端 + `pro2-frontend`(Vue3) 前端。
游客默认只读；登录后可发帖/点赞/收藏/评论/上传；ADMIN 管理员可软删除任意内容并共同维护唯一一条**置顶规则帖**。

### 🆕 v2 新增能力
- **应用更名 PulseTalk**（前后端界面/文档同步）
- **消息板块**：通知中心看「谁赞了我 / 评论了我 / 收藏了我 / 加我好友」，评论通知一键跳转帖子定位楼层
- **私聊**：点头像进入对方主页 → 了解彼此、私聊、加好友；**非好友双方各限 1 条**打招呼消息，成为好友后畅聊
- **好友 + 黑名单**：申请/通过/拒绝/删除、好友管理页；**拉黑即双向阻断**（互发私信、会话隐藏、自动解除好友）
- **聊天记录 7 天清理**：定时任务每天物理清理过期消息
- **置顶规则帖**：全站仅一条，管理员可共同编辑/置顶/取消（管理员可编辑非本人发布的置顶帖）

---

## ✨ 核心特性

| 领域 | 说明 |
| --- | --- |
| 身份模式 | 游客优先只读（浏览帖子/评论/榜单），写操作需登录；鉴权失败返回业务码 401，前端弹「去登录 / 取消」确认框，杜绝 403 空白页 |
| 会话方案 | Sa-Token 完成登录，token 与 Session 持久化到 **Redis**（`sa-token-redis-jackson`，Jackson 序列化） |
| 计数模型 | 点赞/收藏/浏览 **只写 Redis**，定时任务每 5 分钟批量同步 MySQL（脏标记集合驱动，不实时写库） |
| 缓存策略 | Cache-Aside 旁路缓存热点帖子详情；空值缓存防穿透；TTL 随机偏移防雪崩 |
| 热度榜单 | Redis **ZSet** 维护 `score = 点赞×3 + 收藏×5 + 浏览×0.1`，点赞±3/收藏±5/浏览+0.1 实时增量 |
| 接口限流 | Redis 计数器固定窗口：发帖 3 次/分钟、评论 10 次/分钟 |
| 删除逻辑 | 全部逻辑软删除 `is_deleted=1`，无物理 DELETE；管理员 `@SaCheckRole("admin")` |
| 对象存储 | MinIO `community-upload` 桶自动创建 + 公共读策略；头像/配图上传，格式(魔数)校验 + 5MB 上限，拒绝可执行脚本伪装 |
| 前端动效 | ECharts 热度统计图表；D3.js 帖子卡片入场、hover 上浮、榜单条目动态渲染 |

---

## 🧱 技术栈

| 层 | 技术 |
| --- | --- |
| 后端 | JDK 17 LTS · Spring Boot 3.2.5 · MyBatis-Plus 3.5.7 · Sa-Token 1.39.0 · Spring Data Redis(Lettuce) |
| 存储 | MySQL 8（`community_db`）· Redis 7 · MinIO |
| 前端 | Vue 3 · Vite 5 · Element-Plus · ECharts · D3.js · marked + DOMPurify · axios |

---

## 📁 目录结构

```
pro2/
├── pom.xml                          # 单模块后端工程(禁止多模块聚合)
├── Dockerfile                       # 仅复制并运行 target jar(eclipse-temurin:17-jre-alpine)
├── docker-compose.yml               # mysql8 + redis:7-alpine + minio + pro2 四服务编排
├── .dockerignore
├── sql/
│   └── init.sql                     # Docker MySQL 初始化脚本(建表+预置账号/分类)，含 DROP TABLE IF EXISTS
├── src/main/
│   ├── java/com/community/
│   │   ├── Pro2Application.java
│   │   ├── common/                  # R 统一响应 / ResultCode / 全局异常 / RedisKeys / PasswordUtil / 图片校验
│   │   ├── config/                  # Sa-Token拦截器·Redis持久层绑定·MyBatisPlus分页·字段填充·MinIO·CORS·角色源
│   │   ├── controller/              # Auth / Post / Comment / Demo
│   │   ├── dto/  entity/  mapper/   # 请求体 / 表实体 / MyBatis-Plus Mapper
│   │   ├── service/ (+impl)         # 业务服务：发帖·详情缓存·互动·榜单·限流·上传·同步
│   │   ├── task/                    # PostCountSyncTask 每5分钟 Redis→MySQL 计数同步
│   │   └── vo/                      # 出参模型(含 role 供前端渲染管理员徽章)
│   └── resources/
│       ├── application.yml          # 公共配置(默认激活 dev)
│       ├── application-dev.yml      # 本机开发配置(localhost，禁止修改)
│       └── application-docker.yml   # 容器环境配置(mysql/redis/minio 服务名互访)
└── pro2-frontend/                   # Vue3 前端工程
```

---

## 🚀 快速开始（本地开发）

### 前置
- JDK 17、Maven 3.6+、Node 18+、npm/pnpm
- 本机 MySQL 8（root/root2297752516，**手动创建库** `community_db`）
- 本机 Redis 7（默认 6379、无密码）
- 本机 MinIO（9000/9001，账号 minioadmin/minioadmin，或按需修改 `application-dev.yml`）

### 1. 初始化数据库（任选其一）
```bash
# 方式A：手动在 MySQL 执行(会 DROP 重建，仅限空库/测试库)
mysql -uroot -proot2297752516 < sql/init.sql

# 方式B：只建库建表不导入种子数据也可以
#   应用首次启动会自动预置 admin/test 账号与 6 个默认分类(表存在但为空时)
```
> ⚠️ `sql/*.sql` 主要用于 Docker 容器初始化；在本机执行前请确认不影响现有数据。

### 2. 启动后端（默认激活 dev profile，端口 8085）
```bash
mvn spring-boot:run
# 或
mvn clean install -DskipTests
java -jar target/pro2-1.0.0.jar
```
健康检查：`curl http://localhost:8085/demo/ping`

### 3. 启动前端（Vite 代理 /api → 8085，自动携带 satoken 头）
```bash
cd pro2-frontend
npm install
npm run dev
# 浏览器打开 http://localhost:5173
```

### 默认账号
| 账号 | 密码 | 角色 | 能力 |
| --- | --- | --- | --- |
| admin | 123456 | ADMIN | 发帖评论点赞 + 软删除任意帖子/评论(前端显示管理员盾牌徽章与删除按钮) |
| test | 123456 | USER | 普通用户，仅可管理自己发布的内容 |

---

## 🗄️ 数据库表（community_db，字段严格按规范，无多余列）

| 表 | 关键字段 |
| --- | --- |
| sys_user | id, username, password(加密), nickname, avatar_url, role(USER/ADMIN), create_time |
| post_category | id, name, sort, create_time |
| post | id, user_id, category_id, title, content(markdown), img_urls(JSON数组), view_count, like_count, collect_count, is_deleted, create_time, update_time |
| post_comment | id, post_id, user_id, content, is_deleted, create_time |
| user_friend (v2) | id, user_id, friend_id, status(0申请/1通过), create_time, update_time |
| user_blacklist (v2) | id, user_id, black_user_id, create_time |
| chat_message (v2) | id, from_user_id, to_user_id, content, is_read, create_time（7天自动清理） |
| user_notify (v2) | id, user_id, actor_id, type(LIKE/COMMENT/COLLECT/FRIEND), post_id, comment_id, content, is_read, create_time |
| post_pin (v2) | id(固定1=仅一条置顶), post_id, admin_id, update_time |

> 密码存储格式：`salt$sha256(salt+明文)`（见 `PasswordUtil`，常量时间比较；生产可平滑升级 BCrypt）。
> v1 已初始化过的库执行 `sql/upgrade-v2.sql` 增量建新表即可，无需重建。

---

## 🔑 Redis Key 规范（统一前缀 `community:`）

| Key | 类型 | 用途 |
| --- | --- | --- |
| `community:post:like:{postId}` | Set | 点赞用户 id 集合 |
| `community:post:like:count:{postId}` | String | 点赞计数 |
| `community:post:collect:{postId}` | Set | 收藏用户 id 集合 |
| `community:post:collect:count:{postId}` | String | 收藏计数 |
| `community:post:view:{postId}` | String | 浏览量(INCR) |
| `community:post:hot:zset` | ZSet | 热度榜单，score=点赞×3+收藏×5+浏览×0.1 |
| `community:post:cache:{postId}` | String | 热点帖子详情缓存 JSON |
| `community:post:cache:null:{postId}` | String | 空值缓存（防穿透） |
| `community:limit:create:post:{userId}` | String | 发帖限流计数（3 次/分钟） |
| `community:limit:comment:{userId}` | String | 评论限流计数（10 次/分钟） |
| `community:post:sync:dirty` | Set | 【内部辅助】待同步计数帖子 id（定时任务消费） |

**同步任务**：`task/PostCountSyncTask` 每 5 分钟（`0 */5 * * * *`）读取脏集合，
以 Redis 绝对值（点赞=SCARD、收藏=SCARD、浏览=INCR 累计）覆盖写回 post 表，成功后移除脏标记，全程幂等。

---

## 🔌 接口清单（严格按规范实现）

### Auth
| 方法 | 路径 | 说明 | 鉴权 |
| --- | --- | --- | --- |
| POST | /auth/register | 用户注册 | 公开 |
| POST | /auth/login | 登录，返回 sa-token | 公开 |
| POST | /auth/logout | 登出 | 登录 |
| GET | /auth/current-user | 当前登录用户（含 role） | 登录 |
| POST | /auth/upload-avatar | 上传头像，返回 MinIO URL | 登录 |

### Post
| 方法 | 路径 | 说明 | 鉴权 |
| --- | --- | --- | --- |
| GET | /post/category/list | 全部分类 | 游客 |
| GET | /post/list | 帖子分页（支持 categoryId 筛选） | 游客 |
| GET | /post/{id} | 详情（浏览量 Redis 自增 + 热点缓存） | 游客 |
| POST | /post | 新建帖子（限流） | 登录 |
| PUT | /post/{id} | 修改自己的帖子（更新后删缓存） | 登录·作者 |
| DELETE | /post/{id} | 删除自己的帖子（软删除+清缓存） | 登录·作者 |
| DELETE | /post/admin/{id} | 管理员软删除任意帖子 | ADMIN |
| POST | /post/{id}/like | 点赞/取消（仅 Redis） | 登录 |
| POST | /post/{id}/collect | 收藏/取消（仅 Redis） | 登录 |

### Comment
| 方法 | 路径 | 说明 | 鉴权 |
| --- | --- | --- | --- |
| GET | /post/{postId}/comment/list | 评论分页（过滤软删除，带楼层号） | 游客 |
| POST | /post/{postId}/comment | 发表评论（限流） | 登录 |
| DELETE | /comment/{id} | 删除自己的评论 | 登录·作者 |
| DELETE | /comment/admin/{id} | 管理员软删除任意评论 | ADMIN |

### 榜单 / 探测
| 方法 | 路径 | 说明 | 鉴权 |
| --- | --- | --- | --- |
| GET | /post/hot/rank | ZSet 热度榜 TopN（ECharts/D3 渲染） | 游客 |
| GET | /post/pinned (v2) | 当前置顶规则帖 | 游客 |
| PUT | /post/admin/pin/{id} (v2) | 管理员设某帖为置顶规则帖(替换旧置顶) | ADMIN |
| DELETE | /post/admin/pin (v2) | 管理员取消置顶 | ADMIN |
| GET | /demo/ping | 存活探测 | 公开 |

### v2：社交（通知/私聊/好友/黑名单）
| 方法 | 路径 | 说明 | 鉴权 |
| --- | --- | --- | --- |
| GET | /notify/list | 通知分页(点赞/评论/收藏/好友) | 登录 |
| GET | /notify/unread-count | 未读通知数 | 登录 |
| POST | /notify/read | 标记已读(ids空=全部) | 登录 |
| GET | /chat/threads | 会话列表(拉黑会话隐藏) | 登录 |
| GET | /chat/messages/{userId} | 消息分页/afterId 增量轮询 | 登录 |
| POST | /chat/send/{userId} | 发私信(黑名单校验/非好友各限1条) | 登录 |
| POST | /chat/read/{userId} | 会话已读 | 登录 |
| GET | /chat/unread-count | 全部未读私信 | 登录 |
| GET | /user/{id} | 用户主页(含 relation 关系态) | 游客 |
| POST | /friend/request/{userId} | 发好友申请 | 登录 |
| POST | /friend/accept/{userId} | 通过申请 | 登录 |
| POST | /friend/reject/{userId} | 拒绝申请 | 登录 |
| POST | /friend/remove/{userId} | 删除好友 | 登录 |
| GET | /friend/list | 我的好友 | 登录 |
| GET | /friend/pending/list | 收到的申请 | 登录 |
| POST | /blacklist/add/{userId} | 拉黑(双向阻断) | 登录 |
| POST | /blacklist/remove/{userId} | 解除拉黑 | 登录 |
| GET | /blacklist/list | 我的黑名单 | 登录 |

> 说明：为支撑「帖子配图上传」业务新增了唯一的资源型接口 `POST /post/image`（非 CRUD）。

---

## 🎨 前端要点

- **页面**：首页帖子流 / 帖子详情 / 发帖（兼编辑）/ 热度排行榜 / 个人中心（头像上传）
- **组件**：`EmojiPicker`（表情选择器）、`MarkdownViewer`（marked+DOMPurify 消毒渲染）、`ImageUploader`（多图上传预览）、`PostCard`、`LoginDialog`（去登录/取消）、`AdminBadge`（管理员盾牌）
- **身份交互**：游客点击点赞/收藏/评论/发帖/上传 → 置灰样式 + 登录确认弹窗；接口返回 401（登录过期）同样弹出
- **角色渲染**：`ADMIN` 昵称旁渲染盾牌徽章；管理员登录时帖子/评论卡片出现「管理员删除」按钮
- **动效**：D3 卡片错落入场与 hover 上浮（`utils/d3fx.js`）、榜单条目动态渲染；ECharts 热度条形图
- **代理**：Vite `/api` → `http://localhost:8085`（rewrite 去前缀），axios 拦截器自动携带 `satoken` 请求头

---

## 🐳 Docker 一键部署

```bash
# 1) 本地打包(容器不做 Maven 编译)
mvn clean install -DskipTests

# 2) 编排启动(mysql/redis/minio/pro2)
docker compose up -d --build

# 3) 验证
docker compose ps
curl http://localhost:8085/demo/ping
```

访问：前端请另行构建部署或本地 `npm run dev`；后端 API 走 `http://localhost:8085`，
MinIO 控制台 `http://localhost:9001`（minioadmin/minioadmin123）。详见 [deploy-guide.md](./deploy-guide.md)。

### 🚀 正式挂网（阿里云轻量服务器）
> 生产专用文件已就绪：`deploy/docker-compose.prod.yml`（独立编排，仅 Nginx 80 对外开放）、
> `deploy/nginx.prod.conf`（网关反代）、`deploy/.env.prod.example`（强密码+图片公网地址）、
> `GO-LIVE.md`（完整上线清单：安全组/域名/备案/HTTPS/打包上传/验证/运维）。按 `GO-LIVE.md` 操作即可。

---

## 📌 常见问题

0. **从 v1 升级到 v2（已初始化过的 Docker MySQL）**：先执行增量建表脚本，再重启后端：
   ```bash
   docker compose exec -T mysql mysql -uroot -proot2297752516 community_db < sql/upgrade-v2.sql
   ```
1. **为什么有的接口返回 HTTP 200 但 code=401？** 约定业务码在响应体表达，避免游客触发浏览器 403 空白页；前端依据 code 弹「去登录」。
2. **点赞后列表计数不马上变？** 首页列表计数每 5 分钟由任务同步（详情页点赞数为 Redis 实时值）。如需即时一致，可接入消息推送或前端乐观更新。
3. **帖子 ID 为什么是字符串？** 后端将 Long 序列化为字符串，防止 JS 精度丢失（>2^53）。
4. **图片地址打不开？** 检查 `minio.public-endpoint` 是否为本机可访问地址；容器部署为 `http://localhost:9000`，局域网需换主机 IP。
5. **本地库没有表？** 手动执行 `sql/init.sql`（仅空库/测试库）或使用 Docker 中的 MySQL。
6. **docker compose 健康检查不过？** 查看 `docker compose ps` 与 `docker compose logs`；首次 MySQL 初始化需 30~60s。
