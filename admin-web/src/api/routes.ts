import { request } from './http'
import type {
  RouteCard,
  RouteCourseOption,
  RouteDetail,
  RoutePayload,
  SubjectRef
} from '@/types/content'

/** A-R01 学习路线卡片列表 */
export function listRoutes() {
  return request.get<RouteCard[]>('/routes')
}

/** 路线表单学科下拉 */
export function listRouteSubjectOptions() {
  return request.get<SubjectRef[]>('/routes/subject-options')
}

/** A-R02 新建学习路线 */
export function createRoute(payload: RoutePayload) {
  return request.post<number>('/routes', payload)
}

/** A-R03 编辑学习路线 */
export function updateRoute(id: number, payload: RoutePayload) {
  return request.put<void>(`/routes/${id}`, payload)
}

/** A-R04 启用 / 停用 */
export function updateRouteStatus(id: number, status: number) {
  return request.post<void>(`/routes/${id}/status`, { status })
}

/** A-R05 删除学习路线 */
export function deleteRoute(id: number) {
  return request.delete<void>(`/routes/${id}`)
}

/** A-R06 路线详情 / 预览 */
export function getRouteDetail(id: number) {
  return request.get<RouteDetail>(`/routes/${id}/detail`)
}

/** A-R07 从课程库添加的候选课程 */
export function listRouteCourseOptions(routeId: number, keyword?: string) {
  return request.get<RouteCourseOption[]>(`/routes/${routeId}/course-options`, {
    params: { keyword }
  })
}

/** A-R07 添加课程 */
export function addRouteCourses(routeId: number, courseIds: number[]) {
  return request.post<number>(`/routes/${routeId}/courses`, { courseIds })
}

/** A-R08 移除课程 */
export function removeRouteCourse(routeId: number, courseId: number) {
  return request.delete<void>(`/routes/${routeId}/courses/${courseId}`)
}

/** A-R09 路线课程排序（orderedIds 为 routeCourseId 顺序） */
export function sortRouteCourses(routeId: number, orderedIds: number[]) {
  return request.put<void>(`/routes/${routeId}/courses/sort`, { orderedIds })
}
