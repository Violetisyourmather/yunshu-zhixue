<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter, type RouteLocationRaw } from 'vue-router'
import type { Component } from 'vue'
import { ArrowRight, Compass, Microphone, Refresh, RefreshLeft } from '@element-plus/icons-vue'
import NumberTicker from '@/components/NumberTicker.vue'
import {
  getHotSubjects,
  getLearningOverview,
  getPlatformStats,
  type HomeHotSubject,
  type LearningOverview,
  type PlatformStats,
  type WeekTrendPoint
} from '@/api/home'
import { completeProfileGuide } from '@/api/profile'
import { authBypassEnabled } from '@/auth/preview'
import { useUserStore } from '@/store/user'

interface QuickStart {
  icon: Component
  title: string
  description: string
  to: RouteLocationRaw
}

interface FeatureItem {
  index: string
  title: string
  description: string
  cta: string
  to: RouteLocationRaw
}

const router = useRouter()
const userStore = useUserStore()

const platformStats = ref<PlatformStats>({
  studentCount: 0,
  questionCount: 0,
  interviewCount: 0
})
const hotSubjects = ref<HomeHotSubject[]>([])
const learning = ref<LearningOverview | null>(null)
const loadingSubjects = ref(true)
const loadingLearning = ref(false)
const dataError = ref(false)

const metrics = computed(() => [
  { label: '精选面试题', value: platformStats.value.questionCount },
  { label: '注册学员', value: platformStats.value.studentCount },
  { label: 'AI 面试次数', value: platformStats.value.interviewCount }
])

const reportDims = [
  { label: '技术准确性', value: 84 },
  { label: '原理理解', value: 78 },
  { label: '工程实践', value: 81 },
  { label: '表达清晰', value: 73 }
]

const quickStarts: QuickStart[] = [
  {
    icon: RefreshLeft,
    title: '继续上次学习',
    description: '回到未完成的练习，接着答',
    to: { path: '/practice', query: { resume: '1' } }
  },
  {
    icon: Compass,
    title: '规划学习路线',
    description: '按方向看完整课程顺序',
    to: '/routes'
  },
  {
    icon: Refresh,
    title: '随机练习 10 题',
    description: '从题库里随机抽一组，先热个身',
    to: { path: '/practice', query: { mode: 'random', count: '10' } }
  },
  {
    icon: Microphone,
    title: '开始 AI 面试',
    description: '按方向与难度生成一场模拟面试',
    to: '/interview'
  }
]

const features: FeatureItem[] = [
  {
    index: '01',
    title: '学习路线',
    description: '每个方向一条有序课程路线，点开即跳 B 站，不用自己拼凑资料。',
    cta: '查看路线 →',
    to: '/routes'
  },
  {
    index: '02',
    title: '海量题库',
    description: '按学科与阶段组织的判断题、单选、多选与简答，答完即时判分并给出解析。',
    cta: '进入题库 →',
    to: '/bank'
  },
  {
    index: '03',
    title: '刷题练习',
    description: '顺序或随机练习，答题卡跳题、计时、断点续做，答错自动进错题本。',
    cta: '开始练习 →',
    to: '/bank'
  },
  {
    index: '04',
    title: 'AI 模拟面试',
    description: '选择方向、难度与题数，逐题作答，AI 按要点给出 0~10 分与反馈。',
    cta: '配置面试 →',
    to: '/interview'
  }
]

const guideSteps = [
  { title: '选择技术方向', description: '先看学习路线，知道自己要学什么。' },
  { title: '开始刷题练习', description: '一题一判，答错自动进错题本。' },
  { title: 'AI 面试检验', description: '拿到六维度报告，再回来补薄弱项。' }
]

const GUIDE_KEY = 'yszx_portal_guide_finished'
const guideOpen = ref(false)
const guideStartButton = ref<HTMLButtonElement | null>(null)
const guideModal = ref<HTMLElement | null>(null)
let guideTimer: number | undefined
let guideReturnFocus: HTMLElement | null = null

