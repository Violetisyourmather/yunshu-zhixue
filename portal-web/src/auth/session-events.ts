/**
 * 会话失效事件：HTTP 层不直接依赖 router，避免循环引用；
 * 由 main.ts 注册实际的跳转逻辑。
 */
let sessionExpiredHandler: (() => void) | null = null

export function registerSessionExpiredHandler(handler: () => void): void {
  sessionExpiredHandler = handler
}

export function notifySessionExpired(): void {
  sessionExpiredHandler?.()
}
