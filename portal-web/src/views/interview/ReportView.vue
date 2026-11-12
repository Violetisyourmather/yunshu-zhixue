<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ApiError } from '@/api/http'
import { getInterviewReport, type InterviewReport } from '@/api/interview'
import { difficultyLabel } from './session-storage'

const DIMENSIONS = [
  { key: '技术准确性', weight: 30 },
  { key: '知识广度', weight: 10 },
  { key: '原理理解', weight: 20 },
  { key: '工程实践', weight: 20 },
  { key: '逻辑结构', weight: 10 },
  { key: '表达清晰', weight: 10 }
]

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const loadError = ref('')
const report = ref<InterviewReport | null>(null)
const reviewOpen = ref(false)

const percentage = computed(() =>
  report.value ? Math.round(report.value.overallScore * 100) / 10 : 0
)
const ringOffset = computed(() => {
  const circle = 2 * Math.PI * 62
  return circle * (1 - percentage.value / 100)
})
const ringColor = computed(() =>
  percentage.value >= 80 ? 'var(--jade)' : percentage.value >= 65 ? 'var(--amber)' : 'var(--red)'
)
const headline = computed(() => {
  if (percentage.value >= 80) {
    return '整体表现不错，可以开始投递了'
  }
  if (percentage.value >= 65) {
    return '基础达标，还有两处明显短板'
  }
  return '建议先补基础，再回来面一次'
})
const trendText = computed(() => {
  const delta = report.value?.trendDelta
  if (delta == null) {
    return '首次面试'
  }
  const value = delta * 10
  if (Math.abs(value) < 0.05) {
    return '与上次持平'
  }
  return value > 0 ? `相比上次 +${value.toFixed(1)}` : `相比上次 ${value.toFixed(1)}`
})
const trendClass = computed(() => {
  const delta = report.value?.trendDelta
  if (delta == null || Math.abs(delta) < 0.005) {
    return 'badge-amber'
  }
  return delta > 0 ? 'badge-ok' : 'badge-bad'
})
const dimensionItems = computed(() =>
  DIMENSIONS.map((dimension) => {
    const score = report.value?.dimensions?.[dimension.key] ?? 0
    return { ...dimension, score, percent: Math.max(0, Math.min(100, score * 10)) }
  })
)
const accuracy = computed(() =>
  Math.round((report.value?.dimensions?.['技术准确性'] ?? 0) * 10 * 10) / 10
)

function messageOf(error: unknown, fallback: string): string {
  return error instanceof ApiError ? error.message : fallback
}

function formatDate(value: string | null): string {
  return value ? value.slice(0, 16) : '—'
}

function formatDuration(seconds: number): string {
  if (!seconds) {
    return '0 分钟'
  }
  if (seconds < 60) {
    return `${seconds} 秒`
  }
  return `${Math.max(1, Math.round(seconds / 60))} 分钟`
}

function dimensionTone(score: number): string {
  return score >= 8 ? 'score-good' : score >= 6 ? 'score-mid' : 'score-bad'
}

function cardLink(type: string): string {
  if (type === 'bank') {
    return '/bank'
  }
  if (type === 'route') {
    return '/routes'
  }
  return '/interview'
}

function cardLinkText(type: string): string {
  if (type === 'bank') {
    return '去练习 →'
  }
  if (type === 'route') {
    return '看路线 →'
  }
  return '再面一次 →'
}

