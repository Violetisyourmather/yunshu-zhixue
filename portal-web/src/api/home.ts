import { request } from './http'

/** P-H01 平台数据（公开，04 §4.2）。 */
export interface PlatformStats {
  studentCount: number
  questionCount: number
  interviewCount: number
}

/** P-H02 热门学科（公开，04 §4.2）。 */
export interface HomeHotSubject {
  subjectId: number
  subjectName: string
  icon: string
  stageCount: number
  questionCount: number
}

export interface LearningStats {
  consecutiveDays: number
  accuracyRate: number
  totalPracticeCount: number
}

export interface WeekTrendPoint {
  date: string
  count: number
}

/** P-H03 学习概览（登录，04 §4.2）。 */
export interface LearningOverview {
  learningStats: LearningStats
  weekTrend: WeekTrendPoint[]
  todayCount: number
  guideFinished: boolean
}

export function getPlatformStats(): Promise<PlatformStats> {
  return request.get<PlatformStats>('/home/platform-stats', { skipErrorMessage: true })
}

export function getHotSubjects(): Promise<HomeHotSubject[]> {
  return request.get<HomeHotSubject[]>('/home/hot-subjects', { skipErrorMessage: true })
}

export function getLearningOverview(): Promise<LearningOverview> {
  return request.get<LearningOverview>('/home/learning-overview', { skipErrorMessage: true })
}
