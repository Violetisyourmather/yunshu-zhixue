import axios, { AxiosError, type AxiosRequestConfig, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import { clearTokens, getAccessToken, getRefreshToken, setTokens } from '@/auth/session'
import { notifySessionExpired } from '@/auth/session-events'
import type { ApiResponse, FieldError, LoginResult } from '@/types/api'

declare module 'axios' {
  export interface AxiosRequestConfig {
    /** 页面自行处理错误提示时置 true，避免重复弹出 */
    skipErrorMessage?: boolean
  }
}

/** 业务错误：同时携带业务码、HTTP 状态与字段级错误（04 §2.3）。 */
export class ApiError extends Error {
  constructor(
    readonly code: number,
    message: string,
    readonly status?: number,
    readonly fieldErrors?: FieldError[]
  ) {
    super(message)
    this.name = 'ApiError'
  }
}

const baseURL = import.meta.env.VITE_API_BASE_URL

type RetriableConfig = InternalAxiosRequestConfig & { _retried?: boolean }

const http = axios.create({
  baseURL,
  timeout: 15000
})

http.interceptors.request.use((config) => {
  const token = getAccessToken()
  if (token) {
    config.headers.set('Authorization', `Bearer ${token}`)
  }
  return config
})

let refreshPromise: Promise<string> | null = null

function refreshAccessToken(): Promise<string> {
  refreshPromise ??= doRefresh().finally(() => {
    refreshPromise = null
  })
  return refreshPromise
}

async function doRefresh(): Promise<string> {
  const token = getRefreshToken()
  if (!token) {
    throw new ApiError(10001, '登录已过期，请重新登录', 401)
  }
  const response = await axios.post<ApiResponse<LoginResult>>(`${baseURL}/auth/refresh`, null, {
    headers: { 'X-Refresh-Token': token },
    timeout: 15000
  })
  if (response.data.code !== 0 || !response.data.data) {
    throw new ApiError(response.data.code, response.data.message, response.status)
  }
  setTokens(response.data.data)
  return response.data.data.accessToken
}

function isAuthEndpoint(url?: string): boolean {
  if (!url) {
    return false
  }
  return ['/auth/login', '/auth/refresh', '/auth/logout'].some((path) => url.includes(path))
}

function toApiError(error: AxiosError<ApiResponse<unknown>>): ApiError {
  const body = error.response?.data
  if (body && typeof body === 'object' && 'code' in body) {
    return new ApiError(body.code, body.message, error.response?.status, body.fieldErrors ?? undefined)
  }
  if (error.code === 'ECONNABORTED') {
    return new ApiError(-1, '请求超时，请稍后重试')
  }
  return new ApiError(-1, '网络异常，请检查网络连接')
}

http.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ApiResponse<unknown>>) => {
    const config = error.config as RetriableConfig | undefined
    const status = error.response?.status

    if (status === 401 && config && !config._retried && !isAuthEndpoint(config.url)) {
      config._retried = true
      try {
        const token = await refreshAccessToken()
        config.headers.set('Authorization', `Bearer ${token}`)
        return await http.request(config)
      } catch {
        // 刷新失败：按会话失效处理，抛出原始 401
      }
    }

    // 下载类接口（responseType: 'blob'）失败时响应体也是 Blob，先还原为 JSON 以保留业务提示
    if (error.response?.data instanceof Blob) {
      try {
        const text = await error.response.data.text()
        error.response.data = JSON.parse(text) as ApiResponse<unknown>
      } catch {
        // 非 JSON 响应体：保持原样，由 toApiError 兜底
      }
    }

    const apiError = toApiError(error)
    if (status === 401) {
      clearTokens()
      notifySessionExpired()
    }
    if (!config?.skipErrorMessage) {
      ElMessage.error(apiError.message)
    }
    throw apiError
  }
)

function unwrap<T>(response: AxiosResponse<ApiResponse<T>>): T {
  if (response.data.code !== 0) {
    throw new ApiError(response.data.code, response.data.message, response.status, response.data.fieldErrors ?? undefined)
  }
  return response.data.data
}

/** 统一返回 `data`，调用方不需要重复解包。 */
export const request = {
  get<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return http.get<ApiResponse<T>>(url, config).then(unwrap)
  },
  post<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    return http.post<ApiResponse<T>>(url, data, config).then(unwrap)
  },
  put<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    return http.put<ApiResponse<T>>(url, data, config).then(unwrap)
  },
  delete<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return http.delete<ApiResponse<T>>(url, config).then(unwrap)
  },
  /** 同步导出类接口（Excel/CSV）直接取二进制流 */
  download(url: string, config?: AxiosRequestConfig): Promise<Blob> {
    return http.get<Blob>(url, { ...config, responseType: 'blob' }).then((response) => response.data)
  }
}
