# 云枢智学 · 学员端（portal-web）

Vue 3 + TypeScript + Vite + Pinia + Vue Router + Element Plus + ECharts 的独立 SPA（02 D2/D9）。

## 当前状态

**学员端全部页面已完成并接入后端**：首页、注册/登录/找回密码、个人中心、学习路线、题库、刷题、错题本与 AI 面试（配置 / 进行中 / 报告 / 记录）均可使用；路由、登录态、请求层、SSE 消费、状态管理、布局与样式令牌已落地。

已完成：

- 工程配置：Vite 5、TypeScript 严格模式、`@` 别名、`/api` 与 `/files` 开发代理
- 路由：公开区（首页/题库）与登录区（刷题/错题本/路线/AI 面试/个人中心）分区，登录守卫
- 登录态：`localStorage` 双令牌、令牌域 `portal` 前缀、刷新轮换、401 自动刷新重试
- 请求层：统一解包 `{code,message,data}`、业务错误 `ApiError`、字段错误透传、超时/网络兜底
- 状态：`useUserStore`（资料与登录态）、`usePracticeStore`、`useInterviewStore`
- SSE：`api/sse.ts` 按 04 §2.9 消费 `status/token/grade/error` 事件
- 组合式函数：短信倒计时、Web Speech API 语音识别（不支持时降级键盘输入）
- 样式：与 `prototype/` 对齐的设计令牌（纸感底座 + 深墨 + 玉青）
- 学员端布局：顶部导航（首页/学习路线/题库/AI 面试）、用户下拉（个人中心/我的错题/面试记录/退出）、三列页脚与管理后台入口，适配桌面与移动端
- 首页：Hero 平台数据、快速开始、核心功能、学习节奏（统计 + 近 7 天练习）、热门学科、AI 面试介绍、首次访问三步引导
- 首页接口：已按 04 §4.2 封装 P-H01 ~ P-H03，后端接口已实现并联调；平台数据、热门学科、学习概览均来自服务端，接口失败时展示空状态与重试入口
- 账号与认证：注册、密码登录、手机验证码登录、找回密码、退出登录；支持「记住我」切换 localStorage / sessionStorage
- 个人中心：个人信息读取与修改、头像上传、学习数据与统计（P-M01 / P-M02 / P-M04 / P-M05）
- 学习路线：学科卡片切换、路线详情、有序课程列表、B 站跳转、标记完成 / 取消完成与进度统计（P-R01 / P-R02 / P-R03）
- 题库中心：学科栏、阶段列表、题目列表、题型 / 难度 / 关键词筛选、分页加载、题目详情弹窗；游客只看题干与选项，登录后返回答案、解析与错题本状态（P-B01 ~ P-B04）
- 刷题练习：顺序 / 随机组卷、本地断点续做、计时与答题卡、客观题即时判定、简答题 AI 批改、答案解析与练习结束确认（P-P01 / P-P02）
- 错题本：统计卡、高频错题 Top3、状态 / 题型 / 时间 / 排序筛选、批量操作、标记掌握、移除与错题重做（P-W01 ~ P-W05）
- 开发环境短信：后端 `app.sms.mock=true` 时验证码通过接口回显与日志输出，便于本地联调；生产需接入真实短信服务

AI 面试已完成配置、发起、SSE 逐题评分、报告、历史记录与统计。

## 开发

```powershell
npm install
npm run dev          # http://localhost:5173
npm run build        # 类型检查 + 生产构建（dist/）
npm run type-check
```

后端需在 `localhost:8080` 运行（`../backend`），开发服务器会代理 `/api` 与 `/files`。

## 环境变量

| 变量 | 开发值 | 说明 |
|---|---|---|
| `VITE_API_BASE_URL` | `/api/portal/v1` | 学员端接口前缀（04 §1） |
| `VITE_APP_TITLE` | `云枢智学` | 页面标题 |
| `VITE_ADMIN_URL` | `http://localhost:5174/admin/` | 页脚「管理后台」入口，生产为 `/admin/` |

生产环境与后端同源，由 Nginx 统一入口转发；**不要把令牌放入 URL**（04 §2.2）。

## 目录

```
portal-web/
├── src/
│   ├── api/            http（拦截器/刷新）、auth、profile、home、sse
│   ├── auth/           令牌存储与会话失效事件
│   ├── composables/    倒计时、语音识别
│   ├── components/     AuthShell、PagePlaceholder、NumberTicker
│   ├── layouts/        MainLayout（主导航）
│   ├── router/         路由表与登录守卫
│   ├── store/          user / practice / interview
│   ├── styles/         tokens.css（设计令牌）、index.css
│   ├── types/          api.ts、generated/（OpenAPI 生成物，不手改）
│   └── views/          home / auth / questions / practice / wrong-book
│                       routes / interview / profile / error
└── ...
```

## 约定

- 接口一律经 `api/http.ts` 的 `request`，页面不直接使用 axios，也不重复解包响应体。
- 令牌只存 `localStorage`（`yszx_portal_*`），请求头 `Authorization: Bearer`；刷新用 `X-Refresh-Token`。
- 路由守卫只做前端导航控制，真正的鉴权与权限校验由后端执行（README 第 6 节第 8 条）。
- 业务类型优先使用 `pnpm gen:types` 从后端 `/v3/api-docs` 生成的类型，生成物放 `src/types/generated/`，不手工修改（D21）。
- 新增页面必须落在 `views/<模块>`，接口封装落在 `api/<模块>.ts`。