async function load(): Promise<void> {
  const id = Number(route.params.id)
  if (!Number.isInteger(id) || id <= 0) {
    loadError.value = '报告编号不合法'
    loading.value = false
    return
  }
  loading.value = true
  loadError.value = ''
  try {
    report.value = await getInterviewReport(id)
  } catch (error) {
    loadError.value = messageOf(error, '面试报告加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

function tryAgain(): void {
  const current = report.value
  if (!current) {
    void router.push('/interview')
    return
  }
  void router.push({
    path: '/interview',
    query: {
      subjectId: String(current.subjectId),
      difficulty: String(current.difficulty),
      count: String(current.questionCount)
    }
  })
}

onMounted(() => {
  void load()
})
</script>

<template>
  <section class="report-page">
    <div v-if="loading" class="state-panel">正在加载面试报告…</div>

    <div v-else-if="loadError" class="state-panel state-error">
      <p>{{ loadError }}</p>
      <button type="button" class="btn btn-sm" @click="load">重新加载</button>
    </div>

    <template v-else-if="report">
      <header class="page-head">
        <div>
          <p class="eyebrow">Interview Report</p>
          <h1>面试报告</h1>
        </div>
        <div class="head-actions">
          <RouterLink class="btn" to="/interview/history">历史记录</RouterLink>
          <button type="button" class="btn btn-primary" @click="tryAgain">再来一次</button>
        </div>
      </header>

      <section class="report-head">
        <div class="ring-wrap">
          <svg width="150" height="150" viewBox="0 0 150 150" aria-hidden="true">
            <circle cx="75" cy="75" r="62" fill="none" stroke="var(--paper-2)" stroke-width="10" />
            <circle
              cx="75"
              cy="75"
              r="62"
              fill="none"
              :stroke="ringColor"
              stroke-width="10"
              stroke-linecap="round"
              :stroke-dasharray="2 * Math.PI * 62"
              :stroke-dashoffset="ringOffset"
              transform="rotate(-90 75 75)"
            />
          </svg>
          <div class="ring-value">
            <b>{{ percentage.toFixed(1) }}</b>
            <span>综合评分</span>
          </div>
        </div>

        <div class="report-info">
          <div class="meta-badges">
            <span class="badge badge-ink">{{ report.subjectName || '未知方向' }}</span>
            <span class="badge">{{ difficultyLabel(report.difficulty) }}</span>
            <span class="badge">{{ formatDate(report.completedAt) }}</span>
          </div>
          <h2>{{ headline }}</h2>
          <div class="head-stats">
            <span>回答准确度 <b>{{ accuracy.toFixed(1) }}</b></span>
            <span>面试时长 <b>{{ formatDuration(report.totalDuration) }}</b></span>
            <span>答题数量 <b>{{ report.questionCount }} 题</b></span>
            <span class="badge" :class="trendClass">{{ trendText }}</span>
          </div>
        </div>
      </section>

      <section class="report-section">
        <div class="section-head">
          <div>
            <p class="eyebrow">Dimensions</p>
            <h2>能力分析</h2>
          </div>
          <span class="small muted">六个维度，按权重计入综合评分</span>
        </div>
        <div class="dimension-grid">
          <div v-for="dimension in dimensionItems" :key="dimension.key" class="dimension-card">
            <div class="dimension-title">
              <span>{{ dimension.key }}</span>
              <span class="dimension-weight">权重 {{ dimension.weight }}%</span>
            </div>
            <div class="dimension-score" :class="dimensionTone(dimension.score)">
              {{ (dimension.score * 10).toFixed(0) }}<small>/100</small>
            </div>
            <div class="dimension-track">
              <i :style="{ width: `${dimension.percent}%` }" />
            </div>
          </div>
        </div>
      </section>

      <div class="pros-cons">
        <section class="pc-card good">
          <h3>优点</h3>
          <ul>
            <li v-for="(item, index) in report.strengths" :key="index">{{ item }}</li>
            <li v-if="!report.strengths.length">暂无</li>
          </ul>
        </section>
        <section class="pc-card bad">
          <h3>待提升</h3>
          <ul>
            <li v-for="(item, index) in report.weaknesses" :key="index">{{ item }}</li>
            <li v-if="!report.weaknesses.length">暂无</li>
          </ul>
        </section>
      </div>

      <section class="report-section">
        <div class="section-head">
          <div>
            <p class="eyebrow">Advice</p>
            <h2>改进建议</h2>
          </div>
        </div>
        <div class="advice-panel">
          <ol class="advice-list">
            <li v-for="(item, index) in report.suggestions" :key="index">
              <span class="advice-no">{{ String(index + 1).padStart(2, '0') }}</span>
              <span>{{ item }}</span>
            </li>
            <li v-if="!report.suggestions.length">暂无建议</li>
          </ol>
        </div>
      </section>

      <section class="report-section">
        <div class="section-head">
          <div>
            <p class="eyebrow">Next</p>
            <h2>能力提升建议</h2>
          </div>
        </div>
        <div class="improve-grid">
          <div v-for="(card, index) in report.improveCards" :key="index" class="improve-card">
            <h3>{{ card.title }}</h3>
            <p>{{ card.content }}</p>
            <RouterLink class="link small" :to="cardLink(card.type)">
              {{ cardLinkText(card.type) }}
            </RouterLink>
          </div>
        </div>
      </section>

      <div class="report-actions">
        <RouterLink class="btn" to="/interview/history">查看历史记录</RouterLink>
        <button type="button" class="btn" @click="reviewOpen = true">查看面试回放</button>
        <span class="small muted report-no">报告编号 {{ report.reportId }}</span>
      </div>
    </template>

    <Teleport to="body">
      <Transition name="review">
        <div v-if="reviewOpen && report" class="review-overlay" @click.self="reviewOpen = false">
          <section class="review-dialog" role="dialog" aria-modal="true">
            <header class="review-head">
              <div>
                <p class="eyebrow">Replay</p>
                <h2>面试回放</h2>
              </div>
              <button type="button" class="review-close" aria-label="关闭" @click="reviewOpen = false">✕</button>
            </header>
            <div class="review-list">
              <article v-for="item in report.questionReviews" :key="item.order" class="review-item">
                <div class="review-meta">
                  <span class="badge">第 {{ item.order }} 题</span>
                  <span class="review-score" :class="item.score >= 8 ? 'score-good' : item.score >= 6 ? 'score-mid' : 'score-bad'">
                    {{ item.score.toFixed(1) }} / 10
                  </span>
                </div>
                <h3>{{ item.questionText }}</h3>
                <p class="review-answer"><b>你的回答：</b>{{ item.studentAnswer || '未作答' }}</p>
                <p class="review-feedback"><b>评语：</b>{{ item.feedback || '暂无评语' }}</p>
              </article>
            </div>
            <footer class="review-foot">
              <button type="button" class="btn" @click="reviewOpen = false">关闭</button>
            </footer>
          </section>
        </div>
      </Transition>
    </Teleport>
  </section>
</template>

<style scoped>
.report-page {
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

.head-actions {
  display: flex;
  gap: 8px;
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

.report-head {
  display: flex;
  align-items: center;
  gap: 36px;
  margin-top: 22px;
  padding: 28px 32px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r);
  box-shadow: var(--shadow);
}

.ring-wrap {
  position: relative;
  width: 150px;
  height: 150px;
  flex: none;
}

.ring-value {
  position: absolute;
  inset: 0;
  display: grid;
  place-content: center;
  text-align: center;
}

.ring-value b {
  font-family: var(--font-display);
  font-size: 32px;
  line-height: 1;
}

.ring-value span {
  margin-top: 5px;
  color: var(--ink-3);
  font-size: 11px;
}

.report-info {
  flex: 1;
  min-width: 0;
}

.meta-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.badge {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border-radius: 6px;
  background: var(--paper-2);
  color: var(--ink-2);
  font-size: 11px;
  line-height: 1.6;
  white-space: nowrap;
}

.badge-ink {
  background: var(--ink);
  color: #fff;
}

.badge-ok {
  background: var(--jade-soft);
  color: var(--jade-2);
}

.badge-bad {
  background: var(--red-soft);
  color: var(--red);
}

.badge-amber {
  background: var(--amber-soft);
  color: var(--amber);
}

.report-info h2 {
  margin-top: 12px;
  font-family: var(--font-display);
  font-size: 24px;
  letter-spacing: 0;
}

.head-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 18px;
  margin-top: 14px;
  color: var(--ink-3);
  font-size: 12px;
}

.head-stats b {
  margin-left: 3px;
  color: var(--ink);
  font-family: var(--font-mono);
  font-size: 12px;
}

.report-section {
  margin-top: 34px;
}

.section-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 16px;
}

.section-head h2 {
  margin-top: 4px;
  font-family: var(--font-display);
  font-size: 24px;
  letter-spacing: 0;
}

.dimension-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.dimension-card {
  padding: 18px 20px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r-sm);
}

.dimension-title {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
  color: var(--ink-2);
  font-size: 12.5px;
}

.dimension-weight {
  color: var(--ink-4);
  font-size: 10.5px;
}

.dimension-score {
  margin-top: 8px;
  font-family: var(--font-display);
  font-size: 26px;
  line-height: 1.1;
}

.dimension-score small {
  margin-left: 2px;
  color: var(--ink-3);
  font-family: var(--font-ui);
  font-size: 11px;
}

.score-good {
  color: var(--jade);
}

.score-mid {
  color: var(--amber);
}

.score-bad {
  color: var(--red);
}

.dimension-track {
  height: 5px;
  margin-top: 10px;
  overflow: hidden;
  border-radius: 99px;
  background: var(--paper-2);
}

.dimension-track i {
  display: block;
  height: 100%;
  border-radius: 99px;
  background: var(--jade);
}

.pros-cons {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
  margin-top: 22px;
}

.pc-card {
  padding: 20px 22px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r-sm);
}

