/** 系统管理（04 §5.9 ~ §5.13）接口类型。 */

export interface UserItem {
  id: number
  username: string
  mobile: string | null
  email: string | null
  roleId: number
  roleName: string | null
  roleCode: string | null
  status: number
  builtin: boolean
  lastLoginAt: string | null
  createdAt: string
}

export interface RecentOperation {
  id: number
  action: string
  targetType: string
  targetName: string
  ip: string
  createdAt: string
}

export interface UserDetail {
  id: number
  username: string
  mobile: string | null
  email: string | null
  roleId: number
  roleName: string | null
  roleCode: string | null
  status: number
  builtin: boolean
  createdAt: string
  lastLoginAt: string | null
  recentOperations: RecentOperation[]
}

export interface RoleOption {
  id: number
  name: string
  code: string
}

export interface UserQuery {
  keyword?: string
  roleId?: number
  status?: number
  page: number
  pageSize: number
}

export interface UserPayload {
  username: string
  mobile: string
  email: string
  roleId: number
  password?: string
  status: number
}

export interface ResetPasswordResult {
  password: string
}

export interface RoleCard {
  id: number
  name: string
  code: string
  description: string | null
  userCount: number
  permissionCount: number
  permissionNames: string[]
  builtin: boolean
}

export interface RoleCreatePayload {
  name: string
  code: string
  description: string
}

export interface PermissionNode {
  id: number
  name: string
  code: string
  type: number
  checked: boolean
  children: PermissionNode[]
}

export interface OperationLogItem {
  id: number
  userId: number
  username: string
  action: string
  targetType: string
  targetName: string
  ip: string
  createdAt: string
}

export interface LogQuery {
  keyword?: string
  operationType?: string
  startDate?: string
  endDate?: string
  page?: number
  pageSize?: number
}

export interface SystemSettings {
  siteName: string
  siteDescription: string
  logoUrl: string
  defaultAvatar: string
}

export interface ImageUploadResult {
  url: string
}
