import { request } from './http'
import type { SystemSettings } from '@/types/system'

/** A-T01 读取系统基本信息（登录即可） */
export function getSettings() {
  return request.get<SystemSettings>('/settings')
}

/** A-T02 保存系统基本信息 */
export function saveSettings(payload: SystemSettings) {
  return request.put<SystemSettings>('/settings', payload)
}

/** A-T03 恢复默认设置 */
export function resetSettings() {
  return request.post<SystemSettings>('/settings/reset')
}
