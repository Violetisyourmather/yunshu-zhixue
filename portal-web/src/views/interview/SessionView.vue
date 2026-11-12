<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ApiError } from '@/api/http'
import { finishInterview } from '@/api/interview'
import { streamSse } from '@/api/sse'
import { useSpeech } from '@/composables/useSpeech'
import type { SseErrorEvent, SseGradeEvent, SseStatusEvent, SseTokenEvent } from '@/types/api'
import {
  clearInterviewSession,
  difficultyLabel,
  loadInterviewSession,
  newMessageId,
  nowLabel,
  saveInterviewSession,
  type InterviewMessage,
  type InterviewSession
} from './session-storage'

const router = useRouter()
const speech = useSpeech()
const speechSynthesisSupported =
  typeof window !== 'undefined' && 'speechSynthesis' in window

const loading = ref(true)
const resumeOpen = ref(false)
const endConfirmOpen = ref(false)
const finishing = ref(false)
const session = ref<InterviewSession | null>(null)
const input = ref('')
const streamingFeedback = ref('')
const typingLabel = ref('')
const online = ref(typeof navigator === 'undefined' ? true : navigator.onLine)
const sideOpen = ref(true)
const chatRef = ref<HTMLElement | null>(null)
const inputRef = ref<HTMLTextAreaElement | null>(null)

let elapsedTimer: number | null = null
let sseAbort: AbortController | null = null
let micBase = ''

const currentQuestion = computed(() => {
  const current = session.value
  if (!current) {
    return null
  }
  return current.questions[current.currentOrder - 1] ?? null
})
const answeredCount = computed(() => {
  const current = session.value
  return current ? Object.keys(current.scores).length : 0
})
const averageScore = computed(() => {
  const scores = Object.values(session.value?.scores ?? {})
  if (!scores.length) {
    return null
  }
  return scores.reduce((sum, score) => sum + score, 0) / scores.length
})
const progressPercent = computed(() => {
  const current = session.value
  if (!current) {
    return 0
  }
  return (answeredCount.value / current.questionCount) * 100
})
const questionRemaining = computed(() => {
  const current = session.value
  if (!current) {
    return 0
  }
  return Math.max(0, current.timeoutMinutes * 60 - current.questionElapsed)
})
const isBusy = computed(
  () => session.value?.phase === 'analyzing' || session.value?.phase === 'grading'
)
const interviewCompleted = computed(() => session.value?.phase === 'done')
const canSend = computed(
  () =>
    Boolean(session.value) &&
    session.value?.phase === 'ask' &&
    !finishing.value &&
    online.value &&
    input.value.trim().length > 0
)
const resumeSummary = computed(() => {
  const current = session.value
  if (!current) {
    return ''
  }
  return `${current.subjectName} · ${difficultyLabel(current.difficulty)} · 已回答 ${answeredCount.value} / ${current.questionCount} · 用时 ${formatElapsed(current.elapsed)}`
})

function messageOf(error: unknown, fallback: string): string {
  return error instanceof ApiError ? error.message : fallback
}

function formatElapsed(total: number): string {
  const seconds = Math.max(0, Math.floor(total))
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  const rest = seconds % 60
  return [hours, minutes, rest].map((value) => String(value).padStart(2, '0')).join(':')
}

function formatCountdown(seconds: number): string {
  const minutes = Math.floor(seconds / 60)
  const rest = seconds % 60
  return `${String(minutes).padStart(2, '0')}:${String(rest).padStart(2, '0')}`
}

function scoreTone(score: number | undefined): string {
  if (score === undefined) {
    return ''
  }
  return score >= 8 ? 'score-good' : score >= 6 ? 'score-mid' : 'score-bad'
}

function scrollToBottom(): void {
  void nextTick(() => {
    if (chatRef.value) {
      chatRef.value.scrollTop = chatRef.value.scrollHeight
    }
  })
}

function speak(text: string): void {
  const current = session.value
  if (!current?.voiceEnabled || !speechSynthesisSupported || !text) {
    return
  }
  window.speechSynthesis.cancel()
  const utterance = new SpeechSynthesisUtterance(text)
  utterance.lang = 'zh-CN'
  utterance.rate = 1
  window.speechSynthesis.speak(utterance)
}

function replay(message: InterviewMessage): void {
  speak(message.content || message.feedback || '')
}

