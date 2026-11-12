<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import {
  ElMessage,
  ElMessageBox,
  type FormInstance,
  type FormRules,
  type UploadFile,
  type UploadInstance,
  type UploadUserFile
} from 'element-plus'
import { Plus, Refresh, Search, UploadFilled } from '@element-plus/icons-vue'
import AdminPage from '@/components/AdminPage.vue'
import {
  confirmInterviewImport,
  confirmInterviewMarkdown,
  createInterviewQuestion,
  createInterviewTopic,
  deleteInterviewQuestion,
  deleteInterviewTopic,
  downloadInterviewTemplate,
  getInterviewQuestion,
  getInterviewStats,
  listInterviewQuestions,
  listInterviewSubjects,
  listInterviewTopics,
  previewInterviewImport,
  previewInterviewMarkdown,
  searchInterviewQuestions,
  updateInterviewQuestion,
  updateInterviewTopic
} from '@/api/interview'
import type {
  InterviewImportPreview,
  InterviewImportRow,
  InterviewImportRowStatus,
  InterviewMarkdownPreview,
  InterviewQuestionDetail,
  InterviewQuestionItem,
  InterviewSearchResultItem,
  InterviewStats,
  InterviewSubjectOption,
  InterviewTopicItem
} from '@/types/content'

const DIFFICULTY_OPTIONS = [
  { value: 1, label: '简单' },
  { value: 2, label: '中等' },
  { value: 3, label: '困难' }
]

const SOURCE_LABELS: Record<number, string> = {
  1: '文档解析',
  2: '手工录入',
  3: '手动导入'
}

const loading = ref(false)
const optionsLoading = ref(false)
const rows = ref<InterviewQuestionItem[]>([])
const total = ref(0)
const stats = ref<InterviewStats | null>(null)
const subjects = ref<InterviewSubjectOption[]>([])
const topics = ref<InterviewTopicItem[]>([])

const query = reactive({
  keyword: '',
  subjectId: undefined as number | undefined,
  topicId: undefined as number | undefined,
  difficulty: undefined as number | undefined,
  unclassified: false,
  page: 1,
  pageSize: 10
})

let keywordTimer: ReturnType<typeof setTimeout> | undefined

const statCards = computed(() => [
  { label: '面试题总数', value: stats.value?.total ?? 0 },
  { label: '方向数', value: stats.value?.directionCount ?? 0 },
  { label: '未分类', value: stats.value?.unclassifiedCount ?? 0 },
  { label: '本日新增', value: stats.value?.todayAdded ?? 0 }
])

const topicNames = computed(() => new Set(topics.value.map((topic) => topic.name)))
const emptyText = computed(() =>
  query.unclassified
    ? '没有未分类的题目 —— 题库里的题都标好主题了'
    : '没有符合条件的题目'
)

function currentFilters() {
  return {
    keyword: query.keyword.trim() || undefined,
    subjectId: query.subjectId,
    topicId: query.topicId,
    difficulty: query.difficulty,
    unclassified: query.unclassified || undefined,
    page: query.page,
    pageSize: query.pageSize
  }
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const [statsResult, page] = await Promise.all([
      getInterviewStats(),
      listInterviewQuestions(currentFilters())
    ])
    stats.value = statsResult
    rows.value = page.list
    total.value = page.total
  } finally {
    loading.value = false
  }
}

async function loadOptions(): Promise<void> {
  optionsLoading.value = true
  try {
    const [subjectResult, topicResult] = await Promise.all([
      listInterviewSubjects(),
      listInterviewTopics()
    ])
    subjects.value = subjectResult
    topics.value = topicResult
  } finally {
    optionsLoading.value = false
  }
}

function search(): void {
  if (keywordTimer) {
    clearTimeout(keywordTimer)
    keywordTimer = undefined
  }
  query.page = 1
  void load()
}

function handleKeywordInput(): void {
  if (keywordTimer) {
    clearTimeout(keywordTimer)
  }
  keywordTimer = setTimeout(() => {
    query.page = 1
    void load()
  }, 300)
}

function handleTopicChange(): void {
  if (query.topicId !== undefined) {
    query.unclassified = false
  }
  search()
}

function handleUnclassifiedChange(): void {
  if (query.unclassified) {
    query.topicId = undefined
  }
  search()
}

function resetFilters(): void {
  query.keyword = ''
  query.subjectId = undefined
  query.topicId = undefined
  query.difficulty = undefined
  query.unclassified = false
  search()
}

function handleSizeChange(): void {
  query.page = 1
  void load()
}

function difficultyLabel(difficulty: number | null | undefined): string {
  return DIFFICULTY_OPTIONS.find((item) => item.value === difficulty)?.label ?? '—'
}

function difficultyTagType(difficulty: number): 'success' | 'info' | 'warning' {
  if (difficulty === 1) {
    return 'success'
  }
  return difficulty === 2 ? 'info' : 'warning'
}

function sourceLabel(type: number | null | undefined): string {
  return type == null ? '—' : SOURCE_LABELS[type] || '其他'
}

function sourceDetail(source: unknown): string {
  const row = source as InterviewQuestionItem
  const parts = [sourceLabel(row.sourceType)]
  if (row.sourceDocName) {
    parts.push(row.sourceDocName)
  }
  if (row.sourceSeq) {
    parts.push(`第 ${row.sourceSeq} 题`)
  }
  return parts.join(' · ')
}

