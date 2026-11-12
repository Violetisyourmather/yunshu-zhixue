<script setup lang="ts">
import { computed } from 'vue'
import type { RouteDetail } from '@/types/content'

const props = defineProps<{
  modelValue: boolean
  route: RouteDetail | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})

function formatHours(minutes: number): string {
  const hours = minutes / 60
  return `${Number.isInteger(hours) ? hours : hours.toFixed(1)} 小时`
}

function formatDuration(minutes: number): string {
  const hours = Math.floor(minutes / 60)
  const rest = minutes % 60
  if (hours === 0) {
    return `${rest} 分钟`
  }
  return rest === 0 ? `${hours} 小时` : `${hours} 小时 ${rest} 分`
}
</script>

<template>
  <el-dialog v-model="visible" title="学员视角预览" width="640px">
    <div v-if="route" class="preview">
      <p class="preview-eyebrow">学习路线</p>
      <h2 class="preview-title">{{ route.name }}</h2>
      <p class="preview-meta">
        {{ route.subjectName || '—' }}
        <span class="dot">·</span>
        {{ route.targetEducation || '不限' }}
        <span class="dot">·</span>
        {{ route.courseCount }} 门课
        <span class="dot">·</span>
        {{ formatHours(route.totalMinutes) }}
      </p>
      <p v-if="route.description" class="preview-desc">{{ route.description }}</p>

      <div class="preview-divider" />

      <div v-if="route.courses.length" class="preview-list">
        <div v-for="(course, index) in route.courses" :key="course.routeCourseId" class="preview-row">
          <span class="preview-index">{{ String(index + 1).padStart(2, '0') }}</span>
          <div class="preview-course">
            <span class="preview-name">{{ course.name }}</span>
            <span class="preview-sub">
              {{ course.bvid || '—' }} · {{ formatDuration(course.durationMinutes) }}
            </span>
          </div>
          <el-tag v-if="course.status === 2" size="small" type="warning" effect="plain">
            已下架
          </el-tag>
        </div>
      </div>
      <el-empty v-else description="该路线暂无课程" :image-size="72" />
    </div>

    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.preview {
  padding: 4px 4px 0;
}

.preview-eyebrow {
  color: var(--jade);
  font-family: var(--font-mono);
  font-size: 11px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.preview-title {
  margin: 8px 0 10px;
  font-family: var(--font-display);
  font-size: 28px;
  line-height: 1.25;
}

.preview-meta {
  color: var(--ink-3);
  font-size: 13px;
}

.preview-meta .dot {
  margin: 0 6px;
  color: var(--ink-4);
}

.preview-desc {
  margin-top: 12px;
  color: var(--ink-2);
  font-size: 13px;
  line-height: 1.7;
}

.preview-divider {
  margin: 18px 0 6px;
  border-top: 1px solid var(--line);
}

.preview-list {
  display: flex;
  flex-direction: column;
  max-height: 420px;
  overflow: auto;
  padding-right: 4px;
}

.preview-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 11px 2px;
  border-bottom: 1px dashed var(--line);
}

.preview-index {
  flex: none;
  color: var(--ink-4);
  font-family: var(--font-mono);
  font-size: 12px;
}

.preview-course {
  display: flex;
  flex-direction: column;
  gap: 3px;
  flex: 1;
  min-width: 0;
}

.preview-name {
  overflow: hidden;
  color: var(--ink);
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.preview-sub {
  color: var(--ink-3);
  font-family: var(--font-mono);
  font-size: 11px;
}
</style>