function toggleVoice(): void {
  const current = session.value
  if (!current) {
    return
  }
  if (!speechSynthesisSupported) {
    ElMessage.warning('当前浏览器不支持语音播报')
    return
  }
  current.voiceEnabled = !current.voiceEnabled
  if (!current.voiceEnabled) {
    window.speechSynthesis.cancel()
  } else if (currentQuestion.value) {
    speak(currentQuestion.value.content)
  }
  saveInterviewSession(current)
}

function toggleMic(): void {
  if (!session.value || session.value.phase !== 'ask') {
    return
  }
  if (speech.listening.value) {
    speech.stop()
    return
  }
  if (!speech.supported) {
    ElMessage.warning('当前浏览器不支持语音识别，请使用 Chrome 或 Edge 打开')
    return
  }
  micBase = input.value.trim()
  speech.reset()
  speech.start()
}

function appendMessage(message: InterviewMessage, voiceText?: string): void {
  const current = session.value
  if (!current) {
    return
  }
  current.messages.push(message)
  if (voiceText) {
    speak(voiceText)
  }
  scrollToBottom()
}

function startTimers(): void {
  stopTimers()
  elapsedTimer = window.setInterval(() => {
    const current = session.value
    if (!current) {
      return
    }
    current.elapsed += 1
    if (current.phase === 'ask') {
      current.questionElapsed += 1
      if (current.questionElapsed >= current.timeoutMinutes * 60) {
        void sendAnswer('（本题已超时，系统自动提交）', true)
      }
    }
    if (current.elapsed % 10 === 0) {
      saveInterviewSession(current)
    }
  }, 1000)
}

function stopTimers(): void {
  if (elapsedTimer !== null) {
    window.clearInterval(elapsedTimer)
    elapsedTimer = null
  }
}

function begin(): void {
  const current = session.value
  loading.value = false
  resumeOpen.value = false
  // 上一次评分过程中刷新/断网会留下 analyzing/grading 状态，恢复时重置回作答态，
  // 并把已保存的回答填回输入框，避免会话卡死、也避免重复输入。
  if (current && (current.phase === 'analyzing' || current.phase === 'grading')) {
    const order = current.currentOrder
    const pendingAnswer = current.answers[order]?.trim()
    current.phase = 'ask'
    typingLabel.value = ''
    streamingFeedback.value = ''
    if (pendingAnswer && !input.value.trim()) {
      input.value = pendingAnswer
    }
    ElMessage.info('上次评分被中断，请重新提交本题')
  }
  startTimers()
  saveInterviewSession(session.value as InterviewSession)
  scrollToBottom()
  if (session.value?.phase === 'ask' && currentQuestion.value) {
    speak(currentQuestion.value.content)
  }
  void nextTick(() => inputRef.value?.focus())
}

function resumeInterview(): void {
  if (!session.value) {
    return
  }
  begin()
}

function restartInterview(): void {
  clearInterviewSession()
  speech.stop()
  stopTimers()
  if (speechSynthesisSupported) {
    window.speechSynthesis.cancel()
  }
  session.value = null
  resumeOpen.value = false
  void router.push('/interview')
}

async function sendInput(): Promise<void> {
  const text = input.value.trim()
  if (!text || !canSend.value) {
    return
  }
  input.value = ''
  speech.stop()
  await sendAnswer(text, false)
}

async function sendAnswer(answer: string, auto: boolean): Promise<void> {
  const current = session.value
  if (!current || current.phase !== 'ask') {
    return
  }
  const question = currentQuestion.value
  if (!question) {
    return
  }
  const order = current.currentOrder
  const duration = Math.max(0, current.questionElapsed)

  appendMessage({
    id: newMessageId(),
    role: 'student',
    content: answer,
    time: nowLabel(),
    order,
    kind: 'answer'
  })
  current.answers[order] = answer
  current.durations[order] = duration
  current.phase = 'analyzing'
  current.questionElapsed = duration
  streamingFeedback.value = ''
  typingLabel.value = auto ? '本题已超时，正在提交…' : 'AI 面试官正在分析你的回答…'
  saveInterviewSession(current)
  scrollToBottom()

  const controller = new AbortController()
  sseAbort?.abort()
  sseAbort = controller
  try {
    await streamSse(
      `/interview/sessions/${encodeURIComponent(current.sessionId)}/answers`,
      {
        order,
        studentAnswer: answer,
        durationSeconds: duration
      },
      {
        onStatus: (event: SseStatusEvent) => handleStatus(event),
        onToken: (event: SseTokenEvent) => {
          streamingFeedback.value += event.delta
          scrollToBottom()
        },
        onGrade: (event: SseGradeEvent) => handleGrade(order, event),
        onError: (event: SseErrorEvent) => handleStreamError(event),
        onDone: () => {
          if (session.value && (session.value.phase === 'analyzing' || session.value.phase === 'grading')) {
            session.value.phase = 'ask'
            typingLabel.value = ''
            streamingFeedback.value = ''
            ElMessage.warning('评分未完成，请重新提交本题')
            saveInterviewSession(session.value)
          }
        }
      },
      controller.signal
    )
  } catch (error) {
    if (!controller.signal.aborted) {
      handleStreamFailure(messageOf(error, '评分请求失败，请稍后重试'))
    }
  } finally {
    if (sseAbort === controller) {
      sseAbort = null
    }
  }
}

