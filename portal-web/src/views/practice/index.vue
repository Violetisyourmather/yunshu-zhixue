<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ApiError } from '@/api/http'
import {
  startPractice,
  submitPractice,
  type AiPoint,
  type PracticeQuestion,
  type PracticeResult,
  type PracticeStartResult
} from '@/api/practice'
import { startWrongRedo } from '@/api/wrong-book'

interface PracticeEntry {
  picked: string[]
  text: string
  graded: boolean
  correct: boolean | null
  score: number | null
  comment: string
  points: AiPoint[]
  correctAnswer: string
  analysis: string
  degraded: boolean
  durationSeconds: number
}

interface StartSource {
  source: 'question' | 'subject' | 'wrong_redo'
  questionId?: number
  subjectId?: number
  stageId?: number
  defaultCount?: number
  wrongIds?: number[]
}

interface PendingPractice {
  response: PracticeStartResult
  start: StartSource
}

interface PracticeSession {
  practiceId: string
  title: string
  mode: 1 | 2
  questions: PracticeQuestion[]
  index: number
  entries: Record<number, PracticeEntry>
  elapsed: number
  savedAt: number
  start: StartSource
}

type Overlay = 'none' | 'mode' | 'resume' | 'shortcuts'

const STORAGE_KEY = 'yszx_portal_practice_session'
const PENDING_PRACTICE_KEY = 'yszx_portal_pending_practice'
const TYPE_LABELS: Record<number, string> = { 1: '单选', 2: '多选', 3: '判断', 4: '简答' }
const DIFFICULTY_LABELS: Record<number, string> = { 1: '简单', 2: '中等', 3: '困难' }

const route = useRoute()
const router = useRouter()

const initializing = ref(true)
const starting = ref(false)
const submitting = ref(false)
const session = ref<PracticeSession | null>(null)
const savedSession = ref<PracticeSession | null>(null)
const overlay = ref<Overlay>('none')
const pendingStart = ref<StartSource | null>(null)
const randomCount = ref(10)
const questionStartedAt = ref(Date.now())
const mainRef = ref<HTMLElement | null>(null)
const resultRef = ref<HTMLElement | null>(null)
const overlayPrimary = ref<HTMLButtonElement | null>(null)
const overlayDialog = ref<HTMLElement | null>(null)
let timer: number | null = null

const currentQuestion = computed(() => {
  const current = session.value
  if (!current) {
    return null
  }
  return current.questions[current.index] ?? null
})
const currentEntry = computed(() => {
  const question = currentQuestion.value
  const current = session.value
  if (!question || !current) {
    return null
  }
  return current.entries[question.id] ?? null
})
const currentOptions = computed(() => {
  const question = currentQuestion.value
  if (!question) {
    return []
  }
  if (question.type === 3) {
    return [
      { key: '正确', text: '正确' },
      { key: '错误', text: '错误' }
    ]
  }
  return question.options
})
const answeredCount = computed(() => {
  const current = session.value
  if (!current) {
    return 0
  }
  return current.questions.filter((question) => current.entries[question.id]?.graded).length
})
const progressPercent = computed(() => {
  const current = session.value
  if (!current || !current.questions.length) {
    return 0
  }
  return ((current.index + 1) / current.questions.length) * 100
})
const isLastQuestion = computed(() => {
  const current = session.value
  return current ? current.index === current.questions.length - 1 : false
})
const firstTag = computed(() => splitTags(currentQuestion.value?.tags ?? null)[0] ?? '')
const correctKeys = computed(() => {
  const question = currentQuestion.value
  if (!question || (question.type !== 1 && question.type !== 2) || !question.answer) {
    return new Set<string>()
  }
  return new Set(
    question.answer
      .split(',')
      .map((key) => key.trim())
      .filter(Boolean)
  )
})
const resumeSummary = computed(() => {
  const saved = savedSession.value
  if (!saved) {
    return ''
  }
  return `${saved.title} · 进度 ${saved.index + 1} / ${saved.questions.length} · ${formatElapsed(saved.elapsed)}`
})

function messageOf(error: unknown, fallback: string): string {
  return error instanceof ApiError ? error.message : fallback
}

function blankEntry(): PracticeEntry {
  return {
    picked: [],
    text: '',
    graded: false,
    correct: null,
    score: null,
    comment: '',
    points: [],
    correctAnswer: '',
    analysis: '',
    degraded: false,
    durationSeconds: 0
  }
}

function typeLabel(type: number): string {
  return TYPE_LABELS[type] ?? '未知'
}

function difficultyLabel(difficulty: number): string {
  return DIFFICULTY_LABELS[difficulty] ?? '未知'
}

function difficultyClass(difficulty: number): string {
  return `badge-diff-${difficulty}`
}

function splitTags(tags: string | null): string[] {
  if (!tags) {
    return []
  }
  return tags
    .split(/[,，]/)
    .map((tag) => tag.trim())
    .filter(Boolean)
}

function formatElapsed(total: number): string {
  const seconds = Math.max(0, Math.floor(total))
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  const rest = seconds % 60
  return [hours, minutes, rest].map((value) => String(value).padStart(2, '0')).join(':')
}

function formatScore(score: number | null): string {
  if (score === null) {
    return '0'
  }
  return Number.isInteger(score) ? String(score) : score.toFixed(1)
}

