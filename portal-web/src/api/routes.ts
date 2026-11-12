import { request } from './http'

/** 学习路线页学科选择卡。 */
export interface RouteSubjectItem {
  subjectId: number
  subjectName: string
  icon: string
  courseCount: number
  routeId: number | null
  routeName: string | null
}

/** P-R01 学员可见的学习路线。 */
export interface RouteCardItem {
  id: number
  name: string
  subjectId: number
  subjectName: string | null
  targetEducation: string | null
  description: string | null
  courseCount: number
  completedCourseCount: number
  progress: number
}

/** P-R02 路线中的课程。 */
export interface RouteCourseItem {
  routeCourseId: number
  courseId: number
  name: string
  bvid: string | null
  durationMinutes: number
  source: number
  coverUrl: string | null
  completed: boolean
  sortOrder: number
}

/** P-R02 路线详情。 */
export interface RouteDetail {
  id: number
  name: string
  subjectId: number
  subjectName: string | null
  targetEducation: string | null
  description: string | null
  courses: RouteCourseItem[]
}

export function getRouteSubjects() {
  return request.get<RouteSubjectItem[]>('/routes/subjects', { skipErrorMessage: true })
}

export function getRoutes() {
  return request.get<RouteCardItem[]>('/routes', { skipErrorMessage: true })
}

export function getRouteDetail(id: number) {
  return request.get<RouteDetail>(`/routes/${id}`, { skipErrorMessage: true })
}

/** P-R03 标记课程完成，幂等。 */
export function completeRouteCourse(routeCourseId: number, courseId: number) {
  return request.post<void>(`/route-courses/${routeCourseId}/complete`, { courseId }, {
    skipErrorMessage: true
  })
}

/** P-R03a 取消课程完成，幂等。 */
export function uncompleteRouteCourse(routeCourseId: number) {
  return request.delete<void>(`/route-courses/${routeCourseId}/complete`, {
    skipErrorMessage: true
  })
}
