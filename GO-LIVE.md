# PulseTalk 上线挂网调整清单（阿里云轻量服务器）

> 目标架构：**唯一对外入口 = Nginx(80/443)**；MySQL/Redis/MinIO/后端全部只在容器内网互访。
> 前端静态页面 + `/api` 反代 + `/files`(MinIO 图片) 都由 Nginx 一层接管，公网不用开放 8085/9000/9001。
> 下述「✅ 已就绪」的文件改动我已完成；「🛠 需你在部署时操作」项必须按步骤执行。

---

## 0. 交付文件（已生成，随项目上传）
| 文件 | 作用 |
| --- | --- |
| `src/main/resources/application-docker.yml` | 已参数化：数据库/Redis/MinIO/图片公网地址全部支持环境变量注入，生产不再打印 SQL |
| `deploy/nginx.prod.conf` | 生产网关：静态页面 + `/api` → pro2:8085 + `/files` → minio:9000 |
| `deploy/docker-compose.prod.yml` | **独立**生产编排：仅 web(80) 对外开放，其余服务零端口暴露 |
| `deploy/.env.prod.example` | 生产环境变量样例（强密码 + 图片公网地址） |
| `deploy/www/` | 放置前端构建产物 `pro2-frontend/dist/*` |

---

## 1. 需要你调整的地方（部署时逐项执行）

### 1.1 修改生产密码（🛠 必做）
复制样例并填入**强密码**（不要用默认值）：
```bash
cp deploy/.env.prod.example deploy/.env.prod
vi deploy/.env.prod
```
必改 4 项：`MYSQL_ROOT_PASSWORD`、`MINIO_ACCESS_KEY`、`MINIO_SECRET_KEY`、`MINIO_PUBLIC_ENDPOINT`。

### 1.2 公网图片地址 `MINIO_PUBLIC_ENDPOINT`（🛠 必改，最容易踩坑）
浏览器加载头像/配图走的是该地址，**不能是内网 localhost**：
```
# 有域名(推荐)：https://talk.example.com/files
# 只有公网IP： http://47.xx.xx.xx/files
```
它对应 Nginx 的 `location /files/`，会把 `/files/community-upload/...` 反代到 MinIO，全程只走 80/443。

### 1.3 阿里云轻量服务器防火墙（🛠 必开）
控制台 → 防火墙 → 添加规则：

| 端口 | 协议 | 是否必开 | 说明 |
| --- | --- | --- | --- |
| 80 | TCP | ✅ 必开 | 站点入口(HTTP) |
| 443 | TCP | 开启 HTTPS 时必开 | HTTPS |
| 22 | TCP | ✅ 必开 | SSH 部署 |
| 8085 / 9000 / 9001 / 13306 / 16379 | TCP | ❌ 不要开 | 全走内网/反代 |

### 1.4 域名解析 + 备案（可选但推荐）
- 阿里云 DNS 加 A 记录指向服务器公网 IP。
- 国内服务器 80/443 需完成 **ICP 备案** 后才可绑定域名访问；未备案先用 `http://公网IP`。
- 免费 HTTPS：数字证书管理服务 → 免费证书 → nginx 版 → 参考 `deploy/nginx.prod.conf` 文末启用（同时把 `MINIO_PUBLIC_ENDPOINT` 改为 `https://域名/files`）。

### 1.5 默认账号口令（🛠 上线后立刻改）
admin/123456、test/123456 是种子账号，请尽快改掉/删除 test 号。把自建账号提权为管理员：
```bash
docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env.prod \
    exec -T mysql mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" community_db \
    -e "UPDATE sys_user SET role='ADMIN' WHERE username='你的新管理员';"
```

---

## 2. 打包与上传（本地 Windows 执行）

```bash
# 1) 打后端 jar
mvn clean install -DskipTests

# 2) 构建前端
cd pro2-frontend && npm install && npm run build && cd ..

# 3) 组装静态目录 deploy/www
rmdir /s /q deploy\www
mkdir deploy\www
xcopy pro2-frontend\dist\* deploy\www\ /e /i

# 4) 上传到服务器(以 scp 为例；不需要 node_modules/.idea/target 除 jar 外的内容)
scp -r .\Dockerfile .\docker-compose.yml .\GO-LIVE.md root@47.xx.xx.xx:/opt/pulsetalk/
scp -r .\sql .\deploy root@47.xx.xx.xx:/opt/pulsetalk/
scp -r .\src .\pom.xml root@47.xx.xx.xx:/opt/pulsetalk/
scp .\target\pro2-1.0.0.jar root@47.xx.xx.xx:/opt/pulsetalk/target/
```
> 服务器项目目录结构示例：`/opt/pulsetalk/{Dockerfile, docker-compose.yml, pom.xml, sql/, src/, target/pro2-1.0.0.jar, deploy/{docker-compose.prod.yml, nginx.prod.conf, .env.prod, www/}}`。
> 后端 jar 的 Dockerfile 在项目根，构建上下文在 prod 编排里指向 `..`（即 /opt/pulsetalk）。
> `sql/init.sql` 已含 v2 全部 9 张表，MySQL 首次启动自动执行。

---

## 3. 服务器上启动（生产模式）

```bash
cd /opt/pulsetalk
# 首次：准备 env
cp deploy/.env.prod.example deploy/.env.prod && vi deploy/.env.prod

# 启动(独立生产编排，仅 web 暴露 80)
docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env.prod up -d --build

# 健康状态(期望 web/pro2/mysql/redis/minio 全部 healthy)
docker compose -f deploy/docker-compose.prod.yml ps
```

## 4. 上线验证

```bash
# 经 Nginx 探测后端(注意走 /api 前缀)
curl http://127.0.0.1/api/demo/ping
curl http://公网IP/api/demo/ping

# 登录(浏览器同源自动携带 satoken，无跨域问题)
curl -X POST http://公网IP/api/auth/login \
  -H "Content-Type: application/json" -d '{"username":"admin","password":"123456"}'

# 页面
浏览器打开 http://公网IP/ → 首页/发帖/消息/私聊/好友/官方消息逐项验证
```
图片验证：个人中心换头像 → 图片 URL 应为 `http://公网IP/files/community-upload/...` 且可直接打开。

## 5. 运维速查
```bash
P="docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env.prod"
# 日志
docker compose -f deploy/docker-compose.prod.yml logs -f pro2 web minio
# 重启后端
docker compose -f deploy/docker-compose.prod.yml up -d pro2
# 备份数据库(数据卷)
docker run --rm -v pulsetalk_mysql-data:/data -v /opt/pulsetalk/backup:/backup \
    alpine tar czf /backup/mysql-$(date +%F).tgz -C /data .
# 完全重置(慎用，会删除数据)
docker compose -f deploy/docker-compose.prod.yml down -v
```
常见问题：
- **图片打不开**：99% 是 `MINIO_PUBLIC_ENDPOINT` 仍为 localhost/9000 → 改 `.env.prod` 后 `docker compose -f deploy/docker-compose.prod.yml up -d pro2` 重启。
- **页面能开但接口 404**：请从 `http://IP/` 访问；接口统一走 `/api`，不要直连 8085。
- **首次 MySQL 初始化慢**：等 `mysql` 变 healthy 再访问（pro2/web 均依赖其健康）。
- **服务器内存偏小(1G)**：可在 mysql `command` 追加 `--innodb-buffer-pool-size=128M`，并给 pro2 容器限 `mem_limit: 768m`。
