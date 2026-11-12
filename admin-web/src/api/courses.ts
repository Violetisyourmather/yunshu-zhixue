import { request } from './http'
import type { PageResult } from '@/types/api'
import type {
  CourseImportPreview,
  CourseImportResult,
  CourseItem,
  CoursePayload,
  CourseQuery,
  CourseStats,
  SubjectRef
} from '@/types/content'

/** A-C01 课程统计卡 */
export function getCourseStats() {
  return request.get<CourseStats>('/courses/stats')
}

/** A-C02 课程列表 */
export function listCourses(params: CourseQuery) {
  return request.get<PageResult<CourseItem>>('/courses', { params })
}

/** 课程表单 / 筛选学科下拉 */
export function listCourseSubjectOptions() {
  return request.get<SubjectRef[]>('/courses/subject-options')
}

/** A-C03 新增课程 */
export function createCourse(payload: CoursePayload) {
  return request.post<number>('/courses', payload)
}

/** A-C04 编辑课程 */
export function updateCourse(id: number, payload: CoursePayload) {
  return request.put<void>(`/courses/${id}`, payload)
}

/** A-C05 删除课程 */
export function deleteCourse(id: number) {
  return request.delete<void>(`/courses/${id}`)
}

/** A-C08 上架 / 下架 */
export function updateCourseStatus(id: number, status: number) {
  return request.put<void>(`/courses/${id}/status`, { status })
}

/** A-C06 批量导入预览（Excel） */
export function previewCourseImport(file: File) {
  const form = new FormData()
  form.append('file', file)
  return request.post<CourseImportPreview>('/courses/import/preview', form)
}

/** A-C09 批量导入预览（粘贴文本行） */
export function previewCourseImportText(text: string) {
  return request.post<CourseImportPreview>('/courses/import/preview-text', { text })
}

/** A-C07 确认批量导入 */
export function confirmCourseImport(token: string) {
  return request.post<CourseImportResult>(
    '/courses/import/confirm',
    { token },
    { headers: { 'Idempotency-Key': token } }
  )
}
