<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Refresh } from '@element-plus/icons-vue'
import AdminPage from '@/components/AdminPage.vue'
import {
  getDashboardOverview,
  getQuestionDistribution,
  getRecentActivities,
  getUserGrowth,
  type DashboardOverview,
  type QuestionDistributionItem,
  type RecentActivityItem,
  type UserGrowthPoint
} from '@/api/dashboard'
import { ApiError } from '@/api/http'

const router = useRouter()

const CHART_W = 620
const CHART_H = 190
const CHART_PAD = 12
const RANGES = [7, 30, 90] as const
const ACTIVITY_SIZE = 6

const loading = ref(true)
const refreshing = ref(false)
const growthLoading = ref(false)
const activitiesLoading = ref(false)
const loadError = ref('')
const lastLoadedAt = ref('')

const overview = ref<DashboardOverview | null>(null)
const growth = ref<UserGrowthPoint[]>([])
const distribution = ref<QuestionDistributionItem[]>([])
const activities = ref<RecentActivityItem[]>([])
const range = ref<(typeof RANGES)[number]>(7)

function messageOf(error: unknown, fallback: string): string {
  return error instanceof ApiError ? error.message : fallback
}

function formatNumber(value: number): string {
  return value.toLocaleString('zh-CN')
}

/**
 * 后端 JacksonConfig 已统一输出 Asia/Shanghai 的 `yyyy-MM-dd HH:mm:ss`，
 * 这里只做展示裁剪，不能再次按时区换算，否则会多加 8 小时。
 */
function formatActivityTime(value: string | null): string {
  if (!value) {
    return '—'
  }
  return value.replace('T', ' ').slice(0, 16)
}

