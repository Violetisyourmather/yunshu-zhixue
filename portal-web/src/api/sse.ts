import { getAccessToken } from '@/auth/session'
import type { SseErrorEvent, SseGradeEvent, SseStatusEvent, SseTokenEvent } from '@/types/api'

/**
 * SSE 消费封装（04 §2.9）：POST + fetch + ReadableStream。
 * EventSource 只支持 GET，不适用；认证仍走 Authorization 头。
 */
export interface SseHandlers {
  onStatus?: (event: SseStatusEvent) => void
  onToken?: (event: SseTokenEvent) => void
  onGrade?: (event: SseGradeEvent) => void
  onError?: (event: SseErrorEvent) => void
  onDone?: () => void
}

export async function streamSse(
  path: string,
  payload: unknown,
  handlers: SseHandlers,
  signal?: AbortSignal
): Promise<void> {
  const baseURL = import.meta.env.VITE_API_BASE_URL
  const token = getAccessToken()
  const response = await fetch(`${baseURL}${path}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream',
      ...(token ? { Authorization: `Bearer ${token}` } : {})
    },
    body: JSON.stringify(payload),
    signal
  })

  if (!response.ok) {
    const body = (await response.json().catch(() => null)) as SseErrorEvent | null
    handlers.onError?.(body ?? { code: -1, message: '流式请求失败' })
    return
  }
  if (!response.body) {
    handlers.onError?.({ code: -1, message: '当前浏览器不支持流式响应' })
    return
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''

  while (true) {
    const { done, value } = await reader.read()
    if (done) {
      break
    }
    buffer += decoder.decode(value, { stream: true })

    let boundary = buffer.indexOf('\n\n')
    while (boundary >= 0) {
      const frame = buffer.slice(0, boundary)
      buffer = buffer.slice(boundary + 2)
      dispatchFrame(frame, handlers)
      boundary = buffer.indexOf('\n\n')
    }
  }

  handlers.onDone?.()
}

function dispatchFrame(frame: string, handlers: SseHandlers): void {
  const data = frame
    .split('\n')
    .filter((line) => line.startsWith('data:'))
    .map((line) => line.slice(5).trimStart())
    .join('\n')

  // 心跳帧 `: keep-alive` 与空帧直接忽略
  if (!data) {
    return
  }

  let event: unknown
  try {
    event = JSON.parse(data)
  } catch {
    return
  }

  if (isStatusEvent(event)) {
    handlers.onStatus?.(event)
  } else if (isTokenEvent(event)) {
    handlers.onToken?.(event)
  } else if (isGradeEvent(event)) {
    handlers.onGrade?.(event)
  } else if (isErrorEvent(event)) {
    handlers.onError?.(event)
  }
}

function isStatusEvent(value: unknown): value is SseStatusEvent {
  return typeof value === 'object' && value !== null && 'phase' in value
}

function isTokenEvent(value: unknown): value is SseTokenEvent {
  return typeof value === 'object' && value !== null && 'delta' in value
}

function isGradeEvent(value: unknown): value is SseGradeEvent {
  return typeof value === 'object' && value !== null && 'score' in value && 'order' in value
}

function isErrorEvent(value: unknown): value is SseErrorEvent {
  return typeof value === 'object' && value !== null && 'code' in value && 'message' in value
}