function formatAnswer(type: number, value: string | null): string {
  if (!value) {
    return '—'
  }
  if ((type === 1 || type === 2) && value.includes(',')) {
    return value
      .split(',')
      .map((item) => item.trim())
      .join(' / ')
  }
  return value
}

function dotClass(question: PracticeQuestion, index: number): Record<string, boolean> {
  const current = session.value
  const entry = current?.entries[question.id]
  return {
    current: current?.index === index,
    right: Boolean(entry?.graded && entry.correct),
    wrong: Boolean(entry?.graded && entry.correct === false)
  }
}

function optionClass(key: string): string {
  const question = currentQuestion.value
  const entry = currentEntry.value
  if (!question || !entry) {
    return ''
  }
  const picked = entry.picked.includes(key)
  if (!entry.graded) {
    return picked ? 'selected' : ''
  }
  if (question.type === 3) {
    if ((question.answer ?? '').trim() === key) {
      return 'correct'
    }
    return picked ? 'wrong' : ''
  }
  if (correctKeys.value.has(key)) {
    return 'correct'
  }
  return picked ? 'wrong' : ''
}

function sourceFromQuery(): StartSource | null {
  const questionId = Number(route.query.questionId)
  if (Number.isInteger(questionId) && questionId > 0) {
    return { source: 'question', questionId }
  }
  const stageId = Number(route.query.stageId)
  if (Number.isInteger(stageId) && stageId > 0) {
    return { source: 'subject', stageId }
  }
  if (route.query.mode === 'random') {
    const count = Number(route.query.count)
    return {
      source: 'subject',
      defaultCount: Number.isInteger(count) && count > 0 ? count : 10
    }
  }
  const subjectId = Number(route.query.subjectId)
  if (Number.isInteger(subjectId) && subjectId > 0) {
    return { source: 'subject', subjectId }
  }
  return null
}

function createSession(
  response: { practiceId: string; title: string; questions: PracticeQuestion[] },
  source: StartSource,
  mode: 1 | 2
): PracticeSession {
  const entries: Record<number, PracticeEntry> = {}
  for (const question of response.questions) {
    entries[question.id] = blankEntry()
  }
  return {
    practiceId: response.practiceId,
    title: response.title,
    mode,
    questions: response.questions,
    index: 0,
    entries,
    elapsed: 0,
    savedAt: Date.now(),
    start: source
  }
}

function saveSession(): void {
  if (!session.value) {
    return
  }
  session.value.savedAt = Date.now()
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(session.value))
  } catch {
    // 本地存储不可用时不影响当前练习
  }
}

function loadSavedSession(): PracticeSession | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) {
      return null
    }
    const data = JSON.parse(raw) as PracticeSession
    if (!data?.practiceId || !Array.isArray(data.questions) || !data.questions.length) {
      localStorage.removeItem(STORAGE_KEY)
      return null
    }
    data.entries ??= {}
    for (const question of data.questions) {
      data.entries[question.id] = { ...blankEntry(), ...(data.entries[question.id] ?? {}) }
    }
    data.mode = data.mode === 2 ? 2 : 1
    data.index = Math.min(Math.max(data.index ?? 0, 0), data.questions.length - 1)
    data.elapsed = Math.max(0, data.elapsed ?? 0)
    data.start ??= { source: 'subject' }
    data.savedAt ??= Date.now()
    return data
  } catch {
    localStorage.removeItem(STORAGE_KEY)
    return null
  }
}

function clearSavedSession(): void {
  try {
    localStorage.removeItem(STORAGE_KEY)
  } catch {
    // ignore
  }
}

function takePendingPractice(): PendingPractice | null {
  try {
    const raw = localStorage.getItem(PENDING_PRACTICE_KEY)
    if (!raw) {
      return null
    }
    const data = JSON.parse(raw) as PendingPractice
    localStorage.removeItem(PENDING_PRACTICE_KEY)
    if (!data?.response?.practiceId
      || !Array.isArray(data.response.questions)
      || !data.start) {
      return null
    }
    return data
  } catch {
    localStorage.removeItem(PENDING_PRACTICE_KEY)
    return null
  }
}

function clearPendingPractice(): void {
  try {
    localStorage.removeItem(PENDING_PRACTICE_KEY)
  } catch {
    // ignore
  }
}

function startTimer(): void {
  stopTimer()
  timer = window.setInterval(() => {
    if (!session.value) {
      return
    }
    session.value.elapsed += 1
    if (session.value.elapsed % 10 === 0) {
      saveSession()
    }
  }, 1000)
}

function stopTimer(): void {
  if (timer !== null) {
    window.clearInterval(timer)
    timer = null
  }
}

function resumeSaved(): void {
  if (!savedSession.value) {
    return
  }
  session.value = savedSession.value
  savedSession.value = null
  overlay.value = 'none'
  questionStartedAt.value = Date.now()
  startTimer()
  void router.replace('/practice')
}

