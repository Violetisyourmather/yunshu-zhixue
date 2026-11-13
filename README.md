# 🎓 云枢智学 · AI 智能面试训练平台 (YunShu ZhiXue)

![Java](https://img.shields.io/badge/Java-17-blue.svg)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-green.svg)
![Vue](https://img.shields.io/badge/Vue.js-3.5-4FC08D.svg)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1.svg)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED.svg)
![License](https://img.shields.io/badge/License-Private-lightgrey.svg)

`云枢智学` 是一个面向 **AI 应用** 与 **Java 后端** 方向的一站式智能面试训练平台。它把「学、练、测、面」串成闭环：学员按学科与阶段浏览题库、刷题、维护错题本、沿学习路线学习课程，并通过 AI 模拟面试获得逐题评分与六维能力报告；管理端提供学科、课程、题库、面试题库、学习路线、学员与系统管理的完整运营后台。

后端基于 **Spring Boot 3 + MyBatis-Plus + MySQL + Redis + MinIO** 构建模块化单体；前端为两个独立的 Vue 3 SPA（学员端 / 管理端）；AI 能力通过 **OpenAI 兼容协议** 接入大模型，未配置 Key 时自动降级为本地评分，不阻断主要流程。

## 🎯 核心解决的问题

- **面试准备碎片化**：题库、课程、学习路线、错题记录分散在不同工具里，难以形成连续的学习路径。
- **缺少真实面试反馈**：传统刷题只有对错，无法模拟面试中的追问、表达和临场压力。
- **学习路径不清晰**：不知道先学什么、练什么、什么时候适合开始模拟面试。
- **内容运营效率低**：题库、面试题、课程依赖手工录入，缺少批量导入、预览校验与版本化数据迁移。

本项目将题库训练、错题复习、学习路线、AI 模拟面试和内容运营整合到一个平台中，适合作为 AI 面试训练产品原型、教学内容中台或毕业设计/课程项目。

---

## 📑 目录

- [✨ 亮点能力](#-亮点能力)
- [💡 典型使用场景](#-典型使用场景)
- [🛠️ 技术栈](#️-技术栈)
- [🚀 快速体验](#-快速体验)
  - [1. 环境要求](#1-环境要求)
  - [2. 启动基础设施](#2-启动基础设施)
  - [3. 配置 AI 模型](#3-配置-ai-模型)
  - [4. 启动后端](#4-启动后端)
  - [5. 启动学员端](#5-启动学员端)
  - [6. 启动管理端](#6-启动管理端)
  - [7. 默认账号](#7-默认账号)
- [🏗️ 架构概览](#️-架构概览)
- [🔌 核心接口](#-核心接口)
- [⚙️ 关键机制](#️-关键机制)
- [💻 前端说明](#-前端说明)
- [📁 目录结构](#-目录结构)
- [🧪 测试与 CI](#-测试与-ci)
- [📦 生产部署](#-生产部署)
- [❓ 常见问题](#-常见问题)
- [🔮 后续可扩展方向](#-后续可扩展方向)
- [📚 文档与许可](#-文档与许可)

---

## ✨ 亮点能力

| 能力 | 说明 | 价值 |
| :--- | :--- | :--- |
| **学员端 + 管理端双端一体** | 学员端负责学习与训练，管理端负责内容、用户与运营 | 一套系统覆盖「学」与「管」 |
| **AI 模拟面试** | 选择方向 / 难度 / 题数，逐题作答，SSE 流式返回评分与评语，结束后生成六维能力报告 | 接近真实面试的连续追问与即时反馈 |
| **智能刷题** | 单选 / 多选 / 判断 / 简答四类题型；客观题服务端重判，简答题 AI 批改 | 兼顾刷题效率与主观题表达训练 |
| **错题本闭环** | 错误次数、连续答对次数、待复习 / 已掌握状态；连续答对 3 次自动掌握 | 把「做错」转化为可复习的学习资产 |
| **结构化内容体系** | 9 个面试方向、760 道面试题、20 个主题；学科 → 阶段 → 题目三级结构 | 内容组织清晰，可按方向/主题/难度训练 |
| **内容运营后台** | 学科与阶段、B 站课程库、题库、面试题库、学习路线、学员、用户、角色、日志、系统设置 | 运营人员可独立维护全部内容 |
| **批量导入与预览** | 课程支持 Excel / 粘贴文本；面试题支持 Excel / Markdown；两段式预览 + 确认，最多 500 行 | 导入前可逐行校验，避免脏数据入库 |
| **安全与稳定性** | 双端独立 JWT、35 个权限点、IP + 账号双维度频控、Redis 故障降级、操作日志 | 满足基础生产化要求 |
| **工程化交付** | Docker Compose、Nginx / HTTPS、数据库迁移、GitHub Actions、单元测试 | 从本地开发到生产部署有完整路径 |

## 💡 典型使用场景

### 1. 学员刷题与错题复习
**学员操作：**
> 选择「Java后端开发」→「JVM」阶段 → 顺序/随机练习 → 提交答案 → 查看解析与 AI 批改。

**系统行为：**
- 客观题由服务端判定，简答题调用 AI 按关键点评分并给出评语；
- 答错自动进入错题本，记录错误次数与原答案；
- 错题重做连续答对 3 次后自动标记「已掌握」。

### 2. AI 模拟面试
**学员操作：**
> 选择方向、难度和题数 → 开始面试 → 逐题作答 → 查看六维能力报告与提升建议。

**系统行为：**
- 单题 5 分钟倒计时，SSE 流式返回评分与评语；
- 每日最多 5 次面试，题目从对应方向的面试题库中抽取；
- 结束后生成技术准确性、知识广度、原理理解、工程实践、逻辑结构、表达清晰六维报告；
- AI 不可用时使用基于逐题得分与参考答案的本地降级报告。

### 3. 内容运营与题库导入
**管理员操作：**
> 进入面试题库 → 上传 Markdown → 预览题目与答案 → 确认导入 → 维护主题与难度。

**系统行为：**
- 预览结果暂存 Redis（30 分钟 token），确认时按 token 入库；
- 支持 Excel 两段式、Markdown 两段式、手工录入三种来源；
- 课程支持 Excel 与「粘贴文本」两种导入方式，字段语义一致。

### 4. 学习路线
**学员操作：**
> 选择学科 → 查看有序课程 → 跳转 B 站学习 → 手动勾选「已完成」。

**系统行为：**
- 路线按学科归属，课程可跨学科补充；
- 完成记录计入学习进度，管理端可维护路线与课程顺序。

---

## 🛠️ 技术栈

**后端 (Backend):**
- Java 17、Spring Boot 3.3.5（Web / Security / Validation / AOP / Actuator）
- MyBatis-Plus 3.5.7 + MySQL 8.0
- Redis 7（登录态、频控、幂等、导入预览 token、看板缓存）
- MinIO 8.5.12（头像、Logo、默认头像等对象存储）
- JJWT 0.12.6（学员端 / 管理端双令牌）
- EasyExcel 4.0.3、commonmark（Excel / Markdown 导入导出）
- springdoc OpenAPI（Swagger UI）

**前端 (Frontend):**
- Vue 3.5 + TypeScript 5.6 + Vite 5
- Pinia + Vue Router
- Element Plus 2.14 + ECharts 5
- Axios（统一请求层、双令牌刷新）

**AI 能力:**
- OpenAI 兼容协议，默认接入 `mimo-v2.6-pro`（可通过环境变量切换）
- SSE 流式评分与评语推送
- 简答题批改、面试报告在模型不可用时自动降级为本地评分

**工程与部署:**
- Docker Compose（MySQL / Redis / MinIO / 后端 / Nginx / 迁移任务）
- Nginx（学员端、管理端、API、SSE、MinIO 统一入口）
- 数据库版本化迁移（`schema.sql` + `scripts/sql/migrations`）
- GitHub Actions（后端测试 + 前端类型检查与构建）

---

## 🚀 快速体验

### 1. 环境要求

- JDK 17
- Maven 3.9+
- Node.js 20+
- Docker Desktop / Docker Compose v2

### 2. 启动基础设施

在项目根目录执行：

```bash
# 可选：复制环境变量模板；不复制则使用 compose 中的开发默认值
cp deploy/env/.env.example deploy/.env

docker compose -f deploy/docker-compose.yml up -d
docker compose -f deploy/docker-compose.yml ps
```

默认会启动：

| 服务 | 端口 | 说明 |
| :--- | :--- | :--- |
| `mysql` | `3306` | 业务库 `yszx`，首次启动执行 `schema.sql` |
| `redis` | `6379` | 登录态、频控、缓存、导入 token |
| `minio` | `9000` / `9001` | 对象存储 API / 控制台 |
| `migrate` | 无 | 一次性任务，补齐 Java 方向与 760 道面试题后退出 |

首次初始化会自动执行 `scripts/sql/schema.sql` 与 `scripts/sql/migrations` 下的迁移，面试题库包含 **Java 后端开发 744 题 + AI 方向种子 16 题**。

### 3. 配置 AI 模型

AI Key 只从环境变量读取，不写入代码或数据库。未配置时，简答题批改与面试报告会降级为本地评分，仍可完整体验主要流程。

**PowerShell：**

```powershell
$env:MIMO_API_KEY="your-mimo-key"
```

**macOS / Linux：**

```bash
export MIMO_API_KEY="your-mimo-key"
```

常用配置项：

| 配置项 | 默认值 | 说明 |
| :--- | :--- | :--- |
| `MIMO_API_KEY` | 空 | OpenAI 兼容模型密钥 |
| `MIMO_BASE_URL` | `https://api.xiaomimimo.com/v1` | 模型服务地址 |
| `MIMO_MODEL` | `mimo-v2.6-pro` | 模型名称 |
| `MIMO_TIMEOUT_SECONDS` | `60` | 单次模型调用超时 |
| `APP_DEFAULT_PASSWORD` | `Yszx@123456` | 内置管理员初始密码 / 重置密码默认值 |

### 4. 启动后端

```bash
cd backend
mvn spring-boot:run
```

- 服务地址：`http://localhost:8080`
- 健康检查：`http://localhost:8080/actuator/health`
- 接口文档：`http://localhost:8080/swagger-ui.html`
- 默认 profile：`dev`（短信 mock、SQL 日志、内测免登录通道开启）

如需构建可执行 Jar：

```bash
cd backend
mvn -DskipTests package
java -jar target/yunshu-zhixue-backend-0.1.0-SNAPSHOT.jar
```

### 5. 启动学员端

```bash
cd portal-web
npm install
npm run dev
```

- 学员端地址：`http://localhost:5173`
- 开发代理：`/api`、`/files` → `http://localhost:8080`

### 6. 启动管理端

```bash
cd admin-web
npm install
npm run dev
```

- 管理端地址：`http://localhost:5174/admin/`
- 开发代理：`/api`、`/files` → `http://localhost:8080`

### 7. 默认账号

| 端 | 账号 | 密码 | 说明 |
| :--- | :--- | :--- | :--- |
| 管理端 | `admin` | `Yszx@123456` | 内置超级管理员；可通过 `APP_DEFAULT_PASSWORD` 覆盖，生产必须修改 |
| 学员端 | 自行注册 | — | 开发环境短信验证码通过接口回显与日志输出，便于联调 |
| 学员端（内测） | 管理端顶栏「进入学员端」 | — | 仅 `PREVIEW_PORTAL_LOGIN_ENABLED=true` 时可用，生产默认关闭 |

> 生产环境请勿使用默认密码，并在首次登录后立即修改。

---

## 🏗️ 架构概览

```text
学员端 portal-web (5173)          管理端 admin-web (5174/admin)
            \                          /
             \                        /
              Nginx / Vite 开发代理
                        |
                Spring Boot API (8080)
   ┌────────────────────┼────────────────────┐
   │                    │                    │
 内容域                学习域              系统 / AI
 subject / stage      practice            auth / RBAC
 course               wrong-book          dashboard
 question             route               logs / settings
 interview-bank       interview           AI grading / report
   │                    │                    │
   └────────────────────┼────────────────────┘
                        |
          MySQL 8.0 / Redis 7 / MinIO
```

后端按业务领域分包，模块间保持单向依赖；前端两个 SPA 独立构建，生产由 Nginx 统一托管：

- 学员端：`/`
- 管理端：`/admin/`
- 后端 API：`/api/`
- 对象存储文件：`/files/`

---

## 🔌 核心接口

| 模块 | 方法 | 路径 | 说明 |
| :--- | :--- | :--- | :--- |
| 管理端认证 | `POST` | `/api/admin/v1/auth/login` | 管理员登录，返回 access / refresh 双令牌 |
| 学员端认证 | `POST` | `/api/portal/v1/auth/login` | 学员密码 / 短信验证码登录 |
| 面试配置 | `GET` | `/api/portal/v1/interview/config` | 方向、难度、题数、每日剩余次数 |
| 发起面试 | `POST` | `/api/portal/v1/interview/sessions` | 创建面试会话并抽题 |
| 单题评分 | `POST` | `/api/portal/v1/interview/sessions/{id}/answers` | SSE 流式返回评分与评语 |
| 面试报告 | `GET` | `/api/portal/v1/interview/reports/{id}` | 六维能力、优缺点、建议、提升卡 |
| 刷题开始 | `POST` | `/api/portal/v1/practice/start` | 顺序 / 随机组卷 |
| 刷题提交 | `POST` | `/api/portal/v1/practice/{practiceId}/submit` | 客观题重判、简答题 AI 批改 |
| 课程导入预览 | `POST` | `/api/admin/v1/courses/import/preview` | Excel 两段式① |
| 课程粘贴文本预览 | `POST` | `/api/admin/v1/courses/import/preview-text` | 粘贴文本两段式①（A-C09） |
| 课程导入确认 | `POST` | `/api/admin/v1/courses/import/confirm` | 凭预览 token 入库 |
| 面试题 Markdown 预览 | `POST` | `/api/admin/v1/interview-questions/import/markdown/preview` | Markdown 两段式① |
| 面试题 Markdown 确认 | `POST` | `/api/admin/v1/interview-questions/import/markdown/confirm` | 按 token 入库并补主题 |

> 完整接口契约见 [`docs/design/04.API接口设计.md`](docs/design/04.API接口设计.md)，本地启动后也可通过 Swagger UI 查看。

---

## ⚙️ 关键机制

### 双端 JWT 与 RBAC
- 学员端与管理端使用独立密钥、独立令牌域，access 2 小时、refresh 14 天。
- 登出、改密后旧令牌进入 Redis 黑名单，强制重新登录。
- 管理端 9 组 35 个权限点，后端通过 `@RequirePermission` 校验，前端 `v-permission` 只控制按钮可见性。

### AI 面试与降级
- 面试会话存 Redis，单题超时 5 分钟，每日上限 5 次。
- 单题评分通过 SSE 推送，前端实时渲染评分与评语。
- 面试报告一次调用模型汇总六维能力、优缺点、建议与提升卡。
- 模型不可用或未配置 Key 时，使用逐题得分与参考答案生成本地降级报告；简答题批改降级为要点匹配并标记 `degraded=true`。

### 频控与幂等
- 登录、注册、短信、面试发起等接口按 **IP + 账号** 双维度计数，窗口 60 秒。
- Redis 故障时降级放行，避免缓存故障拖垮核心链路。
- 导入确认支持 `Idempotency-Key`，重复提交返回首次结果，避免重复入库。

### 导入两段式
- Excel / Markdown / 粘贴文本统一走「预览 → 确认」两段式。
- 预览结果按 token 暂存 Redis 30 分钟，单次最多 500 行。
- 预习阶段完成表头识别、学科匹配、BV 号查重、字段校验，确认阶段单事务写入。

### 数据与时间
- 全站逻辑删除（`delete_flag`），操作日志与角色权限关系表例外。
- 时间统一 UTC 存储，展示层按 `Asia/Shanghai` 输出。
- 数据库结构变更使用独立迁移脚本，执行记录写入 `schema_migration`。

---

## 💻 前端说明

### 学员端（portal-web）
- 首页：平台数据、快速开始、学习统计、热门学科、AI 面试介绍、新手引导。
- 账号：注册、密码登录、短信登录、找回密码、个人中心、头像上传。
- 题库：学科 / 阶段 / 题目三级浏览，题型、难度、关键词筛选。
- 刷题：顺序 / 随机组卷、本地断点续做、计时与答题卡、AI 简答批改。
- 错题本：统计卡、高频错题、筛选排序、批量掌握 / 移除、错题重做。
- 学习路线：路线详情、有序课程、B 站跳转、完成勾选与进度统计。
- AI 面试：配置、进行中、报告、历史记录。

### 管理端（admin-web）
- 数据看板：统计卡、用户增长、题目学科分布、最近活动。
- 内容管理：学科与阶段、B 站课程库、题库、面试题库、学习路线。
- 学员与系统：学员管理、用户管理、角色权限树、操作日志、系统设置。
- 批量导入：Excel / 粘贴文本 / Markdown 预览与确认。

开发环境通过 Vite 将 `/api` 与 `/files` 代理到 `http://localhost:8080`；管理端构建 `base` 为 `/admin/`，生产由 Nginx 托管。

---

## 📁 目录结构

```text
云枢智学/
├─ backend/                     # Spring Boot 模块化单体
│  ├─ src/main/java/com/yunshu/zhixue/
│  │  ├─ common/                # 响应体、异常、常量、幂等、分页
│  │  ├─ config/                # MyBatis-Plus、Jackson、OpenAPI、线程池
│  │  ├─ security/              # 双端 JWT、权限注解、频控
│  │  └─ modules/               # auth / subject / course / question /
│  │                            # interviewbank / study / interview /
│  │                            # route / student / stats / system / ai
│  └─ src/test/                 # 单元测试
├─ portal-web/                  # 学员端 Vue 3 SPA
├─ admin-web/                   # 管理端 Vue 3 SPA
├─ deploy/                      # Docker Compose、Nginx、HTTPS、环境模板
├─ docs/                        # 需求、架构、数据库、API、交付说明
│  ├─ design/
│  ├─ rules/
│  ├─ P2交付说明.md
│  └─ P3交付说明.md
├─ scripts/
│  ├─ sql/                      # schema.sql、迁移脚本、迁移执行器
│  ├─ interview-md/             # 面试题 Markdown 与转换/校验脚本
│  └─ e2e/                      # 端到端冒烟测试
├─ prototype/                   # 页面原型与截图
├─ .github/workflows/ci.yml     # CI
└─ README.md
```

---

## 🧪 测试与 CI

**后端单元测试：**

```bash
cd backend
mvn test
```

当前覆盖 28 个用例，包括：

- Markdown 面试题解析与入库字段
- 课程粘贴文本解析（表头 / 两种列顺序 / 空行 / 无效行）
- 接口频控与 Redis 故障降级
- 幂等键并发与结果缓存
- 看板缓存、用户增长补零、最近活动分页
- 错误码定义

**前端检查：**

```bash
cd portal-web && npm run type-check && npm run build
cd admin-web && npm run type-check && npm run build
```

**CI：** `.github/workflows/ci.yml` 在 push / PR 时执行后端测试与两个前端的类型检查和构建。

**端到端冒烟（可选，需先启动完整开发环境）：**

```bash
python scripts/e2e/smoke_test.py
```

---

## 📦 生产部署

生产部署使用 `deploy/docker-compose.prod.yml`，一条 Compose 启动 MySQL、Redis、MinIO、后端、迁移任务与 Nginx：

```bash
cp deploy/env/.env.prod.example deploy/.env
# 填写 MySQL / Redis / MinIO / JWT / 默认密码等密钥
docker compose -f deploy/docker-compose.prod.yml up -d --build
```

访问：

- 学员端：`http://<host>:8088/`
- 管理端：`http://<host>:8088/admin/`
- API：`http://<host>:8088/api/`

### HTTPS

使用 Nginx 直接终止 TLS：

```bash
# 将证书放到 deploy/certs/fullchain.pem 与 privkey.pem
docker compose -f deploy/docker-compose.prod.yml \
  -f deploy/docker-compose.https.yml up -d --build
```

也可以由外层负载均衡 / SLB 终止 TLS，再转发到 `WEB_PORT`。详细说明见 [`deploy/README.md`](deploy/README.md)。

### 数据库迁移

`schema.sql` 只在 MySQL 数据卷首次初始化时执行；后续结构与数据变更放在 `scripts/sql/migrations/`：

```bash
# 开发环境
bash scripts/sql/migrate.sh

# 生产环境（新增迁移文件后）
docker compose -f deploy/docker-compose.prod.yml run --rm migrate
```

迁移执行记录写入 `schema_migration` 表，已执行版本自动跳过。命名与流程见 [`scripts/sql/README.md`](scripts/sql/README.md)。

---

## ❓ 常见问题

### 1. 后端启动失败，提示无法连接 MySQL / Redis / MinIO
**排查步骤：**
- 确认基础设施已启动：`docker compose -f deploy/docker-compose.yml ps`
- 等待 MySQL / Redis 健康后再启动后端
- 检查 `deploy/.env` 或环境变量中的端口、密码是否与 compose 一致

### 2. AI 评分不可用或返回降级结果
- 检查 `MIMO_API_KEY` 是否配置、模型地址与模型名是否正确
- 未配置 Key 时，简答题批改与面试报告会自动降级为本地评分，属于预期行为
- 查看后端日志中的模型调用异常信息

### 3. 管理端登录失败
- 默认账号 `admin` / `Yszx@123456`
- 如通过 `APP_DEFAULT_PASSWORD` 覆盖，请使用覆盖后的密码
- 生产环境首次部署后请立即修改默认密码

### 4. 导入提示「预览已失效」
- 预览 token 默认 30 分钟过期
- 重新上传文件或粘贴文本，重新解析后再确认

### 5. 生产库只有 8 个 AI 方向
- `schema.sql` 只包含 8 个 AI 方向的基础种子
- 「Java后端开发」方向与题库由迁移脚本补齐
- 确认 `migrate` 服务已执行，或手动运行 `bash scripts/sql/migrate.sh`

### 6. 管理端刷新页面 404
- 开发环境请使用 `http://localhost:5174/admin/` 访问
- 生产环境确认 Nginx 已配置 `/admin/` 的 SPA 回退

### 7. 端口冲突
- 修改 `deploy/.env` 中的 `MYSQL_PORT`、`REDIS_PORT`、`MINIO_API_PORT`、`WEB_PORT` 等变量

---

## 🔮 后续可扩展方向

- [ ] 增加更多面试方向与题库来源，支持题目版本与批量上下架审计。
- [ ] 增加语音面试与实时转写，完善表达维度的评分依据。
- [ ] 增加 AI 面试追问策略与按岗位定制的面试模板。
- [ ] 增加 RAG 知识检索，让面试题推荐与解析更贴合岗位与学习记录。
- [ ] 增加题库/课程导入的失败重试、批次回滚与导入历史。
- [ ] 增加更细粒度的可观测性（模型调用耗时、Token 成本、限流命中率）。
- [ ] 增加前端 E2E 自动化与更多边界场景测试。

---

## 📚 文档与许可

**设计文档：**

- [需求文档](docs/云枢智学-需求文档.md)
- [功能模块梳理](docs/design/01.功能模块梳理.md)
- [整体架构设计](docs/design/02.整体架构设计.md)
- [数据库设计](docs/design/03.数据库设计.md)
- [API 接口设计](docs/design/04.API接口设计.md)
- [P2 交付说明](docs/P2交付说明.md)
- [P3 交付说明](docs/P3交付说明.md)
- [项目协作规范](docs/rules/协作规范.md)
- [生产部署说明](deploy/README.md)
- [数据库迁移说明](scripts/sql/README.md)

**License：** 本项目当前为私有项目，未声明开源许可证。如需开源，请补充 `LICENSE` 文件。

---

> 如果这个项目对你有帮助，欢迎 Star ⭐ 或提交 Issue / PR。
