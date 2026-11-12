# 数据库脚本

| 文件/目录 | 用途 | 执行时机 |
|---|---|---|
| `schema.sql` | 建表 + 8 个 AI 方向、角色、权限、系统设置等基础种子 | **仅** MySQL 数据卷首次初始化 |
| `migrations/` | 数据与表结构增量变更 | 每次部署/升级，由迁移器按版本执行 |
| `migrate.sh` | 迁移执行器，维护 `schema_migration` 表 | 手工或 Compose `migrate` 服务调用 |
| `dev-seed-student.sql` | 开发环境学员测试数据 | 仅开发环境手工执行 |

## 迁移文件命名

```
V<版本>__<说明>.sql
```

示例：

```
V20261112_02__seed_production_interview_bank.sql
V20261112_03__fix_interview_answers.sql
V20261201_01__add_course_source_index.sql
```

- 版本按文件名字典序执行，建议使用 `YYYYMMDD_NN`；
- `V20261112_01` 预留给 `schema.sql` 基线，迁移器会自动写入该版本记录；
- 每个迁移文件都应尽量幂等，重复执行不产生重复数据；
- 文件使用 UTF-8（无 BOM），换行使用 LF。

## 开发环境执行

```bash
# 默认通过 docker exec 连接 yszx-mysql
bash scripts/sql/migrate.sh

# 指定其他容器 / 数据库
MYSQL_CONTAINER=yszx-mysql MYSQL_DATABASE=yszx bash scripts/sql/migrate.sh

# 直连（容器外有 mysql 客户端时）
MYSQL_HOST=127.0.0.1 MYSQL_PORT=3306 MYSQL_USER=root \
MYSQL_PASSWORD=yszx_root_2026 bash scripts/sql/migrate.sh
```

## 生产环境执行

`deploy/docker-compose.prod.yml` 中的 `migrate` 服务会在 MySQL 健康后自动执行；
backend 使用 `service_completed_successfully` 等待其成功。

手工执行：

```bash
docker compose -f deploy/docker-compose.prod.yml run --rm migrate
```

查看已执行版本：

```sql
SELECT version, description, applied_at FROM schema_migration ORDER BY version;
```

## 新增结构变更的流程

1. 新建 `migrations/V<版本>__<说明>.sql`；
2. 在开发库执行 `bash scripts/sql/migrate.sh`；
3. 验证迁移结果与幂等性（至少执行两次）；
4. 同步更新 `docs/design/03.数据库设计.md` 与本次交付说明；
5. 提交迁移文件，部署时自动执行。

> 禁止直接修改已执行过的迁移文件。需要修正时新增一个版本，避免不同环境记录不一致。
