<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElPagination } from 'element-plus'
import { ApiError } from '@/api/http'
import {
  getInterviewRecords,
  getInterviewStats,
  type InterviewRecordItem,
  type InterviewStats
} from '@/api/interview'
import { difficultyLabel } from './session-storage'

const PAGE_SIZE = 10

const router = useRouter()

const loading = ref(true)
const recordsLoading = ref(false)
const statsError = ref('')
const recordsError = ref('')
const stats = ref<InterviewStats | null>(null)
const records = ref<InterviewRecordItem[]>([])
const total = ref(0)
const currentPage = ref(1)

function messageOf(error: unknown, fallback: string): string {
  return error instanceof ApiError ? error.message : fallback
}

function percentScore(value: number | null): string {
  if (value == null) {
    return '—'
  }
  return (value * 10).toFixed(1)
}

function totalDurationLabel(seconds: number): string {
  if (!seconds) {
    return '0 分钟'
  }
  if (seconds < 3600) {
    return `${Math.max(1, Math.round(seconds / 60))} 分钟`
  }
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.round((seconds % 3600) / 60)
  return minutes > 0 ? `${hours} 小时 ${minutes} 分` : `${hours} 小时`
}

function recordDuration(seconds: number | null): string {
  // 单场时长与「面试报告」保持同口径：不足 1 分钟按秒显示，避免 5 秒显示成「1 分钟」。
  const value = seconds ?? 0
  if (!value) {
    return '0 秒'
  }
  if (value < 60) {
    return `${value} 秒`
  }
  return totalDurationLabel(value)
}

function formatDate(value: string | null): string {
  return value ? value.slice(0, 16) : '—'
}

function scoreTone(value: number | null): string {
  if (value == null) {
    return ''
  }
  return value >= 8 ? 'score-good' : value >= 6 ? 'score-mid' : 'score-bad'
}

async function loadStats(): Promise<void> {
  statsError.value = ''
  try {
    stats.value = await getInterviewStats()
  } catch (error) {
    statsError.value = messageOf(error, '面试统计加载失败，请稍后重试')
  }
}

async function loadRecords(): Promise<void> {
  recordsLoading.value = true
  recordsError.value = ''
  try {
    const result = await getInterviewRecords(currentPage.value, PAGE_SIZE)
    records.value = result.list
    total.value = result.total
  } catch (error) {
    records.value = []
    total.value = 0
    recordsError.value = messageOf(error, '面试记录加载失败，请稍后重试')
  } finally {
    recordsLoading.value = false
  }
}

async function loadAll(): Promise<void> {
  loading.value = true
  await Promise.all([loadStats(), loadRecords()])
  loading.value = false
}

function handlePageChange(page: number): void {
  currentPage.value = page
  void loadRecords()
}

function viewReport(item: InterviewRecordItem): void {
  if (item.reportId) {
    void router.push(`/interview/report/${item.reportId}`)
  }
}

function tryAgain(item: InterviewRecordItem): void {
  void router.push({
    path: '/interview',
    query: {
      subjectId: String(item.subjectId),
      difficulty: String(item.difficulty),
      count: String(item.questionCount)
    }
  })
}

onMounted(() => {
  void loadAll()
})
</script>

<template>
  <section class="history-page">
    <header class="page-head">
      <div>
        <p class="eyebrow">Interview History</p>
        <h1>面试记录</h1>
        <p class="lead">每场面试的得分、时长与能力变化都保留在这里，可以随时回看报告。</p>
      </div>
      <RouterLink class="btn btn-primary" to="/interview">再面一次</RouterLink>
    </header>

    <div v-if="loading" class="state-panel">正在加载面试记录…</div>

    <template v-else>
      <div v-if="statsError" class="state-panel state-error">
        <p>{{ statsError }}</p>
        <button type="button" class="btn btn-sm" @click="loadStats">重新加载</button>
      </div>

      <div v-else-if="stats" class="stats-grid">
        <div class="stat-card">
          <div class="k">面试总次数</div>
          <div class="v">{{ stats.interviewTotal }}</div>
        </div>
        <div class="stat-card">
          <div class="k">平均分数</div>
          <div class="v">{{ percentScore(stats.avgScore) }}</div>
        </div>
        <div class="stat-card">
          <div class="k">最高分数</div>
          <div class="v">{{ percentScore(stats.maxScore) }}</div>
        </div>
        <div class="stat-card">
          <div class="k">累计时长</div>
          <div class="v small-value">{{ totalDurationLabel(stats.totalDuration) }}</div>
        </div>
      </div>

      <div v-if="recordsLoading && !records.length" class="state-panel">正在加载面试记录…</div>

      <div v-else-if="recordsError" class="state-panel state-error">
        <p>{{ recordsError }}</p>
        <button type="button" class="btn btn-sm" @click="loadRecords">重新加载</button>
      </div>

      <div v-else-if="records.length" class="record-list">
        <article v-for="item in records" :key="item.recordId" class="record-row">
          <div class="score-circle" :class="scoreTone(item.overallScore)">
            <b>{{ item.overallScore == null ? '—' : (item.overallScore * 10).toFixed(1) }}</b>
            <span>综合分</span>
          </div>
          <div class="record-info">
            <h2>{{ item.subjectName || '未知方向' }}</h2>
            <div class="record-meta">
              <span>{{ formatDate(item.completedAt) }}</span>
              <span>{{ recordDuration(item.totalDuration) }}</span>
              <span>{{ item.questionCount }} 题</span>
              <span>{{ difficultyLabel(item.difficulty) }}</span>
              <span v-if="!item.hasReport" class="badge badge-amber">报告生成中</span>
            </div>
          </div>
          <div class="record-actions">
            <button
              v-if="item.hasReport"
              type="button"
              class="btn btn-sm"
              @click="viewReport(item)"
            >
              查看报告
            </button>
            <button type="button" class="btn btn-sm btn-primary" @click="tryAgain(item)">
              再来一次
            </button>
          </div>
        </article>

        <div v-if="total > PAGE_SIZE" class="pager">
          <ElPagination
            :current-page="currentPage"
            :page-size="PAGE_SIZE"
            :total="total"
            layout="prev, pager, next"
            background
            @current-change="handlePageChange"
          />
        </div>
      </div>

      <div v-else class="empty-panel">
        <h2>还没有面试记录</h2>
        <p>完成一场 AI 模拟面试后，这里会显示得分、时长与能力报告。</p>
        <RouterLink class="btn btn-primary" to="/interview">开始第一场面试</RouterLink>
      </div>
    </template>
  </section>
