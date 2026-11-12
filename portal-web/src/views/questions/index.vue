<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElPagination } from 'element-plus'
import { ApiError } from '@/api/http'
import {
  getBankQuestionDetail,
  getBankQuestions,
  getBankStages,
  getBankSubjects,
  type BankQuestionDetail,
  type BankQuestionItem,
  type BankStageItem,
  type BankSubjectItem
} from '@/api/questions'

interface FilterOption {
  label: string
  value: number | null
}

const TYPE_LABELS: Record<number, string> = { 1: '单选', 2: '多选', 3: '判断', 4: '简答' }
const DIFFICULTY_LABELS: Record<number, string> = { 1: '简单', 2: '中等', 3: '困难' }
const PAGE_SIZE = 10

const typeOptions: FilterOption[] = [
  { label: '全部', value: null },
  { label: '单选', value: 1 },
  { label: '多选', value: 2 },
  { label: '判断', value: 3 },
  { label: '简答', value: 4 }
]
const difficultyOptions: FilterOption[] = [
  { label: '全部', value: null },
  { label: '简单', value: 1 },
  { label: '中等', value: 2 },
  { label: '困难', value: 3 }
]

const route = useRoute()

const loadingSubjects = ref(true)
const loadError = ref('')
const subjects = ref<BankSubjectItem[]>([])
const selectedSubjectId = ref<number | null>(null)

const loadingStages = ref(false)
const stageError = ref('')
const stages = ref<BankStageItem[]>([])
const selectedStage = ref<BankStageItem | null>(null)

const loadingQuestions = ref(false)
const questionError = ref('')
const questions = ref<BankQuestionItem[]>([])
const total = ref(0)
const currentPage = ref(1)
const filters = reactive({
  type: null as number | null,
  difficulty: null as number | null,
  keyword: ''
})

const detailOpen = ref(false)
const detailLoading = ref(false)
const detailError = ref('')
const activeQuestion = ref<BankQuestionDetail | null>(null)
const activeQuestionLabel = ref('')
const detailQuestionId = ref<number | null>(null)
const closeButton = ref<HTMLButtonElement | null>(null)
const dialogRef = ref<HTMLElement | null>(null)
let detailReturnFocus: HTMLElement | null = null

