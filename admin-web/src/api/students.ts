import { request } from './http'
import type { PageResult } from '@/types/api'
import type { StudentDetail, StudentInterviewItem, StudentItem, StudentQuery } from '@/types/content'

/** A-U01 学员列表 */
export function listStudents(params: StudentQuery) {
  return request.get<PageResult<StudentItem>>('/students', { params })
}

/** A-U02 启用 / 禁用学员 */
export function updateStudentStatus(id: number, status: number) {
  return request.put<void>(`/students/${id}/status`, { status })
}

/** A-U03 批量启用 / 禁用 */
export function batchUpdateStudentStatus(ids: number[], status: number) {
  return request.put<number>('/students/batch-status', { ids, status })
}

/** A-U04 删除学员 */
export function deleteStudent(id: number) {
  return request.delete<void>(`/students/${id}`)
}

/** A-U05 批量删除 */
export function batchDeleteStudents(ids: number[]) {
  return request.delete<void>('/students/batch', { data: { ids } })
}

/** A-U06 学员详情 */
export function getStudentDetail(id: number) {
  return request.get<StudentDetail>(`/students/${id}/detail`)
}

/** A-U07 学员面试记录 */
export function listStudentInterviews(id: number, page: number, pageSize: number) {
  return request.get<PageResult<StudentInterviewItem>>(`/students/${id}/interviews`, {
    params: { page, pageSize }
  })
}

/** A-U08 导出学员列表 */
export function exportStudents(params: StudentQuery) {
  return request.download('/students/export', { params })
}