</template>

<style scoped>
.history-page {
  padding: 40px 0 24px;
}

.page-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
}

.page-head h1 {
  margin-top: 6px;
  font-family: var(--font-display);
  font-size: clamp(30px, 3.2vw, 38px);
  letter-spacing: 0;
}

.page-head .lead {
  max-width: 640px;
  margin-top: 8px;
  color: var(--ink-2);
  font-size: 14px;
  line-height: 1.75;
}

.state-panel {
  margin-top: 20px;
  padding: 24px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r);
  color: var(--ink-3);
  font-size: 13px;
}

.state-error {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  color: var(--red);
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-top: 22px;
}

.stat-card {
  padding: 18px 20px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r);
}

.stat-card .k {
  color: var(--ink-3);
  font-size: 11.5px;
}

.stat-card .v {
  margin-top: 6px;
  font-family: var(--font-display);
  font-size: 30px;
  line-height: 1.1;
  letter-spacing: -0.01em;
  font-variant-numeric: tabular-nums;
}

.stat-card .small-value {
  font-size: 20px;
  line-height: 1.5;
}

.record-list {
  display: grid;
  gap: 10px;
  margin-top: 22px;
}

.record-row {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 18px 20px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r-sm);
  transition: border-color 0.16s ease, transform 0.16s ease;
}

.score-circle {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 58px;
  height: 58px;
  flex: none;
  border: 2px solid var(--line-2);
  border-radius: 50%;
}

.score-circle b {
  font-family: var(--font-mono);
  font-size: 13px;
  line-height: 1.2;
}

.score-circle span {
  color: var(--ink-4);
  font-size: 8.5px;
}

.score-good {
  border-color: var(--jade);
  color: var(--jade-2);
}

.score-mid {
  border-color: var(--amber);
  color: var(--amber);
}

.score-bad {
  border-color: var(--red);
  color: var(--red);
}

.record-info {
  flex: 1;
  min-width: 0;
}

.record-info h2 {
  font-family: var(--font-display);
  font-size: 18px;
  line-height: 1.4;
  letter-spacing: 0;
}

.record-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 5px;
  color: var(--ink-3);
  font-size: 11.5px;
}

.badge {
  display: inline-flex;
  align-items: center;
  padding: 2px 7px;
  border-radius: 5px;
  font-size: 10.5px;
}

.badge-amber {
  background: var(--amber-soft);
  color: var(--amber);
}

.record-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: none;
}

.pager {
  display: flex;
  justify-content: center;
  padding: 16px 0 4px;
}

.pager :deep(.el-pagination) {
  flex-wrap: wrap;
  justify-content: center;
  row-gap: 6px;
}

.empty-panel {
  margin-top: 24px;
  padding: 46px 24px;
  background: var(--surface-2);
  border: 1px dashed var(--line-2);
  border-radius: var(--r);
  text-align: center;
}

.empty-panel h2 {
  font-family: var(--font-display);
  font-size: 24px;
  letter-spacing: 0;
}

.empty-panel p {
  margin-top: 8px;
  color: var(--ink-3);
  font-size: 13px;
}

.empty-panel .btn {
  margin-top: 18px;
}

@media (hover: hover) {
  .record-row:hover {
    transform: translateY(-1px);
    border-color: var(--line-2);
  }
}

@media (max-width: 800px) {
  .history-page {
    padding-top: 24px;
  }

  .page-head {
    flex-direction: column;
    align-items: flex-start;
    gap: 14px;
  }

  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .record-row {
    flex-wrap: wrap;
  }

  .record-actions {
    width: 100%;
    justify-content: flex-end;
  }
}

@media (prefers-reduced-motion: reduce) {
  .record-row {
    transition: none;
  }
}
</style>