function localDateKey(date: Date): string {
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${date.getFullYear()}-${month}-${day}`
}

function buildEmptyWeek(): WeekTrendPoint[] {
  const today = new Date()
  return Array.from({ length: 7 }, (_, index) => {
    const date = new Date(today)
    date.setDate(today.getDate() - (6 - index))
    return { date: localDateKey(date), count: 0 }
  })
}

function shortDate(value: string): string {
  const datePart = value.slice(0, 10)
  const matched = datePart.match(/^\d{4}-(\d{2})-(\d{2})$/)
  return matched ? `${matched[1]}-${matched[2]}` : value
}

const weekTrend = computed(() => {
  const points = learning.value?.weekTrend?.length ? learning.value.weekTrend : buildEmptyWeek()
  const todayKey = localDateKey(new Date())
  const todayShort = shortDate(todayKey)
  return points.map((point) => {
    const datePart = point.date.slice(0, 10)
    return {
      label: shortDate(point.date),
      count: point.count,
      today: datePart === todayKey || datePart === todayShort
    }
  })
})

const weekMax = computed(() => Math.max(1, ...weekTrend.value.map((point) => point.count)))

function barHeight(count: number): string {
  return `${Math.max(4, Math.round((count / weekMax.value) * 100))}%`
}

const learningStats = computed(
  () =>
    learning.value?.learningStats ?? {
      consecutiveDays: 0,
      accuracyRate: 0,
      totalPracticeCount: 0
    }
)

const todayCount = computed(() => learning.value?.todayCount ?? 0)
const todayTarget = 15
const remainingToday = computed(() => Math.max(0, todayTarget - todayCount.value))

const todayGoalText = computed(() => {
  if (remainingToday.value > 0) {
    return `今日已刷 ${todayCount.value} 题 · 再答 ${remainingToday.value} 题达成今日目标`
  }
  return `今日已刷 ${todayCount.value} 题 · 今日目标已达成`
})

function formatAccuracy(value: number): string {
  return Number.isInteger(value) ? `${value}%` : `${value.toFixed(1)}%`
}

function subjectLink(subject: HomeHotSubject): RouteLocationRaw {
  return { path: '/bank', query: { subjectId: String(subject.subjectId) } }
}

function hasFinishedGuide(): boolean {
  try {
    return localStorage.getItem(GUIDE_KEY) === '1'
  } catch {
    return false
  }
}

function markGuideFinished(): void {
  try {
    localStorage.setItem(GUIDE_KEY, '1')
  } catch {
    // 本地存储不可用时仅本次会话内不再重复弹出
  }
}

function maybeShowGuide(): void {
  if (hasFinishedGuide()) {
    return
  }
  if (learning.value?.guideFinished) {
    markGuideFinished()
    return
  }
  if (new URLSearchParams(window.location.search).get('nou') === '1') {
    return
  }
  guideTimer = window.setTimeout(() => {
    guideOpen.value = true
  }, 420)
}

function closeGuide(): void {
  markGuideFinished()
  if (!authBypassEnabled && userStore.isLoggedIn) {
    void completeProfileGuide().catch(() => {
      // 引导状态同步失败不影响本次关闭，下次登录会再次补同步
    })
  }
  guideOpen.value = false
}

function startLearning(): void {
  closeGuide()
  void router.push('/routes')
}

function trapGuideFocus(event: KeyboardEvent): void {
  if (event.key !== 'Tab' || !guideOpen.value) {
    return
  }
  const modal = guideModal.value
  if (!modal) {
    return
  }
  const focusable = Array.from(
    modal.querySelectorAll<HTMLElement>(
      'button:not([disabled]), [href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])'
    )
  )
  if (!focusable.length) {
    return
  }
  const first = focusable[0]
  const last = focusable[focusable.length - 1]
  if (event.shiftKey && document.activeElement === first) {
    event.preventDefault()
    last.focus()
  } else if (!event.shiftKey && document.activeElement === last) {
    event.preventDefault()
    first.focus()
  }
}

function emptyLearningOverview(): LearningOverview {
  return {
    learningStats: {
      consecutiveDays: 0,
      accuracyRate: 0,
      totalPracticeCount: 0
    },
    weekTrend: buildEmptyWeek(),
    todayCount: 0,
    guideFinished: false
  }
}

async function loadHomeData(): Promise<void> {
  let failed = false
  dataError.value = false
  loadingSubjects.value = true
  const [statsResult, subjectsResult] = await Promise.allSettled([getPlatformStats(), getHotSubjects()])

  if (statsResult.status === 'fulfilled') {
    platformStats.value = statsResult.value
  } else {
    failed = true
  }

  if (subjectsResult.status === 'fulfilled') {
    hotSubjects.value = subjectsResult.value
  } else {
    hotSubjects.value = []
    failed = true
  }
  loadingSubjects.value = false

  if (authBypassEnabled) {
    // 预览模式没有真实令牌，学习概览保持空态，不展示虚构统计
    learning.value = emptyLearningOverview()
  } else if (userStore.isLoggedIn) {
    loadingLearning.value = true
    try {
      learning.value = await getLearningOverview()
    } catch {
      learning.value = emptyLearningOverview()
      failed = true
    } finally {
      loadingLearning.value = false
    }
  }

  dataError.value = failed
  maybeShowGuide()
}

watch(guideOpen, async (open) => {
  if (open) {
    guideReturnFocus = document.activeElement instanceof HTMLElement ? document.activeElement : null
    document.body.style.overflow = 'hidden'
    await nextTick()
    guideStartButton.value?.focus()
  } else {
    document.body.style.overflow = ''
    if (guideReturnFocus?.isConnected) {
      guideReturnFocus.focus()
    }
    guideReturnFocus = null
  }
})

onMounted(() => {
  void loadHomeData()
})

onBeforeUnmount(() => {
  if (guideTimer) {
    window.clearTimeout(guideTimer)
  }
  document.body.style.overflow = ''
})
</script>

<template>
  <div class="home-page">
    <section class="hero">
      <div class="hero-copy">
        <p class="eyebrow">AI 面试训练平台 · YUNSHU ACADEMY</p>
        <h1>把一条学习路线<br />走到<em>面试桌上</em></h1>
        <p class="lead">
          学完不等于会答。这里用「路线 + 刷题 + AI 面试」把掌握程度拆开来看：哪一阶段没学透、哪一类题总出错、面试里哪个维度在拖分。
        </p>
        <div class="hero-actions">
          <RouterLink class="btn btn-primary btn-lg" to="/bank">海量题库</RouterLink>
          <RouterLink class="btn btn-lg" to="/interview">开始 AI 面试</RouterLink>
        </div>
        <div class="hero-meta">
          <div v-for="item in metrics" :key="item.label">
            <div class="num"><NumberTicker :value="item.value" /></div>
            <div class="lbl">{{ item.label }}</div>
          </div>
        </div>
        <p v-if="dataError" class="data-error">
          部分首页数据加载失败，请稍后重试。
          <button type="button" class="data-retry" @click="loadHomeData">重新加载</button>
        </p>
      </div>

      <div class="hero-art" aria-hidden="true">
        <div class="halo" />
        <div class="float-tag t1"><span class="dot" />面试进行中 · 第 4 / 10 题</div>
        <div class="float-tag t2">错题本 · 今日新增 3 道</div>

        <div class="score-card">
          <div class="top">
            <span>AI 面试报告</span>
            <span>2026-11-06</span>
          </div>
          <div class="ring">
            <div class="big">82.4</div>
            <div class="ring-aside">
              <span class="badge badge-jade">较上次 +6.1</span>
              <div class="ring-sub">AI 应用与智能体开发 · 中等</div>
            </div>
          </div>
          <div class="dims">
            <div v-for="dim in reportDims" :key="dim.label" class="dim">
              <span>{{ dim.label }}</span>
              <div class="bar"><i :style="{ width: `${dim.value}%` }" /></div>
              <span class="mono">{{ (dim.value / 10).toFixed(1) }}</span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="home-section">
      <div class="section-head">
        <div>
          <p class="eyebrow">Quick Start</p>
          <h2>不知道从哪开始，就先做这四件事</h2>
        </div>
        <RouterLink class="section-link" to="/bank">查看全部 →</RouterLink>
      </div>
      <div class="quick-grid">
        <RouterLink v-for="item in quickStarts" :key="item.title" class="quick-card" :to="item.to">
          <span class="ico"><component :is="item.icon" /></span>
          <h3>{{ item.title }}</h3>
          <p>{{ item.description }}</p>
          <span class="go">→</span>
        </RouterLink>
      </div>
    </section>

    <section class="home-section">
      <div class="section-head">
        <div>
          <p class="eyebrow">Core</p>
          <h2>四个模块，一条闭环</h2>
          <p>学 → 练 → 测 → 评，每一步都留下可回看的数据。</p>
        </div>
      </div>
      <div class="feature-grid">
        <RouterLink v-for="item in features" :key="item.index" class="feature-card" :to="item.to">
          <span class="n">{{ item.index }}</span>
          <h3>{{ item.title }}</h3>
          <p>{{ item.description }}</p>
          <span class="foot">{{ item.cta }}</span>
        </RouterLink>
      </div>
    </section>

    <section class="home-section">
      <div class="section-head">
        <div>
          <p class="eyebrow">Progress</p>
          <h2>你的学习节奏</h2>
          <p>每一次作答都会留下记录，统计以服务端数据为准。</p>
        </div>
      </div>

      <div v-if="!userStore.isLoggedIn" class="guest-panel">
        <div>
          <h3>登录后，看见自己的学习节奏</h3>
          <p>连续学习天数、平均正确率和近 7 天练习量会按服务端记录汇总在这里。</p>
        </div>
        <div class="guest-actions">
          <RouterLink class="btn btn-primary" to="/login">登录</RouterLink>
          <RouterLink class="btn" to="/register">注册</RouterLink>
        </div>
      </div>

      <div v-else class="stats-panel" :aria-busy="loadingLearning">
        <div class="stats-left">
          <div class="stat-line">
            <span class="k">连续学习天数</span>
            <span class="v">{{ learningStats.consecutiveDays }}<small> 天</small></span>
          </div>
          <div class="stat-line">
            <span class="k">平均正确率</span>
            <span class="v">{{ formatAccuracy(learningStats.accuracyRate) }}</span>
          </div>
          <div class="stat-line">
            <span class="k">累计练习</span>
            <span class="v">{{ learningStats.totalPracticeCount.toLocaleString('zh-CN') }}<small> 题</small></span>
          </div>
          <p class="stat-note">{{ todayGoalText }}</p>
        </div>

        <div class="stats-right">
          <div class="between">
            <span class="small muted">近 7 天练习量</span>
            <span class="badge badge-jade">今日 {{ todayCount }} 题</span>
          </div>
          <div
            class="bars"
            role="img"
            :aria-label="`近 7 天练习量：${weekTrend.map((point) => `${point.label} ${point.count} 题`).join('，')}`"
          >
            <div v-for="point in weekTrend" :key="point.label" class="bar-col" :class="{ today: point.today }">
              <div class="bar-track">
                <div class="bar-fill" :style="{ height: barHeight(point.count) }">
                  <span class="bar-value">{{ point.count }}</span>
                </div>
              </div>
              <span class="bar-day">{{ point.label }}</span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="home-section">
      <div class="section-head">
        <div>
          <p class="eyebrow">Hot Subjects</p>
          <h2>热门学科</h2>
        </div>
        <RouterLink class="section-link" to="/bank">全部题库 →</RouterLink>
      </div>

      <div v-if="loadingSubjects" class="stage-grid" aria-hidden="true">
        <div v-for="index in 6" :key="index" class="stage-card skeleton-card">
          <span class="skeleton-block skeleton-icon" />
          <div class="skeleton-lines">
            <span class="skeleton-block skeleton-line wide" />
            <span class="skeleton-block skeleton-line" />
          </div>
        </div>
      </div>

      <div v-else-if="hotSubjects.length" class="stage-grid">
        <RouterLink
          v-for="subject in hotSubjects"
          :key="subject.subjectId"
          class="stage-card"
          :to="subjectLink(subject)"
        >
          <span class="ico" aria-hidden="true">{{ subject.icon }}</span>
          <div class="stage-copy">
            <h3>{{ subject.subjectName }}</h3>
            <p>{{ subject.stageCount }} 个阶段 · {{ subject.questionCount }} 道题目</p>
          </div>
          <span class="arrow" aria-hidden="true"><ArrowRight /></span>
        </RouterLink>
      </div>

      <div v-else class="empty-panel">热门学科正在整理中，稍后再来看看。</div>
    </section>

    <section class="home-section">
      <div class="interview-band">
        <div class="band-copy">
          <p class="eyebrow band-eyebrow">AI Interview</p>
          <h2>面试不是考试，<br />是一次可以重来的体检</h2>
          <p class="band-lead">
            每一题给出 0~10 分与要点命中，整场结束再汇总六个维度。你可以随时重来，直到某个维度的分数不再拖后腿。
          </p>
          <ul class="sell-points">
            <li>智能出题：按方向与难度组卷</li>
            <li>实时评分：每题即时给分</li>
            <li>面试报告：六维度能力画像</li>
            <li>知识覆盖：八大学科方向</li>
          </ul>
          <RouterLink class="btn btn-primary" to="/interview">立即体验</RouterLink>
        </div>

        <div class="demo-chat" aria-hidden="true">
          <div class="demo-msg ai">
            请说明 Agent 中「工具调用」的完整链路，并指出两个容易出错的边界。
          </div>
          <div class="demo-msg me">
            模型先选择工具并生成结构化参数，运行时校验后执行，再把结果回填上下文决定下一步。边界主要是参数校验失败和死循环……
          </div>
          <div class="demo-msg ai">
            要点命中 3 / 4 · 补充追问：如果工具超时，你会怎么处理？
            <span class="demo-score">本题得分 8.5 / 10</span>
          </div>
        </div>
      </div>
    </section>

    <Teleport to="body">
      <Transition name="overlay">
        <div
          v-if="guideOpen"
          class="guide-overlay"
          @click.self="closeGuide"
          @keydown.esc="closeGuide"
          @keydown="trapGuideFocus"
        >
          <section
            ref="guideModal"
            class="guide-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="guide-title"
          >
            <header class="guide-head">
              <div>
                <p class="eyebrow">Getting Started</p>
                <h2 id="guide-title">三步开始</h2>
              </div>
              <button type="button" class="guide-close" aria-label="关闭引导" @click="closeGuide">✕</button>
            </header>

            <div class="guide-body">
              <div
                v-for="(step, index) in guideSteps"
                :key="step.title"
                class="guide-step"
                :class="{ active: index === 0 }"
              >
                <span class="guide-no">{{ index + 1 }}</span>
                <div>
                  <h3>{{ step.title }}</h3>
                  <p>{{ step.description }}</p>
                </div>
              </div>
            </div>

            <footer class="guide-foot">
              <button type="button" class="btn" @click="closeGuide">跳过引导</button>
              <button ref="guideStartButton" type="button" class="btn btn-primary" @click="startLearning">
                开始学习
              </button>
            </footer>
          </section>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<style scoped>
.home-page {
  padding: 26px 0 8px;
}

.home-section {
  margin-top: 64px;
}

.section-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 22px;
}

.section-head h2 {
  margin-top: 4px;
  font-family: var(--font-display);
  font-size: clamp(24px, 2.7vw, 32px);
  line-height: 1.3;
  letter-spacing: 0;
  text-wrap: balance;
}

.section-head p:not(.eyebrow) {
  margin-top: 6px;
  color: var(--ink-3);
  font-size: 13.5px;
  line-height: 1.7;
  text-wrap: pretty;
}

.section-link {
  flex: none;
  color: var(--jade);
  font-size: 12.5px;
  font-weight: 500;
}

/* ---------- Hero ---------- */
.hero {
  display: grid;
  grid-template-columns: 1.12fr 0.88fr;
  align-items: center;
  gap: 48px;
  padding: 26px 0 8px;
}

.hero h1 {
  margin-top: 10px;
  font-family: var(--font-display);
  font-size: clamp(34px, 4.4vw, 56px);
  line-height: 1.16;
  letter-spacing: 0;
  text-wrap: balance;
}

.hero h1 em {
  font-style: normal;
  background: linear-gradient(transparent 68%, rgba(14, 107, 85, 0.18) 68%);
}

.hero .lead {
  max-width: 540px;
  margin-top: 18px;
  color: var(--ink-2);
  font-size: 15px;
  line-height: 1.8;
  text-wrap: pretty;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 28px;
}

.hero-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 30px;
  margin-top: 40px;
  padding-top: 22px;
  border-top: 1px solid var(--line);
}

.hero-meta .num {
  font-family: var(--font-display);
  font-size: 28px;
  line-height: 1.1;
  letter-spacing: -0.01em;
}

.hero-meta .lbl {
  margin-top: 3px;
  color: var(--ink-3);
  font-size: 11.5px;
  letter-spacing: 0.04em;
}

.data-error {
  margin-top: 14px;
  color: var(--red);
  font-size: 11.5px;
}

.data-retry {
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--jade);
  font-size: 11.5px;
  font-weight: 500;
  text-decoration: underline;
  text-underline-offset: 3px;
}

.hero-art {
  position: relative;
  display: grid;
  place-items: center;
  min-height: 380px;
}

.hero-art .halo {
  position: absolute;
  inset: 8% 4%;
  border-radius: 50%;
  background: radial-gradient(circle at 50% 40%, rgba(14, 107, 85, 0.16), transparent 62%);
  filter: blur(6px);
}

.score-card {
  position: relative;
  width: min(340px, 100%);
  padding: 22px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: 18px;
  box-shadow: var(--shadow-lg);
  transform: rotate(-1.4deg);
}

.score-card .top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: var(--ink-3);
  font-size: 11.5px;
  letter-spacing: 0.06em;
}

.score-card .ring {
  display: flex;
  align-items: flex-end;
  gap: 14px;
  margin: 16px 0 6px;
}

.score-card .big {
  font-family: var(--font-display);
  font-size: 52px;
  line-height: 1;
  letter-spacing: -0.02em;
}

.ring-aside {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
  padding-bottom: 6px;
}

.ring-sub {
  color: var(--ink-3);
  font-size: 11.5px;
}

.score-card .dims {
  display: grid;
  gap: 9px;
  margin-top: 18px;
}

.score-card .dim {
  display: grid;
  grid-template-columns: 62px 1fr 30px;
  align-items: center;
  gap: 10px;
  color: var(--ink-2);
  font-size: 11.5px;
}

.score-card .bar {
  height: 5px;
  overflow: hidden;
  border-radius: 99px;
  background: var(--paper-2);
}

.score-card .bar i {
  display: block;
  height: 100%;
  border-radius: 99px;
  background: var(--jade);
}

.float-tag {
  position: absolute;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 8px 12px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: 10px;
  box-shadow: var(--shadow);
  color: var(--ink-2);
  font-size: 11.5px;
}

.float-tag .dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--jade);
  box-shadow: 0 0 0 4px var(--jade-soft);
}

.float-tag.t1 {
  top: 18px;
  right: 4px;
  transform: rotate(2deg);
}

.float-tag.t2 {
  bottom: 26px;
  left: 0;
  transform: rotate(-2deg);
}

/* ---------- 快速开始 ---------- */
.quick-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
}

.quick-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-height: 184px;
  padding: 20px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r);
  transition: transform 0.18s cubic-bezier(0.16, 1, 0.3, 1), border-color 0.18s ease,
    box-shadow 0.18s ease;
}

.quick-card .ico {
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  border-radius: 10px;
  background: var(--jade-soft);
  color: var(--jade);
  font-size: 19px;
}

.quick-card h3 {
  font-size: 14px;
  line-height: 1.5;
  letter-spacing: 0;
}

.quick-card p {
  color: var(--ink-3);
  font-size: 12.5px;
  line-height: 1.65;
}

.quick-card .go {
  margin-top: auto;
  color: var(--jade);
  font-size: 12px;
  font-weight: 500;
}

/* ---------- 核心功能 ---------- */
.feature-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
}

.feature-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-height: 204px;
  padding: 24px 22px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r);
  transition: transform 0.18s cubic-bezier(0.16, 1, 0.3, 1), border-color 0.18s ease,
    box-shadow 0.18s ease;
}

.feature-card .n {
  color: var(--ink-4);
  font-family: var(--font-mono);
  font-size: 11px;
  letter-spacing: 0.1em;
}

.feature-card h3 {
  margin-top: 2px;
  font-size: 16px;
  line-height: 1.45;
  letter-spacing: 0;
}

.feature-card p {
  color: var(--ink-3);
  font-size: 12.5px;
  line-height: 1.7;
  text-wrap: pretty;
}

.feature-card .foot {
  margin-top: auto;
  color: var(--jade);
  font-size: 12px;
}

/* ---------- 学习节奏 ---------- */
.guest-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 32px;
  background: var(--surface-2);
  border: 1px dashed var(--line-2);
  border-radius: var(--r);
}

.guest-panel h3 {
  font-size: 16px;
  line-height: 1.5;
  letter-spacing: 0;
}

.guest-panel p {
  margin-top: 6px;
  color: var(--ink-3);
  font-size: 13px;
  line-height: 1.7;
}

.guest-actions {
  display: flex;
  gap: 10px;
  flex: none;
}

.stats-panel {
  display: grid;
  grid-template-columns: 300px 1fr;
  overflow: hidden;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r);
}

.stats-left {
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding: 24px;
  border-right: 1px solid var(--line);
}

.stat-line {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
}

.stat-line .k {
  color: var(--ink-3);
  font-size: 12.5px;
}

.stat-line .v {
  font-family: var(--font-display);
  font-size: 26px;
  line-height: 1.1;
  letter-spacing: -0.01em;
}

.stat-line .v small {
  color: var(--ink-3);
  font-family: var(--font-ui);
  font-size: 11.5px;
  font-weight: 400;
  letter-spacing: 0;
}

.stat-note {
  margin-top: 2px;
  color: var(--ink-3);
  font-size: 11.5px;
  line-height: 1.65;
}

.stats-right {
  padding: 22px 24px 16px;
}

.between {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.small {
  font-size: 12px;
}

.muted {
  color: var(--ink-3);
}

.badge {
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 8px;
  border-radius: 6px;
  background: var(--paper-2);
  color: var(--ink-2);
  font-size: 11.5px;
  font-weight: 500;
  white-space: nowrap;
}

.badge-jade {
  background: var(--jade-soft);
  color: var(--jade-2);
}

.bars {
  display: flex;
  align-items: flex-end;
  gap: 18px;
  height: 150px;
  margin-top: 24px;
}

.bar-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  height: 100%;
  justify-content: flex-end;
}

.bar-track {
  display: flex;
  align-items: flex-end;
  justify-content: center;
  width: 100%;
  height: 100%;
}

.bar-fill {
  position: relative;
  width: 100%;
  max-width: 42px;
  background: var(--paper-2);
  border-radius: 6px 6px 2px 2px;
  transition: background-color 0.2s ease;
}

.bar-col.today .bar-fill {
  background: var(--jade);
}

.bar-value {
  position: absolute;
  top: -20px;
  left: 50%;
  transform: translateX(-50%);
  color: var(--ink-3);
  font-family: var(--font-mono);
  font-size: 11px;
}

.bar-day {
  color: var(--ink-4);
  font-size: 11px;
}

/* ---------- 热门学科 ---------- */
.stage-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
}

.stage-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 20px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r);
  transition: transform 0.16s ease, border-color 0.16s ease;
}

.stage-card .ico {
  display: grid;
  place-items: center;
  flex: none;
  width: 44px;
  height: 44px;
  border-radius: 11px;
  background: var(--paper);
  font-size: 20px;
}

.stage-copy {
  min-width: 0;
}

.stage-copy h3 {
  font-size: 14px;
  line-height: 1.45;
  letter-spacing: 0;
}

.stage-copy p {
  margin-top: 2px;
  color: var(--ink-3);
  font-size: 12px;
}

.stage-card .arrow {
  display: flex;
  margin-left: auto;
  color: var(--ink-4);
}

.stage-card .arrow svg {
  width: 15px;
  height: 15px;
}

.skeleton-card {
  pointer-events: none;
}

.skeleton-lines {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
}

.skeleton-block {
  display: block;
  border-radius: 8px;
  background: linear-gradient(90deg, var(--paper-2) 25%, var(--surface-2) 37%, var(--paper-2) 63%);
  background-size: 400% 100%;
  animation: shimmer 1.4s ease infinite;
}

.skeleton-icon {
  width: 44px;
  height: 44px;
  flex: none;
}

.skeleton-line {
  height: 10px;
  width: 45%;
}

.skeleton-line.wide {
  width: 72%;
}

@keyframes shimmer {
  0% {
    background-position: 100% 0;
  }
  100% {
    background-position: 0 0;
  }
}

.empty-panel {
  padding: 36px;
  background: var(--surface-2);
  border: 1px dashed var(--line-2);
  border-radius: var(--r);
  color: var(--ink-3);
  font-size: 13px;
  text-align: center;
}

/* ---------- AI 面试 ---------- */
.interview-band {
  position: relative;
  display: grid;
  grid-template-columns: 1fr 420px;
  align-items: center;
  gap: 48px;
  overflow: hidden;
  padding: 44px;
  background: var(--ink);
  border-radius: 20px;
  color: rgba(255, 255, 255, 0.88);
}

.interview-band::after {
  content: '';
  position: absolute;
  right: -140px;
  top: -140px;
  width: 380px;
  height: 380px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(14, 107, 85, 0.5), transparent 65%);
}

.band-copy {
  position: relative;
  z-index: 1;
}

.band-eyebrow {
  color: #7fd3b8;
}

.interview-band h2 {
  margin-top: 8px;
  color: #f4f1e9;
  font-family: var(--font-display);
  font-size: clamp(26px, 3vw, 34px);
  line-height: 1.35;
  letter-spacing: 0;
  text-wrap: balance;
}

.band-lead {
  margin-top: 12px;
  color: rgba(255, 255, 255, 0.66);
  font-size: 14px;
  line-height: 1.8;
  text-wrap: pretty;
}

.sell-points {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px 18px;
  margin-top: 26px;
}

.sell-points li {
  display: flex;
  gap: 9px;
  color: rgba(255, 255, 255, 0.82);
  font-size: 13px;
}

.sell-points li::before {
  content: '◇';
  color: #6fc2a8;
}

.band-copy .btn-primary {
  margin-top: 26px;
}

.demo-chat {
  position: relative;
  z-index: 1;
  display: grid;
  gap: 12px;
  padding: 20px;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 16px;
}

.demo-msg {
  max-width: 88%;
  padding: 11px 14px;
  border-radius: 12px;
  font-size: 12.5px;
  line-height: 1.7;
}

.demo-msg.ai {
  background: rgba(255, 255, 255, 0.1);
}

.demo-msg.me {
  margin-left: auto;
  background: var(--jade);
  color: #fff;
}

.demo-score {
  display: block;
  margin-top: 6px;
  color: #8fd6bf;
  font-family: var(--font-mono);
  font-size: 11px;
}

/* ---------- 新手引导 ---------- */
.guide-overlay {
  position: fixed;
  inset: 0;
  z-index: 100;
  display: grid;
  place-items: center;
  padding: 20px;
  background: rgba(21, 24, 29, 0.46);
  overscroll-behavior: contain;
}

.guide-modal {
  width: min(520px, 100%);
  overflow: hidden;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: 18px;
  box-shadow: var(--shadow-lg);
}

.guide-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 24px 26px 0;
}

.guide-head h2 {
  margin-top: 4px;
  font-family: var(--font-display);
  font-size: 26px;
  letter-spacing: 0;
}

.guide-close {
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  border: 0;
  border-radius: 50%;
  background: transparent;
  color: var(--ink-3);
  font-size: 15px;
}

.guide-body {
  display: grid;
  gap: 10px;
  padding: 20px 26px 6px;
}

.guide-step {
  display: flex;
  gap: 14px;
  padding: 14px 16px;
  background: var(--surface-2);
  border: 1px solid var(--line);
  border-radius: 12px;
}

.guide-step.active {
  background: var(--jade-soft);
  border-color: var(--jade);
}

.guide-no {
  display: grid;
  place-items: center;
  flex: none;
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: var(--paper-2);
  color: var(--ink-2);
  font-family: var(--font-mono);
  font-size: 12px;
}

.guide-step.active .guide-no {
  background: var(--jade);
  color: #fff;
}

.guide-step h3 {
  font-size: 13.5px;
  line-height: 1.5;
  letter-spacing: 0;
}

.guide-step p {
  margin-top: 3px;
  color: var(--ink-3);
  font-size: 12.5px;
  line-height: 1.6;
}

.guide-foot {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 18px 26px 24px;
}

.overlay-enter-active,
.overlay-leave-active {
  transition: opacity 0.18s ease;
}

.overlay-enter-from,
.overlay-leave-to {
  opacity: 0;
}

.overlay-enter-active .guide-modal,
.overlay-leave-active .guide-modal {
  transition: transform 0.2s cubic-bezier(0.16, 1, 0.3, 1);
}

.overlay-enter-from .guide-modal,
.overlay-leave-to .guide-modal {
  transform: translateY(12px) scale(0.98);
}

/* ---------- 交互与响应式 ---------- */
:where(.quick-card, .feature-card, .stage-card, .section-link, .btn, .guide-close):focus-visible {
  outline: 2px solid var(--jade);
  outline-offset: 3px;
}

@media (hover: hover) {
  .quick-card:hover,
  .feature-card:hover,
  .stage-card:hover {
    transform: translateY(-3px);
    border-color: var(--line-2);
    box-shadow: var(--shadow);
  }

  .section-link:hover {
    text-decoration: underline;
    text-underline-offset: 3px;
  }

  .guide-close:hover {
    background: var(--paper-2);
  }
}

@media (max-width: 1080px) {
  .hero {
    grid-template-columns: 1fr;
    gap: 30px;
  }

  .hero-art {
    min-height: 340px;
  }

  .quick-grid,
  .feature-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .stats-panel {
    grid-template-columns: 1fr;
  }

  .stats-left {
    border-right: 0;
    border-bottom: 1px solid var(--line);
  }

  .stage-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .interview-band {
    grid-template-columns: 1fr;
    gap: 32px;
  }
}

@media (max-width: 760px) {
  .home-page {
    padding-top: 8px;
  }

  .home-section {
    margin-top: 48px;
  }

  .hero h1 {
    font-size: clamp(30px, 9vw, 42px);
  }

  .hero-meta {
    gap: 20px;
  }

  .float-tag {
    display: none;
  }

  .score-card {
    transform: none;
  }

  .section-head {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .quick-grid,
  .feature-grid,
  .stage-grid {
    grid-template-columns: 1fr;
  }

  .quick-card,
  .feature-card {
    min-height: auto;
  }

  .bars {
    gap: 10px;
  }

  .guest-panel {
    flex-direction: column;
    align-items: flex-start;
    padding: 24px;
  }

  .interview-band {
    padding: 28px 22px;
  }

  .sell-points {
    grid-template-columns: 1fr;
  }

  .guide-head,
  .guide-foot {
    padding-left: 18px;
    padding-right: 18px;
  }

  .guide-body {
    padding-left: 18px;
    padding-right: 18px;
  }
}

@media (max-width: 420px) {
  .hero-actions .btn {
    width: 100%;
  }
}

@media (prefers-reduced-motion: reduce) {
  .quick-card,
  .feature-card,
  .stage-card,
  .bar-fill,
  .overlay-enter-active,
  .overlay-leave-active,
  .overlay-enter-active .guide-modal,
  .overlay-leave-active .guide-modal {
    transition: none;
  }

  .skeleton-block {
    animation: none;
  }
}
</style>
