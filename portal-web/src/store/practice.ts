import { ref } from 'vue'
import { defineStore } from 'pinia'

/**
 * 练习会话状态（02 §3.3）：当前题目、作答与断点续练。
 * 具体答题与判分逻辑随 P-P01/P-P02 实现，此处先固定状态形状。
 */
export const usePracticeStore = defineStore('practice', () => {
  const sessionId = ref<number | null>(null)
  const currentIndex = ref(0)
  const answers = ref<Record<number, string[]>>({})

  function reset(): void {
    sessionId.value = null
    currentIndex.value = 0
    answers.value = {}
  }

  return { sessionId, currentIndex, answers, reset }
})
