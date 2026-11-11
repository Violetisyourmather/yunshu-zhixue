import axios from 'axios'
import type { ApiResponse } from '@/types/api'

/** 平台基本信息（P-T01，公开只读），对应 system_setting 的 4 项配置。 */
export interface SiteSettings {
  siteName: string
  siteDescription: string
  logoUrl: string
  defaultAvatar: string
}

const siteClient = axios.create({
  baseURL: '/api/portal/v1',
  timeout: 15000
})

export async function getSiteSettings(): Promise<SiteSettings> {
  const response = await siteClient.get<ApiResponse<SiteSettings>>('/settings')
  const body = response.data
  if (body.code !== 0 || !body.data) {
    throw new Error(body.message || '平台信息读取失败')
  }
  return body.data
}
