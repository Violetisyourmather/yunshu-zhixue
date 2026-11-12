<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ApiError } from '@/api/http'
import {
  getInterviewConfig,
  startInterview,
  type InterviewConfig,
  type InterviewSubjectOption
} from '@/api/interview'
import {
  createInterviewSession,
  loadInterviewSession,
  saveInterviewSession
} from './session-storage'

const SUBJECT_ICONS: Record<number, string> = {
  1: '🤖',
  2: '🧠',
  3: '🦾',
  4: '🔧',
  5: '🧪',
  6: '🛠️',
  7: '🎨',
  8: '🎬',
  17: '☕'
}

const DIFFICULTIES = [
  { value: 1, title: '简单', subtitle: '基础面试', description: '适合刚学完一个阶段' },
  { value: 2, title: '中等', subtitle: '进阶面试', description: '适合做过 1–2 个项目' },
  { value: 3, title: '困难', subtitle: '高级面试', description: '适合准备正式跳槽' }
]

const DAILY_LIMIT = 5

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const loadError = ref('')
const starting = ref(false)
const hasSavedSession = ref(false)
const config = ref<InterviewConfig | null>(null)
const subjectId = ref<number | null>(null)
const difficulty = ref(2)
const count = ref(10)

function messageOf(error: unknown, fallback: string): string {
  return error instanceof ApiError ? error.message : fallback
}

function iconOf(subject: InterviewSubjectOption): string {
  return SUBJECT_ICONS[subject.subjectId] ?? '🎯'
}

function resolveInitial<T>(value: unknown, allowed: T[], fallback: T): T {
  const numeric = Number(value)
  return allowed.includes(numeric as T) ? (numeric as T) : fallback
}

