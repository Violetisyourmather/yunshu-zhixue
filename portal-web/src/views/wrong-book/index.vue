<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, ElPagination } from 'element-plus'
import { ApiError } from '@/api/http'
import {
  getWrongQuestionStats,
  getWrongQuestions,
  markWrongMastered,
  removeWrongQuestion,
  startWrongRedo,
  type WrongQuestionItem,
  type WrongQuestionStats,
  type WrongTopItem
} from '@/api/wrong-book'

const PENDING_PRACTICE_KEY = 'yszx_portal_pending_practice'
const PAGE_SIZE = 10
const TYPE_LABELS: Record<number, string> = { 1: '单选', 2: '多选', 3: '判断', 4: '简答' }

const router = useRouter()

const loading = ref(true)
const listLoading = ref(false)
const statsError = ref('')
const listError = ref('')
const stats = ref<WrongQuestionStats | null>(null)
const items = ref<WrongQuestionItem[]>([])
const total = ref(0)
const currentPage = ref(1)
const selected = reactive(new Set<number>())
const actionPending = reactive(new Set<number>())
const batchBusy = ref(false)
const startingRedo = ref(false)
const filters = reactive({
  status: null as number | null,
  type: '' as string,
  days: '' as string,
  sort: 'recent' as 'recent' | 'count'
})

const selectedItems = computed(() => items.value.filter((item) => selected.has(item.id)))
const hasActiveFilters = computed(
  () =>
    filters.status !== null ||
    filters.type !== '' ||
    filters.days !== '' ||
    filters.sort !== 'recent'
)

function messageOf(error: unknown, fallback: string): string {
  return error instanceof ApiError ? error.message : fallback
}

function typeLabel(type: number): string {
  return TYPE_LABELS[type] ?? '未知'
}

function errorLevel(item: WrongQuestionItem): string {
  if (item.errorCount >= 3) {
    return '高频易错'
  }
  return item.errorCount === 2 ? '多次错误' : '偶尔错误'
}

function errorLevelClass(item: WrongQuestionItem): string {
  return item.errorCount >= 3 ? 'badge-red' : 'badge-amber'
}

function lastWrongDate(value: string | null): string {
  return value ? value.slice(0, 10) : '—'
}

function formatRate(value: number): string {
  return Number.isInteger(value) ? `${value}%` : `${value.toFixed(1)}%`
}

function formatAnswer(type: number, value: string | null): string {
  if (!value) {
    return '未作答'
  }
  if ((type === 1 || type === 2) && value.includes(',')) {
    return value
      .split(',')
      .map((key) => key.trim())
      .join(' / ')
  }
  return value
}

function queryType(): number | undefined {
  return filters.type === '' ? undefined : Number(filters.type)
}

function queryDays(): number | undefined {
  return filters.days === '' ? undefined : Number(filters.days)
}

async function loadStats(): Promise<void> {
  statsError.value = ''
  try {
    stats.value = await getWrongQuestionStats()
  } catch (error) {
    statsError.value = messageOf(error, '错题统计加载失败，请稍后重试')
  }
}

async function loadList(): Promise<void> {
  listLoading.value = true
  listError.value = ''
  try {
    const result = await getWrongQuestions({
      status: filters.status ?? undefined,
      type: queryType(),
      days: queryDays(),
      sort: filters.sort,
      page: currentPage.value,
      pageSize: PAGE_SIZE
    })
    items.value = result.list
    total.value = result.total
  } catch (error) {
    items.value = []
    total.value = 0
    listError.value = messageOf(error, '错题列表加载失败，请稍后重试')
  } finally {
    listLoading.value = false
  }
}

async function loadAll(): Promise<void> {
  loading.value = true
  await Promise.all([loadStats(), loadList()])
  loading.value = false
}

async function refreshAfterMutation(): Promise<void> {
  selected.clear()
  await Promise.all([loadStats(), loadList()])
  const maxPage = Math.max(1, Math.ceil(total.value / PAGE_SIZE))
  if (currentPage.value > maxPage) {
    currentPage.value = maxPage
    await loadList()
  }
}

function applyFilters(): void {
  currentPage.value = 1
  selected.clear()
  void loadList()
}

function setStatus(status: number | null): void {
  if (filters.status === status) {
    return
  }
  filters.status = status
  applyFilters()
}

function clearFilters(): void {
  filters.status = null
  filters.type = ''
  filters.days = ''
  filters.sort = 'recent'
  applyFilters()
}

function handlePageChange(page: number): void {
  currentPage.value = page
  selected.clear()
  void loadList()
}

