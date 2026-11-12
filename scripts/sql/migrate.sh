#!/bin/sh
# ============================================================
# 云枢智学 · 数据库迁移执行器
#
# 用法：
#   开发环境（宿主机，通过 docker exec 执行）：
#     bash scripts/sql/migrate.sh
#
#   生产容器内（由 docker-compose.prod.yml 的 migrate 服务调用）：
#     MYSQL_HOST=mysql MYSQL_DATABASE=yszx MYSQL_USER=root \
#     MYSQL_PASSWORD=... MIGRATIONS_DIR=/migrations sh /migrate.sh
#
# 规则：
#   1. schema.sql 仅用于首次初始化，不重复执行；
#   2. migrations/V<版本>__<说明>.sql 按文件名排序执行，执行成功的版本记入
#      schema_migration 表，后续运行自动跳过；
#   3. 每个迁移文件应自身保证可重复执行（幂等）；
#   4. 任一迁移失败立即退出，不写入版本记录。
# ============================================================
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
MIGRATIONS_DIR=${MIGRATIONS_DIR:-"$SCRIPT_DIR/migrations"}
MYSQL_DATABASE=${MYSQL_DATABASE:-yszx}
MYSQL_USER=${MYSQL_USER:-root}
MYSQL_PASSWORD=${MYSQL_PASSWORD:-${MYSQL_ROOT_PASSWORD:-yszx_root_2026}}
MYSQL_PORT=${MYSQL_PORT:-3306}
MYSQL_CONTAINER=${MYSQL_CONTAINER:-yszx-mysql}

if [ -n "${MYSQL_HOST:-}" ]; then
    MODE="direct"
    export MYSQL_PWD="$MYSQL_PASSWORD"
    mysql_run() {
        mysql --default-character-set=utf8mb4 --protocol=tcp \
            -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$MYSQL_USER" "$@"
    }
else
    MODE="docker"
    if ! docker inspect "$MYSQL_CONTAINER" >/dev/null 2>&1; then
        echo "错误：找不到容器 $MYSQL_CONTAINER。请先启动 MySQL，或设置 MYSQL_HOST 直连。" >&2
        exit 1
    fi
    mysql_run() {
        docker exec -i -e MYSQL_PWD="$MYSQL_PASSWORD" "$MYSQL_CONTAINER" \
            mysql --default-character-set=utf8mb4 -u "$MYSQL_USER" "$@"
    }
fi

echo "迁移模式：$MODE；数据库：$MYSQL_DATABASE；目录：$MIGRATIONS_DIR"

mysql_run "$MYSQL_DATABASE" -e "
CREATE TABLE IF NOT EXISTS \`schema_migration\` (
  \`version\` VARCHAR(64) NOT NULL COMMENT '迁移版本',
  \`description\` VARCHAR(255) NOT NULL COMMENT '迁移说明',
  \`applied_at\` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '执行时间（UTC）',
  PRIMARY KEY (\`version\`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='数据库迁移记录';
INSERT IGNORE INTO \`schema_migration\` (\`version\`,\`description\`)
VALUES ('V20261112_01','baseline: scripts/sql/schema.sql');
"

applied_count=0
skipped_count=0

for file in "$MIGRATIONS_DIR"/*.sql; do
    [ -e "$file" ] || continue
    base=$(basename "$file")
    version=${base%%__*}
    description=${base#*__}
    description=${description%.sql}
    escaped_description=$(printf '%s' "$description" | sed "s/'/''/g")

    if [ "$version" = "$base" ]; then
        echo "跳过无法识别版本的文件：$base（应命名为 V<版本>__<说明>.sql）" >&2
        continue
    fi

    applied=$(mysql_run "$MYSQL_DATABASE" -N -B \
        -e "SELECT COUNT(*) FROM \`schema_migration\` WHERE \`version\` = '$version';" \
        | tr -d '[:space:]')

    case "$applied" in
        0)
            ;;
        ''|*[!0-9]*)
            echo "错误：无法读取迁移状态（$base）：$applied" >&2
            exit 1
            ;;
        *)
            echo "跳过 $base（已执行）"
            skipped_count=$((skipped_count + 1))
            continue
            ;;
    esac

    echo "执行 $base"
    mysql_run "$MYSQL_DATABASE" < "$file"
    mysql_run "$MYSQL_DATABASE" -e "
INSERT INTO \`schema_migration\` (\`version\`,\`description\`)
VALUES ('$version', '$escaped_description');"
    applied_count=$((applied_count + 1))
done

echo "迁移完成：执行 $applied_count 个，跳过 $skipped_count 个。"
