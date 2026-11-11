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

/**
 * 后台登录结果（04 §2.2 明确双令牌命名）。
 *
 * `admin` 为可选扩展：04 尚未定义登录响应的管理员信息与权限码字段。
 * 缺失时前端从访问令牌的 `perms` 声明解析权限码（与后端 JwtTokenProvider 一致）。
 */
export interface LoginResult {
  accessToken: string
  refreshToken: string
  admin?: AdminProfile
}

export interface AdminProfile {
  id: number
  username: string
  roleCode: string
  roleName?: string
  permissions?: string[]
}

export interface AdminLoginPayload {
  account: string
  password: string
}