function handleStatus(event: SseStatusEvent): void {
  const current = session.value
  if (!current) {
    return
  }
  current.phase = event.phase === 'grading' ? 'grading' : 'analyzing'
  typingLabel.value = event.phase === 'grading'
    ? '评分中…'
    : 'AI 面试官正在分析你的回答…'
  saveInterviewSession(current)
  scrollToBottom()
}

function handleGrade(order: number, event: SseGradeEvent): void {
  const current = session.value
  if (!current) {
    return
  }
  current.scores[order] = event.score
  current.feedbacks[order] = event.feedback
  current.referencePoints[order] = event.referencePoints ?? []
  current.degraded[order] = event.degraded
  streamingFeedback.value = ''
  typingLabel.value = ''
  appendMessage({
    id: newMessageId(),
    role: 'ai',
    content: '',
    time: nowLabel(),
    order,
    score: event.score,
    feedback: event.feedback,
    referencePoints: event.referencePoints ?? [],
    degraded: event.degraded,
    kind: 'grade'
  })

  if (order < current.questionCount) {
    current.currentOrder = order + 1
    current.questionElapsed = 0
    current.phase = 'ask'
    const next = current.questions[current.currentOrder - 1]
    appendMessage({
      id: newMessageId(),
      role: 'ai',
      content: next.content,
      time: nowLabel(),
      order: next.order,
      kind: 'question'
    }, next.content)
    void nextTick(() => inputRef.value?.focus())
  } else {
    current.phase = 'done'
    const endText = '本次面试结束。你的综合表现已生成报告，可以查看六个维度的得分与改进建议。'
    appendMessage({
      id: newMessageId(),
      role: 'ai',
      content: endText,
      time: nowLabel(),
      kind: 'end'
    }, endText)
    window.setTimeout(() => {
      void promptReport()
    }, 500)
  }
  saveInterviewSession(current)
  scrollToBottom()
}

function handleStreamError(event: SseErrorEvent): void {
  const current = session.value
  if (!current) {
    return
  }
  if (event.code === 20008) {
    clearInterviewSession()
    session.value = null
    ElMessage.error('面试会话已过期，请重新开始')
    void router.replace('/interview')
    return
  }
  current.phase = 'ask'
  typingLabel.value = ''
  streamingFeedback.value = ''
  saveInterviewSession(current)
  ElMessage.error(event.message || '评分失败，请稍后重试')
}

function handleStreamFailure(message: string): void {
  const current = session.value
  if (!current) {
    return
  }
  current.phase = 'ask'
  typingLabel.value = ''
  streamingFeedback.value = ''
  saveInterviewSession(current)
  ElMessage.error(message)
}

function promptReport(): void {
  if (!session.value || session.value.phase !== 'done' || finishing.value) {
    return
  }
  endConfirmOpen.value = true
}

function endInterview(): void {
  if (!session.value || finishing.value) {
    return
  }
  endConfirmOpen.value = true
}

async function confirmEnd(): Promise<void> {
  endConfirmOpen.value = false
  await finishReport()
}