function toggleSelect(id: number): void {
  if (selected.has(id)) {
    selected.delete(id)
  } else {
    selected.add(id)
  }
}

function clearSelection(): void {
  selected.clear()
}

async function markMastered(item: WrongQuestionItem): Promise<void> {
  if (item.status === 2 || actionPending.has(item.id)) {
    return
  }
  actionPending.add(item.id)
  try {
    await markWrongMastered(item.id)
    selected.delete(item.id)
    ElMessage.success('已标记为掌握')
    await refreshAfterMutation()
  } catch (error) {
    ElMessage.error(messageOf(error, '操作失败，请稍后重试'))
  } finally {
    actionPending.delete(item.id)
  }
}

async function removeItem(item: WrongQuestionItem): Promise<void> {
  if (actionPending.has(item.id)) {
    return
  }
  try {
    await ElMessageBox.confirm(
      '移除后该题不再出现在错题本，可随时重新答错加入。',
      '移除错题',
      { confirmButtonText: '移除', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  actionPending.add(item.id)
  try {
    await removeWrongQuestion(item.id)
    selected.delete(item.id)
    ElMessage.success('已移除')
    await refreshAfterMutation()
  } catch (error) {
    ElMessage.error(messageOf(error, '操作失败，请稍后重试'))
  } finally {
    actionPending.delete(item.id)
  }
}

async function startRedo(ids: number[]): Promise<void> {
  if (!ids.length || startingRedo.value) {
    return
  }
  startingRedo.value = true
  try {
    const response = await startWrongRedo(ids)
    const pending = {
      response,
      start: { source: 'wrong_redo', wrongIds: ids }
    }
    try {
      localStorage.setItem(PENDING_PRACTICE_KEY, JSON.stringify(pending))
    } catch {
      ElMessage.error('本地存储不可用，无法开始重做')
      return
    }
    await router.push('/practice?wrongRedo=1')
  } catch (error) {
    ElMessage.error(messageOf(error, '开始重做失败，请稍后重试'))
  } finally {
    startingRedo.value = false
  }
}

function redoOne(item: WrongQuestionItem): void {
  void startRedo([item.id])
}

function redoTop(item: WrongTopItem): void {
  void startRedo([item.id])
}

async function redoAll(): Promise<void> {
  if (!stats.value?.toReviewCount) {
    ElMessage.info('没有待复习的错题')
    return
  }
  startingRedo.value = true
  try {
    const result = await getWrongQuestions({
      status: 1,
      page: 1,
      pageSize: 100,
      sort: 'recent'
    })
    const ids = result.list.map((item) => item.id)
    if (!ids.length) {
      ElMessage.info('没有待复习的错题')
      return
    }
    if (result.total > ids.length) {
      ElMessage.info('一次最多重做 100 道，已选择前 100 道')
    }
    startingRedo.value = false
    await startRedo(ids)
  } catch (error) {
    ElMessage.error(messageOf(error, '获取待复习错题失败，请稍后重试'))
  } finally {
    startingRedo.value = false
  }
}

async function batchMaster(): Promise<void> {
  const ids = selectedItems.value.map((item) => item.id)
  if (!ids.length) {
    return
  }
  try {
    await ElMessageBox.confirm(
      `将对 ${ids.length} 道错题标记为已掌握。`,
      '批量标记已掌握',
      { confirmButtonText: '确认', cancelButtonText: '取消', type: 'info' }
    )
  } catch {
    return
  }
  batchBusy.value = true
  try {
    await Promise.all(ids.map((id) => markWrongMastered(id)))
    ElMessage.success('操作成功')
    clearSelection()
    await refreshAfterMutation()
  } catch (error) {
    ElMessage.error(messageOf(error, '批量操作失败，请刷新后重试'))
    await refreshAfterMutation()
  } finally {
    batchBusy.value = false
  }
}

async function batchRemove(): Promise<void> {
  const ids = selectedItems.value.map((item) => item.id)
  if (!ids.length) {
    return
  }
  try {
    await ElMessageBox.confirm(
      `将对 ${ids.length} 道错题执行移除，移除后不再出现在错题本。`,
      '批量移除',
      { confirmButtonText: '移除', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  batchBusy.value = true
  try {
    await Promise.all(ids.map((id) => removeWrongQuestion(id)))
    ElMessage.success('操作成功')
    clearSelection()
    await refreshAfterMutation()
  } catch (error) {
    ElMessage.error(messageOf(error, '批量操作失败，请刷新后重试'))
    await refreshAfterMutation()
  } finally {
    batchBusy.value = false
  }
}

onMounted(() => {
  void loadAll()
})
</script>

<template>
  <section class="wrong-page">
    <header class="page-head">
      <div>
        <p class="eyebrow">Wrong Book</p>
        <h1>错题本</h1>
        <p class="lead">连续答对 3 次才算已掌握，避免“蒙对一次就消失”。</p>
      </div>
      <button
        type="button"
        class="btn btn-primary"
        :disabled="startingRedo || !stats?.toReviewCount"
        @click="redoAll"
      >
        {{ startingRedo ? '准备中…' : '重做待复习题' }}
      </button>
    </header>

    <div v-if="loading" class="state-panel">正在加载错题本…</div>

    <template v-else>
      <div v-if="statsError" class="state-panel state-error">
        <p>{{ statsError }}</p>
        <button type="button" class="btn btn-sm" @click="loadStats">重新加载</button>
      </div>

      <template v-else-if="stats">
        <div class="wb-stats">
          <div class="wb-stat">
            <div class="k">错题总数</div>
            <div class="v">{{ stats.total }}</div>
          </div>
          <div class="wb-stat">
            <div class="k">待复习</div>
            <div class="v">{{ stats.toReviewCount }}</div>
          </div>
          <div class="wb-stat">
            <div class="k">已掌握</div>
            <div class="v">{{ stats.masteredCount }}</div>
          </div>
          <div class="wb-stat">
            <div class="k">掌握率</div>
            <div class="v">{{ formatRate(stats.masteryRate) }}</div>
          </div>
        </div>

        <section class="top3-panel">
          <header class="panel-head">
            <h3>高频错题 Top 3</h3>
            <span class="small muted">按错误次数降序</span>
          </header>
          <div class="panel-body">
            <div v-if="stats.top3.length" class="top3-list">
              <div v-for="top in stats.top3" :key="top.id" class="top3-row">
                <span class="top3-content">{{ top.content }}</span>
                <span class="badge badge-red">错 {{ top.errorCount }} 次</span>
                <button
                  type="button"
                  class="btn btn-sm"
                  :disabled="startingRedo"
                  @click="redoTop(top)"
                >
                  重做
                </button>
              </div>
            </div>
            <p v-else class="muted small">暂无待复习的高频错题</p>
          </div>
        </section>
      </template>

      <div class="toolbar">
        <div class="chip-group" role="group" aria-label="错题状态">
          <button
            type="button"
            class="chip"
            :class="{ active: filters.status === null }"
            @click="setStatus(null)"
          >
            全部
          </button>
          <button
            type="button"
            class="chip"
            :class="{ active: filters.status === 1 }"
            @click="setStatus(1)"
          >
            待复习
          </button>
          <button
            type="button"
            class="chip"
            :class="{ active: filters.status === 2 }"
            @click="setStatus(2)"
          >
            已掌握
          </button>
        </div>

        <select v-model="filters.type" class="select compact" aria-label="题型筛选" @change="applyFilters">
          <option value="">全部题型</option>
          <option value="1">单选</option>
          <option value="2">多选</option>
          <option value="3">判断</option>
          <option value="4">简答</option>
        </select>
        <select v-model="filters.days" class="select compact" aria-label="时间筛选" @change="applyFilters">
          <option value="">全部时间</option>
          <option value="7">近 7 天</option>
          <option value="30">近 30 天</option>
        </select>
        <select v-model="filters.sort" class="select compact" aria-label="排序方式" @change="applyFilters">
          <option value="recent">最近答错</option>
          <option value="count">错误次数</option>
        </select>

        <button v-if="hasActiveFilters" type="button" class="link-button" @click="clearFilters">
          清除筛选
        </button>
        <span class="list-count">共 {{ total }} 条</span>
      </div>

      <div v-if="listLoading && !items.length" class="state-panel">正在加载错题…</div>

      <div v-else-if="listError" class="state-panel state-error">
        <p>{{ listError }}</p>
        <button type="button" class="btn btn-sm" @click="loadList">重新加载</button>
      </div>

      <div v-else-if="items.length" class="wb-list">
        <article
          v-for="item in items"
          :key="item.id"
          class="wb-card"
          :class="{ 'is-mastered': item.status === 2 }"
        >
          <label class="wb-checkbox">
            <input
              type="checkbox"
              :checked="selected.has(item.id)"
              :aria-label="`选择第 ${item.questionId} 题`"
              @change="toggleSelect(item.id)"
            />
          </label>

          <div class="wb-body">
            <div class="wb-meta">
              <span class="badge" :class="errorLevelClass(item)">{{ errorLevel(item) }}</span>
              <span class="badge badge-type">{{ typeLabel(item.type) }}</span>
              <span class="small muted">已纠正 {{ item.correctedCount }}/3 次</span>
              <span class="small muted">最近答错 {{ lastWrongDate(item.lastWrongAt) }}</span>
              <span class="small muted">错 {{ item.errorCount }} 次</span>
              <span v-if="item.status === 2" class="badge badge-ok">已掌握</span>
            </div>

            <p class="wb-stem">{{ item.content }}</p>
            <p class="wb-class">{{ item.subjectName || '未分类' }} / {{ item.stageName || '未分类' }}</p>

            <div class="wb-answers">
              <div class="answer-row">
                <span class="label">你的答案</span>
                <pre class="value">{{ formatAnswer(item.type, item.userAnswer) }}</pre>
              </div>
              <div class="answer-row">
                <span class="label">正确答案</span>
                <pre class="value">{{ formatAnswer(item.type, item.correctAnswer) }}</pre>
              </div>
            </div>

            <div class="wb-actions">
              <button
                type="button"
                class="btn btn-sm"
                :disabled="startingRedo"
                @click="redoOne(item)"
              >
                重新做题
              </button>
              <button
                v-if="item.status === 1"
                type="button"
                class="btn btn-sm"
                :disabled="actionPending.has(item.id)"
                @click="markMastered(item)"
              >
                标记已掌握
              </button>
              <button
                type="button"
                class="btn btn-sm btn-ghost"
                :disabled="actionPending.has(item.id)"
                @click="removeItem(item)"
              >
                移除
              </button>
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
        <h3>{{ hasActiveFilters ? '没有符合条件的错题' : '暂无错题' }}</h3>
        <p v-if="!hasActiveFilters">先去题库练习，答错的题目会自动进入错题本。</p>
        <RouterLink v-if="!hasActiveFilters" class="btn btn-primary" to="/bank">去刷题</RouterLink>
        <button v-else type="button" class="btn" @click="clearFilters">清除筛选</button>
      </div>
    </template>

    <div v-if="selected.size" class="batch-bar">
      <span>已选择 <b>{{ selected.size }}</b> 项</span>
      <button type="button" class="btn btn-sm" :disabled="batchBusy" @click="batchMaster">
        批量标记已掌握
      </button>
      <button
        type="button"
        class="btn btn-sm batch-danger"
        :disabled="batchBusy"
        @click="batchRemove"
      >
        批量移除
      </button>
      <button type="button" class="btn btn-sm btn-ghost batch-clear" @click="clearSelection">
        取消选择
      </button>
    </div>
  </section>
</template>

<style scoped>
.wrong-page {
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
  line-height: 1.2;
  letter-spacing: 0;
}

.page-head .lead {
  max-width: 620px;
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

/* ---------- 统计卡 ---------- */
.wb-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-top: 22px;
}

.wb-stat {
  padding: 18px 20px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r);
}

.wb-stat .v {
  margin-top: 6px;
  font-family: var(--font-display);
  font-size: 30px;
  line-height: 1.1;
  letter-spacing: -0.01em;
  font-variant-numeric: tabular-nums;
}

.wb-stat .k {
  color: var(--ink-3);
  font-size: 11.5px;
}

/* ---------- Top3 ---------- */
.top3-panel {
  margin-top: 18px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r);
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 16px 22px;
  border-bottom: 1px solid var(--line);
}

.panel-head h3 {
  font-size: 14.5px;
  letter-spacing: 0;
}

.panel-body {
  padding: 14px 22px 16px;
}

.top3-list {
  display: grid;
  gap: 4px;
}

.top3-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
  border-bottom: 1px dashed var(--line);
}

.top3-row:last-child {
  border-bottom: 0;
}

.top3-content {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  color: var(--ink-2);
  font-size: 12.5px;
  line-height: 1.6;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ---------- 工具栏 ---------- */
.toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  margin: 22px 0 12px;
}

.chip-group {
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

.select.compact {
  width: auto;
  min-width: 112px;
  height: 32px;
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

.list-count {
  margin-left: auto;
  color: var(--ink-3);
  font-size: 12px;
}

/* ---------- 错题卡片 ---------- */
.wb-list {
  display: grid;
  gap: 10px;
}

.wb-card {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 18px 20px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r-sm);
  transition: border-color 0.14s ease, background-color 0.14s ease;
}

.wb-card.is-mastered {
  background: var(--surface-2);
  opacity: 0.74;
}

.wb-checkbox {
  padding-top: 3px;
}

.wb-checkbox input {
  width: 15px;
  height: 15px;
  accent-color: var(--jade);
}

.wb-body {
  flex: 1;
  min-width: 0;
}

.wb-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  color: var(--ink-3);
  font-size: 11.5px;
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

.badge-red {
  background: var(--red-soft);
  color: var(--red);
}

.badge-amber {
  background: var(--amber-soft);
  color: var(--amber);
}

.badge-ok {
  background: var(--jade-soft);
  color: var(--jade-2);
}

.badge-type {
  background: var(--blue-soft);
  color: var(--blue);
}

.wb-stem {
  margin: 10px 0 4px;
  color: var(--ink);
  font-size: 14px;
  line-height: 1.75;
  white-space: pre-wrap;
  word-break: break-word;
}

.wb-class {
  color: var(--ink-3);
  font-size: 11.5px;
}

.wb-answers {
  display: grid;
  gap: 6px;
  margin-top: 10px;
}

.answer-row {
  display: grid;
  grid-template-columns: 72px 1fr;
  align-items: start;
  gap: 10px;
}

.answer-row .label {
  padding-top: 8px;
  color: var(--ink-3);
  font-size: 11.5px;
}

.answer-row .value {
  max-height: 110px;
  margin: 0;
  padding: 8px 10px;
  overflow: auto;
  background: var(--surface-2);
  border: 1px solid var(--line);
  border-radius: 8px;
  color: var(--ink-2);
  font-family: var(--font-ui);
  font-size: 12px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.wb-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
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
  margin-top: 16px;
  padding: 40px 24px;
  background: var(--surface-2);
  border: 1px dashed var(--line-2);
  border-radius: var(--r);
  color: var(--ink-3);
  font-size: 13px;
  text-align: center;
}

.empty-panel h3 {
  color: var(--ink);
  font-family: var(--font-display);
  font-size: 22px;
  letter-spacing: 0;
}

.empty-panel p {
  margin-top: 6px;
}

.empty-panel .btn {
  margin-top: 16px;
}

/* ---------- 批量操作栏 ---------- */
.batch-bar {
  position: fixed;
  left: 50%;
  bottom: 22px;
  z-index: 50;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  padding: 10px 16px;
  background: var(--ink);
  border-radius: 12px;
  color: #fff;
  box-shadow: var(--shadow-lg);
  transform: translateX(-50%);
}

.batch-bar > span {
  font-size: 12.5px;
  white-space: nowrap;
}

.batch-bar b {
  font-family: var(--font-mono);
  color: #8fd6bf;
}

.batch-bar .btn {
  height: 30px;
  padding: 0 12px;
  background: rgba(255, 255, 255, 0.12);
  border-color: rgba(255, 255, 255, 0.18);
  color: #fff;
  font-size: 12px;
}

.batch-bar .batch-danger {
  background: var(--red);
  border-color: var(--red);
}

.batch-bar .batch-clear {
  background: transparent;
  border-color: transparent;
  color: rgba(255, 255, 255, 0.6);
}

:where(.chip, .link-button, .wb-checkbox input, .wb-actions .btn):focus-visible {
  outline: 2px solid var(--jade);
  outline-offset: 2px;
}

@media (hover: hover) {
  .chip:hover {
    border-color: var(--line-2);
  }

  .wb-card:hover {
    border-color: var(--line-2);
  }

  .link-button:hover {
    text-decoration: underline;
    text-underline-offset: 3px;
  }

  .batch-bar .btn:hover {
    background: rgba(255, 255, 255, 0.2);
  }
}

@media (max-width: 800px) {
  .wrong-page {
    padding-top: 24px;
  }

  .page-head {
    flex-direction: column;
    align-items: flex-start;
    gap: 14px;
  }

  .wb-stats {
    grid-template-columns: repeat(2, 1fr);
  }

  .list-count {
    margin-left: 0;
  }
}

@media (max-width: 600px) {
  .wb-card {
    padding: 16px;
  }

  .answer-row {
    grid-template-columns: 1fr;
    gap: 3px;
  }

  .answer-row .label {
    padding-top: 0;
  }

  .batch-bar {
    width: calc(100% - 32px);
    justify-content: space-between;
  }

  .batch-bar .batch-clear {
    width: 100%;
    text-align: center;
  }

  .state-error {
    flex-direction: column;
    align-items: flex-start;
  }
}

@media (prefers-reduced-motion: reduce) {
  .wb-card {
    transition: none;
  }
}
</style>