async function restartSaved(): Promise<void> {
  const saved = savedSession.value
  if (!saved) {
    return
  }
  const querySource = sourceFromQuery()
  const source = querySource ?? saved.start

  if (source.source === 'wrong_redo' && !querySource) {
    const ids = source.wrongIds ?? []
    clearSavedSession()
    savedSession.value = null
    session.value = null
    overlay.value = 'none'
    if (!ids.length) {
      void router.push('/wrong-book')
      return
    }
    initializing.value = true
    try {
      const response = await startWrongRedo(ids)
      session.value = createSession(response, source, 1)
      questionStartedAt.value = Date.now()
      startTimer()
      saveSession()
      void router.replace('/practice')
    } catch (error) {
      ElMessage.error(messageOf(error, '重新开始失败，请返回错题本重试'))
      void router.push('/wrong-book')
    } finally {
      initializing.value = false
    }
    return
  }

  clearSavedSession()
  savedSession.value = null
  session.value = null
  if (source) {
    pendingStart.value = source
    randomCount.value = source.defaultCount ?? 10
    overlay.value = 'mode'
  } else {
    overlay.value = 'none'
  }
}

async function selectMode(mode: 1 | 2): Promise<void> {
  const source = pendingStart.value
  if (!source || source.source === 'wrong_redo' || starting.value) {
    return
  }
  starting.value = true
  try {
    const response = await startPractice({
      source: source.source,
      questionId: source.questionId,
      subjectId: source.subjectId,
      stageId: source.stageId,
      mode,
      count: mode === 2 && source.source === 'subject' ? randomCount.value : undefined
    })
    if (!response.questions.length) {
      ElMessage.warning('该范围暂无可练习的题目')
      return
    }
    session.value = createSession(response, source, mode)
    pendingStart.value = null
    overlay.value = 'none'
    questionStartedAt.value = Date.now()
    startTimer()
    saveSession()
    void router.replace('/practice')
  } catch (error) {
    ElMessage.error(messageOf(error, '开始练习失败，请稍后重试'))
  } finally {
    starting.value = false
  }
}

function cancelMode(): void {
  overlay.value = 'none'
  pendingStart.value = null
  void router.push('/bank')
}

function toggleOption(key: string): void {
  const question = currentQuestion.value
  const entry = currentEntry.value
  if (!question || !entry || entry.graded || submitting.value) {
    return
  }
  if (question.type === 2) {
    entry.picked = entry.picked.includes(key)
      ? entry.picked.filter((item) => item !== key)
      : [...entry.picked, key]
  } else {
    entry.picked = [key]
  }
  saveSession()
}

async function submitCurrent(): Promise<void> {
  const current = session.value
  const question = currentQuestion.value
  const entry = currentEntry.value
  if (!current || !question || !entry || entry.graded || submitting.value) {
    return
  }

  let answer = ''
  if (question.type === 4) {
    answer = entry.text.trim()
    if (!answer) {
      ElMessage.warning('请先填写答案')
      return
    }
  } else {
    if (!entry.picked.length) {
      ElMessage.warning('请先选择答案')
      return
    }
    answer = entry.picked.slice().sort().join(',')
  }

  const durationSeconds = Math.max(
    0,
    Math.round((Date.now() - questionStartedAt.value) / 1000)
  )
  submitting.value = true
  try {
    const response = await submitPractice(
      current.practiceId,
      [{ questionId: question.id, answer, durationSeconds }],
      current.mode
    )
    const result: PracticeResult | undefined =
      response.results.find((item) => item.questionId === question.id) ?? response.results[0]
    if (!result) {
      throw new Error('empty result')
    }
    entry.graded = true
    entry.correct = result.isCorrect
    entry.score = result.score
    entry.comment = result.aiFeedback ?? ''
    entry.points = result.referencePoints ?? []
    entry.correctAnswer = result.answer ?? question.answer ?? ''
    entry.analysis = result.analysis ?? question.analysis ?? ''
    entry.degraded = result.degraded
    entry.durationSeconds = durationSeconds
    saveSession()
  } catch (error) {
    ElMessage.error(messageOf(error, '提交失败，请稍后重试'))
  } finally {
    submitting.value = false
  }
}

