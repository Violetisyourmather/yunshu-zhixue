# 云枢智学 · 管理端（admin-web）

Vue 3 + TypeScript + Vite + Pinia + Vue Router + Element Plus + ECharts 的独立 SPA（02 D2/D9）。

## 当前状态

**基础工程已搭建完成**：登录、路由守卫、权限菜单、`v-permission`、请求层、布局与样式令牌可用；
业务页面均为占位，等待按 A1~A4 模块逐步实现。

已完成：

- 工程配置：Vite 5、TypeScript 严格模式、`@` 别名、`base=/admin/`、`/api` 与 `/files` 开发代理
- 路由：全部需登录，按权限码校验；角色权限页仅超级管理员可进
- 权限：登录后从访问令牌 `perms` 声明解析 35 个权限码，菜单按权限动态渲染
- `v-permission` 指令：按钮级可见性控制（仅界面控制，后端仍校验）
- 登录态：`localStorage` 双令牌、令牌域 `admin` 前缀、刷新轮换、401 自动刷新重试
- 请求层：统一解包 `{code,message,data}`、业务错误 `ApiError`、字段错误透传
- 状态：`useUserStore`（管理员/角色/权限码）、`usePermissionStore`（菜单可见性）
- 布局：深墨侧栏 + 顶栏 + 内容区；样式令牌与学员端一致

未实现：全部业务页面与接口联调、权限树、导入向导、导出、图表。

## 开发

```powershell
pnpm install
pnpm dev          # http://localhost:5174/admin/
pnpm build        # 类型检查 + 生产构建（dist/）
pnpm type-check
```

后端需在 `localhost:8080` 运行（`../backend`），开发服务器会代理 `/api` 与 `/files`。

## 环境变量

| 变量 | 开发值 | 说明 |
|---|---|---|
| `VITE_API_BASE_URL` | `/api/admin/v1` | 管理端接口前缀（04 §1） |
| `VITE_PORTAL_BASE_URL` | `http://localhost:5173` | 学员端根地址，登录页「前往学员端登录」入口；生产留空（同域根路径） |
| `VITE_APP_TITLE` | `云枢智学 · 管理后台` | 页面标题 |

生产环境由 Nginx 挂载在 `/admin`（02 §2.4），与后端同源。

## 目录

```
admin-web/
├── src/
│   ├── api/            http（拦截器/刷新）、auth
│   ├── auth/           令牌与管理员身份存储、会话失效事件
│   ├── directives/     v-permission
│   ├── components/     PagePlaceholder
│   ├── layouts/        AdminLayout（侧栏 + 顶栏）
│   ├── router/         路由表、菜单定义、权限守卫
│   ├── store/          user / permission
│   ├── styles/         tokens.css、index.css
│   ├── types/          api.ts、permissions.ts（35 权限码）、generated/
│   ├── utils/          jwt 解码（仅展示用）
│   └── views/          login / dashboard / content / student / system / error
└── ...
```

## 权限模型

| 来源 | 用途 |
|---|---|
| JWT `perms` 声明 | 菜单与按钮可见性（后端 `JwtTokenProvider` 写入） |
| 角色 `SUPER_ADMIN` | 角色管理菜单/路由仅超管可见 |

> 待确认契约：04 未定义登录响应中的管理员信息与角色字段。当前实现从令牌解析权限码；
> 若登录响应返回 `admin`（`username/roleCode/permissions`）则优先使用。后端令牌未携带角色声明，
> 因此角色管理入口在 `roleCode=SUPER_ADMIN` 时可见——该字段需在认证模块实现时补齐。

## 约定

- 接口一律经 `api/http.ts` 的 `request`；令牌只由 `auth/session.ts` 读写。
- 所有管理端接口都由后端 `@RequirePermission` 校验，前端隐藏菜单/按钮不代表授权（README 第 6 节第 8 条）。
- 新增页面必须落在 `views/<模块>`，并在 `router/menus.ts` 中声明权限码。
- 业务类型优先使用 `pnpm gen:types` 从 `/v3/api-docs` 生成，生成物不手工修改（D21）。
