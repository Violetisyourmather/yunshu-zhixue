import { request } from './http'

export interface PracticeOption {
  key: string
  text: string
}

/** P-P01 练习题目，含答案与解析，用于前端即时展示。 */
export interface PracticeQuestion {
  id: number
  type: number
  difficulty: number
  content: string
  options: PracticeOption[]
  answer: string | null
  analysis: string | null
  stageName: string | null
  subjectName: string | null
  tags: string | null
  doneCount: number
  accuracyRate: number
}

export interface PracticeStartPayload {
  source: 'question' | 'subject'
  questionId?: number
  subjectId?: number
  stageId?: number
  type?: number
  difficulty?: number
  count?: number
  mode: 1 | 2
}

export interface PracticeStartResult {
  practiceId: string
  title: string
  questions: PracticeQuestion[]
}

export interface PracticeAnswerPayload {
  questionId: number
  answer: string
  durationSeconds: number
}

export interface AiPoint {
  point: string
  hit: boolean
}

/** P-P02 单题判分结果。 */
export interface PracticeResult {
  questionId: number
  isCorrect: boolean
  score: number
  answer: string | null
  analysis: string | null
  aiFeedback: string | null
  referencePoints: AiPoint[]
  degraded: boolean
}

export interface PracticeSubmitResult {
  results: PracticeResult[]
}

export function startPractice(payload: PracticeStartPayload) {
  return request.post<PracticeStartResult>('/practice/start', payload, {
    skipErrorMessage: true
  })
}

export function submitPractice(
  practiceId: string,
  answers: PracticeAnswerPayload[],
  mode: 1 | 2
) {
  const questionKey = answers.map((item) => item.questionId).join(',')
  return request.post<PracticeSubmitResult>(
    `/practice/${encodeURIComponent(practiceId)}/submit`,
    { answers, mode },
    {
      skipErrorMessage: true,
      // 同一练习的同一题重复提交（网络重试）返回首次结果，避免重复记入错题/统计
      headers: { 'Idempotency-Key': `${practiceId}:${questionKey}` }
    }
  )
}
