import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import * as authApi from '@/api/auth'
import { getProfile } from '@/api/profile'
import { accessToken, clearTokens, getRefreshToken, setTokens } from '@/auth/session'
import { authBypassEnabled } from '@/auth/preview'
import type {
  LoginResult,
  PasswordLoginPayload,
  RegisterPayload,
  SmsLoginPayload,
  StudentProfile
} from '@/types/api'

export const useUserStore = defineStore('user', () => {
  const profile = ref<StudentProfile | null>(null)
  const loadingProfile = ref(false)
  /** 复用进行中的资料请求，避免页面并发调用时拿到空结果。 */
  let profileRequest: Promise<void> | null = null
  /** 登出时递增版本号，丢弃登出前尚未返回的资料请求。 */
  let profileRequestVersion = 0

  const isLoggedIn = computed(() => authBypassEnabled || Boolean(accessToken.value))
  const displayName = computed(() =>
    authBypassEnabled ? '预览学员' : profile.value?.nickname || '我的'
  )

  /** 登录成功即生效；资料接口暂不可用时降级为顶栏默认称呼，不阻断登录 */
  async function loadProfileQuietly(): Promise<void> {
    try {
      await loadProfile()
    } catch {
      // P-M01 尚未接入或网络异常：保留令牌，后续 ensureProfile 会再次尝试
    }
  }

  async function loginByPassword(payload: PasswordLoginPayload, remember = true): Promise<LoginResult> {
    const result = await authApi.loginByPassword(payload)
    setTokens(result, remember)
    await loadProfileQuietly()
    return result
  }

  async function loginBySms(payload: SmsLoginPayload, remember = true): Promise<LoginResult> {
    const result = await authApi.loginBySms(payload)
    setTokens(result, remember)
    await loadProfileQuietly()
    return result
  }

  async function registerAndLogin(payload: RegisterPayload): Promise<LoginResult> {
    const result = await authApi.register(payload)
    setTokens(result, true)
    await loadProfileQuietly()
    return result
  }

  function loadProfile(): Promise<void> {
    if (authBypassEnabled || !isLoggedIn.value) {
      return Promise.resolve()
    }
    if (profileRequest) {
      return profileRequest
    }
    const version = ++profileRequestVersion
    loadingProfile.value = true
    profileRequest = getProfile()
      .then((data) => {
        if (version === profileRequestVersion && isLoggedIn.value) {
          profile.value = data
        }
      })
      .finally(() => {
        if (version === profileRequestVersion) {
          loadingProfile.value = false
          profileRequest = null
        }
      })
    return profileRequest
  }

  /** 有令牌但尚未取到资料时补取（刷新页面后恢复顶栏昵称） */
  function ensureProfile(): void {
    if (authBypassEnabled) {
      return
    }
    if (isLoggedIn.value && !profile.value && !loadingProfile.value) {
      void loadProfile().catch(() => {
        // 401 由 HTTP 层统一处理；其他错误不影响页面渲染
      })
    }
  }

  async function logout(): Promise<void> {
    const token = getRefreshToken()
    try {
      if (token) {
        await authApi.logout(token)
      }
    } catch {
      // 登出幂等，网络异常也继续清理本地登录态
    } finally {
      clearTokens()
      profileRequestVersion += 1
      profileRequest = null
      loadingProfile.value = false
      profile.value = null
    }
  }

  return {
    profile,
    loadingProfile,
    isLoggedIn,
    displayName,
    loginByPassword,
    loginBySms,
    registerAndLogin,
    loadProfile,
    ensureProfile,
    logout
  }
})
