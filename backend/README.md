# 云枢智学 · 后端服务

Java 17 + Spring Boot 3 的模块化单体，按业务领域分包，模块间单向依赖（02 §2.1、§3.2）。

## 当前状态

本目录是**完整的 Spring Boot 后端**：可编译、可启动、已接通 MySQL / Redis / MinIO，覆盖学员端、管理端与 AI 能力的全部业务接口。

已完成：

- Maven 工程与依赖（Web / Validation / Security / AOP / MyBatis-Plus / Redis / JJWT / springdoc / MinIO / EasyExcel / commonmark）
- 统一响应体、分页结构、业务错误码表、全局异常处理（04 §2.3~§2.7）
- 双端独立 JWT：签发/校验、令牌域隔离、登出黑名单、RBAC 权限注解（04 §2.2、§2.8）
- MyBatis-Plus：分页、乐观锁、全表更新拦截、逻辑删除、UTC 时间自动填充
- 时间序列化：UTC 存储 ↔ Asia/Shanghai 输出（04 §2.11、D17）
- 对象存储抽象 `StorageService` + MinIO 实现（自动建桶与公开读策略）
- 学员端：首页统计、注册登录、找回密码、个人中心、题库浏览、刷题、错题本、学习路线、AI 面试（SSE 评分 + 六维报告）
- 管理端：看板、学科与阶段、B 站课程库、题库、面试题库、学习路线、学员、用户、角色、日志、系统设置
- 批量导入：Excel 两段式、课程粘贴文本、面试题 Markdown 两段式、题库 Excel 导入导出
- AI 能力层：OpenAI 兼容协议接入，简答题批改与面试报告支持本地降级
- 频控与幂等：IP + 账号双维度计数、导入确认幂等键、Redis 故障降级
- 操作日志、异步线程池、Actuator 健康检查、OpenAPI 文档

测试：`mvn test`，当前 28 个用例全部通过。

## 目录结构

```
backend/
├── pom.xml
└── src/
    ├── main/java/com/yunshu/zhixue/
    │   ├── YunshuZhixueApplication.java
    │   ├── common/          response / exception / enums / constant / persistence
    │   ├── config/          MyBatis-Plus / Jackson / OpenAPI / 异步线程池
    │   ├── security/        双端 JWT、令牌黑名单、权限注解与切面
    │   ├── support/storage/ 对象存储抽象与 MinIO 实现
    │   └── modules/         auth · system · student · subject · course · question
    │                        interviewbank · route · study · interview · stats · ai
    ├── main/resources/
    │   ├── application.yml / application-dev.yml / application-prod.yml
    │   ├── prompts/         AI 提示词模板（待补充）
    │   ├── mapper/          复杂统计 SQL
    │   └── db/migration/    增量迁移脚本
    └── test/java/           单元测试
```

模块内标准结构：`controller/portal`、`controller/admin`、`service`、`mapper`、`entity`、
`dto`、`convert`（02 §4.3）。新增代码必须落到 `modules/<领域>`。

## 运行前提

1. 基础服务已启动：在 `../deploy` 执行 `docker compose up -d`（MySQL 3306 / Redis 6379 / MinIO 9000）。
2. Java 17。Maven 可选（未安装时用 Docker 镜像构建）。

## 本地启动

```powershell
# 方式一：本机 Maven
mvn spring-boot:run

# 方式二：Docker Maven（无需本机安装 Maven）
docker run --rm -it -p 8080:8080 -v "${PWD}:/app" -w /app `
  maven:3.9-eclipse-temurin-17 mvn spring-boot:run
```

启动后：

| 地址 | 说明 |
|---|---|
| `http://localhost:8080/actuator/health` | 健康检查（数据库/Redis 状态） |
| `http://localhost:8080/swagger-ui.html` | 接口文档（仅 dev 开放） |

默认连接参数与 `../deploy/docker-compose.yml` 一致（库 `yszx`，账号 `yszx/yszx_2026`）。

## 打包

```powershell
mvn clean package -DskipTests
java -jar target/yunshu-zhixue-backend-0.1.0-SNAPSHOT.jar --spring.profiles.active=prod
```

## 关键配置（环境变量）

| 变量 | 默认值 | 说明 |
|---|---|---|
| `MYSQL_HOST` / `MYSQL_PORT` / `MYSQL_DATABASE` | `127.0.0.1` / `3306` / `yszx` | 数据库 |
| `MYSQL_USER` / `MYSQL_PASSWORD` | `yszx` / `yszx_2026` | 应用账号 |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | `127.0.0.1` / `6379` / `yszx_redis_2026` | 缓存 |
| `JWT_PORTAL_SECRET` / `JWT_ADMIN_SECRET` | 开发默认值 | **上线必须替换**，≥32 字节 |
| `MINIO_ENDPOINT` / `MINIO_ACCESS_KEY` / `MINIO_SECRET_KEY` / `MINIO_BUCKET` | `127.0.0.1:9000` / `yszxadmin` / `yszx_minio_2026` / `yszx-media` | 对象存储 |
| `APP_DEFAULT_PASSWORD` | `Yszx@123456` | 后台重置密码的默认值（D4-10 Q9） |
| `PREVIEW_PORTAL_LOGIN_ENABLED` | 生产 `false` / dev `true` | 后台「学员端（内测账号）」免登录通道开关 |
| `PREVIEW_STUDENT_MOBILE` | `13800138000` | 上述通道使用的内测学员手机号 |
| `MIMO_API_KEY` | 空 | 简答题 AI 批改的模型 Key，仅从环境变量读取 |
| `MIMO_BASE_URL` / `MIMO_MODEL` | `https://api.xiaomimimo.com/v1` / `mimo-v2.6-pro` | OpenAI 兼容模型地址与模型名 |
| `MIMO_TIMEOUT_SECONDS` | `60` | AI 请求读超时；超时后降级为要点匹配 |

## 开发约定

- 业务异常只抛 `BizException(ErrorCode)`，由 `GlobalExceptionHandler` 统一映射，不手动拼 HTTP 响应。
- 管理端接口加 `@RequirePermission("模块:动作")`，权限码与 `permission` 表一致（9 组 35 项）。
- 当前用户通过 `SecurityUtils.currentUserId()` 获取，禁止直接读 SecurityContext。
- 跨模块取数经对方 Service + DTO，禁止引用对方 Mapper/实体（02 §3.2）。
- 时间统一 UTC：实体字段用 `LocalDateTime`，数据库默认值 UTC；接口进出由 `JacksonConfig` 转换。
- 逻辑删除统一继承 `BaseEntity`（`operation_log`、`role_permission` 例外）。
