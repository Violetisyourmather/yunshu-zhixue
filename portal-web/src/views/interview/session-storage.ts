import type { AiPoint } from '@/api/practice'
import type { InterviewQuestion } from '@/api/interview'

export interface InterviewMessage {
  id: string
  role: 'ai' | 'student'
  content: string
  time: string
  order?: number
  score?: number
  feedback?: string
  referencePoints?: AiPoint[]
  degraded?: boolean
  kind?: 'welcome' | 'question' | 'grade' | 'answer' | 'end'
}

export interface InterviewSession {
  sessionId: string
  subjectId: number
  subjectName: string
  difficulty: number
  questionCount: number
  timeoutMinutes: number
  questions: InterviewQuestion[]
  messages: InterviewMessage[]
  currentOrder: number
  scores: Record<number, number>
  feedbacks: Record<number, string>
  referencePoints: Record<number, AiPoint[]>
  degraded: Record<number, boolean>
  answers: Record<number, string>
  durations: Record<number, number>
  elapsed: number
  questionElapsed: number
  startedAt: number
  phase: 'ask' | 'analyzing' | 'grading' | 'done'
  voiceEnabled: boolean
  justStarted: boolean
  savedAt: number
}

const STORAGE_KEY = 'yszx_portal_interview_session'

export function newMessageId(): string {
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) {
    return crypto.randomUUID()
  }
  return `${Date.now()}-${Math.random().toString(16).slice(2)}`
}

export function nowLabel(): string {
  const now = new Date()
  return [now.getHours(), now.getMinutes()].map((value) => String(value).padStart(2, '0')).join(':')
}

export function difficultyLabel(difficulty: number): string {
  return difficulty === 1 ? '简单' : difficulty === 3 ? '困难' : '中等'
}

export function createInterviewSession(params: {
  sessionId: string
  subjectId: number
  subjectName: string
  difficulty: number
  questionCount: number
  questions: InterviewQuestion[]
  timeoutMinutes: number
}): InterviewSession {
  const first = params.questions[0]
  const welcome: InterviewMessage = {
    id: newMessageId(),
    role: 'ai',
    content: `你好，我是本次的 AI 面试官。方向：${params.subjectName}，难度：${difficultyLabel(params.difficulty)}。我们开始第 1 题。`,
    time: nowLabel(),
    kind: 'welcome'
  }
  const question: InterviewMessage = {
    id: newMessageId(),
    role: 'ai',
    content: first.content,
    time: nowLabel(),
    order: first.order,
    kind: 'question'
  }
  return {
    sessionId: params.sessionId,
    subjectId: params.subjectId,
    subjectName: params.subjectName,
    difficulty: params.difficulty,
    questionCount: params.questionCount,
    timeoutMinutes: params.timeoutMinutes,
    questions: params.questions,
    messages: [welcome, question],
    currentOrder: 1,
    scores: {},
    feedbacks: {},
    referencePoints: {},
    degraded: {},
    answers: {},
    durations: {},
    elapsed: 0,
    questionElapsed: 0,
    startedAt: Date.now(),
    phase: 'ask',
    voiceEnabled: true,
    justStarted: true,
    savedAt: Date.now()
  }
}

export function loadInterviewSession(): InterviewSession | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) {
      return null
    }
    const data = JSON.parse(raw) as InterviewSession
    if (!data?.sessionId || !Array.isArray(data.questions) || !data.questions.length) {
      localStorage.removeItem(STORAGE_KEY)
      return null
    }
    data.messages ??= []
    data.scores ??= {}
    data.feedbacks ??= {}
    data.referencePoints ??= {}
    data.degraded ??= {}
    data.answers ??= {}
    data.durations ??= {}
    data.elapsed = Math.max(0, data.elapsed ?? 0)
    data.questionElapsed = Math.max(0, data.questionElapsed ?? 0)
    data.startedAt ??= Date.now()
    data.timeoutMinutes = data.timeoutMinutes ?? 5
    data.voiceEnabled = data.voiceEnabled !== false
    data.phase = data.phase ?? 'ask'
    data.currentOrder = Math.min(
      Math.max(1, data.currentOrder ?? 1),
      data.questions.length
    )
    data.savedAt ??= Date.now()
    return data
  } catch {
    localStorage.removeItem(STORAGE_KEY)
    return null
  }
}

export function saveInterviewSession(session: InterviewSession): void {
  session.savedAt = Date.now()
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(session))
  } catch {
    // 本地存储不可用时保持当前内存会话
  }
}

export function clearInterviewSession(): void {
  try {
    localStorage.removeItem(STORAGE_KEY)
  } catch {
    // ignore
  }
}