function isOutsideTopic(topic: string | null): boolean {
  return Boolean(topic && !topicNames.value.has(topic))
}

/* ---------- 详情 ---------- */
const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<InterviewQuestionDetail | null>(null)

async function openDetail(source: unknown): Promise<void> {
  const row = source as InterviewQuestionItem
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    detail.value = await getInterviewQuestion(row.id)
  } finally {
    detailLoading.value = false
  }
}

/* ---------- 手工录入 ---------- */
const createVisible = ref(false)
const createFormRef = ref<FormInstance>()
const createSubmitting = ref(false)
const createForm = reactive({
  subjectId: undefined as number | undefined,
  content: '',
  referenceAnswer: '',
  topic: '',
  difficulty: 2,
  tags: ''
})

const createRules: FormRules = {
  subjectId: [{ required: true, message: '请选择所属方向', trigger: 'change' }],
  content: [
    { required: true, message: '请输入题干', trigger: 'blur' },
    { max: 10000, message: '题干不能超过 10000 个字符', trigger: 'blur' }
  ],
  referenceAnswer: [{ max: 10000, message: '参考答案不能超过 10000 个字符', trigger: 'blur' }],
  topic: [{ max: 50, message: '主题不能超过 50 个字符', trigger: 'blur' }],
  tags: [{ max: 255, message: '标签不能超过 255 个字符', trigger: 'blur' }]
}

function resetCreateForm(): void {
  createForm.subjectId = undefined
  createForm.content = ''
  createForm.referenceAnswer = ''
  createForm.topic = ''
  createForm.difficulty = 2
  createForm.tags = ''
}

function openCreate(): void {
  resetCreateForm()
  createVisible.value = true
  void nextTick(() => createFormRef.value?.clearValidate())
}

async function submitCreate(): Promise<void> {
  const valid = await createFormRef.value?.validate().catch(() => false)
  if (!valid || createForm.subjectId === undefined) {
    return
  }
  createSubmitting.value = true
  try {
    await createInterviewQuestion({
      subjectId: createForm.subjectId,
      content: createForm.content.trim(),
      referenceAnswer: createForm.referenceAnswer.trim(),
      topic: createForm.topic.trim(),
      difficulty: createForm.difficulty,
      tags: createForm.tags.trim()
    })
    ElMessage.success('面试题已录入')
    createVisible.value = false
    await load()
    await loadOptions()
  } finally {
    createSubmitting.value = false
  }
}

/* ---------- 编辑（仅主题 + 难度） ---------- */
const editVisible = ref(false)
const editLoading = ref(false)
const editSubmitting = ref(false)
const editDetail = ref<InterviewQuestionDetail | null>(null)
const editFormRef = ref<FormInstance>()
const editForm = reactive({
  topic: '',
  difficulty: 2
})

async function openEdit(source: unknown): Promise<void> {
  const row = source as InterviewQuestionItem
  editVisible.value = true
  editLoading.value = true
  editDetail.value = null
  try {
    const question = await getInterviewQuestion(row.id)
    editDetail.value = question
    editForm.topic = question.topic ?? ''
    editForm.difficulty = question.difficulty
  } finally {
    editLoading.value = false
  }
}

async function submitEdit(): Promise<void> {
  if (!editDetail.value) {
    return
  }
  editSubmitting.value = true
  try {
    await updateInterviewQuestion(editDetail.value.id, {
      topic: editForm.topic.trim(),
      difficulty: editForm.difficulty
    })
    ElMessage.success('主题与难度已更新')
    editVisible.value = false
    await load()
    await loadOptions()
  } finally {
    editSubmitting.value = false
  }
}

