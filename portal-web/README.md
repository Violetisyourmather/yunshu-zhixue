# 云枢智学 · 学员端（portal-web）

Vue 3 + TypeScript + Vite + Pinia + Vue Router + Element Plus + ECharts 的独立 SPA（02 D2/D9）。

## 当前状态

**基础工程已搭建完成**：路由、登录态、请求层、SSE 消费、状态管理、布局与样式令牌可用；
业务页面均为占位，等待按 P1~P4 模块逐步实现。

已完成：

- 工程配置：Vite 5、TypeScript 严格模式、`@` 别名、`/api` 与 `/files` 开发代理
- 路由：公开区（首页/题库）与登录区（刷题/错题本/路线/AI 面试/个人中心）分区，登录守卫
- 登录态：`localStorage` 双令牌、令牌域 `portal` 前缀、刷新轮换、401 自动刷新重试
- 请求层：统一解包 `{code,message,data}`、业务错误 `ApiError`、字段错误透传、超时/网络兜底
- 状态：`useUserStore`（资料与登录态）、`usePracticeStore`、`useInterviewStore`
- SSE：`api/sse.ts` 按 04 §2.9 消费 `status/token/grade/error` 事件
- 组合式函数：短信倒计时、Web Speech API 语音识别（不支持时降级键盘输入）
- 样式：与 `prototype/` 对齐的设计令牌（纸感底座 + 深墨 + 玉青）

未实现：全部业务页面与接口联调、ECharts 图表、练习/面试状态机细节。

## 开发

```powershell
pnpm install
pnpm dev          # http://localhost:5173
pnpm build        # 类型检查 + 生产构建（dist/）
pnpm type-check
```

后端需在 `localhost:8080` 运行（`../backend`），开发服务器会代理 `/api` 与 `/files`。

## 环境变量

| 变量 | 开发值 | 说明 |
|---|---|---|
| `VITE_API_BASE_URL` | `/api/portal/v1` | 学员端接口前缀（04 §1） |
| `VITE_APP_TITLE` | `云枢智学` | 页面标题 |

生产环境与后端同源，由 Nginx 统一入口转发；**不要把令牌放入 URL**（04 §2.2）。

## 目录

```
portal-web/
├── src/
│   ├── api/            http（拦截器/刷新）、auth、profile、sse
│   ├── auth/           令牌存储与会话失效事件
│   ├── composables/    倒计时、语音识别
│   ├── components/     AuthShell、PagePlaceholder
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
