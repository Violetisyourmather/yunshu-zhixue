import { request } from './http'
import type {
  LoginResult,
  PasswordLoginPayload,
  RegisterPayload,
  ResetPasswordPayload,
  ResetVerifyPayload,
  SmsCodePayload,
  SmsLoginPayload
} from '@/types/api'

/** P-A01 发送短信验证码（公开） */
export function sendSmsCode(payload: SmsCodePayload) {
  return request.post<{ expireSeconds: number }>('/auth/sms-code', payload, { skipErrorMessage: true })
}

/** P-A02 注册（公开） */
export function register(payload: RegisterPayload) {
  return request.post<LoginResult>('/auth/register', payload, { skipErrorMessage: true })
}

/** P-A03 密码登录（公开） */
export function loginByPassword(payload: PasswordLoginPayload) {
  return request.post<LoginResult>('/auth/login/password', payload, { skipErrorMessage: true })
}

/** P-A04 验证码登录（公开） */
export function loginBySms(payload: SmsLoginPayload) {
  return request.post<LoginResult>('/auth/login/sms', payload, { skipErrorMessage: true })
}

/** P-A05 找回密码·验证验证码（公开） */
export function verifyResetCode(payload: ResetVerifyPayload) {
  return request.post<{ resetToken: string }>('/auth/reset/verify', payload, { skipErrorMessage: true })
}

/** P-A06 找回密码·重置（公开） */
export function resetPassword(payload: ResetPasswordPayload) {
  return request.post<void>('/auth/reset', payload, { skipErrorMessage: true })
}

/** P-A08 登出：携带刷新令牌，幂等 */
export function logout(refreshToken: string) {
  return request.post<void>('/auth/logout', null, {
    headers: { 'X-Refresh-Token': refreshToken },
    skipErrorMessage: true
  })
}