function goTo(index: number): void {
  const current = session.value
  if (!current || index < 0 || index >= current.questions.length) {
    return
  }
  current.index = index
  questionStartedAt.value = Date.now()
  saveSession()
  void nextTick(() => mainRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' }))
}

function previousQuestion(): void {
  const current = session.value
  if (current && current.index > 0) {
    goTo(current.index - 1)
  }
}

function nextQuestion(): void {
  const current = session.value
  const entry = currentEntry.value
  if (!current) {
    return
  }
  if (!entry?.graded) {
    ElMessage.warning('先提交本题答案')
    return
  }
  if (current.index === current.questions.length - 1) {
    void endPractice()
    return
  }
  goTo(current.index + 1)
}

function scrollToResult(): void {
  if (!currentEntry.value?.graded) {
    ElMessage.warning('提交答案后可查看解析')
    return
  }
  void nextTick(() => resultRef.value?.scrollIntoView({ behavior: 'smooth', block: 'nearest' }))
}

async function endPractice(): Promise<void> {
  const current = session.value
  if (!current) {
    return
  }
  const shortUnanswered = current.questions.findIndex(
    (question) => question.type === 4 && !current.entries[question.id]?.graded
  )
  if (shortUnanswered >= 0) {
    ElMessage.warning('还有未提交的简答题，请先完成')
    goTo(shortUnanswered)
    return
  }
  const unfinished = current.questions.filter(
    (question) => !current.entries[question.id]?.graded
  ).length
  const text = unfinished
    ? `还有 ${unfinished} 道题未完成，结束后本次进度将清除。`
    : '全部题目已完成，结束后将返回题库中心。'
  try {
    await ElMessageBox.confirm(text, '结束本次练习？', {
      confirmButtonText: '结束练习',
      cancelButtonText: '继续练习',
      type: 'warning'
    })
  } catch {
    return
  }
  finishPractice()
}

function finishPractice(): void {
  const source = session.value?.start.source
  stopTimer()
  clearSavedSession()
  session.value = null
  overlay.value = 'none'
  ElMessage.success('练习已结束')
  void router.push(source === 'wrong_redo' ? '/wrong-book' : '/bank')
}

function onOverlayBackdrop(): void {
  if (overlay.value === 'mode') {
    cancelMode()
  } else if (overlay.value === 'shortcuts') {
    overlay.value = 'none'
  }
}

function onOverlayEsc(): void {
  if (overlay.value === 'mode') {
    cancelMode()
  } else if (overlay.value === 'shortcuts') {
    overlay.value = 'none'
  }
}

function trapOverlayFocus(event: KeyboardEvent): void {
  if (event.key !== 'Tab' || overlay.value === 'none') {
    return
  }
  const dialog = overlayDialog.value
  if (!dialog) {
    return
  }
  const focusable = Array.from(
    dialog.querySelectorAll<HTMLElement>(
      'button:not([disabled]), [href], input:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])'
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

function handleKeydown(event: KeyboardEvent): void {
  if (overlay.value !== 'none') {
    return
  }
  if (!session.value) {
    return
  }
  const target = event.target as HTMLElement | null
  const tag = target?.tagName
  const inInput = tag === 'INPUT' || tag === 'TEXTAREA' || Boolean(target?.isContentEditable)

  if (event.key === 'ArrowLeft' && !inInput) {
    event.preventDefault()
    previousQuestion()
    return
  }
  if (event.key === 'ArrowRight' && !inInput) {
    event.preventDefault()
    nextQuestion()
    return
  }
  if (event.key === 'Enter' && !event.shiftKey && tag !== 'TEXTAREA') {
    event.preventDefault()
    if (!currentEntry.value?.graded) {
      void submitCurrent()
    } else {
      nextQuestion()
    }
    return
  }
  if (event.key === '?' && !inInput) {
    event.preventDefault()
    overlay.value = 'shortcuts'
  }
}

function boot(): void {
  if (route.query.wrongRedo === '1') {
    const pending = takePendingPractice()
    if (pending) {
      clearSavedSession()
      session.value = createSession(pending.response, pending.start, 1)
      overlay.value = 'none'
      questionStartedAt.value = Date.now()
      startTimer()
      saveSession()
      void router.replace('/practice')
      initializing.value = false
      return
    }
  } else {
    clearPendingPractice()
  }

  const saved = loadSavedSession()
  if (saved) {
    savedSession.value = saved
    overlay.value = 'resume'
    initializing.value = false
    return
  }
  const source = sourceFromQuery()
  if (source) {
    pendingStart.value = source
    randomCount.value = source.defaultCount ?? 10
    overlay.value = 'mode'
  }
  initializing.value = false
}

function handleBeforeUnload(): void {
  saveSession()
}

watch(overlay, async (value) => {
  document.body.style.overflow = value === 'none' ? '' : 'hidden'
  if (value !== 'none') {
    await nextTick()
    overlayPrimary.value?.focus()
  }
})

onMounted(() => {
  boot()
  window.addEventListener('keydown', handleKeydown)
  window.addEventListener('beforeunload', handleBeforeUnload)
})

onBeforeUnmount(() => {
  stopTimer()
  saveSession()
  window.removeEventListener('keydown', handleKeydown)
  window.removeEventListener('beforeunload', handleBeforeUnload)
  document.body.style.overflow = ''
})
</script>

<template>
  <section class="practice-page">
    <div v-if="initializing" class="state-panel">正在准备练习…</div>

    <template v-else-if="session">
      <div class="practice-layout">
        <aside class="practice-side">
          <RouterLink
            class="back-link"
            :to="session.start.source === 'wrong_redo' ? '/wrong-book' : '/bank'"
          >
            {{ session.start.source === 'wrong_redo' ? '← 返回错题本' : '← 返回题库' }}
          </RouterLink>
          <div class="p-title">{{ session.title }}</div>
          <div class="side-row">
            <span class="small muted">进度 {{ session.index + 1 }} / {{ session.questions.length }}</span>
            <span class="small muted">已答 {{ answeredCount }}</span>
          </div>
          <div class="p-progress"><i :style="{ width: `${progressPercent}%` }" /></div>
          <div class="side-row timer-row">
            <span class="p-timer">{{ formatElapsed(session.elapsed) }}</span>
            <span class="small muted">计时中</span>
          </div>

          <div class="answer-card" aria-label="答题卡">
            <button
              v-for="(question, index) in session.questions"
              :key="question.id"
              type="button"
              class="dot"
              :class="dotClass(question, index)"
              :aria-label="`第 ${index + 1} 题`"
              @click="goTo(index)"
            >
              {{ index + 1 }}
            </button>
          </div>
          <div class="legend">
            <span><i class="legend-current" />当前</span>
            <span><i class="legend-right" />正确</span>
            <span><i class="legend-wrong" />错误</span>
            <span><i class="legend-empty" />未答</span>
          </div>

          <button type="button" class="btn btn-sm btn-block end-button" @click="endPractice">
            结束练习
          </button>
        </aside>

        <main v-if="currentQuestion && currentEntry" ref="mainRef" class="practice-main">
          <div class="q-type-row">
            <span class="badge badge-ink">{{ typeLabel(currentQuestion.type) }}</span>
            <span class="badge" :class="difficultyClass(currentQuestion.difficulty)">
              {{ difficultyLabel(currentQuestion.difficulty) }}
            </span>
            <span v-if="firstTag" class="q-tag">{{ firstTag }}</span>
            <span class="q-position">第 {{ session.index + 1 }} 题</span>
          </div>

          <h2 class="q-stem">{{ currentQuestion.content }}</h2>

          <template v-if="currentQuestion.type === 4">
            <div class="short-input">
              <textarea
                v-model="currentEntry.text"
                class="textarea"
                :disabled="currentEntry.graded || submitting"
                placeholder="用 3~5 句话写清你的思路与结论"
              />
              <button
                v-if="!currentEntry.graded"
                type="button"
                class="btn btn-primary"
                :disabled="submitting"
                @click="submitCurrent"
              >
                {{ submitting ? 'AI 评分中…' : '提交答案' }}
              </button>
            </div>
          </template>

          <template v-else>
            <div class="options">
              <button
                v-for="option in currentOptions"
                :key="option.key"
                type="button"
                class="option"
                :class="optionClass(option.key)"
                :disabled="currentEntry.graded || submitting"
                @click="toggleOption(option.key)"
              >
                <span class="option-key">{{ option.key }}</span>
                <span>{{ option.text }}</span>
              </button>
            </div>
            <button
              v-if="!currentEntry.graded"
              type="button"
              class="btn btn-primary submit-button"
              :disabled="submitting"
              @click="submitCurrent"
            >
              {{ submitting ? '判定中…' : '提交答案' }}
            </button>
          </template>

          <div v-if="submitting && currentQuestion.type === 4" class="grading-card">
            <span class="grading-dots" aria-hidden="true"><i /><i /><i /></span>
            AI 正在批改你的答案，请稍候…
          </div>

          <div v-else-if="currentEntry.graded" ref="resultRef" class="result-card">
            <template v-if="currentQuestion.type === 4">
              <div class="result-head">
                <span class="badge" :class="currentEntry.correct ? 'badge-ok' : 'badge-bad'">
                  {{ currentEntry.correct ? '回答通过' : '未通过' }}
                </span>
                <span class="score-badge">得分 {{ formatScore(currentEntry.score) }} / 10</span>
                <span class="small muted">≥6 分视为通过</span>
              </div>
              <p v-if="currentEntry.degraded" class="degraded-note">
                AI 服务暂不可用，本次使用参考答案要点匹配降级评分。
              </p>
              <div class="explain"><b>评语：</b>{{ currentEntry.comment || '暂无评语' }}</div>
              <div v-if="currentEntry.points.length" class="keypoints">
                <span
                  v-for="(point, index) in currentEntry.points"
                  :key="index"
                  class="badge"
                  :class="point.hit ? 'badge-ok' : 'badge-miss'"
                >
                  {{ point.hit ? '✓' : '✕' }} {{ point.point }}
                </span>
              </div>
              <div class="divider" />
              <div class="small muted">参考答案</div>
              <pre class="reference-answer">{{ currentEntry.correctAnswer }}</pre>
              <div v-if="currentEntry.analysis" class="analysis-block">
                <b>解析：</b>{{ currentEntry.analysis }}
              </div>
            </template>

            <template v-else>
              <div class="result-head">
                <span class="badge" :class="currentEntry.correct ? 'badge-ok' : 'badge-bad'">
                  {{ currentEntry.correct ? '回答正确' : '回答错误' }}
                </span>
                <span class="small muted">
                  正确答案：{{ formatAnswer(currentQuestion.type, currentEntry.correctAnswer) }}
                </span>
              </div>
              <div class="explain">{{ currentEntry.analysis || '暂无解析' }}</div>
            </template>
          </div>

          <div class="p-footer">
            <button
              type="button"
              class="btn"
              :disabled="session.index === 0"
              @click="previousQuestion"
            >
              ← 上一题
            </button>
            <button type="button" class="btn btn-ghost" @click="scrollToResult">查看解析</button>
            <span class="spacer" />
            <span class="shortcut-hint">Enter 提交 / 下一题，← → 切题，? 快捷键</span>
            <button type="button" class="btn btn-primary" @click="nextQuestion">
              {{ isLastQuestion ? '完成' : '下一题 →' }}
            </button>
          </div>
        </main>
      </div>
    </template>

    <div v-else-if="overlay === 'none' && !savedSession" class="empty-panel">
      <h2>
        {{
          route.query.wrongRedo === '1'
            ? '没有可继续的重做练习'
            : route.query.resume === '1'
              ? '没有可继续的练习'
              : '还没有进行中的练习'
        }}
      </h2>
      <p>先到题库中心选一个阶段，再选择顺序或随机练习。</p>
      <RouterLink
        class="btn btn-primary"
        :to="route.query.wrongRedo === '1' ? '/wrong-book' : '/bank'"
      >
        {{ route.query.wrongRedo === '1' ? '返回错题本' : '去题库中心' }}
      </RouterLink>
    </div>

    <div v-else class="state-panel">正在准备练习…</div>

    <Teleport to="body">
      <Transition name="practice-dialog">
        <div
          v-if="overlay !== 'none'"
          class="practice-overlay"
          @click.self="onOverlayBackdrop"
          @keydown.esc="onOverlayEsc"
          @keydown="trapOverlayFocus"
        >
          <section
            ref="overlayDialog"
            class="practice-dialog"
            role="dialog"
            aria-modal="true"
          >
            <template v-if="overlay === 'mode'">
              <header class="dialog-head">
                <p class="eyebrow">Practice Mode</p>
                <h2>选择练习模式</h2>
                <p class="muted small">选择后开始计时，答题进度会自动保存在本地。</p>
              </header>
              <div class="mode-list">
                <button
                  ref="overlayPrimary"
                  type="button"
                  class="mode-card"
                  :disabled="starting"
                  @click="selectMode(1)"
                >
                  <h3>顺序练习</h3>
                  <p>按题库顺序逐题作答</p>
                </button>
                <button type="button" class="mode-card" :disabled="starting" @click="selectMode(2)">
                  <h3>随机练习</h3>
                  <p>打乱题目顺序作答，可指定题量</p>
                </button>
              </div>
              <div v-if="pendingStart?.source === 'subject'" class="mode-count">
                <span class="small muted">随机题量</span>
                <div class="count-chips">
                  <button
                    v-for="count in [5, 10, 15, 20]"
                    :key="count"
                    type="button"
                    class="chip"
                    :class="{ active: randomCount === count }"
                    @click="randomCount = count"
                  >
                    {{ count }} 题
                  </button>
                </div>
              </div>
              <footer class="dialog-foot">
                <span v-if="starting" class="small muted">正在组卷…</span>
                <button type="button" class="btn" @click="cancelMode">取消</button>
              </footer>
            </template>

            <template v-else-if="overlay === 'resume'">
              <header class="dialog-head">
                <p class="eyebrow">Continue</p>
                <h2>继续上次练习？</h2>
                <p class="muted small">{{ resumeSummary }}</p>
              </header>
              <footer class="dialog-foot">
                <button type="button" class="btn" @click="restartSaved">重新开始</button>
                <button ref="overlayPrimary" type="button" class="btn btn-primary" @click="resumeSaved">
                  继续练习
                </button>
              </footer>
            </template>

            <template v-else>
              <header class="dialog-head">
                <p class="eyebrow">Shortcuts</p>
                <h2>快捷键</h2>
              </header>
              <div class="shortcut-list">
                <div><kbd>←</kbd><span>上一题</span></div>
                <div><kbd>→</kbd><span>下一题</span></div>
                <div><kbd>Enter</kbd><span>提交答案 / 下一题</span></div>
                <div><kbd>?</kbd><span>打开快捷键面板</span></div>
              </div>
              <footer class="dialog-foot">
                <button ref="overlayPrimary" type="button" class="btn btn-primary" @click="overlay = 'none'">
                  知道了
                </button>
              </footer>
            </template>
          </section>
        </div>
      </Transition>
    </Teleport>
  </section>
</template>

<style scoped>
.practice-page {
  padding: 28px 0 24px;
}

.state-panel {
  padding: 24px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r);
  color: var(--ink-3);
  font-size: 13px;
}

.empty-panel {
  max-width: 620px;
  margin: 48px auto;
  padding: 40px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r);
  text-align: center;
}

.empty-panel h2 {
  font-family: var(--font-display);
  font-size: 26px;
  letter-spacing: 0;
}

.empty-panel p {
  margin-top: 8px;
  color: var(--ink-3);
  font-size: 13px;
}

.empty-panel .btn {
  margin-top: 22px;
}

/* ---------- 两栏布局 ---------- */
.practice-layout {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 20px;
  align-items: start;
}

.practice-side {
  position: sticky;
  top: 88px;
  padding: 18px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r);
}

.back-link {
  color: var(--ink-3);
  font-size: 12px;
}

.p-title {
  margin: 12px 0 6px;
  font-size: 13px;
  font-weight: 600;
  line-height: 1.5;
}

.side-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.timer-row {
  margin-top: 10px;
}

.p-timer {
  color: var(--ink-2);
  font-family: var(--font-mono);
  font-size: 13px;
  font-variant-numeric: tabular-nums;
}

.p-progress {
  height: 5px;
  margin: 10px 0;
  overflow: hidden;
  border-radius: 99px;
  background: var(--paper-2);
}

.p-progress i {
  display: block;
  height: 100%;
  border-radius: 99px;
  background: var(--jade);
  transition: width 0.3s ease;
}

.answer-card {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 7px;
  max-height: 238px;
  margin-top: 14px;
  overflow-y: auto;
  padding-right: 2px;
}

.dot {
  display: grid;
  place-items: center;
  height: 30px;
  border: 1px solid var(--line-2);
  border-radius: 7px;
  background: var(--surface);
  color: var(--ink-3);
  font-family: var(--font-mono);
  font-size: 11.5px;
  transition: background-color 0.14s ease, border-color 0.14s ease, color 0.14s ease;
}

.dot.right {
  background: var(--jade);
  border-color: var(--jade);
  color: #fff;
}

.dot.wrong {
  background: var(--red);
  border-color: var(--red);
  color: #fff;
}

.dot.current {
  background: var(--blue);
  border-color: var(--blue);
  color: #fff;
}

.legend {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 12px;
  margin-top: 12px;
  color: var(--ink-3);
  font-size: 11px;
}

.legend span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.legend i {
  width: 8px;
  height: 8px;
  border-radius: 3px;
}

.legend-current {
  background: var(--blue);
}

.legend-right {
  background: var(--jade);
}

.legend-wrong {
  background: var(--red);
}

.legend-empty {
  border: 1px solid var(--line-2);
  background: #fff;
}

.end-button {
  margin-top: 16px;
}

/* ---------- 答题区 ---------- */
.practice-main {
  display: flex;
  flex-direction: column;
  min-height: 520px;
  padding: 28px 30px 24px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r);
}

