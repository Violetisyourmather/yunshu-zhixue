import { request } from './http'
import type { StudentProfile } from '@/types/api'

/** P-M01 个人信息（登录）；资料加载失败不打断页面，由调用方降级处理 */
export function getProfile() {
  return request.get<StudentProfile>('/profile', { skipErrorMessage: true })
}