async function finishReport(): Promise<void> {
  const current = session.value
  if (!current || finishing.value) {
    return
  }
  finishing.value = true
  stopTimers()
  sseAbort?.abort()
  speech.stop()
  if (speechSynthesisSupported) {
    window.speechSynthesis.cancel()
  }
  const answers = current.questions.map((question) => ({
    order: question.order,
    studentAnswer: current.answers[question.order] ?? null,
    durationSeconds: current.durations[question.order] ?? 0
  }))
  try {
    const result = await finishInterview(current.sessionId, { answers })
    clearInterviewSession()
    session.value = null
    await router.replace(`/interview/report/${result.reportId}`)
  } catch (error) {
    const api = error instanceof ApiError ? error : null
    if (api?.code === 20008) {
      clearInterviewSession()
      session.value = null
      ElMessage.error('面试会话已过期，请重新开始')
      await router.replace('/interview')
      return
    }
    finishing.value = false
    ElMessage.error(messageOf(error, '生成面试报告失败，请稍后重试'))
    if (session.value) {
      startTimers()
    }
  }
}

async function exitInterview(): Promise<void> {
  if (!session.value) {
    await router.push('/interview')
    return
  }
  try {
    await ElMessageBox.confirm(
      '退出后本次会话保存在本地，下次可以继续。',
      '退出面试',
      { confirmButtonText: '退出', cancelButtonText: '继续面试', type: 'warning' }
    )
  } catch {
    return
  }
  stopTimers()
  saveInterviewSession(session.value)
  speech.stop()
  if (speechSynthesisSupported) {
    window.speechSynthesis.cancel()
  }
  await router.push('/interview')
}

function scrollToQuestion(order: number): void {
  const element = document.getElementById(`iv-q-${order}`)
  element?.scrollIntoView({ behavior: 'smooth', block: 'center' })
}

function updateOnline(): void {
  online.value = navigator.onLine
}

function handleBeforeUnload(): void {
  if (session.value) {
    saveInterviewSession(session.value)
  }
}

watch(
  () => speech.transcript.value,
  (value) => {
    if (speech.listening.value) {
      input.value = micBase ? `${micBase} ${value}` : value
    }
  }
)

watch(
  () => speech.error.value,
  (value) => {
    if (value) {
      ElMessage.error(value)
    }
  }
)

onMounted(() => {
  session.value = loadInterviewSession()
  if (!session.value) {
    loading.value = false
    return
  }
  if (session.value.justStarted) {
    session.value.justStarted = false
    saveInterviewSession(session.value)
    begin()
  } else {
    loading.value = false
    resumeOpen.value = true
  }
  window.addEventListener('online', updateOnline)
  window.addEventListener('offline', updateOnline)
  window.addEventListener('beforeunload', handleBeforeUnload)
})

onBeforeUnmount(() => {
  stopTimers()
  sseAbort?.abort()
  speech.stop()
  if (speechSynthesisSupported) {
    window.speechSynthesis.cancel()
  }
  if (session.value) {
    saveInterviewSession(session.value)
  }
  window.removeEventListener('online', updateOnline)
  window.removeEventListener('offline', updateOnline)
  window.removeEventListener('beforeunload', handleBeforeUnload)
})
</script>

