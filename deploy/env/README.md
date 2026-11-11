# 环境变量说明

`deploy/docker-compose.yml` 的所有可配置项均在此定义。**未配置时使用 compose 文件内的默认值**，因此不复制 `.env` 也能直接启动（仅建议在本地开发这样用）。

## 使用方式

```powershell
# 在 deploy/ 目录下
copy env\.env.example .env
docker compose up -d
```

> compose 的变量替换只读取 **compose 文件同目录** 的 `.env`，因此必须放在 `deploy/.env`；`env/.env.example` 只是模板。

## 变量清单

### MySQL

| 变量 | 默认值 | 说明 |
|---|---|---|
| `MYSQL_IMAGE` | `mysql:8.0` | 镜像 |
| `MYSQL_ROOT_PASSWORD` | `yszx_root_2026` | root 密码（仅运维使用） |
| `MYSQL_DATABASE` | `yszx` | 业务库名（与 03 文档一致） |
| `MYSQL_USER` | `yszx` | 应用账号（后端连接用） |
| `MYSQL_PASSWORD` | `yszx_2026` | 应用账号密码 |
| `MYSQL_PORT` | `3306` | 宿主机映射端口 |

### Redis

| 变量 | 默认值 | 说明 |
|---|---|---|
| `REDIS_IMAGE` | `redis:7-alpine` | 镜像 |
| `REDIS_PASSWORD` | `yszx_redis_2026` | 访问密码（AOF 已开启） |
| `REDIS_PORT` | `6379` | 宿主机映射端口 |

### MinIO

| 变量 | 默认值 | 说明 |
|---|---|---|
| `MINIO_IMAGE` | `pgsty/minio:latest` | 镜像（见下方说明） |
| `MINIO_ROOT_USER` | `yszxadmin` | 根账号，长度 ≥ 3 |
| `MINIO_ROOT_PASSWORD` | `yszx_minio_2026` | 根密码，长度 ≥ 8 |
| `MINIO_API_PORT` | `9000` | S3 API 端口 |
| `MINIO_CONSOLE_PORT` | `9001` | 控制台端口 |

## 关键约定

- **时区**：三个容器均为 UTC，MySQL `default-time-zone=+00:00`。与 03 文档「DATETIME 统一 UTC 存储、展示层转 Asia/Shanghai」及 D17 一致，**不要改成 Asia/Shanghai**，否则 `CURRENT_TIMESTAMP` 默认值会写入非 UTC 时间。
- **建表初始化**：`../scripts/sql/schema.sql` 挂载到 MySQL 初始化目录，**仅在数据卷为空时执行一次**（23 表 + 8 学科 + 3 角色 + 35 权限 + 系统设置 + 内置管理员占位）。改了脚本要重新初始化时，执行 `docker compose down -v` 后重启。
- **内置管理员**：种子里的 `admin` 密码是占位哈希，**部署时必须重置**，不能直接登录。
- **数据持久化**：使用命名卷 `yszx-mysql-data` / `yszx-redis-data` / `yszx-minio-data`，`docker compose down` 不会丢数据。

## 关于 MinIO 镜像

官方 `minio/minio` 镜像已不再公开发布（Docker Hub / Quay 均取不到），因此默认改用社区维护的 **`pgsty/minio`**——仍是 MinIO 本体，S3 API 与控制台行为与设计文档一致（D8「S3 兼容协议，后续可切云 OSS/COS」不受影响）。

如需替换为其他 S3 兼容实现，只改 `MINIO_IMAGE` 一行：

| 备选 | 镜像 | 说明 |
|---|---|---|
| RustFS | `rustfs/rustfs:latest` | 活跃维护的 S3 兼容实现，控制台与 mc 用法略有差异 |
| Bitnami（旧版） | `bitnamilegacy/minio:latest` | Bitnami 打包的 MinIO，已停止更新 |

## 后端连接参数（实施阶段用）

| 服务 | 容器内地址 | 宿主机地址 |
|---|---|---|
| MySQL | `mysql:3306` | `127.0.0.1:3306` |
| Redis | `redis:6379` | `127.0.0.1:6379` |
| MinIO | `http://minio:9000` | `http://127.0.0.1:9000` |

后端以 jar 形式跑在宿主机时用 `127.0.0.1`；接入 compose 网络时用服务名（容器内 DNS）。
