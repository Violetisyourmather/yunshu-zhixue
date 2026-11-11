import { ref } from 'vue'
import type { LoginResult } from '@/types/api'

/**
 * 学员端登录态：localStorage + 响应式引用。
 * 令牌键带 `portal` 前缀，与管理端令牌域物理隔离（04 §2.2）。
 */
const ACCESS_KEY = 'yszx_portal_access_token'
const REFRESH_KEY = 'yszx_portal_refresh_token'

export const accessToken = ref(localStorage.getItem(ACCESS_KEY) ?? '')
export const refreshToken = ref(localStorage.getItem(REFRESH_KEY) ?? '')

export function getAccessToken(): string {
  return accessToken.value
}

export function getRefreshToken(): string {
  return refreshToken.value
}

export function setTokens(tokens: LoginResult): void {
  accessToken.value = tokens.accessToken
  refreshToken.value = tokens.refreshToken
  localStorage.setItem(ACCESS_KEY, tokens.accessToken)
  localStorage.setItem(REFRESH_KEY, tokens.refreshToken)
}

export function clearTokens(): void {
  accessToken.value = ''
  refreshToken.value = ''
  localStorage.removeItem(ACCESS_KEY)
  localStorage.removeItem(REFRESH_KEY)
}