<template>
  <section class="iv-page">
    <div v-if="loading" class="state-panel">正在恢复面试会话…</div>

    <template v-else-if="session">
      <div class="iv-topbar">
        <button type="button" class="btn btn-sm btn-ghost" @click="exitInterview">← 退出</button>
        <span class="live-dot" aria-hidden="true" />
        <span class="iv-subject">{{ session.subjectName }} · {{ difficultyLabel(session.difficulty) }}</span>
        <span class="online-pill" :class="{ offline: !online }">
          <i />{{ online ? '在线' : '离线' }}
        </span>
        <span class="iv-progress-text">题目 {{ session.currentOrder }} / {{ session.questionCount }}</span>
        <span class="iv-elapsed">{{ formatElapsed(session.elapsed) }}</span>
        <button type="button" class="btn btn-sm" @click="toggleVoice">
          {{ session.voiceEnabled ? '🔊 播报中' : '🔇 已静音' }}
        </button>
        <button type="button" class="btn btn-sm" @click="sideOpen = !sideOpen">
          {{ sideOpen ? '收起题目' : '展开题目' }}
        </button>
        <button type="button" class="btn btn-sm btn-danger" @click="endInterview">结束面试</button>
      </div>

      <div class="iv-layout" :class="{ 'side-collapsed': !sideOpen }">
        <main class="iv-main">
          <div ref="chatRef" class="iv-chat">
            <template v-for="message in session.messages" :key="message.id">
              <div
                v-if="message.role === 'ai'"
                :id="message.kind === 'question' && message.order ? `iv-q-${message.order}` : undefined"
                class="msg ai"
              >
                <div class="msg-who">
                  <span>AI 面试官</span>
                  <span class="msg-time">{{ message.time }}</span>
                  <button
                    type="button"
                    class="replay-button"
                    aria-label="重播本条消息"
                    @click="replay(message)"
                  >
                    重播
                  </button>
                </div>
                <div v-if="message.kind === 'grade'" class="grade-bubble">
                  <div class="grade-head">
                    <span class="grade-score" :class="scoreTone(message.score)">
                      {{ message.score?.toFixed(1) }}
                    </span>
                    <span class="grade-unit">/ 10</span>
                    <span v-if="message.degraded" class="badge badge-amber">降级评分</span>
                  </div>
                  <p class="grade-feedback">{{ message.feedback }}</p>
                  <div v-if="message.referencePoints?.length" class="grade-points">
                    <span
                      v-for="(point, index) in message.referencePoints"
                      :key="index"
                      class="badge"
                      :class="point.hit ? 'badge-ok' : 'badge-miss'"
                    >
                      {{ point.hit ? '✓' : '✕' }} {{ point.point }}
                    </span>
                  </div>
                </div>
                <div v-else class="msg-bubble">
                  <span v-if="message.kind === 'question' && message.order" class="question-index">
                    第 {{ message.order }} 题
                  </span>
                  <span class="message-text">{{ message.content }}</span>
                </div>
              </div>

              <div v-else class="msg student">
                <div class="msg-who">{{ message.time }}</div>
                <div class="msg-bubble">{{ message.content }}</div>
              </div>
            </template>

            <div v-if="isBusy" class="msg ai typing-msg">
              <div class="msg-who"><span>AI 面试官</span></div>
              <div class="msg-bubble typing-bubble">
                <span class="typing-label">{{ typingLabel }}</span>
                <span class="typing-dots" aria-hidden="true"><i /><i /><i /></span>
                <p v-if="streamingFeedback" class="streaming-text">{{ streamingFeedback }}</p>
              </div>
            </div>
          </div>

          <div v-if="session.phase === 'done'" class="done-banner">
            <span>面试已完成，可以生成并查看这次面试的复盘报告。</span>
            <button type="button" class="btn btn-sm btn-primary" :disabled="finishing" @click="finishReport">
              生成并查看报告
            </button>
          </div>

          <div class="iv-input-area">
            <div class="iv-question-meta">
              <span class="countdown" :class="{ urgent: questionRemaining <= 60 }">
                本题倒计时 {{ formatCountdown(questionRemaining) }}
              </span>
              <span class="small muted">Enter 发送 · Shift + Enter 换行</span>
            </div>
            <div class="iv-input-row">
              <textarea
                ref="inputRef"
                v-model="input"
                class="textarea iv-textarea"
                :disabled="session.phase !== 'ask' || finishing || !online"
                placeholder="输入你的回答，先给结论，再补支撑点…"
                @keydown.enter.exact.prevent="sendInput"
              />
              <button
                type="button"
                class="btn iv-mic"
                :class="{ listening: speech.listening.value }"
                :disabled="session.phase !== 'ask'"
                :aria-label="speech.listening.value ? '停止语音输入' : '开始语音输入'"
                :title="speech.supported ? '语音转文字' : '当前浏览器不支持语音识别'"
                @click="toggleMic"
              >
                {{ speech.listening.value ? '●' : '🎙' }}
              </button>
              <button
                type="button"
                class="btn btn-primary iv-send"
                :disabled="!canSend"
                @click="sendInput"
              >
                发送
              </button>
            </div>
            <p v-if="!online" class="offline-hint">网络已断开，恢复网络后可继续作答。</p>
          </div>
        </main>

        <aside v-show="sideOpen" class="iv-side">
          <div class="side-head">
            <h2>面试题目</h2>
            <span class="badge">{{ answeredCount }} / {{ session.questionCount }} 已答</span>
          </div>
          <div class="side-question-list">
            <button
              v-for="question in session.questions"
              :key="question.order"
              type="button"
              class="side-question"
              :class="{ active: question.order === session.currentOrder }"
              @click="scrollToQuestion(question.order)"
            >
              <span class="side-index">{{ String(question.order).padStart(2, '0') }}</span>
              <span class="side-text">{{ question.content }}</span>
              <span
                class="side-score"
                :class="scoreTone(session.scores[question.order])"
              >
                {{ session.scores[question.order] !== undefined ? session.scores[question.order].toFixed(1) : '—' }}
              </span>
            </button>
          </div>
          <div class="side-divider" />
          <div class="side-stats">
            <div><span>已回答</span><b>{{ answeredCount }} / {{ session.questionCount }}</b></div>
            <div><span>平均分</span><b>{{ averageScore === null ? '—' : averageScore.toFixed(1) }}</b></div>
            <div><span>用时</span><b>{{ formatElapsed(session.elapsed) }}</b></div>
            <div><span>进度</span><b>{{ Math.round(progressPercent) }}%</b></div>
          </div>
          <div class="side-progress"><i :style="{ width: `${progressPercent}%` }" /></div>
        </aside>
      </div>
    </template>

    <div v-else class="empty-panel">
      <h2>还没有进行中的面试</h2>
      <p>先选择方向和难度，再开始一场模拟面试。</p>
      <RouterLink class="btn btn-primary" to="/interview">去配置面试</RouterLink>
    </div>

    <Teleport to="body">
      <Transition name="iv-overlay">
        <div v-if="resumeOpen" class="iv-overlay">
          <section class="iv-dialog" role="dialog" aria-modal="true">
            <p class="eyebrow">Continue</p>
            <h2>继续上次的面试？</h2>
            <p class="muted small">{{ resumeSummary }}</p>
            <footer class="iv-dialog-foot">
              <button type="button" class="btn" @click="restartInterview">重新开始</button>
              <button type="button" class="btn btn-primary" @click="resumeInterview">继续面试</button>
            </footer>
          </section>
        </div>
      </Transition>

      <Transition name="iv-overlay">
        <div v-if="endConfirmOpen" class="iv-overlay">
          <section class="iv-dialog" role="dialog" aria-modal="true">
            <p class="eyebrow">{{ interviewCompleted ? 'Report' : 'Finish' }}</p>
            <h2>{{ interviewCompleted ? '查看面试报告？' : '结束本次面试？' }}</h2>
            <p class="muted small">
              {{
                interviewCompleted
                  ? '本次面试已完成，是否现在生成并查看面试报告？'
                  : '已作答的题目会保留，结束后面试报告将按已答内容生成。'
              }}
            </p>
            <footer class="iv-dialog-foot">
              <button type="button" class="btn" @click="endConfirmOpen = false">
                {{ interviewCompleted ? '稍后' : '继续面试' }}
              </button>
              <button type="button" class="btn btn-primary" @click="confirmEnd">
                {{ interviewCompleted ? '查看报告' : '结束并生成报告' }}
              </button>
            </footer>
          </section>
        </div>
      </Transition>

      <Transition name="iv-overlay">
        <div v-if="finishing" class="iv-overlay">
          <section class="iv-dialog finishing-dialog" role="dialog" aria-modal="true">
            <span class="finishing-spinner" aria-hidden="true" />
            <h2>正在生成面试报告</h2>
            <p class="muted small">正在汇总六维度评分、优缺点与改进建议，请稍候…</p>
          </section>
        </div>
      </Transition>
    </Teleport>
  </section>