.pc-card h3 {
  font-size: 14px;
  letter-spacing: 0;
}

.pc-card ul {
  display: grid;
  gap: 8px;
  margin-top: 10px;
  padding-left: 18px;
  list-style: disc;
}

.pc-card li {
  color: var(--ink-2);
  font-size: 12.5px;
  line-height: 1.7;
}

.pc-card.good {
  border-top: 3px solid var(--jade);
}

.pc-card.bad {
  border-top: 3px solid var(--red);
}

.advice-panel {
  padding: 20px 24px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r-sm);
}

.advice-list {
  display: grid;
  gap: 12px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.advice-list li {
  display: flex;
  gap: 12px;
  color: var(--ink-2);
  font-size: 13px;
  line-height: 1.75;
}

.advice-no {
  flex: none;
  color: var(--ink-4);
  font-family: var(--font-mono);
  font-size: 11px;
}

.improve-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.improve-card {
  display: flex;
  flex-direction: column;
  min-height: 150px;
  padding: 20px 22px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r-sm);
}

.improve-card h3 {
  font-size: 14px;
  letter-spacing: 0;
}

.improve-card p {
  margin-top: 8px;
  color: var(--ink-3);
  font-size: 12.5px;
  line-height: 1.7;
}

.improve-card .link {
  margin-top: auto;
  padding-top: 14px;
}