const selectedSubject = computed(
  () => subjects.value.find((subject) => subject.id === selectedSubjectId.value) ?? null
)
const hasActiveFilters = computed(
  () => filters.type !== null || filters.difficulty !== null || filters.keyword.trim() !== ''
)
const isChoice = computed(() =>
  activeQuestion.value ? activeQuestion.value.type === 1 || activeQuestion.value.type === 2 : false
)
const correctKeys = computed(() => {
  const question = activeQuestion.value
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
const showAnswer = computed(() => Boolean(activeQuestion.value?.answer))

function messageOf(error: unknown, fallback: string): string {
  return error instanceof ApiError ? error.message : fallback
}

function typeLabel(type: number): string {
  return TYPE_LABELS[type] ?? '未知'
}

function difficultyLabel(difficulty: number): string {
  return DIFFICULTY_LABELS[difficulty] ?? '未知'
}

function typeClass(type: number): string {
  return `badge-type-${type}`
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
    .slice(0, 3)
}

function formatRate(rate: number): string {
  return Number.isInteger(rate) ? `${rate}%` : `${rate.toFixed(1)}%`
}

function partLink(params: Record<string, number>) {
  return { path: '/practice', query: params }
}

function querySubjectId(): number | null {
  const raw = route.query.subjectId
  const value = Array.isArray(raw) ? raw[0] : raw
  const id = Number(value)
  return Number.isInteger(id) && id > 0 ? id : null
}

async function loadSubjects(): Promise<void> {
  loadingSubjects.value = true
  loadError.value = ''
  try {
    subjects.value = await getBankSubjects()
    // 从首页「热门学科」进入时带 subjectId，需定位到对应学科；否则默认第一个有阶段的学科。
    const requested = querySubjectId()
    const target =
      subjects.value.find((subject) => subject.id === requested) ??
      subjects.value.find((subject) => subject.stageCount > 0) ??
      subjects.value[0] ??
      null
    if (target) {
      selectedSubjectId.value = target.id
      await loadStages()
    }
  } catch (error) {
    loadError.value = messageOf(error, '题库加载失败，请稍后重试')
  } finally {
    loadingSubjects.value = false
  }
}

async function loadStages(): Promise<void> {
  if (selectedSubjectId.value === null) {
    return
  }
  loadingStages.value = true
  stageError.value = ''
  stages.value = []
  try {
    stages.value = await getBankStages(selectedSubjectId.value)
  } catch (error) {
    stageError.value = messageOf(error, '阶段加载失败，请稍后重试')
  } finally {
    loadingStages.value = false
  }
}

async function selectSubject(subjectId: number): Promise<void> {
  const changed = selectedSubjectId.value !== subjectId
  selectedSubjectId.value = subjectId
  selectedStage.value = null
  questions.value = []
  total.value = 0
  questionError.value = ''
  if (changed || !stages.value.length) {
    await loadStages()
  }
}

function openStage(stage: BankStageItem): void {
  selectedStage.value = stage
  filters.type = null
  filters.difficulty = null
  filters.keyword = ''
  void loadQuestions(1)
}

function backToStages(): void {
  selectedStage.value = null
  questions.value = []
  total.value = 0
  questionError.value = ''
  currentPage.value = 1
}

async function loadQuestions(page = 1): Promise<void> {
  if (selectedSubjectId.value === null || !selectedStage.value) {
    return
  }
  loadingQuestions.value = true
  questionError.value = ''
  questions.value = []
  try {
    const result = await getBankQuestions({
      subjectId: selectedSubjectId.value,
      stageId: selectedStage.value.id,
      type: filters.type ?? undefined,
      difficulty: filters.difficulty ?? undefined,
      keyword: filters.keyword.trim() || undefined,
      page,
      pageSize: PAGE_SIZE
    })
    questions.value = result.list
    total.value = result.total
    currentPage.value = page
  } catch (error) {
    questionError.value = messageOf(error, '题目加载失败，请稍后重试')
  } finally {
    loadingQuestions.value = false
  }
}

function handlePageChange(page: number): void {
  void loadQuestions(page)
}

function setType(value: number | null): void {
  if (filters.type === value) {
    return
  }
  filters.type = value
  void loadQuestions(1)
}

function setDifficulty(value: number | null): void {
  if (filters.difficulty === value) {
    return
  }
  filters.difficulty = value
  void loadQuestions(1)
}

function applyFilters(): void {
  void loadQuestions(1)
}

function clearFilters(): void {
  filters.type = null
  filters.difficulty = null
  filters.keyword = ''
  void loadQuestions(1)
}

async function loadDetail(id: number): Promise<void> {
  detailLoading.value = true
  detailError.value = ''
  activeQuestion.value = null
  try {
    activeQuestion.value = await getBankQuestionDetail(id)
  } catch (error) {
    detailError.value = messageOf(error, '题目详情加载失败，请稍后重试')
  } finally {
    detailLoading.value = false
  }
}

function openDetail(question: BankQuestionItem, index: number): void {
  detailQuestionId.value = question.id
  activeQuestionLabel.value = `第 ${(currentPage.value - 1) * PAGE_SIZE + index + 1} 题`
  detailOpen.value = true
  void loadDetail(question.id)
}

function retryDetail(): void {
  if (detailQuestionId.value !== null) {
    void loadDetail(detailQuestionId.value)
  }
}

function closeDetail(): void {
  detailOpen.value = false
  activeQuestion.value = null
  detailError.value = ''
  detailQuestionId.value = null
}

function trapDialogFocus(event: KeyboardEvent): void {
  if (event.key !== 'Tab' || !detailOpen.value) {
    return
  }
  const dialog = dialogRef.value
  if (!dialog) {
    return
  }
  const focusable = Array.from(
    dialog.querySelectorAll<HTMLElement>(
      'button:not([disabled]), [href], input:not([disabled]), [tabindex]:not([tabindex="-1"])'
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

watch(detailOpen, async (open) => {
  if (open) {
    detailReturnFocus = document.activeElement instanceof HTMLElement ? document.activeElement : null
    document.body.style.overflow = 'hidden'
    await nextTick()
    closeButton.value?.focus()
  } else {
    document.body.style.overflow = ''
    if (detailReturnFocus?.isConnected) {
      detailReturnFocus.focus()
    }
    detailReturnFocus = null
  }
})

// 已在题库页时再次点击首页热门学科，仅 query 变化不会重新挂载组件，这里同步切换学科。
watch(
  () => route.query.subjectId,
  () => {
    const id = querySubjectId()
    if (id === null || id === selectedSubjectId.value) {
      return
    }
    if (!subjects.value.some((subject) => subject.id === id)) {
      return
    }
    void selectSubject(id)
  }
)

onMounted(() => {
  void loadSubjects()
})

onBeforeUnmount(() => {
  document.body.style.overflow = ''
})
</script>

<template>
  <section class="bank-page">
    <header class="page-head">
      <div>
        <p class="eyebrow">Question Bank</p>
        <h1>题库中心</h1>
        <p class="lead">按学科与阶段浏览题目。游客可看题干，登录后查看答案与解析。</p>
      </div>
    </header>

    <div v-if="loadingSubjects" class="state-panel">正在加载题库…</div>

    <div v-else-if="loadError" class="state-panel state-error">
      <p>{{ loadError }}</p>
      <button type="button" class="btn btn-sm" @click="loadSubjects">重新加载</button>
    </div>

    <div v-else class="bank-layout">
      <aside class="subject-rail" aria-label="学科列表">
        <button
          v-for="subject in subjects"
          :key="subject.id"
          type="button"
          class="subject-item"
          :class="{ active: subject.id === selectedSubjectId }"
          :aria-pressed="subject.id === selectedSubjectId"
          @click="selectSubject(subject.id)"
        >
          <span class="ico" aria-hidden="true">{{ subject.icon }}</span>
          <span class="nm">{{ subject.name }}</span>
          <span class="cnt">{{ subject.stageCount }}</span>
        </button>
        <p v-if="!subjects.length" class="rail-empty">暂无学科</p>
      </aside>

      <main class="bank-main">
        <div v-if="stageError" class="state-panel state-error">
          <p>{{ stageError }}</p>
          <button type="button" class="btn btn-sm" @click="loadStages">重新加载</button>
        </div>

        <div v-else-if="loadingStages" class="state-panel">正在加载阶段…</div>

        <template v-else-if="!selectedStage">
          <template v-if="selectedSubject">
            <div class="bank-head">
              <div class="ico-lg" aria-hidden="true">{{ selectedSubject.icon }}</div>
              <div class="head-copy">
                <h2>{{ selectedSubject.name }}</h2>
                <p>{{ selectedSubject.description }}</p>
              </div>
              <span class="pill">共 {{ selectedSubject.stageCount }} 个阶段</span>
            </div>

            <div v-if="stages.length" class="stage-list">
              <div v-for="stage in stages" :key="stage.id" class="stage-row">
                <div class="ico" aria-hidden="true">{{ stage.icon || '📚' }}</div>
                <div class="stage-copy">
                  <h4>{{ stage.name }}</h4>
                  <p>共 {{ stage.questionCount }} 道题</p>
                </div>
                <div class="acts">
                  <button type="button" class="btn btn-sm" @click="openStage(stage)">查看题目</button>
                  <RouterLink
                    v-if="stage.questionCount > 0"
                    class="btn btn-sm btn-primary"
                    :to="partLink({ stageId: stage.id })"
                  >
                    开始练习
                  </RouterLink>
                  <button v-else type="button" class="btn btn-sm" disabled>开始练习</button>
                </div>
              </div>
            </div>

            <div v-else class="empty-panel">该学科暂无阶段，题目正在录入中</div>
          </template>

          <div v-else class="empty-panel">请选择一个学科方向</div>
        </template>

        <template v-else>
          <div class="bank-crumb">
            <button type="button" class="back-link" @click="backToStages">‹ 返回阶段列表</button>
            <div class="crumb">
              <b>{{ selectedSubject?.name }}</b>
              <span>/</span>
              <span>{{ selectedStage?.name }}</span>
            </div>
          </div>

          <div class="toolbar">
            <div class="filter-group">
              <span class="filter-label">题型</span>
              <div class="chip-group" role="group" aria-label="题型筛选">
                <button
                  v-for="option in typeOptions"
                  :key="`type-${option.value ?? 'all'}`"
                  type="button"
                  class="chip"
                  :class="{ active: filters.type === option.value }"
                  @click="setType(option.value)"
                >
                  {{ option.label }}
                </button>
              </div>
            </div>
            <div class="filter-group">
              <span class="filter-label">难度</span>
              <div class="chip-group" role="group" aria-label="难度筛选">
                <button
                  v-for="option in difficultyOptions"
                  :key="`diff-${option.value ?? 'all'}`"
                  type="button"
                  class="chip"
                  :class="{ active: filters.difficulty === option.value }"
                  @click="setDifficulty(option.value)"
                >
                  {{ option.label }}
                </button>
              </div>
            </div>
            <div class="search">
              <input
                v-model="filters.keyword"
                class="input"
                placeholder="搜索题干或标签"
                @keyup.enter="applyFilters"
              />
              <button type="button" class="btn btn-sm" @click="applyFilters">搜索</button>
            </div>
          </div>

          <div class="list-meta">
            <span>共 {{ total }} 题</span>
            <button v-if="hasActiveFilters" type="button" class="link-button" @click="clearFilters">
              清除筛选
            </button>
          </div>

          <div v-if="loadingQuestions && !questions.length" class="state-panel">正在加载题目…</div>

          <div v-else-if="questionError" class="state-panel state-error">
            <p>{{ questionError }}</p>
            <button type="button" class="btn btn-sm" @click="loadQuestions(currentPage)">
              重新加载
            </button>
          </div>

          <div v-else-if="questions.length" class="q-list">
            <article v-for="(question, index) in questions" :key="question.id" class="qcard">
              <div class="q-meta">
                <span class="q-no">第 {{ (currentPage - 1) * PAGE_SIZE + index + 1 }} 题</span>
                <span class="badge" :class="typeClass(question.type)">{{ typeLabel(question.type) }}</span>
                <span class="badge" :class="difficultyClass(question.difficulty)">
                  {{ difficultyLabel(question.difficulty) }}
                </span>
                <span v-if="question.stageName">{{ question.stageName }}</span>
              </div>

              <p class="q-stem">{{ question.content }}</p>

              <div class="q-foot">
                <span>{{ question.doneCount }} 人做过</span>
                <span>正确率 {{ formatRate(question.accuracyRate) }}</span>
                <span v-for="tag in splitTags(question.tags)" :key="tag" class="tag">{{ tag }}</span>
                <div class="q-actions">
                  <button type="button" class="btn btn-sm" @click="openDetail(question, index)">
                    查看解析
                  </button>
                  <RouterLink
                    class="btn btn-sm btn-primary"
                    :to="partLink({ questionId: question.id })"
                  >
                    开始练习
                  </RouterLink>
                </div>
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
            {{ hasActiveFilters ? '没有符合条件的题目' : '该阶段暂无已上架题目，审核通过后会在这里展示。' }}
          </div>
        </template>
      </main>
    </div>

    <Teleport to="body">
      <Transition name="q-dialog">
        <div
          v-if="detailOpen"
          class="q-overlay"
          @click.self="closeDetail"
          @keydown.esc="closeDetail"
          @keydown="trapDialogFocus"
        >
          <section
            ref="dialogRef"
            class="q-dialog"
            role="dialog"
            aria-modal="true"
            aria-labelledby="q-dialog-title"
          >
            <header class="q-dialog-head">
              <div>
                <p class="eyebrow">Question</p>
                <h2 id="q-dialog-title">{{ activeQuestionLabel }}</h2>
              </div>
              <button ref="closeButton" type="button" class="q-close" aria-label="关闭" @click="closeDetail">
                ✕
              </button>
            </header>

            <div class="q-dialog-body">
              <div v-if="detailLoading" class="state-panel">正在加载题目…</div>

              <div v-else-if="detailError" class="state-panel state-error">
                <p>{{ detailError }}</p>
                <button type="button" class="btn btn-sm" @click="retryDetail">重新加载</button>
              </div>

              <template v-else-if="activeQuestion">
                <div class="detail-meta">
                  <span class="badge" :class="typeClass(activeQuestion.type)">
                    {{ typeLabel(activeQuestion.type) }}
                  </span>
                  <span class="badge" :class="difficultyClass(activeQuestion.difficulty)">
                    {{ difficultyLabel(activeQuestion.difficulty) }}
                  </span>
                  <span v-if="activeQuestion.subjectName">{{ activeQuestion.subjectName }}</span>
                  <span v-if="activeQuestion.stageName">{{ activeQuestion.stageName }}</span>
                  <span>{{ activeQuestion.doneCount }} 人做过</span>
                  <span>正确率 {{ formatRate(activeQuestion.accuracyRate) }}</span>
                  <span v-if="activeQuestion.inWrongBook" class="wrong-badge">已在错题本</span>
                </div>

                <p class="detail-stem">{{ activeQuestion.content }}</p>

                <div v-if="isChoice" class="detail-options">
                  <div
                    v-for="option in activeQuestion.options"
                    :key="option.key"
                    class="detail-option"
                    :class="{ correct: correctKeys.has(option.key) }"
                  >
                    <span class="option-key">{{ option.key }}</span>
                    <span>{{ option.text }}</span>
                  </div>
                </div>

                <div v-if="showAnswer" class="detail-answer">
                  <div class="answer-line" :class="{ long: activeQuestion.type === 4 }">
                    <span class="label">{{ activeQuestion.type === 4 ? '参考答案' : '正确答案' }}</span>
                    <pre v-if="activeQuestion.type === 4" class="reference-answer">{{ activeQuestion.answer }}</pre>
                    <strong v-else>{{ activeQuestion.answer }}</strong>
                  </div>
                  <div v-if="activeQuestion.analysis" class="analysis-block">
                    <h3>答案解析</h3>
                    <p>{{ activeQuestion.analysis }}</p>
                  </div>
                  <p v-else class="muted small">暂无解析</p>
                </div>

                <div v-else class="answer-locked">
                  <p>登录后可查看正确答案与解析。</p>
                  <RouterLink
                    class="btn btn-sm btn-primary"
                    :to="{ path: '/login', query: { redirect: '/bank' } }"
                    @click="closeDetail"
                  >
                    去登录
                  </RouterLink>
                </div>
              </template>
            </div>

            <footer class="q-dialog-foot">
              <button type="button" class="btn" @click="closeDetail">关闭</button>
              <RouterLink
                v-if="activeQuestion"
                class="btn btn-primary"
                :to="partLink({ questionId: activeQuestion.id })"
                @click="closeDetail"
              >
                开始练习
              </RouterLink>
            </footer>
          </section>
        </div>
      </Transition>
    </Teleport>
  </section>
</template>

<style scoped>
.bank-page {
  padding: 40px 0 24px;
}

.page-head h1 {
  margin-top: 6px;
  font-family: var(--font-display);
  font-size: clamp(30px, 3.2vw, 38px);
  line-height: 1.2;
  letter-spacing: 0;
}

.page-head .lead {
  max-width: 640px;
  margin-top: 8px;
  color: var(--ink-2);
  font-size: 14px;
  line-height: 1.75;
  text-wrap: pretty;
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

.empty-panel {
  margin-top: 16px;
  padding: 36px 24px;
  background: var(--surface-2);
  border: 1px dashed var(--line-2);
  border-radius: var(--r);
  color: var(--ink-3);
  font-size: 13px;
  text-align: center;
}

/* ---------- 左侧学科栏 ---------- */
.bank-layout {
  display: grid;
  grid-template-columns: 264px 1fr;
  gap: 22px;
  align-items: start;
  margin-top: 24px;
}

.subject-rail {
  position: sticky;
  top: 90px;
  padding: 10px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r);
}

.subject-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 10px 11px;
  border: 0;
  border-radius: 10px;
  background: transparent;
  color: var(--ink-2);
  text-align: left;
  transition: background-color 0.14s ease, color 0.14s ease;
}

.subject-item.active {
  background: var(--ink);
  color: #fff;
}

.subject-item .ico {
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  flex: none;
  border-radius: 8px;
  background: var(--paper);
  font-size: 15px;
}

.subject-item.active .ico {
  background: rgba(255, 255, 255, 0.14);
}

.subject-item .nm {
  font-size: 12.5px;
  line-height: 1.35;
}

.subject-item .cnt {
  margin-left: auto;
  color: var(--ink-4);
  font-family: var(--font-mono);
  font-size: 11px;
}

.subject-item.active .cnt {
  color: rgba(255, 255, 255, 0.55);
}

.rail-empty {
  padding: 18px 10px;
  color: var(--ink-3);
  font-size: 12px;
  text-align: center;
}

.bank-main {
  min-width: 0;
}

/* ---------- 阶段列表 ---------- */
.bank-head {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 22px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r);
}

.bank-head .ico-lg {
  display: grid;
  place-items: center;
  width: 52px;
  height: 52px;
  flex: none;
  border-radius: 14px;
  background: var(--paper);
  font-size: 24px;
}

.head-copy {
  min-width: 0;
}

.bank-head h2 {
  font-family: var(--font-display);
  font-size: 24px;
  line-height: 1.3;
  letter-spacing: 0;
}

.bank-head p {
  margin-top: 3px;
  color: var(--ink-3);
  font-size: 12.5px;
  line-height: 1.65;
}

.pill {
  display: inline-flex;
  align-items: center;
  height: 26px;
  margin-left: auto;
  padding: 0 11px;
  flex: none;
  border: 1px solid var(--line-2);
  border-radius: 999px;
  background: var(--surface);
  color: var(--ink-2);
  font-size: 12px;
}

.stage-list {
  display: grid;
  gap: 10px;
  margin-top: 16px;
}

.stage-row {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 18px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r-sm);
}

.stage-row .ico {
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  flex: none;
  border-radius: 10px;
  background: var(--paper);
  font-size: 17px;
}

.stage-copy {
  min-width: 0;
}

.stage-row h4 {
  font-size: 14px;
  line-height: 1.45;
  letter-spacing: 0;
}

.stage-row p {
  margin-top: 2px;
  color: var(--ink-3);
  font-size: 12px;
}

.stage-row .acts {
  display: flex;
  gap: 8px;
  margin-left: auto;
  flex: none;
}

/* ---------- 题目列表 ---------- */
.bank-crumb {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.back-link {
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--jade);
  font-size: 12.5px;
  font-weight: 500;
}

.crumb {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--ink-3);
  font-size: 12.5px;
}

.crumb b {
  color: var(--ink-2);
  font-weight: 500;
}

.toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  margin: 16px 0;
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-label {
  color: var(--ink-3);
  font-size: 11.5px;
  white-space: nowrap;
}

.chip-group {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.chip {
  display: inline-flex;
  align-items: center;
  height: 30px;
  padding: 0 12px;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: var(--surface);
  color: var(--ink-2);
  font-size: 12px;
  transition: background-color 0.14s ease, border-color 0.14s ease, color 0.14s ease;
}

.chip.active {
  background: var(--ink);
  border-color: var(--ink);
  color: #fff;
}

.search {
  display: flex;
  flex: 1;
  gap: 8px;
  min-width: 200px;
}

.search .input {
  flex: 1;
  min-width: 0;
}

.list-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
  color: var(--ink-3);
  font-size: 12px;
}

.link-button {
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--jade);
  font-size: 12px;
  font-weight: 500;
}