.q-type-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  color: var(--ink-3);
  font-size: 12px;
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

.badge-ink {
  background: var(--ink);
  color: #fff;
}

.badge-type-1 {
  background: var(--jade-soft);
  color: var(--jade-2);
}

.badge-type-2 {
  background: var(--blue-soft);
  color: var(--blue);
}

.badge-type-3 {
  background: var(--amber-soft);
  color: var(--amber);
}

.badge-type-4 {
  background: var(--paper-2);
  color: var(--ink-2);
}

.badge-diff-1 {
  background: var(--jade-soft);
  color: var(--jade-2);
}

.badge-diff-2 {
  background: var(--amber-soft);
  color: var(--amber);
}

.badge-diff-3 {
  background: var(--red-soft);
  color: var(--red);
}

.badge-ok {
  background: var(--jade-soft);
  color: var(--jade-2);
}

.badge-bad {
  background: var(--red-soft);
  color: var(--red);
}

.badge-miss {
  background: var(--paper-2);
  color: var(--ink-3);
}

.q-tag {
  color: var(--ink-3);
}

.q-position {
  margin-left: auto;
}

.q-stem {
  margin: 16px 0 20px;
  color: var(--ink);
  font-size: 17px;
  line-height: 1.8;
  letter-spacing: 0;
  white-space: pre-wrap;
  word-break: break-word;
}

