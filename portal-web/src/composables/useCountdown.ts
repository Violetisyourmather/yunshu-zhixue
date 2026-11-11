import { computed, onScopeDispose, ref } from 'vue'

/** 通用倒计时：短信 60 秒冷却、练习/面试计时均可复用。 */
export function useCountdown(initialSeconds = 60) {
  const remaining = ref(0)
  let timer: number | null = null

  const running = computed(() => remaining.value > 0)
  const label = computed(() => (remaining.value > 0 ? `${remaining.value}s` : ''))

  function stop(): void {
    if (timer !== null) {
      window.clearInterval(timer)
      timer = null
    }
  }

  function start(seconds = initialSeconds): void {
    stop()
    remaining.value = seconds
    timer = window.setInterval(() => {
      remaining.value -= 1
      if (remaining.value <= 0) {
        stop()
      }
    }, 1000)
  }

  function reset(): void {
    stop()
    remaining.value = 0
  }

  onScopeDispose(stop)

  return { remaining, running, label, start, stop, reset }
}