.q-list {
  display: grid;
  gap: 10px;
}

.qcard {
  padding: 18px 20px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r-sm);
  transition: border-color 0.14s ease;
}

.q-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  color: var(--ink-3);
  font-size: 11.5px;
}

.q-no {
  color: var(--ink-4);
  font-family: var(--font-mono);
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

.q-stem {
  margin: 10px 0 12px;
  color: var(--ink);
  font-size: 14px;
  line-height: 1.75;
  text-wrap: pretty;
  white-space: pre-wrap;
  word-break: break-word;
}

.q-foot {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  color: var(--ink-3);
  font-size: 12px;
}

.q-foot .tag {
  padding: 2px 7px;
  border-radius: 5px;
  background: var(--paper-2);
  font-size: 11px;
}

.q-actions {
  display: flex;
  gap: 8px;
  margin-left: auto;
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

/* ---------- 详情弹窗 ---------- */
.q-overlay {
  position: fixed;
  inset: 0;
  z-index: 100;
  display: grid;
  place-items: center;
  padding: 20px;
  background: rgba(21, 24, 29, 0.46);
  overscroll-behavior: contain;
}

.q-dialog {
  width: min(720px, 100%);
  max-height: calc(100vh - 40px);
  overflow: auto;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: 18px;
  box-shadow: var(--shadow-lg);
  overscroll-behavior: contain;
}

.q-dialog-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 22px 26px 0;
}

.q-dialog-head h2 {
  margin-top: 4px;
  font-family: var(--font-display);
  font-size: 24px;
  letter-spacing: 0;
}

.q-close {
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

.q-dialog-body {
  padding: 18px 26px;
}

.detail-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  color: var(--ink-3);
  font-size: 11.5px;
}

.wrong-badge {
  padding: 2px 8px;
  border-radius: 6px;
  background: var(--red-soft);
  color: var(--red);
  font-weight: 500;
}

.detail-stem {
  margin: 14px 0 16px;
  color: var(--ink);
  font-size: 15px;
  line-height: 1.8;
  text-wrap: pretty;
  white-space: pre-wrap;
  word-break: break-word;
}

.detail-options {
  display: grid;
  gap: 8px;
}

.detail-option {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 11px 14px;
  background: var(--surface-2);
  border: 1px solid var(--line);
  border-radius: 10px;
  color: var(--ink-2);
  font-size: 13px;
  line-height: 1.65;
}

.option-key {
  display: grid;
  place-items: center;
  width: 24px;
  height: 24px;
  flex: none;
  border-radius: 6px;
  background: var(--paper-2);
  color: var(--ink-2);
  font-family: var(--font-mono);
  font-size: 11px;
}

.detail-option.correct {
  border-color: var(--jade);
  background: var(--jade-soft);
}

.detail-option.correct .option-key {
  background: var(--jade);
  color: #fff;
}

.detail-answer {
  margin-top: 18px;
  padding: 18px;
  background: var(--surface-2);
  border: 1px solid var(--line);
  border-radius: 12px;
}

.answer-line {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.answer-line.long {
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
}

.answer-line .label {
  color: var(--ink-3);
  font-size: 12px;
}

.answer-line strong {
  color: var(--jade-2);
  font-family: var(--font-display);
  font-size: 20px;
  line-height: 1.4;
}

.reference-answer {
  width: 100%;
  margin: 0;
  padding: 12px 14px;
  overflow-x: auto;
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
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px dashed var(--line);
}

.analysis-block h3 {
  margin-bottom: 6px;
  font-size: 13px;
  letter-spacing: 0;
}

.analysis-block p {
  color: var(--ink-2);
  font-size: 13px;
  line-height: 1.8;
  white-space: pre-wrap;
}

.answer-locked {
  margin-top: 18px;
  padding: 20px;
  border: 1px dashed var(--line-2);
  border-radius: 12px;
  text-align: center;
}

.answer-locked p {
  color: var(--ink-3);
  font-size: 13px;
}

.answer-locked .btn {
  margin-top: 10px;
}

.q-dialog-foot {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 16px 26px 24px;
  border-top: 1px solid var(--line);
}

.q-dialog-enter-active,
.q-dialog-leave-active {
  transition: opacity 0.18s ease;
}

.q-dialog-enter-from,
.q-dialog-leave-to {
  opacity: 0;
}

.q-dialog-enter-active .q-dialog,
.q-dialog-leave-active .q-dialog {
  transition: transform 0.2s cubic-bezier(0.16, 1, 0.3, 1);
}

.q-dialog-enter-from .q-dialog,
.q-dialog-leave-to .q-dialog {
  transform: translateY(12px) scale(0.98);
}

:where(.subject-item, .chip, .back-link, .link-button, .q-close, .qcard .btn):focus-visible {
  outline: 2px solid var(--jade);
  outline-offset: 2px;
}

@media (hover: hover) {
  .subject-item:not(.active):hover {
    background: var(--surface-2);
  }

  .chip:hover {
    border-color: var(--line-2);
  }

  .qcard:hover {
    border-color: var(--line-2);
  }

  .q-close:hover {
    background: var(--paper-2);
  }

  .link-button:hover,
  .back-link:hover {
    text-decoration: underline;
    text-underline-offset: 3px;
  }
}

@media (max-width: 1000px) {
  .bank-layout {
    grid-template-columns: 1fr;
  }

  .subject-rail {
    position: static;
    display: flex;
    overflow-x: auto;
    gap: 6px;
    padding: 8px;
  }

  .subject-item {
    width: auto;
    flex: none;
  }

  .subject-item .nm {
    white-space: nowrap;
  }
}

@media (max-width: 700px) {
  .bank-page {
    padding-top: 24px;
  }

  .bank-head {
    flex-wrap: wrap;
  }

  .bank-head .pill {
    margin-left: 0;
  }

  .stage-row {
    flex-wrap: wrap;
  }

  .stage-row .acts {
    width: 100%;
    justify-content: flex-end;
  }

  .search {
    flex-basis: 100%;
  }

  .q-actions {
    width: 100%;
    justify-content: flex-end;
  }

  .state-error {
    flex-direction: column;
    align-items: flex-start;
  }

  .q-dialog-head,
  .q-dialog-body,
  .q-dialog-foot {
    padding-left: 18px;
    padding-right: 18px;
  }
}

@media (max-width: 480px) {
  .bank-head h2 {
    font-size: 22px;
  }

  .qcard {
    padding: 16px;
  }

  .chip {
    padding: 0 10px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .subject-item,
  .chip,
  .qcard,
  .q-dialog-enter-active,
  .q-dialog-leave-active,
  .q-dialog-enter-active .q-dialog,
  .q-dialog-leave-active .q-dialog {
    transition: none;
  }
}
</style>