async function removeQuestion(source: unknown): Promise<void> {
  const row = source as InterviewQuestionItem
  const risk = row.sourceType === 2
    ? '该题为手工录入，无源文档，删除后不可恢复。'
    : row.sourceType === 3
      ? '该题为手动导入，删除后不会影响原始 Excel 文件。'
      : '该题来自文档解析，删除后不会影响原始文档。'
  const confirmed = await ElMessageBox.confirm(
    `确定删除这道面试题吗？${risk}`,
    '删除面试题',
    { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
  ).catch(() => false)
  if (!confirmed) {
    return
  }
  await deleteInterviewQuestion(row.id)
  ElMessage.success('面试题已删除')
  detailVisible.value = false
  await load()
  await loadOptions()
}

/* ---------- 主题管理 ---------- */
const topicManagerVisible = ref(false)
const topicEditingId = ref<number | null>(null)
const topicSubmitting = ref(false)
const topicForm = reactive({
  name: '',
  subjectId: undefined as number | undefined
})

function openTopicManager(): void {
  void loadOptions()
  resetTopicForm()
  topicManagerVisible.value = true
}

function resetTopicForm(): void {
  topicEditingId.value = null
  topicForm.name = ''
  topicForm.subjectId = undefined
}

function editTopic(source: unknown): void {
  const topic = source as InterviewTopicItem
  topicEditingId.value = topic.id
  topicForm.name = topic.name
  topicForm.subjectId = topic.subjectId ?? undefined
}

async function submitTopic(): Promise<void> {
  const name = topicForm.name.trim()
  if (!name) {
    ElMessage.warning('请输入主题名称')
    return
  }
  topicSubmitting.value = true
  try {
    const payload = { name, subjectId: topicForm.subjectId ?? null }
    if (topicEditingId.value === null) {
      await createInterviewTopic(payload)
      ElMessage.success('主题已新增')
    } else {
      await updateInterviewTopic(topicEditingId.value, payload)
      ElMessage.success('主题已更新')
    }
    resetTopicForm()
    await loadOptions()
    await load()
  } finally {
    topicSubmitting.value = false
  }
}

async function removeTopic(source: unknown): Promise<void> {
  const topic = source as InterviewTopicItem
  const confirmed = await ElMessageBox.confirm(
    `确定删除主题「${topic.name}」吗？主题下有题目时不可删除。`,
    '删除主题',
    { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
  ).catch(() => false)
  if (!confirmed) {
    return
  }
  try {
    await deleteInterviewTopic(topic.id)
    ElMessage.success('主题已删除')
    if (query.topicId === topic.id) {
      query.topicId = undefined
    }
    if (topicEditingId.value === topic.id) {
      resetTopicForm()
    }
    await loadOptions()
    await load()
  } catch {
    // 30007 由全局拦截器提示
  }
}

/* ---------- Excel 导入 ---------- */
const importVisible = ref(false)
const importFile = ref<File | null>(null)
const importFileList = ref<UploadUserFile[]>([])
const uploadRef = ref<UploadInstance>()
const importParsing = ref(false)
const importConfirming = ref(false)
const importPreview = ref<InterviewImportPreview | null>(null)

const importableCount = computed(() =>
  importPreview.value ? importPreview.value.validCount + importPreview.value.warningCount : 0
)

function openImport(): void {
  resetImport()
  importVisible.value = true
}

function resetImport(): void {
  importFile.value = null
  importFileList.value = []
  importPreview.value = null
  uploadRef.value?.clearFiles()
}

function handleFileChange(file: UploadFile): void {
  importFile.value = file.raw ?? null
  importPreview.value = null
}

function handleFileRemove(): void {
  importFile.value = null
  importPreview.value = null
}

function handleExceed(): void {
  ElMessage.warning('一次只能选择一个 Excel 文件，请先移除已选文件')
}

async function parseImport(): Promise<void> {
  if (!importFile.value) {
    ElMessage.warning('请选择 Excel 文件')
    return
  }
  importParsing.value = true
  try {
    importPreview.value = await previewInterviewImport(importFile.value)
  } finally {
    importParsing.value = false
  }
}

async function confirmImport(): Promise<void> {
  const token = importPreview.value?.token
  if (!token) {
    return
  }
  importConfirming.value = true
  try {
    const result = await confirmInterviewImport(token)
    ElMessage.success(
      `已导入 ${result.importedCount} 道面试题` +
        (result.createdTopicCount > 0 ? `，新建 ${result.createdTopicCount} 个主题` : '')
    )
    importVisible.value = false
    resetImport()
    await load()
    await loadOptions()
  } finally {
    importConfirming.value = false
  }
}

/* ---------- Markdown 导入 ---------- */
const markdownVisible = ref(false)
const markdownFile = ref<File | null>(null)
const markdownFileList = ref<UploadUserFile[]>([])
const markdownUploadRef = ref<UploadInstance>()
const markdownSubjectId = ref<number | undefined>(undefined)
const markdownDifficulty = ref(2)
const markdownTopic = ref('')
const markdownTags = ref('')
const markdownParsing = ref(false)
const markdownConfirming = ref(false)
const markdownPreview = ref<InterviewMarkdownPreview | null>(null)

function openMarkdownImport(): void {
  resetMarkdownImport()
  markdownVisible.value = true
}

function resetMarkdownImport(): void {
  markdownFile.value = null
  markdownFileList.value = []
  markdownPreview.value = null
  markdownSubjectId.value = undefined
  markdownDifficulty.value = 2
  markdownTopic.value = ''
  markdownTags.value = ''
  markdownUploadRef.value?.clearFiles()
}

function handleMarkdownFileChange(file: UploadFile): void {
  markdownFile.value = file.raw ?? null
  markdownPreview.value = null
}

function handleMarkdownFileRemove(): void {
  markdownFile.value = null
  markdownPreview.value = null
}

function handleMarkdownExceed(): void {
  ElMessage.warning('一次只能选择一个 Markdown 文件，请先移除已选文件')
}

function formatFileSize(bytes: number): string {
  if (bytes < 1024) {
    return `${bytes} B`
  }
  if (bytes < 1024 * 1024) {
    return `${(bytes / 1024).toFixed(1)} KB`
  }
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}

async function parseMarkdown(): Promise<void> {
  if (!markdownFile.value) {
    ElMessage.warning('请选择 Markdown 文件')
    return
  }
  markdownParsing.value = true
  try {
    markdownPreview.value = await previewInterviewMarkdown(markdownFile.value)
  } finally {
    markdownParsing.value = false
  }
}

async function confirmMarkdown(): Promise<void> {
  const preview = markdownPreview.value
  if (!preview) {
    return
  }
  if (markdownSubjectId.value === undefined) {
    ElMessage.warning('请选择所属方向')
    return
  }
  markdownConfirming.value = true
  try {
    const result = await confirmInterviewMarkdown({
      token: preview.token,
      subjectId: markdownSubjectId.value,
      difficulty: markdownDifficulty.value,
      topic: markdownTopic.value.trim() || undefined,
      tags: markdownTags.value.trim() || undefined
    })
    ElMessage.success(`已导入 ${result.importedCount} 道面试题`)
    markdownVisible.value = false
    resetMarkdownImport()
    await load()
    await loadOptions()
  } finally {
    markdownConfirming.value = false
  }
}

/* ---------- 检索测试 ---------- */
const searchTestVisible = ref(false)
const searchTestKeyword = ref('')
const searchTestLoading = ref(false)
const searchTestDone = ref(false)
const searchTestResults = ref<InterviewSearchResultItem[]>([])

function openSearchTest(): void {
  searchTestKeyword.value = ''
  searchTestResults.value = []
  searchTestDone.value = false
  searchTestVisible.value = true
}

async function runSearchTest(): Promise<void> {
  const keyword = searchTestKeyword.value.trim()
  if (!keyword) {
    ElMessage.warning('请输入检索关键词')
    return
  }
  searchTestLoading.value = true
  try {
    searchTestResults.value = await searchInterviewQuestions(keyword)
    searchTestDone.value = true
  } finally {
    searchTestLoading.value = false
  }
}

function searchSource(item: InterviewSearchResultItem): string {
  if (!item.sourceDocName) {
    return '—'
  }
  return item.sourceSeq == null
    ? item.sourceDocName
    : `${item.sourceDocName} · 第 ${item.sourceSeq} 题`
}

function rowIssues(source: unknown): string {
  const row = source as InterviewImportRow
  return [...row.errors, ...row.warnings]
    .map((issue) => `${issue.field}：${issue.reason}`)
    .join('；')
}

function rowStatusLabel(status: InterviewImportRowStatus): string {
  if (status === 'valid') {
    return '有效'
  }
  return status === 'warning' ? '警告' : '无效'
}

function rowStatusType(status: InterviewImportRowStatus): 'success' | 'warning' | 'danger' {
  if (status === 'valid') {
    return 'success'
  }
  return status === 'warning' ? 'warning' : 'danger'
}

function saveBlob(blob: Blob, filename: string): void {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}

async function downloadTemplate(): Promise<void> {
  const blob = await downloadInterviewTemplate()
  saveBlob(blob, '面试题库导入模板.xlsx')
}

onMounted(() => {
  void load()
  void loadOptions()
})
</script>

<template>
  <AdminPage
    title="面试题库管理"
    description="面试题按方向（= 学科）归属；题干与参考答案只读，仅可维护主题与难度。"
  >
    <template #actions>
      <el-button v-permission="'interviewbank:upload'" @click="openImport">Excel 导入</el-button>
      <el-button v-permission="'interviewbank:upload'" @click="openMarkdownImport">
        Markdown 导入
      </el-button>
      <el-button v-permission="'interviewbank:edit'" @click="openTopicManager">
        主题管理
      </el-button>
      <el-button v-permission="'interviewbank:search_test'" @click="openSearchTest">
        检索测试
      </el-button>
      <el-button
        v-permission="'interviewbank:upload'"
        type="primary"
        :icon="Plus"
        @click="openCreate"
      >
        录入题目
      </el-button>
    </template>

    <div class="stat-grid">
      <div v-for="card in statCards" :key="card.label" class="stat-card">
        <span class="stat-label">{{ card.label }}</span>
        <strong class="stat-value">{{ card.value }}</strong>
      </div>
    </div>

    <div class="panel table-panel">
      <div class="interview-filter-bar">
        <el-form class="filter-form" @submit.prevent="search">
          <el-form-item>
            <el-input
              v-model="query.keyword"
              class="filter-keyword"
              placeholder="搜索题干"
              clearable
              :prefix-icon="Search"
              @input="handleKeywordInput"
              @keyup.enter="search"
              @clear="search"
            />
          </el-form-item>
          <el-form-item>
            <el-select v-model="query.subjectId" placeholder="全部方向" clearable @change="search">
              <el-option
                v-for="subject in subjects"
                :key="subject.id"
                :label="subject.name"
                :value="subject.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-select
              v-model="query.topicId"
              placeholder="全部主题"
              clearable
              filterable
              :disabled="query.unclassified"
              @change="handleTopicChange"
            >
              <el-option
                v-for="topic in topics"
                :key="topic.id"
                :label="`${topic.name}（${topic.questionCount}）`"
                :value="topic.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-select v-model="query.difficulty" placeholder="全部难度" clearable @change="search">
              <el-option
                v-for="item in DIFFICULTY_OPTIONS"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-checkbox
              v-model="query.unclassified"
              :disabled="query.topicId !== undefined"
              @change="handleUnclassifiedChange"
            >
              只看未分类
            </el-checkbox>
          </el-form-item>
          <el-form-item>
            <el-button :icon="Refresh" @click="resetFilters">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-table
        v-loading="loading"
        :data="rows"
        row-key="id"
        :empty-text="emptyText"
      >
        <el-table-column label="题目" min-width="330">
          <template #default="{ row }">
            <div class="question-line">
              <span class="question-link" @click="openDetail(row)">{{ row.content }}</span>
              <el-tag
                v-if="row.sourceType === 2 || row.sourceType === 3"
                size="small"
                effect="plain"
                :type="row.sourceType === 2 ? 'info' : 'warning'"
              >
                {{ sourceLabel(row.sourceType) }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="来源" min-width="230">
          <template #default="{ row }">
            <div class="source-subject">{{ row.subjectName || '—' }}</div>
            <div class="cell-sub">{{ sourceDetail(row) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="难度" width="88">
          <template #default="{ row }">
            <el-tag size="small" effect="plain" :type="difficultyTagType(row.difficulty)">
              {{ difficultyLabel(row.difficulty) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="主题" min-width="190">
          <template #default="{ row }">
            <template v-if="row.topic">
              <el-tag size="small" effect="plain">{{ row.topic }}</el-tag>
              <el-tag
                v-if="isOutsideTopic(row.topic)"
                size="small"
                type="warning"
                effect="plain"
                class="outside-tag"
              >
                词表外
              </el-tag>
            </template>
            <el-tag v-else size="small" type="info" effect="plain">未分类</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <div class="row-actions">
              <el-button text type="primary" size="small" @click="openDetail(row)">详情</el-button>
              <el-button
                v-permission="'interviewbank:edit'"
                text
                type="primary"
                size="small"
                @click="openEdit(row)"
              >
                编辑
              </el-button>
              <el-button
                v-permission="'interviewbank:delete'"
                text
                type="danger"
                size="small"
                @click="removeQuestion(row)"
              >
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="load"
          @size-change="handleSizeChange"
        />
      </div>
    </div>

    <!-- 详情 -->
    <el-dialog v-model="detailVisible" title="面试题详情" width="720px">
      <div v-loading="detailLoading" class="detail-body">
        <template v-if="detail">
          <div class="detail-tags">
            <el-tag size="small" effect="plain" :type="difficultyTagType(detail.difficulty)">
              {{ difficultyLabel(detail.difficulty) }}
            </el-tag>
            <el-tag v-if="detail.topic" size="small" effect="plain">{{ detail.topic }}</el-tag>
            <el-tag v-else size="small" type="info" effect="plain">未分类</el-tag>
            <el-tag
              v-if="isOutsideTopic(detail.topic)"
              size="small"
              type="warning"
              effect="plain"
            >
              词表外
            </el-tag>
            <span class="cell-sub">{{ detail.subjectName || '—' }}</span>
          </div>

          <p class="detail-content">{{ detail.content }}</p>

          <div class="block">
            <div class="block-title">参考答案</div>
            <pre class="answer-text">{{ detail.referenceAnswer || '—' }}</pre>
          </div>

          <div v-if="detail.tags" class="block">
            <div class="block-title">标签</div>
            <el-tag size="small" effect="plain">{{ detail.tags }}</el-tag>
          </div>

          <div class="detail-source">
            来源：{{ sourceLabel(detail.sourceType) }}
            <template v-if="detail.sourceDocName"> · {{ detail.sourceDocName }}</template>
            <template v-if="detail.sourceSeq"> · 第 {{ detail.sourceSeq }} 题</template>
            <span class="cell-sub"> · {{ detail.createdAt }}</span>
          </div>
        </template>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 录入题目 -->
    <el-dialog
      v-model="createVisible"
      title="录入面试题"
      width="680px"
      :close-on-click-modal="false"
    >
      <el-form ref="createFormRef" class="interview-form" :model="createForm" :rules="createRules" label-width="92px">
        <el-form-item label="所属方向" prop="subjectId">
          <el-select v-model="createForm.subjectId" placeholder="请选择方向" filterable style="width: 100%">
            <el-option
              v-for="subject in subjects"
              :key="subject.id"
              :label="subject.name"
              :value="subject.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="题干" prop="content">
          <el-input
            v-model="createForm.content"
            type="textarea"
            :rows="4"
            maxlength="10000"
            show-word-limit
            placeholder="请输入面试题题干"
          />
        </el-form-item>

        <el-form-item label="参考答案" prop="referenceAnswer">
          <el-input
            v-model="createForm.referenceAnswer"
            type="textarea"
            :rows="4"
            maxlength="10000"
            show-word-limit
            placeholder="选填，参考答案全文"
          />
        </el-form-item>

        <div class="form-row">
          <el-form-item label="主题" prop="topic">
            <el-select
              v-model="createForm.topic"
              placeholder="选择或输入主题"
              filterable
              allow-create
              clearable
              default-first-option
              style="width: 100%"
            >
              <el-option v-for="topic in topics" :key="topic.id" :label="topic.name" :value="topic.name" />
            </el-select>
          </el-form-item>
          <el-form-item label="难度">
            <el-select v-model="createForm.difficulty" style="width: 100%">
              <el-option
                v-for="item in DIFFICULTY_OPTIONS"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
        </div>

        <el-form-item label="标签" prop="tags">
          <el-input v-model="createForm.tags" maxlength="255" placeholder="选填，多个标签用逗号分隔" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="createSubmitting" @click="submitCreate">保存</el-button>
      </template>
    </el-dialog>

    <!-- 编辑（仅主题 + 难度） -->
    <el-dialog
      v-model="editVisible"
      title="编辑面试题"
      width="680px"
      :close-on-click-modal="false"
    >
      <div v-loading="editLoading">
        <el-form ref="editFormRef" class="interview-form" :model="editForm" label-width="92px">
          <el-form-item label="所属方向">
            <el-input :model-value="editDetail?.subjectName || '—'" disabled />
          </el-form-item>
          <el-form-item label="题干">
            <el-input :model-value="editDetail?.content || ''" type="textarea" :rows="3" disabled />
          </el-form-item>
          <el-form-item label="参考答案">
            <el-input
              :model-value="editDetail?.referenceAnswer || ''"
              type="textarea"
              :rows="3"
              disabled
            />
          </el-form-item>
          <div class="form-row">
            <el-form-item label="主题">
              <el-select
                v-model="editForm.topic"
                placeholder="未分类"
                filterable
                allow-create
                clearable
                default-first-option
                style="width: 100%"
              >
                <el-option v-for="topic in topics" :key="topic.id" :label="topic.name" :value="topic.name" />
              </el-select>
            </el-form-item>
            <el-form-item label="难度">
              <el-select v-model="editForm.difficulty" style="width: 100%">
                <el-option
                  v-for="item in DIFFICULTY_OPTIONS"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </div>
        </el-form>
        <p class="edit-tip">题干与参考答案只读，仅可维护主题与难度。</p>
      </div>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="editSubmitting" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 主题管理 -->
    <el-dialog
      v-model="topicManagerVisible"
      title="主题管理"
      width="760px"
      :close-on-click-modal="false"
    >
      <div class="topic-editor">
        <el-input
          v-model="topicForm.name"
          class="topic-name-input"
          maxlength="50"
          placeholder="主题名称"
        />
        <el-select
          v-model="topicForm.subjectId"
          clearable
          filterable
          placeholder="全局主题"
          style="width: 190px"
        >
          <el-option
            v-for="subject in subjects"
            :key="subject.id"
            :label="subject.name"
            :value="subject.id"
          />
        </el-select>
        <el-button type="primary" :loading="topicSubmitting" @click="submitTopic">
          {{ topicEditingId === null ? '新增主题' : '保存修改' }}
        </el-button>
        <el-button v-if="topicEditingId !== null" @click="resetTopicForm">取消编辑</el-button>
      </div>

      <el-table v-loading="optionsLoading" :data="topics" size="small" max-height="380" class="topic-table">
        <el-table-column label="主题名称" prop="name" min-width="180" />
        <el-table-column label="所属方向" width="180">
          <template #default="{ row }">{{ row.subjectName || '全局' }}</template>
        </el-table-column>
        <el-table-column label="题目数" width="90">
          <template #default="{ row }">{{ row.questionCount }}</template>
        </el-table-column>
        <el-table-column label="操作" width="130">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="editTopic(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="removeTopic(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="topicManagerVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- Excel 导入 -->
    <el-dialog
      v-model="importVisible"
      title="手动导入面试题"
      width="900px"
      :close-on-click-modal="false"
      @closed="resetImport"
    >
      <p class="import-tip">
        Excel 表头：所属学科、题干、难度、主题、标签、参考答案。支持多工作表，单次最多 500 道；
        主题不存在时确认导入后会自动补入主题词表。
      </p>
      <div class="import-template-line">
        <el-button link type="primary" @click="downloadTemplate">下载导入模板</el-button>
      </div>

      <el-upload
        ref="uploadRef"
        v-model:file-list="importFileList"
        drag
        accept=".xlsx,.xls"
        :auto-upload="false"
        :limit="1"
        :on-change="handleFileChange"
        :on-remove="handleFileRemove"
        :on-exceed="handleExceed"
      >
        <el-icon class="upload-icon"><UploadFilled /></el-icon>
        <div class="el-upload__text">将 Excel 拖到此处，或 <em>点击选择</em></div>
        <template #tip>
          <div class="el-upload__tip">仅支持 .xlsx / .xls，文件不超过 10MB。</div>
        </template>
      </el-upload>

      <div class="import-actions">
        <el-button type="primary" :loading="importParsing" :disabled="!importFile" @click="parseImport">
          解析预览
        </el-button>
        <el-button v-if="importPreview" @click="resetImport">重新选择</el-button>
      </div>

      <template v-if="importPreview">
        <div class="import-summary">
          <span class="summary-chip valid">有效 {{ importPreview.validCount }}</span>
          <span class="summary-chip warning">警告 {{ importPreview.warningCount }}</span>
          <span class="summary-chip invalid">无效 {{ importPreview.invalidCount }}</span>
          <span class="cell-sub">共解析 {{ importPreview.totalRows }} 行</span>
        </div>

        <el-alert
          v-if="importPreview.newTopics.length"
          class="import-alert"
          type="info"
          :closable="false"
          show-icon
          :title="`将自动补入 ${importPreview.newTopics.length} 个主题：${importPreview.newTopics.join('、')}`"
        />
        <el-alert
          v-if="importPreview.invalidCount > 0"
          class="import-alert"
          type="warning"
          :closable="false"
          show-icon
          title="无效行不会导入；警告行会按提示中的默认值导入。"
        />

        <el-table :data="importPreview.rows" size="small" max-height="380" class="import-table">
          <el-table-column label="工作表 / 行" width="130">
            <template #default="{ row }">
              <span class="cell-sub">{{ row.sheetName }} · 第 {{ row.row }} 行</span>
            </template>
          </el-table-column>
          <el-table-column label="所属学科" width="150" show-overflow-tooltip>
            <template #default="{ row }">{{ row.subjectName || '—' }}</template>
          </el-table-column>
          <el-table-column label="题干" min-width="220" show-overflow-tooltip>
            <template #default="{ row }">{{ row.content || '—' }}</template>
          </el-table-column>
          <el-table-column label="难度" width="76">
            <template #default="{ row }">{{ difficultyLabel(row.difficulty) }}</template>
          </el-table-column>
          <el-table-column label="主题" min-width="160">
            <template #default="{ row }">
              <span>{{ row.topic || '未分类' }}</span>
              <el-tag v-if="row.newTopic" size="small" type="info" effect="plain" class="outside-tag">
                将新建
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="78">
            <template #default="{ row }">
              <el-tag size="small" :type="rowStatusType(row.status)">
                {{ rowStatusLabel(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="说明" min-width="210">
            <template #default="{ row }">
              <span v-if="rowIssues(row)" class="row-issues">{{ rowIssues(row) }}</span>
              <span v-else class="cell-sub">—</span>
            </template>
          </el-table-column>
        </el-table>
      </template>

      <template #footer>
        <el-button @click="importVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="importConfirming"
          :disabled="!importPreview?.token"
          @click="confirmImport"
        >
          确认导入 {{ importableCount }} 道题
        </el-button>
      </template>
    </el-dialog>

    <!-- Markdown 导入 -->
    <el-dialog
      v-model="markdownVisible"
      title="Markdown 导入面试题"
      width="860px"
      :close-on-click-modal="false"
      @closed="resetMarkdownImport"
    >
      <p class="import-tip">
        按 # ~ ### 标题切题：标题为题干、正文为参考答案。先解析预览，再统一指定方向、主题、
        难度与标签后批量入库；单次最多 500 道。
      </p>

      <el-form label-width="96px" class="markdown-form">
        <el-form-item label="所属方向" required>
          <el-select v-model="markdownSubjectId" placeholder="请选择方向" style="width: 280px">
            <el-option
              v-for="subject in subjects"
              :key="subject.id"
              :label="subject.name"
              :value="subject.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="Markdown 文件">
          <el-upload
            ref="markdownUploadRef"
            v-model:file-list="markdownFileList"
            drag
            accept=".md,.markdown"
            :auto-upload="false"
            :limit="1"
            :on-change="handleMarkdownFileChange"
            :on-remove="handleMarkdownFileRemove"
            :on-exceed="handleMarkdownExceed"
          >
            <el-icon class="upload-icon"><UploadFilled /></el-icon>
            <div class="el-upload__text">将 .md 文件拖到此处，或 <em>点击选择</em></div>
            <template #tip>
              <div class="el-upload__tip">仅支持 .md / .markdown，文件不超过 2MB。</div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>

      <div class="import-actions">
        <el-button
          type="primary"
          :loading="markdownParsing"
          :disabled="!markdownFile"
          @click="parseMarkdown"
        >
          解析预览
        </el-button>
        <el-button v-if="markdownPreview" @click="resetMarkdownImport">重新选择</el-button>
      </div>

      <template v-if="markdownPreview">
        <el-alert
          class="import-alert"
          type="success"
          :closable="false"
          show-icon
          :title="`${markdownPreview.fileName} · ${formatFileSize(markdownPreview.fileSize)} · 解析出 ${markdownPreview.questionCount} 道题`"
        />
        <el-table :data="markdownPreview.samples" size="small" class="import-table">
          <el-table-column label="行" width="76">
            <template #default="{ row }">第 {{ row.line }} 行</template>
          </el-table-column>
          <el-table-column prop="title" label="题干" min-width="240" show-overflow-tooltip />
          <el-table-column
            prop="answerPreview"
            label="参考答案摘要"
            min-width="260"
            show-overflow-tooltip
          />
        </el-table>
        <p class="markdown-hint">
          仅展示前 3 道；确认后导入全部 {{ markdownPreview.questionCount }} 道。
        </p>

        <el-form label-width="96px" class="markdown-form">
          <el-form-item label="主题">
            <el-input
              v-model="markdownTopic"
              maxlength="50"
              placeholder="整批统一主题，留空为未分类"
            />
          </el-form-item>
          <el-form-item label="难度">
            <el-select v-model="markdownDifficulty" style="width: 180px">
              <el-option
                v-for="item in DIFFICULTY_OPTIONS"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="标签">
            <el-input v-model="markdownTags" maxlength="255" placeholder="逗号分隔，如：RAG,检索" />
          </el-form-item>
        </el-form>
      </template>

      <template #footer>
        <el-button @click="markdownVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="markdownConfirming"
          :disabled="!markdownPreview || markdownSubjectId === undefined"
          @click="confirmMarkdown"
        >
          确认导入 {{ markdownPreview?.questionCount ?? 0 }} 道题
        </el-button>
      </template>
    </el-dialog>

    <!-- 检索测试 -->
    <el-dialog v-model="searchTestVisible" title="检索测试" width="840px">
      <p class="import-tip">
        输入关键词，按题干与参考答案模糊匹配，用于验证 Markdown 解析与导入结果；最多返回 10 条。
      </p>
      <div class="search-test-bar">
        <el-input
          v-model="searchTestKeyword"
          placeholder="输入关键词，如：RAG"
          clearable
          @keyup.enter="runSearchTest"
        />
        <el-button type="primary" :loading="searchTestLoading" @click="runSearchTest">
          检索
        </el-button>
      </div>

      <el-table
        v-if="searchTestDone && searchTestResults.length"
        :data="searchTestResults"
        size="small"
        max-height="420"
        class="import-table"
      >
        <el-table-column prop="content" label="题干" min-width="260" show-overflow-tooltip />
        <el-table-column label="方向" width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ row.subjectName || '—' }}</template>
        </el-table-column>
        <el-table-column label="主题" width="130" show-overflow-tooltip>
          <template #default="{ row }">{{ row.topic || '未分类' }}</template>
        </el-table-column>
        <el-table-column label="难度" width="76">
          <template #default="{ row }">{{ difficultyLabel(row.difficulty) }}</template>
        </el-table-column>
        <el-table-column label="来源" min-width="190" show-overflow-tooltip>
          <template #default="{ row }">
            {{ searchSource(row as InterviewSearchResultItem) }}
          </template>
        </el-table-column>
      </el-table>
      <p v-else-if="searchTestDone" class="search-empty">没有命中题目</p>
      <p v-else class="search-empty">输入关键词后点击「检索」</p>

      <template #footer>
        <el-button @click="searchTestVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </AdminPage>
</template>

<style scoped>
.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 16px;
}

.stat-card {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 16px 18px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r);
  box-shadow: var(--shadow);
}

.stat-label {
  color: var(--ink-3);
  font-size: 12px;
}

.stat-value {
  font-family: var(--font-display);
  font-size: 28px;
  line-height: 1.15;
}

.interview-filter-bar {
  padding: 16px 18px 0;
}

.interview-filter-bar :deep(.filter-form .el-select) {
  width: 160px;
}

.question-line {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  flex-wrap: wrap;
}

.question-link {
  display: -webkit-box;
  overflow: hidden;
  color: var(--ink);
  font-weight: 500;
  line-height: 1.55;
  cursor: pointer;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.question-link:hover {
  color: var(--jade);
}

.source-subject {
  color: var(--ink);
  font-size: 13px;
  font-weight: 600;
}

.outside-tag {
  margin-left: 6px;
}

.detail-body {
  min-height: 140px;
}

.detail-tags {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 14px;
}

.detail-content {
  margin: 0 0 16px;
  color: var(--ink);
  font-size: 14px;
  line-height: 1.8;
  white-space: pre-wrap;
}

.block {
  margin-top: 16px;
}

.block-title {
  margin-bottom: 6px;
  color: var(--ink-3);
  font-size: 12px;
}

.answer-text {
  margin: 0;
  padding: 12px 14px;
  border: 1px solid var(--line);
  border-radius: var(--r-xs);
  background: var(--surface-2);
  white-space: pre-wrap;
  word-break: break-word;
  font-family: var(--font-mono);
  font-size: 12px;
  line-height: 1.7;
}

.detail-source {
  margin-top: 18px;
  color: var(--ink-3);
  font-size: 12px;
  line-height: 1.6;
}

.interview-form :deep(.el-form-item) {
  margin-bottom: 16px;
}

.form-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 20px;
}

.form-row :deep(.el-form-item) {
  min-width: 0;
}

.edit-tip {
  margin: 0;
  color: var(--ink-3);
  font-size: 12px;
}

.topic-editor {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 14px;
}

.topic-name-input {
  width: 240px;
}

.topic-table {
  border: 1px solid var(--line);
  border-radius: var(--r-xs);
}

.import-tip {
  margin: 0 0 6px;
  color: var(--ink-3);
  font-size: 12px;
  line-height: 1.6;
}

.import-template-line {
  margin-bottom: 10px;
}

.upload-icon {
  color: var(--ink-4);
  font-size: 46px;
}

.import-actions {
  display: flex;
  gap: 8px;
  margin-top: 14px;
}

.markdown-form {
  margin-top: 4px;
}

.markdown-form :deep(.el-form-item:last-child) {
  margin-bottom: 0;
}

.markdown-form :deep(.el-upload-dragger) {
  padding: 18px 20px;
}

.markdown-hint {
  margin: 8px 0 14px;
  color: var(--ink-4);
  font-size: 12px;
}

.search-test-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 14px;
}

.search-empty {
  padding: 40px 0;
  text-align: center;
  color: var(--ink-4);
  font-size: 13px;
}

.import-summary {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin: 4px 0 12px;
}

.summary-chip {
  padding: 3px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.summary-chip.valid {
  background: var(--jade-soft);
  color: var(--jade-2);
}

.summary-chip.warning {
  background: var(--amber-soft);
  color: var(--amber);
}

.summary-chip.invalid {
  background: var(--red-soft);
  color: var(--red);
}

.import-alert {
  margin-bottom: 10px;
}

.import-table {
  border: 1px solid var(--line);
  border-radius: var(--r-xs);
}

.row-issues {
  color: var(--ink-3);
  font-size: 12px;
  line-height: 1.5;
}

@media (max-width: 900px) {
  .stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .form-row {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (max-width: 560px) {
  .stat-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
