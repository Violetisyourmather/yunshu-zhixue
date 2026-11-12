import { request } from './http'
import type { PageResult } from '@/types/api'
import type {
  QuestionBatchResult,
  QuestionDetail,
  QuestionImportPreview,
  QuestionImportResult,
  QuestionItem,
  QuestionPayload,
  QuestionQuery,
  QuestionStageGroup,
  QuestionStats
} from '@/types/content'

/** A-Q01 题库统计卡 */
export function getQuestionStats() {
  return request.get<QuestionStats>('/questions/stats')
}

/** 题库表单 / 筛选的阶段分组 */
export function getQuestionFormOptions() {
  return request.get<QuestionStageGroup[]>('/questions/form-options')
}

/** A-Q02 题目列表 */
export function listQuestions(params: QuestionQuery) {
  return request.get<PageResult<QuestionItem>>('/questions', { params })
}

/** A-Q03 题目详情 */
export function getQuestionDetail(id: number) {
  return request.get<QuestionDetail>(`/questions/${id}`)
}

/** A-Q04 新增题目 */
export function createQuestion(payload: QuestionPayload) {
  return request.post<number>('/questions', payload)
}

/** A-Q05 编辑题目 */
export function updateQuestion(id: number, payload: QuestionPayload) {
  return request.put<void>(`/questions/${id}`, payload)
}

/** A-Q07 审核（pass=false 时必须传驳回原因） */
export function auditQuestion(id: number, pass: boolean, rejectReason?: string) {
  return request.post<void>(`/questions/${id}/audit`, { pass, rejectReason })
}

/** A-Q08 上架 / 下架（action: on|off） */
export function updateQuestionStatus(id: number, action: 'on' | 'off') {
  return request.post<void>(`/questions/${id}/status`, { action })
}

/** A-Q09 重新提交审核 */
export function resubmitQuestion(id: number) {
  return request.post<void>(`/questions/${id}/resubmit`)
}

/** A-Q10 批量上架 / 下架 */
export function batchUpdateQuestionStatus(ids: number[], action: 'on' | 'off') {
  return request.post<QuestionBatchResult>('/questions/batch-status', { ids, action })
}

/** A-Q11 批量删除 */
export function batchDeleteQuestions(ids: number[]) {
  return request.post<void>('/questions/batch-delete', { ids })
}

/** A-Q06 删除题目 */
export function deleteQuestion(id: number) {
  return request.delete<void>(`/questions/${id}`)
}

/** A-Q12 批量导入预览（Excel + 目标学科） */
export function previewQuestionImport(file: File, subjectId: number) {
  const form = new FormData()
  form.append('file', file)
  form.append('subjectId', String(subjectId))
  return request.post<QuestionImportPreview>('/questions/import/preview', form)
}

/** A-Q13 确认批量导入 */
export function confirmQuestionImport(token: string) {
  return request.post<QuestionImportResult>(
    '/questions/import/confirm',
    { token },
    { headers: { 'Idempotency-Key': token } }
  )
}

/** A-Q14 下载导入模板 */
export function downloadQuestionTemplate() {
  return request.download('/questions/import/template')
}

/** A-Q15 按筛选条件导出题库 */
export function exportQuestions(params: QuestionQuery) {
  return request.download('/questions/export', { params })
}
