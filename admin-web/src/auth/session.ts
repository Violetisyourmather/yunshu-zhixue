import { ref } from 'vue'
import type { LoginResult } from '@/types/api'

/**
 * 管理端登录态：localStorage（记住登录）或 sessionStorage（关闭浏览器失效）。
 * 令牌键带 `admin` 前缀，与学员端令牌域物理隔离（04 §2.2）。
 */
const ACCESS_KEY = 'yszx_admin_access_token'
const REFRESH_KEY = 'yszx_admin_refresh_token'
const NAME_KEY = 'yszx_admin_name'
const ROLE_KEY = 'yszx_admin_role'

function readStored(key: string): string {
  return localStorage.getItem(key) ?? sessionStorage.getItem(key) ?? ''
}

/** 刷新令牌轮换时沿用登录时选择的持久化方式。 */
let persistSession = localStorage.getItem(ACCESS_KEY) !== null
  || sessionStorage.getItem(ACCESS_KEY) === null

export const accessToken = ref(readStored(ACCESS_KEY))
export const refreshToken = ref(readStored(REFRESH_KEY))
export const adminName = ref(readStored(NAME_KEY))
export const roleCode = ref(readStored(ROLE_KEY))

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

/** 保存双令牌；remember=true 写入 localStorage，否则写入 sessionStorage。 */
export function setTokens(tokens: LoginResult, remember = persistSession): void {
  persistSession = remember
  clearStorage()
  const storage = remember ? localStorage : sessionStorage
  storage.setItem(ACCESS_KEY, tokens.accessToken)
  storage.setItem(REFRESH_KEY, tokens.refreshToken)
  accessToken.value = tokens.accessToken
  refreshToken.value = tokens.refreshToken
  tokenListeners.forEach((listener) => listener(tokens))
}

export function setAdminIdentity(name: string, role: string, remember = persistSession): void {
  const storage = remember ? localStorage : sessionStorage
  adminName.value = name
  roleCode.value = role
  storage.setItem(NAME_KEY, name)
  storage.setItem(ROLE_KEY, role)
}

export function clearTokens(): void {
  accessToken.value = ''
  refreshToken.value = ''
  adminName.value = ''
  roleCode.value = ''
  clearStorage()
}

function clearStorage(): void {
  localStorage.removeItem(ACCESS_KEY)
  localStorage.removeItem(REFRESH_KEY)
  localStorage.removeItem(NAME_KEY)
  localStorage.removeItem(ROLE_KEY)
  sessionStorage.removeItem(ACCESS_KEY)
  sessionStorage.removeItem(REFRESH_KEY)
  sessionStorage.removeItem(NAME_KEY)
  sessionStorage.removeItem(ROLE_KEY)
}
