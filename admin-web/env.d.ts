/// <reference types="vite/client" />

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<Record<string, unknown>, Record<string, unknown>, unknown>
  export default component
}

interface ImportMetaEnv {
  readonly VITE_APP_TITLE: string
  readonly VITE_API_BASE_URL: string
  /** 学员端根地址：开发为 http://localhost:5173，生产为空（同域根路径） */
  readonly VITE_PORTAL_BASE_URL?: string
  /** 开发环境免登录预览开关，设为 true 开启（默认关闭，生产构建忽略） */
  readonly VITE_AUTH_BYPASS?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
