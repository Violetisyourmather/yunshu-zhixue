import { request } from './http'
import type { PageResult } from '@/types/api'

/** A-D01 统计卡：总量 + 本自然月新增量（delta）。 */
export interface DashboardOverview {
  subjectCount: number
  subjectDelta: number
  questionCount: number
  questionDelta: number
  studentCount: number
  studentDelta: number
  interviewCount: number
  interviewDelta: number
}

/** A-D02 用户增长日粒度数据点（上海自然日）。 */
export interface UserGrowthPoint {
  date: string
  count: number
}

/** A-D03 题目学科分布项。 */
export interface QuestionDistributionItem {
  subjectId: number
  subjectName: string
  questionCount: number
}

/** A-D04 最近活动行。 */
export interface RecentActivityItem {
  id: number
  username: string
  action: string
  targetType: string
  targetName: string
  ip: string
  createdAt: string
}

export function getDashboardOverview() {
  return request.get<DashboardOverview>('/dashboard/overview')
}

export function getUserGrowth(range: 7 | 30 | 90) {
  return request.get<UserGrowthPoint[]>('/dashboard/user-growth', { params: { range } })
}

export function getQuestionDistribution() {
  return request.get<QuestionDistributionItem[]>('/dashboard/question-distribution')
}

export function getRecentActivities(page = 1, pageSize = 6) {
  return request.get<PageResult<RecentActivityItem>>('/dashboard/recent-activities', {
    params: { page, pageSize }
  })
}
