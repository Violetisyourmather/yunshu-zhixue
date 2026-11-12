<script setup lang="ts">
import { onBeforeUnmount, ref, watch } from 'vue'

/** 首页平台数据递增动画；系统开启「减少动态效果」时直接展示目标值。 */
const props = withDefaults(
  defineProps<{
    value: number
    duration?: number
  }>(),
  { duration: 900 }
)

const display = ref(0)
let frame = 0

function animate(target: number): void {
  cancelAnimationFrame(frame)

  const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  if (reduceMotion || props.duration <= 0) {
    display.value = target
    return
  }

  const from = display.value
  const start = performance.now()
  const tick = (now: number): void => {
    const progress = Math.min(1, (now - start) / props.duration)
    const eased = 1 - Math.pow(1 - progress, 3)
    display.value = Math.round(from + (target - from) * eased)
    if (progress < 1) {
      frame = requestAnimationFrame(tick)
    }
  }
  frame = requestAnimationFrame(tick)
}

watch(
  () => props.value,
  (value) => animate(value),
  { immediate: true }
)

onBeforeUnmount(() => cancelAnimationFrame(frame))
</script>

<template>
  <span class="number-ticker">{{ display.toLocaleString('zh-CN') }}</span>
</template>

<style scoped>
.number-ticker {
  font-variant-numeric: tabular-nums;
}
</style>
