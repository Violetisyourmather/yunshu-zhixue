import { request } from './http'
import type { PageResult } from '@/types/api'
import type { ResetPasswordResult, RoleOption, UserDetail, UserItem, UserPayload, UserQuery } from '@/types/system'

/** A-M01 用户列表 */
export function listUsers(params: UserQuery) {
  return request.get<PageResult<UserItem>>('/users', { params })
}

/** 用户管理页角色下拉 */
export function listRoleOptions() {
  return request.get<RoleOption[]>('/users/role-options')
}

/** A-M02 新增用户 */
export function createUser(payload: UserPayload) {
  return request.post<UserItem>('/users', payload)
}

/** A-M03 编辑用户 */
export function updateUser(id: number, payload: UserPayload) {
  return request.put<void>(`/users/${id}`, payload)
}

/** A-M04 批量启用/禁用 */
export function batchUpdateUserStatus(ids: number[], status: number) {
  return request.put<void>('/users/batch-status', { ids, status })
}

/** A-M05 删除用户 */
export function deleteUser(id: number) {
  return request.delete<void>(`/users/${id}`)
}

/** A-M06 批量删除 */
export function batchDeleteUsers(ids: number[]) {
  return request.delete<void>('/users/batch', { data: { ids } })
}

/** A-M07 重置密码为部署期默认密码 */
export function resetUserPassword(id: number) {
  return request.post<ResetPasswordResult>(`/users/${id}/reset-password`)
}

/** A-M08 用户详情与最近操作 */
export function getUserDetail(id: number) {
  return request.get<UserDetail>(`/users/${id}/detail`)
}
