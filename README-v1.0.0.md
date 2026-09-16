~~~markdown
# PulseTalk
PulseTalk 是一款基于 SpringBoot + Vue3 开发的社区即时通讯项目，支持帖子发布、评论、私聊聊天，适合作为全栈学习实战项目。

> 当前版本：v1.0.0
> 项目状态：初代可用版本

## ✨ 功能清单（v1.0.0）
- 用户注册、登录
- 用户头像上传
- 社区帖子发布、浏览、评论
- 帖子点赞
- 实时私聊消息
- MinIO 文件对象存储
- Redis 缓存会话信息
- MySQL 存储业务数据

## 🧰 技术栈
### 后端
- SpringBoot 3.x
- MyBatis-Plus
- MySQL 8.0
- Redis
- MinIO 对象存储
- Sa-Token 权限认证

### 前端
- Vue3
- Vite
- Axios
- Element Plus

## 📦 部署方式（Docker Compose）
本项目使用 docker compose 一键启动全套服务：MySQL / Redis / MinIO / 后端服务 / Nginx

前置条件：服务器已安装 Docker & Docker Compose

```bash
# 拉取代码
git clone https://github.com/你的用户名/pulsetalk.git
cd pulsetalk

# 修改环境配置文件 deploy/.env.prod
# 配置数据库密码、minio密钥等

# 启动全部服务
docker compose -f deploy/docker-compose.prod.yml up -d
~~~

## 📌 项目说明

v1.0.0 为项目第一个正式版本，基础通讯与社区功能可用。

> ⚠️ 版本限制：v1.0.0 缺少密保找回、管理员用户管理模块；数据库表结构无 security_question、update_time 等字段。
>
> 如需完整功能请升级到 v1.0.1。

## 📝 License

MIT License，仅供学习交流使用。



```markdown
## 使用方法
1. 执行 `git checkout c7eedb6` 切到v1.0.0提交
2. 把仓库里旧的 README.md 全部替换成上面这段内容，保存
3. 然后继续执行前面的git add、commit、重建tag、强推tag整套命令

如果你想，我还可以精简一版，或者加一个快速启动故障排查小节放到这个v1.0.0文档里。
```

