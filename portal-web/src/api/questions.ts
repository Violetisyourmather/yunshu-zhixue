import { request } from './http'
import type { PageResult } from '@/types/api'

/** P-B01 题库学科卡。 */
export interface BankSubjectItem {
  id: number
  name: string
  icon: string
  description: string
  stageCount: number
  sortOrder: number
}

/** P-B02 阶段卡。 */
export interface BankStageItem {
  id: number
  name: string
  icon: string | null
  questionCount: number
  sortOrder: number
}

/** P-B03 题目卡，不含选项、答案与解析。 */
export interface BankQuestionItem {
  id: number
  type: number
  difficulty: number
  content: string
  subjectName: string | null
  stageName: string | null
  tags: string | null
  doneCount: number
  accuracyRate: number
}

export interface BankQuestionOption {
  key: string
  text: string
}

/** P-B04 题目详情；游客的 answer/analysis 为 null。 */
export interface BankQuestionDetail {
  id: number
  type: number
  difficulty: number
  content: string
  subjectName: string | null
  stageName: string | null
  tags: string | null
  options: BankQuestionOption[]
  answer: string | null
  analysis: string | null
  inWrongBook: boolean
  doneCount: number
  accuracyRate: number
}

export interface BankQuestionQuery {
  subjectId?: number
  stageId?: number
  type?: number
  difficulty?: number
  keyword?: string
  page?: number
  pageSize?: number
}

export function getBankSubjects() {
  return request.get<BankSubjectItem[]>('/subjects', { skipErrorMessage: true })
}

export function getBankStages(subjectId: number) {
  return request.get<BankStageItem[]>(`/subjects/${subjectId}/stages`, { skipErrorMessage: true })
}

export function getBankQuestions(params: BankQuestionQuery) {
  return request.get<PageResult<BankQuestionItem>>('/questions', {
    params,
    skipErrorMessage: true
  })
}

export function getBankQuestionDetail(id: number) {
  return request.get<BankQuestionDetail>(`/questions/${id}`, { skipErrorMessage: true })
}