.options {
  display: grid;
  gap: 10px;
}

.option {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 13px 16px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r-sm);
  color: var(--ink-2);
  font-size: 14px;
  line-height: 1.6;
  text-align: left;
  transition: border-color 0.14s ease, background-color 0.14s ease;
}

.option .option-key {
  display: grid;
  place-items: center;
  width: 24px;
  height: 24px;
  flex: none;
  border: 1px solid var(--line-2);
  border-radius: 50%;
  color: var(--ink-2);
  font-family: var(--font-mono);
  font-size: 12px;
}

.option.selected {
  border-color: var(--jade);
  background: var(--jade-soft);
}

.option.selected .option-key {
  background: var(--jade);
  border-color: var(--jade);
  color: #fff;
}

.option.correct {
  border-color: var(--jade);
  background: var(--jade-soft);
}

.option.correct .option-key {
  background: var(--jade);
  border-color: var(--jade);
  color: #fff;
}

.option.wrong {
  border-color: var(--red);
  background: var(--red-soft);
}

.option.wrong .option-key {
  background: var(--red);
  border-color: var(--red);
  color: #fff;
}

.submit-button {
  align-self: flex-start;
  margin-top: 16px;
}

.short-input {
  display: grid;
  gap: 10px;
  align-items: start;
}

