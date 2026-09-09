# 部署指南（deploy-guide.md）

> PulseTalk 社区（原名点评贴吧 pro2）· 单模块后端 + Vue3 前端
> 目标环境：一台安装了 Docker（含 Compose 插件）的 Linux/Windows 主机；后端以容器方式运行。

> **v1 → v2 升级**（曾用 v1 镜像/数据卷初始化过的库，需增量建表后重启）：
> ```bash
> docker compose exec -T mysql mysql -uroot -proot2297752516 community_db < sql/upgrade-v2.sql
> docker compose up -d --build pro2
> ```

---

## 0. 环境要求

| 软件 | 版本 | 用途 |
| --- | --- | --- |
| JDK | 17 LTS | 本机构建（打 jar） |
| Maven | 3.6+ | 本机构建 |
| Docker | 20.10+ / Compose v2 | 编排 mysql/redis/minio/pro2 |
| Node | 18+ | （可选）构建前端 |

> 容器内**不执行 Maven 编译**：先在本机执行 `mvn clean install -DskipTests` 产出 `target/pro2-1.0.0.jar`，
> Dockerfile 仅负责 COPY jar 并用 `eclipse-temurin:17-jre-alpine` 运行。

---

## 1. 本机构建 jar

在项目根目录执行（本机需能访问 Maven 中央仓库以下载依赖）：

```bash
mvn clean install -DskipTests
# 产物: target/pro2-1.0.0.jar
```

---

## 2. 一键编排启动

```bash
docker compose up -d --build
```

编排内容（`docker-compose.yml`）：

| 服务 | 镜像 | 端口(宿主机) | 说明 |
| --- | --- | --- | --- |
| mysql | mysql:8.0 | 13306(容器3306) | root/root2297752516，库 community_db，`./sql` 挂载自动初始化，卷持久化 |
| redis | redis:7-alpine | 16379(容器6379) | AOF 持久化 |
| minio | minio/minio:latest | 9000(S3 API) / 9001(控制台) | minioadmin/minioadmin123 |
| pro2 | 本工程 Dockerfile | 8085 | SPRING_PROFILES_ACTIVE=docker |

设计约束（均已落实）：

- ✅ 全部服务 `restart: always` + 命名数据卷持久化
- ✅ 每个服务配置 healthcheck；`depends_on: condition: service_healthy`
- ✅ 容器间使用服务名互访：`mysql` / `redis` / `minio`，无 localhost
- ✅ 后端仅复制运行 jar，容器内不编译
- ✅ sql 初始化脚本含 `DROP TABLE IF EXISTS`，预置 admin 账号

查看状态：

```bash
docker compose ps                # 期望 4 个服务均 healthy
docker compose logs -f pro2      # 后端日志
```

---

## 3. 部署验证

```bash
# 1) 存活探测
curl http://localhost:8085/demo/ping
# => {"code":200,...,"data":{"pong":"ok",...}}

# 2) 登录管理员获取 token
curl -X POST http://localhost:8085/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'

# 3) 携带 token 访问当前用户(应返回 role=ADMIN)
curl http://localhost:8085/auth/current-user -H "satoken: <上一步返回的tokenValue>"
```

MinIO 控制台：`http://<主机IP>:9001`（minioadmin / minioadmin123），可查看 `community-upload` 桶。

---

## 4. 前端部署（二选一）

**方案 A：本地开发（推荐联调）**

```bash
cd pro2-frontend
npm install
npm run dev        # http://localhost:5173，/api 代理到 http://localhost:8085
```

**方案 B：构建静态资源 + Nginx 反代**（生产）

```bash
cd pro2-frontend
npm install
npm run build      # 产物 dist/
```

Nginx 示例（`/api` 转发后端、其余走前端静态）：

```nginx
server {
    listen 80;
    server_name your-domain.com;

    root /opt/pro2-frontend/dist;
    index index.html;

    location /api/ {
        proxy_pass http://127.0.0.1:8085/;      # 去掉 /api 前缀转发后端
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    location / {
        try_files $uri $uri/ /index.html;        # history 路由回退
    }
}
```

> 若后端图片地址需经域名访问，将 `application-docker.yml` 中 `minio.public-endpoint` 改为
> `http://your-domain.com:9000`（并放通 9000 端口），前端即可以该地址加载图片。

---

## 5. 升级与重新部署

```bash
mvn clean install -DskipTests       # 重新打包
docker compose up -d --build pro2   # 仅重建后端应用
docker compose ps
```

## 6. 数据卷管理

| 卷 | 挂载点 | 内容 |
| --- | --- | --- |
| mysql-data | /var/lib/mysql | 业务表数据 |
| redis-data | /data | Redis AOF |
| minio-data | /data | 头像/配图对象 |

- 备份：`docker run --rm -v pro2-community_mysql-data:/data -v $PWD:/backup alpine tar czf /backup/mysql.tgz -C /data .`
- 完全重置（删除全部数据并重新初始化，慎用）：
  ```bash
  docker compose down -v
  docker compose up -d --build
  ```

---

## 7. 故障排查

| 现象 | 排查 |
| --- | --- |
| `pro2` 一直 unhealthy | `docker compose logs pro2`；确认 mysql/redis/minio 已 healthy（`depends_on` 健康门槛） |
| MySQL 初始化很慢/失败 | 首次建卷需 30~60s；看 `docker compose logs mysql`；确认 `sql/init.sql` 语法在当前 MySQL 版本无兼容问题 |
| MinIO 健康检查失败 | 检查镜像是否内置 `curl`（个别精简 tag 没有，可按注释换成 `mc ready local` 或安装 curl） |
| 容器内连不上 mysql/redis | 确认使用服务名而非 localhost；`docker compose exec pro2 sh` 里 `wget mysql:3306` 探活 |
| 端口占用 | 默认仅暴露 8085/9000/9001/13306/16379；与本机服务冲突时修改 `docker-compose.yml` 映射 |
| 图片加载 403/打不开 | 检查桶 `community-upload` 公共读策略与 `public-endpoint` 是否可从浏览器访问 |
| 点赞计数长时间不落库 | Redis 键是否被手动删除；查看 `PostCountSyncTask` 日志（每 5 分钟一轮） |

---

## 8. 生产化建议

- 修改默认口令：`docker-compose.yml`(mysql/minio) + `application-docker.yml` + `sql/init.sql`
- 为 MySQL/Redis/MinIO 开启访问密码与网络隔离；如无公网需求勿暴露 9001/13306/16379
- Redis 开启 AOF/RDB 持久化策略并配置 `maxmemory`；MyBatis-Plus 关闭 SQL 日志
- 密码散列升级为 BCrypt（替换 `PasswordUtil` 与初始化脚本中的散列即可）
- 多人并发写入场景可引入：热度分定时重算（全量 ZREBUILD）、限流升级滑动窗口/Lua 原子脚本、榜单结果缓存
