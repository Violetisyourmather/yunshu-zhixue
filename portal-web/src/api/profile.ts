import { request } from './http'
import type { AvatarUploadResult, ProfileUpdatePayload, StudentProfile } from '@/types/api'

/** P-M01 个人信息（登录）；资料加载失败不打断页面，由调用方降级处理 */
export function getProfile() {
  return request.get<StudentProfile>('/profile', { skipErrorMessage: true })
}

/** P-M02 修改资料（登录） */
export function updateProfile(payload: ProfileUpdatePayload) {
  return request.put<void>('/profile', payload, { skipErrorMessage: true })
}

/** P-M04 完成新手引导（幂等） */
export function completeProfileGuide() {
  return request.post<void>('/profile/guide', null, { skipErrorMessage: true })
}

/** P-M05 上传头像（登录） */
export function uploadAvatar(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<AvatarUploadResult>('/files/avatar', formData, { skipErrorMessage: true })
}