.textarea {
  width: 100%;
  min-height: 150px;
  padding: 12px 14px;
  background: var(--surface);
  border: 1px solid var(--line-2);
  border-radius: var(--r-sm);
  outline: none;
  color: var(--ink);
  font-size: 14px;
  line-height: 1.75;
  resize: vertical;
}

.textarea:focus {
  border-color: var(--jade);
  box-shadow: 0 0 0 3px rgba(14, 107, 85, 0.12);
}

.textarea:disabled {
  background: var(--surface-2);
  color: var(--ink-3);
}

.short-input .btn {
  justify-self: start;
}

/* ---------- AI 评分 / 结果 ---------- */
.grading-card {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 20px;
  padding: 18px;
  background: var(--surface-2);
  border: 1px solid var(--line);
  border-radius: var(--r-sm);
  color: var(--ink-2);
  font-size: 13px;
}

.grading-dots {
  display: inline-flex;
  gap: 4px;
}

.grading-dots i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--jade);
  animation: grading-pulse 1s ease-in-out infinite;
}

.grading-dots i:nth-child(2) {
  animation-delay: 0.15s;
}

.grading-dots i:nth-child(3) {
  animation-delay: 0.3s;
}

@keyframes grading-pulse {
  0%,
  100% {
    opacity: 0.3;
    transform: translateY(0);
  }
  50% {
    opacity: 1;
    transform: translateY(-2px);
  }
}

.result-card {
  margin-top: 20px;
  padding: 16px 18px;
  background: var(--surface-2);
  border: 1px solid var(--line);
  border-radius: var(--r-sm);
}