</template>

<style scoped>
.iv-page {
  padding: 22px 0 24px;
}

.state-panel {
  padding: 24px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r);
  color: var(--ink-3);
  font-size: 13px;
}

.iv-topbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  padding: 10px 14px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r-sm);
  box-shadow: var(--shadow);
}

.live-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--jade);
  box-shadow: 0 0 0 4px var(--jade-soft);
}

.iv-subject {
  font-size: 13px;
  font-weight: 500;
}

.online-pill {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 3px 8px;
  border-radius: 999px;
  background: var(--jade-soft);
  color: var(--jade-2);
  font-size: 11px;
}

.online-pill i {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: currentColor;
}

.online-pill.offline {
  background: var(--red-soft);
  color: var(--red);
}

.iv-progress-text,
.iv-elapsed {
  color: var(--ink-3);
  font-family: var(--font-mono);
  font-size: 12px;
}

.iv-elapsed {
  margin-left: auto;
}

.iv-layout {
  display: grid;
  grid-template-columns: 1fr 300px;
  gap: 18px;
  align-items: start;
  margin-top: 16px;
}

.iv-layout.side-collapsed {
  grid-template-columns: 1fr;
}

.iv-main {
  display: flex;
  flex-direction: column;
  min-width: 0;
  height: calc(100vh - 224px);
  min-height: 560px;
  max-height: 860px;
  overflow: hidden;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r);
}

