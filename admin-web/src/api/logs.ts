import { request } from './http'
import type { PageResult } from '@/types/api'
import type { LogQuery, OperationLogItem } from '@/types/system'

/** A-G01 日志列表 */
export function listLogs(params: LogQuery) {
  return request.get<PageResult<OperationLogItem>>('/logs', { params })
}

/** A-G02 日志详情 */
export function getLog(id: number) {
  return request.get<OperationLogItem>(`/logs/${id}`)
}

/** A-G03 导出 CSV（同步下载流） */
export function exportLogs(params: Omit<LogQuery, 'page' | 'pageSize'>) {
  return request.download('/logs/export', { params })
}

/** A-G04 清空日志（物理删除） */
export function clearLogs() {
  return request.delete<number>('/logs')
}
