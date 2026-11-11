import { ref } from 'vue'
import type { LoginResult } from '@/types/api'

/**
 * 管理端登录态：localStorage + 响应式引用。
 * 令牌键带 `admin` 前缀，与学员端令牌域物理隔离（04 §2.2）。
 */
const ACCESS_KEY = 'yszx_admin_access_token'
const REFRESH_KEY = 'yszx_admin_refresh_token'
const NAME_KEY = 'yszx_admin_name'
const ROLE_KEY = 'yszx_admin_role'

export const accessToken = ref(localStorage.getItem(ACCESS_KEY) ?? '')
export const refreshToken = ref(localStorage.getItem(REFRESH_KEY) ?? '')
export const adminName = ref(localStorage.getItem(NAME_KEY) ?? '')
export const roleCode = ref(localStorage.getItem(ROLE_KEY) ?? '')

type TokensChangedListener = (tokens: LoginResult) => void
const tokenListeners = new Set<TokensChangedListener>()

/** 刷新令牌轮换后通知上层重新解析权限码 */
export function onTokensChanged(listener: TokensChangedListener): () => void {
  tokenListeners.add(listener)
  return () => tokenListeners.delete(listener)
}

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
  tokenListeners.forEach((listener) => listener(tokens))
}

export function setAdminIdentity(name: string, role: string): void {
  adminName.value = name
  roleCode.value = role
  localStorage.setItem(NAME_KEY, name)
  localStorage.setItem(ROLE_KEY, role)
}

export function clearTokens(): void {
  accessToken.value = ''
  refreshToken.value = ''
  adminName.value = ''
  roleCode.value = ''
  localStorage.removeItem(ACCESS_KEY)
  localStorage.removeItem(REFRESH_KEY)
  localStorage.removeItem(NAME_KEY)
  localStorage.removeItem(ROLE_KEY)
}