async function load(): Promise<void> {
  loading.value = true
  loadError.value = ''
  try {
    const data = await getInterviewConfig()
    config.value = data
    const ids = data.subjects.map((subject) => subject.subjectId)
    subjectId.value = resolveInitial(route.query.subjectId, ids, ids[0] ?? null)
    difficulty.value = resolveInitial(route.query.difficulty, data.difficulties, 2)
    count.value = resolveInitial(
      route.query.count,
      data.questionCountOptions,
      data.defaultCount
    )
    hasSavedSession.value = loadInterviewSession() !== null
  } catch (error) {
    loadError.value = messageOf(error, '面试配置加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

function selectSubject(id: number): void {
  subjectId.value = id
}

function selectDifficulty(value: number): void {
  difficulty.value = value
}

function selectCount(value: number): void {
  count.value = value
}

function continueSaved(): void {
  void router.push('/interview/session')
}

async function handleStart(): Promise<void> {
  const current = config.value
  if (!current || subjectId.value === null || starting.value) {
    return
  }
  if (current.remainingCount <= 0) {
    ElMessage.warning('今日 AI 面试次数已用完，明天再来')
    return
  }
  if (hasSavedSession.value) {
    try {
      await ElMessageBox.confirm(
        '开始新面试会覆盖本地未完成的面试进度，是否继续？',
        '开始新面试',
        { confirmButtonText: '继续', cancelButtonText: '取消', type: 'warning' }
      )
    } catch {
      return
    }
  }

  const subject = current.subjects.find((item) => item.subjectId === subjectId.value)
  if (!subject) {
    return
  }
  starting.value = true
  try {
    const result = await startInterview({
      subjectId: subject.subjectId,
      difficulty: difficulty.value,
      count: count.value
    })
    const session = createInterviewSession({
      sessionId: result.sessionId,
      subjectId: subject.subjectId,
      subjectName: subject.subjectName,
      difficulty: difficulty.value,
      questionCount: result.questionCount,
      questions: result.questions,
      timeoutMinutes: current.timeoutMinutes
    })
    saveInterviewSession(session)
    await router.push('/interview/session')
  } catch (error) {
    ElMessage.error(messageOf(error, '发起面试失败，请稍后重试'))
    await load()
  } finally {
    starting.value = false
  }
}

onMounted(() => {
  void load()
})
</script>

<template>
  <section class="interview-config-page">
    <header class="page-head">
      <div>
        <p class="eyebrow">AI Interview</p>
        <h1>开始一场模拟面试</h1>
        <p class="lead">选择方向、难度与题数。每题限时 5 分钟，提交后立即给出评分与反馈。</p>
      </div>
      <RouterLink class="btn" to="/interview/history">面试记录 →</RouterLink>
    </header>

    <div v-if="loading" class="state-panel">正在加载面试配置…</div>

    <div v-else-if="loadError" class="state-panel state-error">
      <p>{{ loadError }}</p>
      <button type="button" class="btn btn-sm" @click="load">重新加载</button>
    </div>

    <template v-else-if="config">
      <div v-if="hasSavedSession" class="resume-banner">
        <span>有一场未完成的 AI 面试，可以继续上次的进度。</span>
        <button type="button" class="btn btn-sm" @click="continueSaved">继续面试</button>
      </div>

      <section class="config-panel">
        <div class="config-section">
          <h2>技术方向</h2>
          <div class="direction-grid">
            <button
              v-for="subject in config.subjects"
              :key="subject.subjectId"
              type="button"
              class="direction-card"
              :class="{ active: subjectId === subject.subjectId }"
              @click="selectSubject(subject.subjectId)"
            >
              <span class="direction-icon" aria-hidden="true">{{ iconOf(subject) }}</span>
              <span class="direction-name">{{ subject.subjectName }}</span>
            </button>
          </div>
        </div>

        <div class="divider" />

        <div class="config-section">
          <h2>面试难度</h2>
          <div class="difficulty-grid">
            <button
              v-for="item in DIFFICULTIES"
              :key="item.value"
              type="button"
              class="difficulty-card"
              :class="{ active: difficulty === item.value }"
              @click="selectDifficulty(item.value)"
            >
              <div class="difficulty-head">
                <h3>{{ item.title }}</h3>
                <span>{{ item.subtitle }}</span>
              </div>
              <p>{{ item.description }}</p>
            </button>
          </div>
        </div>

        <div class="divider" />

        <div class="config-row">
          <div class="config-section">
            <h2>面试题目数量</h2>
            <div class="count-chips">
              <button
                v-for="value in config.questionCountOptions"
                :key="value"
                type="button"
                class="chip"
                :class="{ active: count === value }"
                @click="selectCount(value)"
              >
                {{ value }} 题
              </button>
            </div>
          </div>
          <div class="config-section">
            <h2>回答超时时间</h2>
            <span class="timeout-pill">固定 {{ config.timeoutMinutes }} 分钟 / 题 · 不可修改</span>
          </div>
        </div>

        <div class="divider" />

        <div class="quota-box">
          <div class="quota-icon" aria-hidden="true">🎟</div>
          <div class="quota-copy">
            <div class="quota-title">
              今日剩余 AI 面试次数：
              <strong>{{ config.remainingCount }} / {{ DAILY_LIMIT }}</strong>
            </div>
            <p>每日 5 次，次日 0 点重置。中断不退还配额，请确认后再开始。</p>
          </div>
          <button
            type="button"
            class="btn btn-primary btn-lg start-button"
            :disabled="starting || config.remainingCount <= 0 || subjectId === null"
            @click="handleStart"
          >
            {{ starting ? '正在发起…' : config.remainingCount <= 0 ? '今日次数已用完' : '开始 AI 面试' }}
          </button>
        </div>
      </section>
    </template>
  </section>
</template>

<style scoped>
.interview-config-page {
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
  font-size: clamp(32px, 3.6vw, 42px);
  line-height: 1.2;
  letter-spacing: 0;
}

.page-head .lead {
  max-width: 680px;
  margin-top: 8px;
  color: var(--ink-2);
  font-size: 14px;
  line-height: 1.75;
}

.state-panel {
  margin-top: 22px;
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

.resume-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: 20px;
  padding: 12px 18px;
  background: var(--jade-soft);
  border: 1px solid var(--line);
  border-radius: var(--r-sm);
  color: var(--jade-2);
  font-size: 13px;
}

.config-panel {
  margin-top: 22px;
  padding: 28px 30px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r);
  box-shadow: var(--shadow);
}

.config-section h2 {
  font-size: 14px;
  letter-spacing: 0;
}

.divider {
  height: 1px;
  margin: 24px 0;
  background: var(--line);
}

.direction-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
  margin-top: 16px;
}

.direction-card {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 58px;
  padding: 12px 14px;
  background: var(--surface-2);
  border: 1px solid var(--line);
  border-radius: 12px;
  color: var(--ink-2);
  text-align: left;
  transition: border-color 0.16s ease, background-color 0.16s ease;
}

.direction-card.active {
  border-color: var(--jade);
  background: var(--jade-soft);
  color: var(--jade-2);
}

.direction-icon {
  font-size: 21px;
  line-height: 1;
}

.direction-name {
  font-size: 13px;
  line-height: 1.45;
}

.difficulty-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  margin-top: 16px;
}