.iv-chat {
  flex: 1;
  overflow-y: auto;
  padding: 20px 22px 10px;
  scroll-behavior: smooth;
}

.msg {
  margin-bottom: 16px;
}

.msg-who {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 5px;
  color: var(--ink-3);
  font-size: 11px;
}

.msg.student .msg-who {
  justify-content: flex-end;
}

.msg-time {
  color: var(--ink-4);
  font-family: var(--font-mono);
}

.replay-button {
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--jade);
  font-size: 11px;
}

.msg-bubble {
  display: inline-block;
  max-width: 82%;
  padding: 11px 14px;
  background: var(--surface-2);
  border: 1px solid var(--line);
  border-radius: 14px;
  color: var(--ink-2);
  font-size: 13.5px;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
}

.msg.student .msg-bubble {
  float: right;
  background: var(--jade);
  border-color: var(--jade);
  color: #fff;
}

.msg.student::after {
  content: '';
  display: block;
  clear: both;
}

.question-index {
  display: inline-block;
  margin-right: 8px;
  padding: 1px 7px;
  border-radius: 5px;
  background: var(--jade-soft);
  color: var(--jade-2);
  font-size: 11px;
  font-weight: 500;
  vertical-align: 1px;
}

.message-text {
  white-space: pre-wrap;
}

.grade-bubble {
  max-width: 82%;
  padding: 14px 16px;
  background: var(--surface-2);
  border: 1px solid var(--line);
  border-radius: 14px;
}

.grade-head {
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.grade-score {
  font-family: var(--font-display);
  font-size: 28px;
  line-height: 1;
}

.grade-unit {
  color: var(--ink-3);
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

.grade-feedback {
  margin-top: 8px;
  color: var(--ink-2);
  font-size: 12.5px;
  line-height: 1.75;
  white-space: pre-wrap;
}

.grade-points {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
  margin-top: 10px;
}

.badge {
  display: inline-flex;
  align-items: center;
  padding: 2px 7px;
  border-radius: 6px;
  background: var(--paper-2);
  color: var(--ink-2);
  font-size: 11px;
  line-height: 1.5;
}

.badge-ok {
  background: var(--jade-soft);
  color: var(--jade-2);
}

.badge-miss {
  background: var(--paper-2);
  color: var(--ink-3);
}

.badge-amber {
  background: var(--amber-soft);
  color: var(--amber);
}

.typing-bubble {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.typing-label {
  color: var(--ink-3);
}

.typing-dots {
  display: inline-flex;
  gap: 3px;
}

.typing-dots i {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--jade);
  animation: typing-pulse 1s ease-in-out infinite;
}

.typing-dots i:nth-child(2) {
  animation-delay: 0.15s;
}

.typing-dots i:nth-child(3) {
  animation-delay: 0.3s;
}

@keyframes typing-pulse {
  0%,
  100% {
    opacity: 0.25;
    transform: translateY(0);
  }
  50% {
    opacity: 1;
    transform: translateY(-2px);
  }
}

.streaming-text {
  flex-basis: 100%;
  margin-top: 4px;
  color: var(--ink-2);
  font-size: 12.5px;
  line-height: 1.7;
  white-space: pre-wrap;
}

.done-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 12px 20px;
  background: var(--jade-soft);
  border-top: 1px solid var(--line);
  color: var(--jade-2);
  font-size: 12.5px;
}

.iv-input-area {
  padding: 12px 18px 16px;
  border-top: 1px solid var(--line);
  background: var(--surface);
}

.iv-question-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.countdown {
  color: var(--ink-3);
  font-family: var(--font-mono);
  font-size: 11.5px;
}

.countdown.urgent {
  color: var(--red);
  font-weight: 600;
}

.iv-input-row {
  display: flex;
  align-items: stretch;
  gap: 8px;
}

.iv-textarea {
  flex: 1;
  min-height: 74px;
  max-height: 160px;
  resize: vertical;
}

.iv-mic {
  width: 52px;
  padding: 0;
  font-size: 18px;
}

.iv-mic.listening {
  background: var(--red-soft);
  border-color: var(--red);
  color: var(--red);
}

.iv-send {
  width: 82px;
}

.offline-hint {
  margin-top: 6px;
  color: var(--red);
  font-size: 11.5px;
}

.iv-side {
  position: sticky;
  top: 88px;
  padding: 18px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r);
}

