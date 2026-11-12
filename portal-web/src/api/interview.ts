import { request } from './http'
import type { PageResult } from '@/types/api'

export interface InterviewSubjectOption {
  subjectId: number
  subjectName: string
}

/** P-I01 面试配置。 */
export interface InterviewConfig {
  subjects: InterviewSubjectOption[]
  difficulties: number[]
  questionCountOptions: number[]
  defaultCount: number
  remainingCount: number
  timeoutMinutes: number
}

export interface InterviewQuestion {
  order: number
  subjectId: number
  difficulty: number
  content: string
}

/** P-I02 发起面试。 */
export interface InterviewStartPayload {
  subjectId: number
  difficulty: number
  count: number
}

export interface InterviewStartResult {
  sessionId: string
  questions: InterviewQuestion[]
  questionCount: number
}

export interface InterviewAnswerPayload {
  order: number
  studentAnswer: string | null
  durationSeconds: number
}

export interface InterviewFinishPayload {
  answers: InterviewAnswerPayload[]
}

export interface InterviewFinishResult {
  reportId: number
  overallScore: number
}

export interface InterviewImproveCard {
  title: string
  content: string
  type: string
}

export interface InterviewQuestionReview {
  order: number
  questionText: string
  studentAnswer: string | null
  score: number
  feedback: string | null
}

/** P-I05 面试报告。 */
export interface InterviewReport {
  reportId: number
  recordId: number
  subjectId: number
  subjectName: string | null
  difficulty: number
  questionCount: number
  overallScore: number
  trendDelta: number | null
  dimensions: Record<string, number>
  strengths: string[]
  weaknesses: string[]
  suggestions: string[]
  improveCards: InterviewImproveCard[]
  questionReviews: InterviewQuestionReview[]
  totalDuration: number
  completedAt: string | null
}

/** P-I07 面试记录。 */
export interface InterviewRecordItem {
  recordId: number
  reportId: number | null
  subjectId: number
  subjectName: string | null
  difficulty: number
  questionCount: number
  overallScore: number | null
  totalDuration: number | null
  completedAt: string | null
  hasReport: boolean
}

/** P-I08 面试统计，分数口径 0~10。 */
export interface InterviewStats {
  interviewTotal: number
  avgScore: number | null
  maxScore: number | null
  totalDuration: number
}

export function getInterviewConfig() {
  return request.get<InterviewConfig>('/interview/config', { skipErrorMessage: true })
}

export function startInterview(payload: InterviewStartPayload) {
  return request.post<InterviewStartResult>('/interview/sessions', payload, {
    skipErrorMessage: true
  })
}

export function finishInterview(sessionId: string, payload: InterviewFinishPayload) {
  return request.post<InterviewFinishResult>(
    `/interview/sessions/${encodeURIComponent(sessionId)}/finish`,
    payload,
    {
      skipErrorMessage: true,
      // 结束面试会同步调用 AI 汇总报告（后端单次 AI 超时 60s），
      // 沿用全局 15s 会在 AI 稍慢时提前中断，导致后端已落库而前端拿不到 reportId。
      timeout: 120000
    }
  )
}

export function getInterviewReport(reportId: number) {
  return request.get<InterviewReport>(`/interview/reports/${reportId}`, {
    skipErrorMessage: true
  })
}

export function getInterviewRecords(page = 1, pageSize = 10) {
  return request.get<PageResult<InterviewRecordItem>>('/interview/records', {
    params: { page, pageSize },
    skipErrorMessage: true
  })
}

export function getInterviewStats() {
  return request.get<InterviewStats>('/interview/records/stats', {
    skipErrorMessage: true
  })
}
