/** 与 04 §2.3~§2.4 一致的统一响应结构与分页结构。 */
export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  fieldErrors?: FieldError[] | null
  fileErrors?: FileError[] | null
}

export interface FieldError {
  field: string
  reason: string
}

export interface FileError {
  row: number
  field: string
  reason: string
}

export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  pageSize: number
}

/** P-A07 刷新与登录共用：双令牌（04 §2.2）。 */
export interface LoginResult {
  accessToken: string
  refreshToken: string
}

export type SmsScene = 'register' | 'login' | 'reset'

export interface SmsCodePayload {
  mobile: string
  type: SmsScene
}

export interface PasswordLoginPayload {
  account: string
  password: string
}

export interface SmsLoginPayload {
  mobile: string
  code: string
}

export interface RegisterPayload {
  mobile: string
  code: string
  password: string
}

export interface ResetVerifyPayload {
  mobile: string
  code: string
}

export interface ResetPasswordPayload {
  resetToken: string
  newPassword: string
}

/** P-M01 个人信息（04 §4.8）。 */
export interface StudentProfile {
  id: number
  nickname: string
  mobile: string
  email: string | null
  gender: number
  preferredSubjectId: number | null
  avatar: string | null
  guideFinished: boolean
  stats: {
    practiceCount: number
    interviewCount: number
    wrongQuestionCount: number
  }
  study: {
    totalDays: number
    accuracyRate: number
  }
}

/** 04 §2.9 SSE 事件负载。 */
export interface SseStatusEvent {
  phase: 'analyzing' | 'grading' | 'thinking'
}

export interface SseTokenEvent {
  delta: string
}

export interface SseGradeEvent {
  order: number
  score: number
  feedback: string
  degraded: boolean
  referencePoints: string[]
}

export interface SseErrorEvent {
  code: number
  message: string
}
