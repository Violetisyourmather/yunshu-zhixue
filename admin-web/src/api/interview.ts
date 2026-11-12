import { request } from './http'
import type { PageResult } from '@/types/api'
import type {
  InterviewImportPreview,
  InterviewImportResult,
  InterviewMarkdownConfirmPayload,
  InterviewMarkdownPreview,
  InterviewMarkdownResult,
  InterviewQuestionCreatePayload,
  InterviewQuestionDetail,
  InterviewQuestionItem,
  InterviewQuestionQuery,
  InterviewQuestionUpdatePayload,
  InterviewSearchResultItem,
  InterviewStats,
  InterviewSubjectOption,
  InterviewTopicItem,
  InterviewTopicPayload
} from '@/types/content'

/** 面试题库统计卡 */
export function getInterviewStats() {
  return request.get<InterviewStats>('/interview-questions/stats')
}

/** 方向（学科）下拉 */
export function listInterviewSubjects() {
  return request.get<InterviewSubjectOption[]>('/interview-questions/subject-options')
}

/** A-I01 面试题列表 */
export function listInterviewQuestions(params: InterviewQuestionQuery) {
  return request.get<PageResult<InterviewQuestionItem>>('/interview-questions', { params })
}

/** A-I02 面试题详情 */
export function getInterviewQuestion(id: number) {
  return request.get<InterviewQuestionDetail>(`/interview-questions/${id}`)
}

/** A-I03 手工录入 */
export function createInterviewQuestion(payload: InterviewQuestionCreatePayload) {
  return request.post<number>('/interview-questions', payload)
}

/** A-I04 编辑（仅主题 + 难度） */
export function updateInterviewQuestion(id: number, payload: InterviewQuestionUpdatePayload) {
  return request.put<void>(`/interview-questions/${id}`, payload)
}

/** A-I05 删除 */
export function deleteInterviewQuestion(id: number) {
  return request.delete<void>(`/interview-questions/${id}`)
}

/** A-I10 主题列表 */
export function listInterviewTopics() {
  return request.get<InterviewTopicItem[]>('/interview-topics')
}

/** A-I11 新增主题 */
export function createInterviewTopic(payload: InterviewTopicPayload) {
  return request.post<number>('/interview-topics', payload)
}

/** A-I12 编辑主题 */
export function updateInterviewTopic(id: number, payload: InterviewTopicPayload) {
  return request.put<void>(`/interview-topics/${id}`, payload)
}

/** A-I13 删除主题 */
export function deleteInterviewTopic(id: number) {
  return request.delete<void>(`/interview-topics/${id}`)
}

/** Excel 导入预览 */
export function previewInterviewImport(file: File) {
  const form = new FormData()
  form.append('file', file)
  return request.post<InterviewImportPreview>('/interview-questions/import/preview', form)
}

/** Excel 确认导入（token 作为幂等键，重复提交返回首次结果） */
export function confirmInterviewImport(token: string) {
  return request.post<InterviewImportResult>(
    '/interview-questions/import/confirm',
    { token },
    { headers: { 'Idempotency-Key': token } }
  )
}

/** Markdown 导入预览（按 # ~ ### 标题切题） */
export function previewInterviewMarkdown(file: File) {
  const form = new FormData()
  form.append('file', file)
  return request.post<InterviewMarkdownPreview>(
    '/interview-questions/import/markdown/preview',
    form
  )
}

/** Markdown 确认导入（token 作为幂等键） */
export function confirmInterviewMarkdown(payload: InterviewMarkdownConfirmPayload) {
  return request.post<InterviewMarkdownResult>(
    '/interview-questions/import/markdown/confirm',
    payload,
    { headers: { 'Idempotency-Key': payload.token } }
  )
}

/** A-I09 检索测试 */
export function searchInterviewQuestions(keyword: string) {
  return request.post<InterviewSearchResultItem[]>('/interview-questions/search-test', {
    keyword
  })
}

/** 下载导入模板 */
export function downloadInterviewTemplate() {
  return request.download('/interview-questions/import/template')
}
