import axios from 'axios'
import type { ApiResponse } from '@/types/api'
import type { SystemSettings } from '@/types/system'

/**
 * 平台信息公开接口（P-T01）：学员端前缀，未登录可读。
 * 管理端登录页与整站品牌都依赖它，故独立于带鉴权的 request 实例。
 */
const siteClient = axios.create({
  baseURL: '/api/portal/v1',
  timeout: 15000
})

export async function getSiteSettings(): Promise<SystemSettings> {
  const response = await siteClient.get<ApiResponse<SystemSettings>>('/settings')
  const body = response.data
  if (body.code !== 0 || !body.data) {
    throw new Error(body.message || '平台信息读取失败')
  }
  return body.data
}
