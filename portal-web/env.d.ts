/// <reference types="vite/client" />

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<Record<string, unknown>, Record<string, unknown>, unknown>
  export default component
}

interface ImportMetaEnv {
  readonly VITE_APP_TITLE: string
  readonly VITE_API_BASE_URL: string
  /** 管理端入口地址（生产为 /admin/，开发默认指向 5174 端口） */
  readonly VITE_ADMIN_URL?: string
  /** 开发环境免登录预览开关，设为 true 可开启（生产构建忽略） */
  readonly VITE_AUTH_BYPASS?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
