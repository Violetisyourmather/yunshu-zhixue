/**
 * 开发环境免登录预览：默认关闭，走真实登录流程。
 * 需要仅查看页面布局时，在 `.env.development` 中设置 `VITE_AUTH_BYPASS=true`。
 * 生产构建中 `import.meta.env.DEV` 恒为 false，不受此开关影响。
 */
export const authBypassEnabled =
  import.meta.env.DEV && import.meta.env.VITE_AUTH_BYPASS === 'true'
