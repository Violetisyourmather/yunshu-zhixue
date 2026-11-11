import { ref } from 'vue'
import { defineStore } from 'pinia'

export interface InterviewMessage {
  role: 'interviewer' | 'student'
  content: string
  order?: number
}

/**
 * 面试会话状态（02 §3.3）：消息流、当前题序与逐题评分。
 * 会话仅前端本地保存用于续答，服务端 Redis 暂存快照（D16）。
 */
export const useInterviewStore = defineStore('interview', () => {
  const sessionId = ref<number | null>(null)
  const messages = ref<InterviewMessage[]>([])
  const currentOrder = ref(1)
  const scores = ref<Record<number, number>>({})

  function reset(): void {
    sessionId.value = null
    messages.value = []
    currentOrder.value = 1
    scores.value = {}
  }

  return { sessionId, messages, currentOrder, scores, reset }
})
