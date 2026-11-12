import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import * as authApi from '@/api/auth'
import {
  accessToken,
  adminName,
  clearTokens,
  getAccessToken,
  getRefreshToken,
  roleCode,
  setAdminIdentity,
  setTokens
} from '@/auth/session'
import { decodeJwtPayload } from '@/utils/jwt'
import { authBypassEnabled } from '@/auth/preview'
import { ALL_PERMISSION_CODES } from '@/types/permissions'
import type { AdminLoginPayload, LoginResult } from '@/types/api'

export const useUserStore = defineStore('user', () => {
  const permissions = ref<Set<string>>(
    new Set<string>(authBypassEnabled ? ALL_PERMISSION_CODES : [])
  )

  const isLoggedIn = computed(() => authBypassEnabled || Boolean(accessToken.value))
  const displayName = computed(() =>
    authBypassEnabled ? '预览管理员' : adminName.value || '管理员'
  )
  /** 后端 role.code 为小写 super_admin（03 / schema.sql 种子） */
  const isSuperAdmin = computed(
    () => authBypassEnabled || roleCode.value.toLowerCase() === 'super_admin'
  )

  /** 当前管理员 ID：访问令牌 sub 即 admin_user.id（JwtTokenProvider） */
  const currentAdminId = computed<number | null>(() => {
    if (authBypassEnabled) {
      return null
    }
    const sub = decodeJwtPayload(getAccessToken())?.sub
    const id = sub ? Number(sub) : Number.NaN
    return Number.isInteger(id) && id > 0 ? id : null
  })

  /** 权限码来自访问令牌的 `perms` 声明（后端 JwtTokenProvider） */
  function syncPermissions(): void {
    if (authBypassEnabled) {
      return
    }
    const claims = decodeJwtPayload(getAccessToken())
    permissions.value = new Set(claims?.perms ?? [])
  }

  function applySession(result: LoginResult, account: string, remember: boolean): void {
    setTokens(result, remember)
    const profile = result.admin
    setAdminIdentity(profile?.username ?? account, profile?.roleCode ?? '', remember)
    permissions.value = new Set(profile?.permissions ?? decodeJwtPayload(result.accessToken)?.perms ?? [])
  }

  async function login(payload: AdminLoginPayload, remember = true): Promise<void> {
    const result = await authApi.login(payload)
    applySession(result, payload.account, remember)
  }

  function hasPermission(code: string): boolean {
    return permissions.value.has(code)
  }

  /** 是否为当前登录账号本人（用于自操作防护的前端置灰） */
  function isSelf(id: number): boolean {
    return currentAdminId.value === id
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
      permissions.value = new Set()
    }
  }

  syncPermissions()

  return {
    permissions,
    isLoggedIn,
    displayName,
    isSuperAdmin,
    currentAdminId,
    syncPermissions,
    login,
    hasPermission,
    isSelf,
    logout
  }
})
