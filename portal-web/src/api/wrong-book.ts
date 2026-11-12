import { request } from './http'
import type { PageResult } from '@/types/api'
import type { PracticeOption, PracticeStartResult } from './practice'

export interface WrongTopItem {
  id: number
  questionId: number
  content: string
  errorCount: number
}

/** P-W01 错题统计。 */
export interface WrongQuestionStats {
  total: number
  toReviewCount: number
  masteredCount: number
  masteryRate: number
  top3: WrongTopItem[]
}

/** P-W02 错题列表项。 */
export interface WrongQuestionItem {
  id: number
  questionId: number
  type: number
  content: string
  options: PracticeOption[]
  correctAnswer: string | null
  userAnswer: string | null
  analysis: string | null
  errorCount: number
  correctedCount: number
  status: number
  lastWrongAt: string | null
  subjectName: string | null
  stageName: string | null
}

export interface WrongQuestionQuery {
  status?: number
  type?: number
  days?: number
  sort?: 'recent' | 'count'
  page?: number
  pageSize?: number
}

export function getWrongQuestionStats() {
  return request.get<WrongQuestionStats>('/wrong-questions/stats', {
    skipErrorMessage: true
  })
}

export function getWrongQuestions(params: WrongQuestionQuery) {
  return request.get<PageResult<WrongQuestionItem>>('/wrong-questions', {
    params,
    skipErrorMessage: true
  })
}

/** P-W03 错题重做组卷，返回与开始练习一致的题目包。 */
export function startWrongRedo(ids: number[]) {
  return request.post<PracticeStartResult>('/wrong-questions/redo-start', { ids }, {
    skipErrorMessage: true
  })
}

/** P-W04 标记已掌握。 */
export function markWrongMastered(id: number) {
  return request.post<void>(`/wrong-questions/${id}/mastered`, null, {
    skipErrorMessage: true
  })
}

/** P-W05 移除错题。 */
export function removeWrongQuestion(id: number) {
  return request.delete<void>(`/wrong-questions/${id}`, {
    skipErrorMessage: true
  })
}