/** 本地当前时间，用于「数据加载于」提示。 */
function nowLabel(): string {
  const now = new Date()
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())} ${pad(now.getHours())}:${pad(now.getMinutes())}`
}

function activityDetail(row: RecentActivityItem): string {
  return [row.targetType, row.targetName].filter(Boolean).join(' · ') || '—'
}

const metrics = computed(() => {
  const data = overview.value
  if (!data) {
    return []
  }
  return [
    { label: '学科数量', value: data.subjectCount, delta: data.subjectDelta },
    { label: '题目数量', value: data.questionCount, delta: data.questionDelta },
    { label: '注册用户', value: data.studentCount, delta: data.studentDelta },
    { label: 'AI 面试次数', value: data.interviewCount, delta: data.interviewDelta }
  ]
})

/* ---------- 用户增长折线图（SVG，口径与原型一致） ---------- */

const chartPoints = computed(() => {
  const data = growth.value
  if (!data.length) {
    return []
  }
  const values = data.map((point) => point.count)
  const max = Math.max(...values)
  const min = Math.min(...values)
  const span = max - min
  return data.map((point, index) => {
    const x =
      data.length === 1
        ? CHART_W / 2
        : CHART_PAD + (index * (CHART_W - CHART_PAD * 2)) / (data.length - 1)
    const ratio = span === 0 ? 0.5 : (point.count - min) / span
    const y = CHART_H - CHART_PAD - ratio * (CHART_H - CHART_PAD * 2)
    return { x, y, point }
  })
})

const linePoints = computed(() =>
  chartPoints.value.map((point) => `${point.x.toFixed(1)},${point.y.toFixed(1)}`).join(' ')
)

const areaPath = computed(() => {
  const points = chartPoints.value
  if (!points.length) {
    return ''
  }
  const bottom = CHART_H - CHART_PAD
  const line = points.map((point) => `L ${point.x.toFixed(1)},${point.y.toFixed(1)}`).join(' ')
  return `M ${points[0].x.toFixed(1)},${bottom} ${line} L ${points[points.length - 1].x.toFixed(1)},${bottom} Z`
})

const axisLabels = computed(() => {
  const points = growth.value
  if (!points.length) {
    return []
  }
  const count = Math.min(5, points.length)
  const indices = new Set<number>()
  for (let index = 0; index < count; index += 1) {
    indices.add(Math.round((index * (points.length - 1)) / Math.max(1, count - 1)))
  }
  return Array.from(indices)
    .sort((left, right) => left - right)
    .map((index) => ({
      key: points[index].date,
      label: points[index].date.slice(5).replace('-', '/')
    }))
})

const growthSummary = computed(() => {
  const points = growth.value
  if (!points.length) {
    return ''
  }
  const peak = Math.max(...points.map((point) => point.count))
  const today = points[points.length - 1]?.count ?? 0
  return `峰值 ${peak} · 今日 ${today}`
})

/* ---------- 题目学科分布 ---------- */

const maxQuestionCount = computed(() =>
  distribution.value.reduce((max, item) => Math.max(max, item.questionCount), 0)
)

function barPercent(count: number): number {
  const max = maxQuestionCount.value
  return max <= 0 ? 0 : Math.round((count / max) * 100)
}

/* ---------- 数据加载 ---------- */

async function loadOverview(): Promise<void> {
  overview.value = await getDashboardOverview()
}

async function loadGrowth(): Promise<void> {
  growthLoading.value = true
  try {
    growth.value = await getUserGrowth(range.value)
  } finally {
    growthLoading.value = false
  }
}

async function loadDistribution(): Promise<void> {
  distribution.value = await getQuestionDistribution()
}

async function loadActivities(): Promise<void> {
  activitiesLoading.value = true
  try {
    const result = await getRecentActivities(1, ACTIVITY_SIZE)
    activities.value = result.list
  } finally {
    activitiesLoading.value = false
  }
}

async function loadAll(): Promise<void> {
  loading.value = true
  loadError.value = ''
  try {
    await Promise.all([loadOverview(), loadGrowth(), loadDistribution(), loadActivities()])
    lastLoadedAt.value = nowLabel()
  } catch (error) {
    loadError.value = messageOf(error, '看板数据加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

async function refreshAll(): Promise<void> {
  refreshing.value = true
  try {
    await loadAll()
  } finally {
    refreshing.value = false
  }
}

async function changeRange(value: (typeof RANGES)[number]): Promise<void> {
  if (range.value === value) {
    return
  }
  range.value = value
  try {
    await loadGrowth()
  } catch (error) {
    loadError.value = messageOf(error, '用户增长数据加载失败，请稍后重试')
  }
}

function createRoute(): void {
  void router.push({ path: '/content/routes', query: { new: '1' } })
}

onMounted(() => {
  void loadAll()
})
</script>

<template>
  <AdminPage title="数据看板" description="概览平台内容与用户的关键指标；统计卡缓存 5 分钟，最近活动实时更新。">
    <template #actions>
      <el-button :icon="Refresh" :loading="refreshing" @click="refreshAll">刷新数据</el-button>
    </template>

    <div v-loading="loading" class="dashboard">
      <el-alert
        v-if="loadError"
        :title="loadError"
        type="error"
        show-icon
        :closable="false"
        class="dashboard-alert"
      >
        <template #default>
          <el-button link type="primary" @click="loadAll">重新加载</el-button>
        </template>
      </el-alert>

      <section class="stat-grid">
        <article v-for="item in metrics" :key="item.label" class="stat-card">
          <p class="stat-label">{{ item.label }}</p>
          <p class="stat-value">{{ formatNumber(item.value) }}</p>
          <p class="stat-trend" :class="{ zero: item.delta === 0 }">
            较上月 <b>+{{ formatNumber(item.delta) }}</b>
          </p>
        </article>
      </section>

      <div class="panel-grid">
        <section class="panel chart-panel">
          <header class="panel-head">
            <div>
              <h3>用户增长趋势</h3>
              <p class="panel-sub">{{ growthSummary }}</p>
            </div>
            <div class="range-chips">
              <button
                v-for="value in RANGES"
                :key="value"
                type="button"
                class="range-chip"
                :class="{ active: range === value }"
                @click="changeRange(value)"
              >
                {{ value }} 天
              </button>
            </div>
          </header>

          <div v-loading="growthLoading" class="chart-box">
            <svg
              v-if="chartPoints.length"
              class="line-chart"
              :viewBox="`0 0 ${CHART_W} ${CHART_H}`"
              preserveAspectRatio="none"
              role="img"
              aria-label="用户增长趋势折线图"
            >
              <defs>
                <linearGradient id="dashGrowthFill" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0" stop-color="#0e6b55" stop-opacity="0.18" />
                  <stop offset="1" stop-color="#0e6b55" stop-opacity="0" />
                </linearGradient>
              </defs>
              <path :d="areaPath" fill="url(#dashGrowthFill)" stroke="none" />
              <polyline
                :points="linePoints"
                fill="none"
                stroke="#0e6b55"
                stroke-width="2"
                stroke-linejoin="round"
                stroke-linecap="round"
              />
            </svg>
            <p v-else-if="!growthLoading" class="chart-empty">暂无用户增长数据</p>
            <div v-if="axisLabels.length" class="axis-row">
              <span v-for="label in axisLabels" :key="label.key">{{ label.label }}</span>
            </div>
          </div>
        </section>

        <section class="panel">
          <header class="panel-head">
            <div>
              <h3>题目学科分布</h3>
              <p class="panel-sub">按学科统计未删除题目数</p>
            </div>
          </header>
          <div v-if="distribution.length" class="bar-list">
            <div v-for="item in distribution" :key="item.subjectId" class="bar-line">
              <span class="bar-name" :title="item.subjectName">{{ item.subjectName }}</span>
              <div class="bar-track">
                <i :style="{ width: `${barPercent(item.questionCount)}%` }" />
              </div>
              <span class="bar-num">{{ formatNumber(item.questionCount) }}</span>
            </div>
          </div>
          <p v-else class="chart-empty">暂无学科数据</p>
        </section>
      </div>

      <section class="panel quick-panel">
        <header class="panel-head">
          <h3>快捷操作</h3>
        </header>
        <div class="quick-actions">
          <el-button @click="router.push('/content/routes')">管理学习路线</el-button>
          <el-button type="primary" plain @click="createRoute">创建新路线</el-button>
          <el-button @click="router.push('/content/courses')">B站课程库</el-button>
        </div>
      </section>

      <section class="panel activity-panel">
        <header class="panel-head">
          <div>
            <h3>最近活动</h3>
            <p class="panel-sub">来自管理端操作日志</p>
          </div>
          <RouterLink v-permission="'system:log_view'" class="view-all" to="/system/logs">
            查看全部 →
          </RouterLink>
        </header>
        <el-table
          v-loading="activitiesLoading"
          :data="activities"
          class="activity-table"
          empty-text="暂无操作记录"
        >
          <el-table-column prop="username" label="用户" min-width="120" />
          <el-table-column prop="action" label="操作" min-width="120" />
          <el-table-column label="详情" min-width="220">
            <template #default="{ row }">
              <span class="cell-sub">{{ activityDetail(row as RecentActivityItem) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="时间" min-width="160">
            <template #default="{ row }">
              <span class="mono cell-sub">
                {{ formatActivityTime((row as RecentActivityItem).createdAt) }}
              </span>
            </template>
          </el-table-column>
        </el-table>
        <p v-if="lastLoadedAt" class="loaded-at">数据加载于 {{ lastLoadedAt }}</p>
      </section>
    </div>
  </AdminPage>
</template>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.dashboard-alert {
  border-radius: var(--r-sm);
}

/* ---------- 统计卡 ---------- */
.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.stat-card {
  padding: 18px 20px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r);
  box-shadow: var(--shadow);
}

.stat-label {
  font-size: 12px;
  letter-spacing: 0.04em;
  color: var(--ink-3);
}

.stat-value {
  margin-top: 8px;
  font-family: var(--font-display);
  font-size: 30px;
  line-height: 1.1;
  color: var(--ink);
}

.stat-trend {
  margin-top: 10px;
  font-size: 12px;
  color: var(--ink-3);
}

.stat-trend b {
  color: var(--jade);
  font-weight: 600;
}

.stat-trend.zero b {
  color: var(--ink-4);
}

/* ---------- 面板通用 ---------- */
.panel {
  padding: 18px 20px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r);
  box-shadow: var(--shadow);
  min-width: 0;
}

.panel-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(0, 1fr);
  gap: 16px;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.panel-head h3 {
  font-family: var(--font-display);
  font-size: 17px;
  line-height: 1.3;
}

.panel-sub {
  margin-top: 4px;
  font-size: 12px;
  color: var(--ink-3);
}

/* ---------- 折线图 ---------- */
.range-chips {
  display: flex;
  gap: 6px;
}

.range-chip {
  height: 26px;
  padding: 0 11px;
  font-size: 12px;
  color: var(--ink-3);
  background: var(--surface-2);
  border: 1px solid var(--line);
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.16s ease;
}

.range-chip:hover {
  color: var(--ink);
  border-color: var(--line-2);
}

.range-chip.active {
  color: var(--surface);
  background: var(--ink);
  border-color: var(--ink);
}

.chart-box {
  position: relative;
  min-height: 220px;
}

.line-chart {
  display: block;
  width: 100%;
  height: 190px;
}

.axis-row {
  display: flex;
  justify-content: space-between;
  margin-top: 6px;
  font-family: var(--font-mono);
  font-size: 11px;
  color: var(--ink-4);
}

.chart-empty {
  padding: 56px 0;
  text-align: center;
  font-size: 13px;
  color: var(--ink-4);
}

/* ---------- 学科分布 ---------- */
.bar-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding-top: 4px;
}

.bar-line {
  display: grid;
  grid-template-columns: minmax(88px, 34%) minmax(0, 1fr) 44px;
  align-items: center;
  gap: 10px;
}

.bar-name {
  overflow: hidden;
  font-size: 12px;
  color: var(--ink-2);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.bar-track {
  height: 8px;
  overflow: hidden;
  background: var(--paper-2);
  border-radius: 999px;
}

.bar-track i {
  display: block;
  height: 100%;
  background: linear-gradient(90deg, var(--jade), #3f947c);
  border-radius: inherit;
  transition: width 0.4s ease;
}

.bar-num {
  font-family: var(--font-mono);
  font-size: 12px;
  color: var(--ink-2);
  text-align: right;
}

/* ---------- 快捷操作 ---------- */
.quick-panel {
  padding-bottom: 18px;
}

.quick-panel .panel-head {
  margin-bottom: 10px;
}

.quick-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

/* ---------- 最近活动 ---------- */
.view-all {
  font-size: 12px;
  color: var(--jade);
}

.view-all:hover {
  color: var(--jade-2);
}

.activity-table {
  --el-table-border-color: var(--line);
  --el-table-header-bg-color: var(--surface-2);
  --el-table-row-hover-bg-color: var(--surface-2);
  width: 100%;
}

.activity-table :deep(th.el-table__cell) {
  background: var(--surface-2);
  color: var(--ink-2);
  font-weight: 600;
}

.activity-table :deep(td.el-table__cell) {
  border-bottom: 1px solid var(--line);
}

.cell-sub {
  color: var(--ink-3);
}

.loaded-at {
  margin-top: 10px;
  font-size: 11px;
  color: var(--ink-4);
  text-align: right;
}

@media (max-width: 1080px) {
  .stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .panel-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (max-width: 640px) {
  .stat-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .stat-value {
    font-size: 26px;
  }
}
</style>
