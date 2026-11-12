# 生产部署（Docker Compose）

一套 Compose 启动完整生产栈：MySQL、Redis、MinIO、Spring Boot 后端、Nginx（学员端 + 管理端静态站）。

## 服务与端口

| 服务 | 容器 | 对外端口 | 说明 |
|---|---|---|---|
| web | yszx-prod-web | `${WEB_PORT}`（默认 8088） | 学员端 `/`、管理端 `/admin/`、API `/api/`、文件 `/files/` |
| backend | yszx-prod-backend | 仅内网 8080 | Spring Boot |
| migrate | yszx-prod-migrate | 无 | 一次性迁移任务，补齐 Java 方向/题库并执行后续结构变更 |
| mysql | yszx-prod-mysql | 仅内网 3306 | 首次启动自动执行 `scripts/sql/schema.sql` |
| redis | yszx-prod-redis | 仅内网 6379 | AOF 持久化 |
| minio | yszx-prod-minio | 仅内网 9000 | 公开桶策略由后端首次写入时设置 |

## 快速开始

1. 准备环境：Docker 24+、Docker Compose v2；
2. 复制并填写密钥：

```bash
cp deploy/env/.env.prod.example deploy/.env
```

3. 构建并启动：

```bash
docker compose -f deploy/docker-compose.prod.yml up -d --build
```

4. 查看状态与日志：

```bash
docker compose -f deploy/docker-compose.prod.yml ps
docker compose -f deploy/docker-compose.prod.yml logs -f backend
```

5. 访问：

- 学员端：`http://<host>:8088/`
- 管理端：`http://<host>:8088/admin/`
- 首次启动内置管理员：`admin` / `${APP_DEFAULT_PASSWORD}`（登录后请立即修改）

## 关键配置

- `PREVIEW_PORTAL_LOGIN_ENABLED` 固定为 `false`，生产禁用「管理端 → 学员端内测账号」通道；
- Swagger（`/v3/api-docs`、`/swagger-ui`）通过环境变量关闭；
- `MINIO_PUBLIC_URL=/files`：后端返回相对地址，由 Nginx 反代到 MinIO；
- 后端使用 `prod` profile（无额外配置文件，沿用 `application.yml` 默认值）；
- MySQL / Redis / MinIO 只加入内部网络，不映射到宿主机；
- `migrate` 在 MySQL 健康后执行 `scripts/sql/migrations`，执行成功的版本记录在
  `schema_migration` 表；backend 等待迁移成功后再启动；
- 首次启动会自动导入 Java 后端开发方向（744 题）与 AI 方向种子题（16 题），
  无需再从开发库手工导出。

## HTTPS

提供两种方式，按基础设施选择一种即可。

### 方式一：云负载均衡 / SLB 终止 TLS

继续使用默认 `deploy/nginx.conf`（容器内监听 80），由外层把 HTTPS 请求转发到
`WEB_PORT`（默认 8088）。无需证书挂载。

### 方式二：Nginx 直接终止 TLS

1. 准备证书：
   - `deploy/certs/fullchain.pem`：服务器证书 + 中间证书链；
   - `deploy/certs/privkey.pem`：私钥。
   - 证书目录可用 `TLS_CERT_DIR` 覆盖，详见 `deploy/certs/README.md`。

2. 叠加 HTTPS 覆盖配置启动：

```bash
docker compose -f deploy/docker-compose.prod.yml \
  -f deploy/docker-compose.https.yml up -d --build
```

3. 访问 `https://<host>/`。容器内 80 端口只做 301 跳转，443 由
   `deploy/nginx.https.conf` 承担，已包含 HSTS、TLS 1.2/1.3 与 SSE 关闭缓冲。

> 证书文件不要提交到版本库；`.gitignore` 已忽略 `deploy/certs/*`，仅保留说明文件。

## 数据与备份

命名卷：`yszx-prod_mysql-data`、`yszx-prod_redis-data`、`yszx-prod_minio-data`。

```bash
# MySQL 备份
docker exec yszx-prod-mysql sh -c 'mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --single-transaction yszx' > yszx-$(date +%F).sql

# MinIO 数据
docker run --rm -v yszx-prod_minio-data:/data -v "$PWD":/backup alpine tar czf /backup/minio-$(date +%F).tar.gz -C /data .
```

> `schema.sql` 只在 MySQL 数据卷首次初始化时执行，仅包含表结构与 8 个 AI 方向。
> 后续数据与表结构变更统一放在 `scripts/sql/migrations/`，由 `migrate` 服务按版本执行，
> 执行记录写入 `schema_migration` 表，不能依赖重启 MySQL。
>
> 当前迁移已包含「Java后端开发」方向、744 道面试题、13 份文档主题、AI 方向 16 道种子题，
> 以及空答案/拆标题修复。全新环境启动后即可使用完整题库；已有环境重复执行会自动跳过。

## 升级

```bash
docker compose -f deploy/docker-compose.prod.yml up -d --build
```

升级时如包含新的迁移文件，建议显式执行一次迁移，再启动/重建服务：

```bash
docker compose -f deploy/docker-compose.prod.yml run --rm migrate
docker compose -f deploy/docker-compose.prod.yml up -d --build
```

`migrate` 会跳过已执行版本，只应用新增文件。手工查看迁移状态：

```bash
docker exec yszx-prod-mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" \
  -e "USE yszx; SELECT version, description, applied_at FROM schema_migration ORDER BY version;"
```

## 常见问题

- **backend 一直不健康**：查看 `logs backend`，多为 MySQL/Redis 未就绪或密钥错误；healthcheck 使用 TCP 8080 探测。
- **图片 404**：确认 `MINIO_PUBLIC_URL=/files`，且 Nginx 的 `/files/` 反代指向 `minio:9000`。
- **管理端刷新 404**：确认使用 `/admin/` 访问，Nginx 已配置 SPA 回退。
- **AI 批改不可用**：`MIMO_API_KEY` 未配置时会降级为本地要点匹配，不阻断流程。
- **端口冲突**：修改 `deploy/.env` 的 `WEB_PORT`。

## 上线安全清单

- [ ] 替换 `JWT_PORTAL_SECRET` / `JWT_ADMIN_SECRET`（≥32 字节，两端独立）
- [ ] 修改 `APP_DEFAULT_PASSWORD` 并登录后立即改密
- [ ] 修改 MySQL / Redis / MinIO 全部默认密码
- [ ] 关闭 Swagger（本编排已通过环境变量关闭）
- [ ] 确认 `PREVIEW_PORTAL_LOGIN_ENABLED=false`
- [ ] 配置 HTTPS（叠加 `docker-compose.https.yml` 或外层 SLB）与安全组，仅开放 Web 端口
- [ ] 配置数据库与对象存储定期备份