.result-head {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.score-badge {
  color: var(--jade-2);
  font-family: var(--font-display);
  font-size: 18px;
  font-weight: 600;
}

.degraded-note {
  margin-top: 8px;
  color: var(--amber);
  font-size: 11.5px;
}

.explain {
  margin-top: 12px;
  color: var(--ink-2);
  font-size: 13px;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
}

.keypoints {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 10px;
}

.divider {
  height: 1px;
  margin: 16px 0 12px;
  background: var(--line);
}

.reference-answer {
  max-height: 280px;
  margin: 8px 0 0;
  padding: 12px 14px;
  overflow: auto;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: 8px;
  color: var(--ink-2);
  font-family: var(--font-mono);
  font-size: 12.5px;
  line-height: 1.75;
  white-space: pre-wrap;
  word-break: break-word;
}

.analysis-block {
  margin-top: 12px;
  color: var(--ink-2);
  font-size: 12.5px;
  line-height: 1.8;
  white-space: pre-wrap;
}

/* ---------- 底部操作 ---------- */
.p-footer {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: auto;
  padding-top: 22px;
  border-top: 1px solid var(--line);
}

.p-footer .spacer {
  margin-left: auto;
}

.shortcut-hint {
  color: var(--ink-4);
  font-size: 11px;
}

/* ---------- 弹窗 ---------- */
.practice-overlay {
  position: fixed;
  inset: 0;
  z-index: 100;
  display: grid;
  place-items: center;
  padding: 20px;
  background: rgba(21, 24, 29, 0.46);
  overscroll-behavior: contain;
}

.practice-dialog {
  width: min(520px, 100%);
  max-height: calc(100vh - 40px);
  overflow: auto;
  padding: 0;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: 18px;
  box-shadow: var(--shadow-lg);
}

.dialog-head {
  padding: 24px 26px 0;
}

.dialog-head h2 {
  margin-top: 4px;
  font-family: var(--font-display);
  font-size: 26px;
  letter-spacing: 0;
}

.dialog-head p:not(.eyebrow) {
  margin-top: 6px;
  color: var(--ink-3);
  font-size: 12.5px;
  line-height: 1.7;
}

.mode-list {
  display: grid;
  gap: 10px;
  margin-top: 18px;
  padding: 0 26px;
}

.mode-card {
  padding: 16px 18px;
  background: var(--surface-2);
  border: 1px solid var(--line);
  border-radius: 12px;
  text-align: left;
  transition: border-color 0.16s ease, background-color 0.16s ease;
}

.mode-card h3 {
  font-size: 14px;
  letter-spacing: 0;
}

.mode-card p {
  margin-top: 4px;
  color: var(--ink-3);
  font-size: 12.5px;
}

.mode-count {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 16px;
  padding: 0 26px;
}

.count-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.chip {
  height: 30px;
  padding: 0 12px;
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

.dialog-foot {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 22px;
  padding: 18px 26px 24px;
  border-top: 1px solid var(--line);
}

.shortcut-list {
  display: grid;
  gap: 12px;
  padding: 18px 26px 0;
}

.shortcut-list div {
  display: flex;
  align-items: center;
  gap: 12px;
  color: var(--ink-2);
  font-size: 13px;
}

.shortcut-list kbd {
  display: inline-grid;
  place-items: center;
  min-width: 34px;
  height: 28px;
  padding: 0 8px;
  border: 1px solid var(--line-2);
  border-radius: 6px;
  background: var(--surface-2);
  font-family: var(--font-mono);
  font-size: 11.5px;
}

.practice-dialog-enter-active,
.practice-dialog-leave-active {
  transition: opacity 0.18s ease;
}

.practice-dialog-enter-from,
.practice-dialog-leave-to {
  opacity: 0;
}

.practice-dialog-enter-active .practice-dialog,
.practice-dialog-leave-active .practice-dialog {
  transition: transform 0.2s cubic-bezier(0.16, 1, 0.3, 1);
}

.practice-dialog-enter-from .practice-dialog,
.practice-dialog-leave-to .practice-dialog {
  transform: translateY(12px) scale(0.98);
}

:where(.back-link, .dot, .option, .mode-card, .chip, .end-button):focus-visible {
  outline: 2px solid var(--jade);
  outline-offset: 2px;
}

@media (hover: hover) {
  .dot:hover {
    border-color: var(--ink-3);
  }

  .option:hover:not(:disabled) {
    border-color: var(--line-2);
    background: var(--surface-2);
  }

  .mode-card:hover:not(:disabled) {
    border-color: var(--jade);
    background: var(--jade-soft);
  }

  .chip:hover {
    border-color: var(--line-2);
  }
}

@media (max-width: 960px) {
  .practice-layout {
    grid-template-columns: 1fr;
  }

  .practice-side {
    position: static;
  }

  .practice-main {
    min-height: auto;
  }
}

@media (max-width: 700px) {
  .practice-page {
    padding-top: 16px;
  }

  .practice-main {
    padding: 20px 18px;
  }

  .q-stem {
    font-size: 15px;
  }

  .answer-card {
    grid-template-columns: repeat(8, 1fr);
    max-height: 190px;
  }

  .shortcut-hint {
    display: none;
  }

  .p-footer .spacer {
    display: none;
  }

  .p-footer {
    justify-content: space-between;
  }

  .p-footer .btn:first-child {
    order: 1;
  }

  .p-footer .btn:last-child {
    order: 2;
  }

  .p-footer .btn-ghost {
    order: 3;
    width: 100%;
  }

  .mode-count {
    flex-direction: column;
    align-items: flex-start;
  }

  .dialog-foot {
    flex-wrap: wrap;
    padding-left: 18px;
    padding-right: 18px;
  }

  .dialog-head,
  .mode-list {
    padding-left: 18px;
    padding-right: 18px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .dot,
  .option,
  .p-progress i,
  .grading-dots i,
  .practice-dialog-enter-active,
  .practice-dialog-leave-active,
  .practice-dialog-enter-active .practice-dialog,
  .practice-dialog-leave-active .practice-dialog {
    transition: none;
    animation: none;
  }
}
</style>