.side-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.side-head h2 {
  font-size: 13px;
  letter-spacing: 0;
}

.side-question-list {
  display: grid;
  gap: 3px;
  max-height: 320px;
  margin-top: 14px;
  overflow-y: auto;
}

.side-question {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: var(--ink-2);
  text-align: left;
}

.side-question.active {
  background: var(--surface-2);
  color: var(--ink);
}

.side-index {
  flex: none;
  color: var(--ink-4);
  font-family: var(--font-mono);
  font-size: 10.5px;
}

.side-text {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.side-score {
  flex: none;
  font-family: var(--font-mono);
  font-size: 11px;
  color: var(--ink-4);
}

.side-divider {
  height: 1px;
  margin: 16px 0;
  background: var(--line);
}

.side-stats {
  display: grid;
  gap: 9px;
}

.side-stats > div {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
  color: var(--ink-3);
  font-size: 12px;
}

.side-stats b {
  color: var(--ink-2);
  font-family: var(--font-mono);
  font-size: 12px;
}

.side-progress {
  height: 4px;
  margin-top: 14px;
  overflow: hidden;
  border-radius: 99px;
  background: var(--paper-2);
}

.side-progress i {
  display: block;
  height: 100%;
  border-radius: 99px;
  background: var(--jade);
  transition: width 0.25s ease;
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
  margin-top: 20px;
}

/* ---------- 弹窗 ---------- */
.iv-overlay {
  position: fixed;
  inset: 0;
  z-index: 100;
  display: grid;
  place-items: center;
  padding: 20px;
  background: rgba(21, 24, 29, 0.46);
  overscroll-behavior: contain;
}

.iv-dialog {
  width: min(500px, 100%);
  padding: 28px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: 18px;
  box-shadow: var(--shadow-lg);
}

.iv-dialog h2 {
  margin-top: 4px;
  font-family: var(--font-display);
  font-size: 26px;
  letter-spacing: 0;
}

.iv-dialog p {
  margin-top: 8px;
}

.iv-dialog-foot {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 24px;
}

.finishing-dialog {
  text-align: center;
}

.finishing-spinner {
  display: inline-block;
  width: 34px;
  height: 34px;
  margin-bottom: 12px;
  border: 3px solid var(--paper-2);
  border-top-color: var(--jade);
  border-radius: 50%;
  animation: spin 0.9s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.iv-overlay-enter-active,
.iv-overlay-leave-active {
  transition: opacity 0.18s ease;
}

.iv-overlay-enter-from,
.iv-overlay-leave-to {
  opacity: 0;
}

:where(.side-question, .replay-button, .iv-mic, .iv-send):focus-visible {
  outline: 2px solid var(--jade);
  outline-offset: 2px;
}

@media (hover: hover) {
  .side-question:hover {
    background: var(--surface-2);
  }

  .replay-button:hover {
    text-decoration: underline;
  }
}

@media (max-width: 960px) {
  .iv-layout,
  .iv-layout.side-collapsed {
    grid-template-columns: 1fr;
  }

  .iv-main {
    height: auto;
    min-height: 540px;
  }

  .iv-chat {
    max-height: 58vh;
    min-height: 380px;
  }

  .iv-side {
    position: static;
  }
}

@media (max-width: 700px) {
  .iv-page {
    padding-top: 14px;
  }

  .iv-elapsed {
    margin-left: 0;
  }

  .msg-bubble,
  .grade-bubble {
    max-width: 92%;
  }

  .iv-chat {
    padding: 16px 14px 8px;
  }

  .iv-input-area {
    padding: 10px 12px 14px;
  }

  .iv-input-row {
    flex-wrap: wrap;
  }

  .iv-textarea {
    flex-basis: 100%;
  }

  .iv-mic,
  .iv-send {
    height: 38px;
    flex: 1;
  }

  .done-banner {
    flex-direction: column;
    align-items: flex-start;
  }

  .iv-dialog {
    padding: 22px 18px;
  }

  .iv-dialog-foot {
    flex-wrap: wrap;
  }
}

@media (prefers-reduced-motion: reduce) {
  .iv-chat,
  .side-progress i,
  .typing-dots i,
  .finishing-spinner,
  .iv-overlay-enter-active,
  .iv-overlay-leave-active {
    transition: none;
    animation: none;
  }
}
</style>