.difficulty-card {
  padding: 16px 18px;
  background: var(--surface-2);
  border: 1px solid var(--line);
  border-radius: 12px;
  text-align: left;
  transition: border-color 0.16s ease, background-color 0.16s ease;
}

.difficulty-card.active {
  border-color: var(--jade);
  background: var(--jade-soft);
}

.difficulty-head {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.difficulty-head h3 {
  font-size: 15px;
  letter-spacing: 0;
}

.difficulty-head span {
  color: var(--ink-3);
  font-size: 11.5px;
}

.difficulty-card p {
  margin-top: 5px;
  color: var(--ink-3);
  font-size: 12.5px;
}

.config-row {
  display: grid;
  grid-template-columns: 1.5fr 1fr;
  gap: 32px;
}

.count-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 16px;
}

.chip {
  height: 32px;
  padding: 0 14px;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: var(--surface);
  color: var(--ink-2);
  font-size: 12px;
}

.chip.active {
  background: var(--ink);
  border-color: var(--ink);
  color: #fff;
}

.timeout-pill {
  display: inline-flex;
  align-items: center;
  height: 32px;
  margin-top: 16px;
  padding: 0 14px;
  border: 1px solid var(--line-2);
  border-radius: 999px;
  background: var(--surface-2);
  color: var(--ink-2);
  font-size: 12px;
}

.quota-box {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 18px 20px;
  background: var(--paper-2);
  border-radius: 14px;
}

.quota-icon {
  font-size: 28px;
  line-height: 1;
}

.quota-copy {
  flex: 1;
  min-width: 0;
}

.quota-title {
  font-size: 13.5px;
  font-weight: 500;
}

.quota-title strong {
  color: var(--jade-2);
  font-family: var(--font-mono);
}

.quota-copy p {
  margin-top: 4px;
  color: var(--ink-3);
  font-size: 11.5px;
  line-height: 1.6;
}

.start-button {
  flex: none;
}

:where(.direction-card, .difficulty-card, .chip, .start-button):focus-visible {
  outline: 2px solid var(--jade);
  outline-offset: 2px;
}

@media (hover: hover) {
  .direction-card:hover:not(.active),
  .difficulty-card:hover:not(.active) {
    border-color: var(--line-2);
  }

  .chip:hover {
    border-color: var(--line-2);
  }
}

@media (max-width: 960px) {
  .direction-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .difficulty-grid {
    grid-template-columns: 1fr;
  }

  .config-row {
    grid-template-columns: 1fr;
    gap: 20px;
  }
}

@media (max-width: 700px) {
  .interview-config-page {
    padding-top: 24px;
  }

  .page-head {
    flex-direction: column;
    align-items: flex-start;
    gap: 14px;
  }

  .config-panel {
    padding: 20px 18px;
  }

  .quota-box {
    flex-wrap: wrap;
  }

  .start-button {
    width: 100%;
  }

  .resume-banner {
    flex-direction: column;
    align-items: flex-start;
  }
}

@media (prefers-reduced-motion: reduce) {
  .direction-card,
  .difficulty-card {
    transition: none;
  }
}
</style>
