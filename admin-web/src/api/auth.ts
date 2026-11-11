import { request } from './http'
import type { AdminLoginPayload, LoginResult } from '@/types/api'

/** A-A01 后台登录（公开） */
export function login(payload: AdminLoginPayload) {
  return request.post<LoginResult>('/auth/login', payload, { skipErrorMessage: true })
}

/** A-A03 登出：携带刷新令牌，幂等 */
export function logout(refreshToken: string) {
  return request.post<void>('/auth/logout', null, {
    headers: { 'X-Refresh-Token': refreshToken },
    skipErrorMessage: true
  })
}
