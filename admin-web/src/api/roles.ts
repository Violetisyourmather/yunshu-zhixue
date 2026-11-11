import { request } from './http'
import type { PermissionNode, RoleCard, RoleCreatePayload } from '@/types/system'

/** A-L01 角色卡片列表（仅超级管理员） */
export function listRoles() {
  return request.get<RoleCard[]>('/roles')
}

/** A-L02 新增角色 */
export function createRole(payload: RoleCreatePayload) {
  return request.post<number>('/roles', payload)
}

/** A-L03 删除角色 */
export function deleteRole(id: number) {
  return request.delete<void>(`/roles/${id}`)
}

/** A-L04 权限树（含指定角色勾选状态） */
export function getPermissionTree(roleId?: number) {
  return request.get<PermissionNode[]>('/permissions/tree', { params: { roleId } })
}

/** A-L05 保存角色权限（全量覆盖） */
export function saveRolePermissions(roleId: number, permissionIds: number[]) {
  return request.put<void>(`/roles/${roleId}/permissions`, { permissionIds })
}
