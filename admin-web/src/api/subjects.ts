import { request } from './http'
import type { StagePayload, SubjectCard, SubjectPayload, SubjectStats } from '@/types/content'

/** A-S01 学科统计卡 */
export function getSubjectStats() {
  return request.get<SubjectStats>('/subjects/stats')
}

/** A-S02 学科卡片列表 */
export function listSubjects() {
  return request.get<SubjectCard[]>('/subjects')
}

/** A-S03 新增学科 */
export function createSubject(payload: SubjectPayload) {
  return request.post<number>('/subjects', payload)
}

/** A-S04 编辑学科 */
export function updateSubject(id: number, payload: SubjectPayload) {
  return request.put<void>(`/subjects/${id}`, payload)
}

/** A-S05 学科上移 / 下移 */
export function sortSubject(id: number, move: 'up' | 'down') {
  return request.put<void>('/subjects/sort', { id, move })
}

/** A-S06 删除学科 */
export function deleteSubject(id: number) {
  return request.delete<void>(`/subjects/${id}`)
}

/** A-S07 新增阶段 */
export function createStage(subjectId: number, payload: StagePayload) {
  return request.post<number>(`/subjects/${subjectId}/stages`, payload)
}

/** A-S08 编辑阶段 */
export function updateStage(subjectId: number, id: number, payload: StagePayload) {
  return request.put<void>(`/subjects/${subjectId}/stages/${id}`, payload)
}

/** A-S09 阶段上移 / 下移 */
export function sortStage(subjectId: number, id: number, move: 'up' | 'down') {
  return request.put<void>(`/subjects/${subjectId}/stages/sort`, { id, move })
}

/** A-S10 启用 / 停用阶段 */
export function updateStageStatus(subjectId: number, id: number, status: number) {
  return request.put<void>(`/subjects/${subjectId}/stages/${id}/status`, { status })
}

/** A-S11 删除阶段 */
export function deleteStage(subjectId: number, id: number) {
  return request.delete<void>(`/subjects/${subjectId}/stages/${id}`)
}
