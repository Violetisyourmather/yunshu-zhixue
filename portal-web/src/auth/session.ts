import { ref } from 'vue'
import type { LoginResult } from '@/types/api'

/**
 * 学员端登录态：localStorage（记住我）或 sessionStorage（关闭浏览器即失效）。
 * 令牌键带 `portal` 前缀，与管理端令牌域物理隔离（04 §2.2）。
 */
const ACCESS_KEY = 'yszx_portal_access_token'
const REFRESH_KEY = 'yszx_portal_refresh_token'

function readStored(key: string): string {
  return localStorage.getItem(key) ?? sessionStorage.getItem(key) ?? ''
}

/** 刷新令牌时沿用登录时选择的持久化方式。 */
let persistSession = localStorage.getItem(ACCESS_KEY) !== null
  || sessionStorage.getItem(ACCESS_KEY) === null

export const accessToken = ref(readStored(ACCESS_KEY))
export const refreshToken = ref(readStored(REFRESH_KEY))

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
}

export function clearTokens(): void {
  accessToken.value = ''
  refreshToken.value = ''
  clearStorage()
}

function clearStorage(): void {
  localStorage.removeItem(ACCESS_KEY)
  localStorage.removeItem(REFRESH_KEY)
  sessionStorage.removeItem(ACCESS_KEY)
  sessionStorage.removeItem(REFRESH_KEY)
}