.report-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 28px;
}

.report-no {
  margin-left: auto;
}

/* ---------- 回放弹窗 ---------- */
.review-overlay {
  position: fixed;
  inset: 0;
  z-index: 100;
  display: grid;
  place-items: center;
  padding: 20px;
  background: rgba(21, 24, 29, 0.46);
  overscroll-behavior: contain;
}

.review-dialog {
  display: flex;
  flex-direction: column;
  width: min(760px, 100%);
  max-height: calc(100vh - 40px);
  overflow: hidden;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: 18px;
  box-shadow: var(--shadow-lg);
}

.review-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 22px 26px 14px;
  border-bottom: 1px solid var(--line);
}

.review-head h2 {
  margin-top: 4px;
  font-family: var(--font-display);
  font-size: 24px;
  letter-spacing: 0;
}

.review-close {
  width: 32px;
  height: 32px;
  border: 0;
  border-radius: 50%;
  background: transparent;
  color: var(--ink-3);
  font-size: 15px;
}

.review-list {
  display: grid;
  gap: 12px;
  padding: 18px 26px;
  overflow-y: auto;
}

.review-item {
  padding: 16px;
  background: var(--surface-2);
  border: 1px solid var(--line);
  border-radius: 10px;
}

.review-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.review-score {
  font-family: var(--font-mono);
  font-size: 11.5px;
}

.review-item h3 {
  margin-top: 8px;
  font-size: 13.5px;
  line-height: 1.6;
  letter-spacing: 0;
}

.review-answer,
.review-feedback {
  margin-top: 8px;
  color: var(--ink-2);
  font-size: 12.5px;
  line-height: 1.7;
  white-space: pre-wrap;
}

.review-foot {
  display: flex;
  justify-content: flex-end;
  padding: 14px 26px 20px;
  border-top: 1px solid var(--line);
}

.review-enter-active,
.review-leave-active {
  transition: opacity 0.18s ease;
}

.review-enter-from,
.review-leave-to {
  opacity: 0;
}

@media (max-width: 960px) {
  .dimension-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .improve-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 700px) {
  .report-page {
    padding-top: 22px;
  }

  .page-head {
    flex-direction: column;
    align-items: flex-start;
    gap: 14px;
  }

  .report-head {
    flex-direction: column;
    align-items: flex-start;
    gap: 18px;
    padding: 22px;
  }

  .dimension-grid,
  .pros-cons {
    grid-template-columns: 1fr;
  }

  .report-actions {
    align-items: flex-start;
  }

  .report-no {
    width: 100%;
    margin-left: 0;
  }

  .review-head,
  .review-list,
  .review-foot {
    padding-left: 18px;
    padding-right: 18px;
  }
}
</style>
