import { request } from './http'
import type { AdminLoginPayload, LoginResult } from '@/types/api'

/** 学员端内测令牌（管理端已登录时签发，用于免二次登录进入学员端）。 */
export interface PortalPreviewTokens {
  accessToken: string
  refreshToken: string
}

export function portalPreviewTokens() {
  return request.post<PortalPreviewTokens>('/auth/portal-preview-token')
}

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
