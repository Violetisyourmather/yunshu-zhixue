<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import {
  ElMessage,
  ElMessageBox,
  type FormInstance,
  type FormRules,
  type UploadFile,
  type UploadInstance,
  type UploadUserFile
} from 'element-plus'
import { Plus, Search, UploadFilled } from '@element-plus/icons-vue'
import AdminPage from '@/components/AdminPage.vue'
import {
  auditQuestion,
  batchDeleteQuestions,
  batchUpdateQuestionStatus,
  confirmQuestionImport,
  createQuestion,
  deleteQuestion,
  downloadQuestionTemplate,
  exportQuestions,
  getQuestionDetail,
  getQuestionFormOptions,
  getQuestionStats,
  listQuestions,
  previewQuestionImport,
  resubmitQuestion,
  updateQuestion,
  updateQuestionStatus
} from '@/api/questions'
import type {
  QuestionDetail,
  QuestionImportPreview,
  QuestionImportRow,
  QuestionImportRowStatus,
  QuestionItem,
  QuestionPayload,
  QuestionQuery,
  QuestionStageGroup,
  QuestionStats
} from '@/types/content'

const TYPE_SINGLE = 1
const TYPE_MULTI = 2
const TYPE_JUDGE = 3
const TYPE_SHORT = 4

const TYPE_OPTIONS = [
  { value: TYPE_SINGLE, label: '单选' },
  { value: TYPE_MULTI, label: '多选' },
  { value: TYPE_JUDGE, label: '判断' },
  { value: TYPE_SHORT, label: '简答' }
]

const DIFFICULTY_OPTIONS = [
  { value: 1, label: '简单' },
  { value: 2, label: '中等' },
  { value: 3, label: '困难' }
]

const STATUS_OPTIONS = [
  { value: 1, label: '已上架' },
  { value: 2, label: '已下架' },
  { value: 3, label: '待审核' },
  { value: 5, label: '已驳回' }
]

const loading = ref(false)
const rows = ref<QuestionItem[]>([])
const total = ref(0)
const stats = ref<QuestionStats | null>(null)
const stageGroups = ref<QuestionStageGroup[]>([])
const selection = ref<QuestionItem[]>([])
const tableRef = ref<{ clearSelection: () => void }>()
const route = useRoute()

const query = reactive({
  keyword: '',
  subjectId: undefined as number | undefined,
  stageId: undefined as number | undefined,
  type: undefined as number | undefined,
  difficulty: undefined as number | undefined,
  status: undefined as number | undefined,
  sort: 'asc' as 'asc' | 'desc',
  page: 1,
  pageSize: 10
})

let keywordTimer: ReturnType<typeof setTimeout> | undefined

const statCards = computed(() => [
  { label: '题目总数', value: stats.value?.total ?? 0 },
  { label: '已上架', value: stats.value?.online ?? 0 },
  { label: '待审核', value: stats.value?.pending ?? 0 },
  { label: '单选题', value: stats.value?.singleChoice ?? 0 },
  { label: '多选题', value: stats.value?.multipleChoice ?? 0 },
  { label: '判断题', value: stats.value?.judge ?? 0 },
  { label: '简答题', value: stats.value?.shortAnswer ?? 0 }
])

const subjectOptions = computed(() =>
  stageGroups.value.map((group) => ({ id: group.subjectId, name: group.subjectName }))
)

const filteredStageGroups = computed(() =>
  query.subjectId === undefined
    ? stageGroups.value
    : stageGroups.value.filter((group) => group.subjectId === query.subjectId)
)

function currentFilters(): QuestionQuery {
  return {
    keyword: query.keyword.trim() || undefined,
    subjectId: query.subjectId,
    stageId: query.stageId,
    type: query.type,
    difficulty: query.difficulty,
    status: query.status,
    sort: query.sort,
    page: query.page,
    pageSize: query.pageSize
  }
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const [statsResult, page] = await Promise.all([
      getQuestionStats(),
      listQuestions(currentFilters())
    ])
    stats.value = statsResult
    rows.value = page.list
    total.value = page.total
  } finally {
    loading.value = false
  }
}

async function loadStageGroups(): Promise<void> {
  stageGroups.value = await getQuestionFormOptions()
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

function handleSubjectChange(): void {
  if (query.subjectId === undefined) {
    search()
    return
  }
  const group = stageGroups.value.find((item) => item.subjectId === query.subjectId)
  if (query.stageId !== undefined && !group?.stages.some((stage) => stage.id === query.stageId)) {
    query.stageId = undefined
  }
  search()
}

function handleSizeChange(): void {
  query.page = 1
  void load()
}

function handleSelectionChange(value: QuestionItem[]): void {
  selection.value = value
}

/** 列表展示用连续序号（跨页连续），不受数据库主键影响。 */
function rowIndex(index: number): number {
  return (query.page - 1) * query.pageSize + index + 1
}

function typeLabel(type: number | null | undefined): string {
  return TYPE_OPTIONS.find((item) => item.value === type)?.label ?? '—'
}

function typeTagType(type: number): 'primary' | 'success' | 'warning' | 'info' {
  if (type === TYPE_SINGLE) {
    return 'primary'
  }
  if (type === TYPE_MULTI) {
    return 'success'
  }
  return type === TYPE_JUDGE ? 'warning' : 'info'
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

function statusLabel(status: number | null | undefined): string {
  if (status === 1) {
    return '已上架'
  }
  if (status === 2) {
    return '已下架'
  }
  if (status === 3) {
    return '待审核'
  }
  if (status === 5) {
    return '已驳回'
  }
  return '—'
}

function statusTagType(status: number): 'success' | 'info' | 'warning' | 'danger' {
  if (status === 1) {
    return 'success'
  }
  if (status === 2) {
    return 'info'
  }
  return status === 3 ? 'warning' : 'danger'
}

function optionLetter(index: number): string {
  return String.fromCharCode(65 + index)
}

/* ---------- 详情 ---------- */
const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<QuestionDetail | null>(null)

async function openDetail(source: unknown): Promise<void> {
  const row = source as QuestionItem
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    detail.value = await getQuestionDetail(row.id)
  } finally {
    detailLoading.value = false
  }
}

/* ---------- 审核 / 状态 ---------- */
async function auditPass(source: unknown): Promise<void> {
  const row = source as QuestionItem
  const confirmed = await ElMessageBox.confirm(
    '审核通过后题目将立即上架，确定通过吗？',
    '审核通过',
    { type: 'success', confirmButtonText: '通过', cancelButtonText: '取消' }
  ).catch(() => false)
  if (!confirmed) {
    return
  }
  await auditQuestion(row.id, true)
  ElMessage.success('审核通过，题目已上架')
  detailVisible.value = false
  await load()
}

async function auditReject(source: unknown): Promise<void> {
  const row = source as QuestionItem
  const result = await ElMessageBox.prompt('请输入驳回原因（必填）', '审核不通过', {
    inputType: 'textarea',
    inputPlaceholder: '例如：选项与题干不匹配',
    inputValidator: (value) => {
      if (!value || !value.trim()) {
        return '请输入驳回原因'
      }
      return value.trim().length <= 255 || '驳回原因不能超过 255 个字符'
    },
    type: 'warning',
    confirmButtonText: '确认驳回',
    cancelButtonText: '取消'
  }).catch(() => null) as { value: string } | null
  if (!result) {
    return
  }
  await auditQuestion(row.id, false, result.value.trim())
  ElMessage.success('题目已驳回')
  detailVisible.value = false
  await load()
}

async function toggleStatus(source: unknown): Promise<void> {
  const row = source as QuestionItem
  if (row.status === 1) {
    const confirmed = await ElMessageBox.confirm(
      '下架后学员端不再展示该题，确定下架吗？',
      '下架题目',
      { type: 'warning', confirmButtonText: '下架', cancelButtonText: '取消' }
    ).catch(() => false)
    if (!confirmed) {
      return
    }
    await updateQuestionStatus(row.id, 'off')
    ElMessage.success('题目已下架')
  } else if (row.status === 2) {
    const confirmed = await ElMessageBox.confirm(
      '重新上架后题目将进入待审核，审核通过后才会展示，确定继续吗？',
      '重新上架',
      { type: 'warning', confirmButtonText: '重新上架', cancelButtonText: '取消' }
    ).catch(() => false)
    if (!confirmed) {
      return
    }
    await updateQuestionStatus(row.id, 'on')
    ElMessage.success('已重新提交审核')
  } else {
    return
  }
  detailVisible.value = false
  await load()
}

async function resubmit(source: unknown): Promise<void> {
  const row = source as QuestionItem
  const confirmed = await ElMessageBox.confirm(
    '重新提交后题目将回到待审核队列，确定提交吗？',
    '重新提交审核',
    { type: 'warning', confirmButtonText: '重新提交', cancelButtonText: '取消' }
  ).catch(() => false)
  if (!confirmed) {
    return
  }
  await resubmitQuestion(row.id)
  ElMessage.success('已重新提交审核')
  detailVisible.value = false
  await load()
}

async function removeQuestion(source: unknown): Promise<void> {
  const row = source as QuestionItem
  const confirmed = await ElMessageBox.confirm(
    '确定删除该题目吗？被练习或错题记录引用的题目不可删除。',
    '删除题目',
    { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
  ).catch(() => false)
  if (!confirmed) {
    return
  }
  try {
    await deleteQuestion(row.id)
    ElMessage.success('题目已删除')
    detailVisible.value = false
    await load()
  } catch {
    // 30007 由全局拦截器提示
  }
}

async function batchStatus(action: 'on' | 'off'): Promise<void> {
  if (!selection.value.length) {
    return
  }
  const actionText = action === 'on' ? '重新上架' : '下架'
  const confirmed = await ElMessageBox.confirm(
    `确定对选中的 ${selection.value.length} 道题执行「${actionText}」吗？不符合状态流转的题目将自动跳过。`,
    `批量${actionText}`,
    { type: 'warning', confirmButtonText: actionText, cancelButtonText: '取消' }
  ).catch(() => false)
  if (!confirmed) {
    return
  }
  const result = await batchUpdateQuestionStatus(
    selection.value.map((item) => item.id),
    action
  )
  ElMessage.success(`已更新 ${result.updated} 道题，跳过 ${result.skipped} 道`)
  tableRef.value?.clearSelection()
  await load()
}

async function batchRemove(): Promise<void> {
  if (!selection.value.length) {
    return
  }
  const count = selection.value.length
  const confirmed = await ElMessageBox.confirm(
    `确定删除选中的 ${count} 道题吗？被练习或错题记录引用的题目不可删除。`,
    '批量删除',
    { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
  ).catch(() => false)
  if (!confirmed) {
    return
  }
  try {
    await batchDeleteQuestions(selection.value.map((item) => item.id))
    ElMessage.success(`已删除 ${count} 道题`)
    tableRef.value?.clearSelection()
    await load()
  } catch {
    // 30007 由全局拦截器提示
  }
}

/* ---------- 新增 / 编辑 ---------- */
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()
const submitting = ref(false)

const form = reactive({
  stageId: undefined as number | undefined,
  type: TYPE_SINGLE,
  difficulty: 2,
  content: '',
  analysis: '',
  tags: '',
  options: [{ text: '' }, { text: '' }] as { text: string }[],
  correctIndexes: [] as number[],
  singleCorrectIndex: undefined as number | undefined,
  judgeAnswer: '正确',
  shortAnswer: '',
  status: 3
})

const isChoice = computed(() => form.type === TYPE_SINGLE || form.type === TYPE_MULTI)
const isSingle = computed(() => form.type === TYPE_SINGLE)
const isJudge = computed(() => form.type === TYPE_JUDGE)
const isShort = computed(() => form.type === TYPE_SHORT)
const dialogTitle = computed(() => (dialogMode.value === 'create' ? '新增题目' : '编辑题目'))

const rules: FormRules = {
  stageId: [{ required: true, message: '请选择阶段', trigger: 'change' }],
  type: [{ required: true, message: '请选择题型', trigger: 'change' }],
  difficulty: [{ required: true, message: '请选择难度', trigger: 'change' }],
  content: [
    { required: true, message: '请输入题目内容', trigger: 'blur' },
    { max: 10000, message: '题目内容不能超过 10000 个字符', trigger: 'blur' }
  ],
  analysis: [{ max: 5000, message: '解析不能超过 5000 个字符', trigger: 'blur' }],
  tags: [{ max: 255, message: '标签不能超过 255 个字符', trigger: 'blur' }]
}

function resetForm(): void {
  form.stageId = undefined
  form.type = TYPE_SINGLE
  form.difficulty = 2
  form.content = ''
  form.analysis = ''
  form.tags = ''
  form.options = [{ text: '' }, { text: '' }]
  form.correctIndexes = []
  form.singleCorrectIndex = undefined
  form.judgeAnswer = '正确'
  form.shortAnswer = ''
  form.status = 3
}

function openCreate(): void {
  dialogMode.value = 'create'
  editingId.value = null
  resetForm()
  dialogVisible.value = true
  void nextTick(() => formRef.value?.clearValidate())
}

async function openEdit(source: unknown): Promise<void> {
  const row = source as QuestionItem
  const question = await getQuestionDetail(row.id)
  detailVisible.value = false
  dialogMode.value = 'edit'
  editingId.value = question.id
  form.stageId = question.stageId
  form.type = question.type
  form.difficulty = question.difficulty
  form.content = question.content
  form.analysis = question.analysis ?? ''
  form.tags = question.tags ?? ''
  form.options = question.options.length
    ? question.options.map((option) => ({ text: option.text }))
    : [{ text: '' }, { text: '' }]
  form.correctIndexes = question.options
    .map((option, index) => (option.correct ? index : -1))
    .filter((index) => index >= 0)
  form.singleCorrectIndex = form.correctIndexes[0]
  form.judgeAnswer = question.type === TYPE_JUDGE ? (question.answer ?? '正确') : '正确'
  form.shortAnswer = question.type === TYPE_SHORT ? (question.answer ?? '') : ''
  form.status = question.status
  dialogVisible.value = true
  void nextTick(() => formRef.value?.clearValidate())
}

function handleTypeChange(): void {
  if (form.type === TYPE_SINGLE) {
    form.singleCorrectIndex = form.correctIndexes[0]
  } else if (form.type === TYPE_MULTI) {
    form.correctIndexes = form.singleCorrectIndex === undefined ? [] : [form.singleCorrectIndex]
  }
  if (isChoice.value && form.options.length < 2) {
    form.options = [{ text: '' }, { text: '' }]
  }
}

function addOption(): void {
  if (form.options.length < 6) {
    form.options.push({ text: '' })
  }
}

function removeOption(index: number): void {
  if (form.options.length <= 2) {
    return
  }
  form.options.splice(index, 1)
  if (form.singleCorrectIndex === index) {
    form.singleCorrectIndex = undefined
  } else if (form.singleCorrectIndex !== undefined && form.singleCorrectIndex > index) {
    form.singleCorrectIndex--
  }
  form.correctIndexes = form.correctIndexes
    .filter((item) => item !== index)
    .map((item) => (item > index ? item - 1 : item))
}

async function submitForm(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid || form.stageId === undefined) {
    return
  }

  let optionsPayload: { key: string; text: string }[] = []
  let answerKeys: string[] = []
  let answer = ''

  if (isChoice.value) {
    const texts = form.options.map((option) => option.text.trim())
    if (texts.some((text) => !text)) {
      ElMessage.warning('请填写所有选项内容')
      return
    }
    optionsPayload = texts.map((text, index) => ({ key: optionLetter(index), text }))
    if (isSingle.value) {
      if (form.singleCorrectIndex === undefined) {
        ElMessage.warning('请选择一个正确答案')
        return
      }
      answerKeys = [optionLetter(form.singleCorrectIndex)]
    } else {
      if (!form.correctIndexes.length) {
        ElMessage.warning('请至少选择一个正确答案')
        return
      }
      answerKeys = [...form.correctIndexes]
        .sort((a, b) => a - b)
        .map((index) => optionLetter(index))
    }
  } else if (isJudge.value) {
    answer = form.judgeAnswer
  } else if (isShort.value) {
    answer = form.shortAnswer.trim()
    if (!answer) {
      ElMessage.warning('请填写简答题参考答案')
      return
    }
  }

  const payload: QuestionPayload = {
    stageId: form.stageId,
    type: form.type,
    difficulty: form.difficulty,
    content: form.content.trim(),
    analysis: form.analysis.trim(),
    tags: form.tags.trim(),
    options: optionsPayload,
    answerKeys,
    answer
  }

  submitting.value = true
  try {
    if (dialogMode.value === 'create') {
      await createQuestion(payload)
      ElMessage.success('题目已提交审核')
    } else if (editingId.value !== null) {
      await updateQuestion(editingId.value, payload)
      ElMessage.success('题目已更新')
    }
    dialogVisible.value = false
    await load()
  } finally {
    submitting.value = false
  }
}

/* ---------- 导入 / 导出 ---------- */
const importVisible = ref(false)
const importSubjectId = ref<number | undefined>(undefined)
const importFile = ref<File | null>(null)
const importFileList = ref<UploadUserFile[]>([])
const uploadRef = ref<UploadInstance>()
const importParsing = ref(false)
const importConfirming = ref(false)
const importPreview = ref<QuestionImportPreview | null>(null)

const importableCount = computed(() =>
  importPreview.value ? importPreview.value.validCount + importPreview.value.warningCount : 0
)

function openImport(): void {
  resetImport()
  importSubjectId.value = undefined
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

function handleImportSubjectChange(): void {
  importPreview.value = null
}

async function parseImport(): Promise<void> {
  if (importSubjectId.value === undefined) {
    ElMessage.warning('请先选择目标学科')
    return
  }
  if (!importFile.value) {
    ElMessage.warning('请选择 Excel 文件')
    return
  }
  importParsing.value = true
  try {
    importPreview.value = await previewQuestionImport(importFile.value, importSubjectId.value)
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
    const result = await confirmQuestionImport(token)
    ElMessage.success(
      `已导入 ${result.importedCount} 道题` +
        (result.createdStageCount > 0 ? `，新建 ${result.createdStageCount} 个阶段` : '')
    )
    importVisible.value = false
    resetImport()
    await load()
    await loadStageGroups()
  } finally {
    importConfirming.value = false
  }
}

function rowIssues(source: unknown): string {
  const row = source as QuestionImportRow
  return [...row.errors, ...row.warnings]
    .map((issue) => `${issue.field}：${issue.reason}`)
    .join('；')
}

function rowStatusLabel(status: QuestionImportRowStatus): string {
  if (status === 'valid') {
    return '有效'
  }
  return status === 'warning' ? '警告' : '无效'
}

function rowStatusType(status: QuestionImportRowStatus): 'success' | 'warning' | 'danger' {
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
  const blob = await downloadQuestionTemplate()
  saveBlob(blob, '题库导入模板.xlsx')
}

async function handleExport(): Promise<void> {
  const blob = await exportQuestions(currentFilters())
  const date = new Date().toISOString().slice(0, 10).replace(/-/g, '')
  saveBlob(blob, `题库导出_${date}.xlsx`)
}

/** 顶栏通知入口：/content/questions?status=3 直接筛选「待审核」。 */
function applyStatusQuery(): void {
  const raw = Array.isArray(route.query.status) ? route.query.status[0] : route.query.status
  const status = Number(raw)
  if (Number.isInteger(status) && status > 0 && query.status !== status) {
    query.status = status
    query.page = 1
  }
}

watch(
  () => route.query.status,
  () => {
    applyStatusQuery()
    void load()
  }
)

onMounted(() => {
  applyStatusQuery()
  void load()
  void loadStageGroups()
})
</script>

<template>
  <AdminPage
    title="题库管理"
    description="题目挂在阶段下；新增后进入待审核，审核通过才在前台可见。"
  >
    <template #actions>
      <el-button v-permission="'question:import'" @click="openImport">导入 Excel</el-button>
      <el-button v-permission="'question:export'" @click="handleExport">导出 Excel</el-button>
      <el-button v-permission="'question:add'" type="primary" :icon="Plus" @click="openCreate">
        新增题目
      </el-button>
    </template>

    <div class="stat-grid">
      <div v-for="card in statCards" :key="card.label" class="stat-card">
        <span class="stat-label">{{ card.label }}</span>
        <strong class="stat-value">{{ card.value }}</strong>
      </div>
    </div>

    <div class="panel table-panel">
      <div class="question-filter-bar">
        <el-form class="filter-form" @submit.prevent="search">
          <el-form-item>
            <el-input
              v-model="query.keyword"
              class="filter-keyword"
              placeholder="搜索题干关键词"
              clearable
              :prefix-icon="Search"
              @input="handleKeywordInput"
              @keyup.enter="search"
              @clear="search"
            />
          </el-form-item>
          <el-form-item>
            <el-select
              v-model="query.subjectId"
              placeholder="全部学科"
              clearable
              @change="handleSubjectChange"
            >
              <el-option
                v-for="subject in subjectOptions"
                :key="subject.id"
                :label="subject.name"
                :value="subject.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-select v-model="query.stageId" placeholder="全部阶段" clearable filterable @change="search">
              <el-option-group
                v-for="group in filteredStageGroups"
                :key="group.subjectId"
                :label="group.subjectName"
              >
                <el-option
                  v-for="stage in group.stages"
                  :key="stage.id"
                  :label="stage.status === 0 ? `${stage.name}（已停用）` : stage.name"
                  :value="stage.id"
                />
              </el-option-group>
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-select v-model="query.type" placeholder="全部类型" clearable @change="search">
              <el-option
                v-for="item in TYPE_OPTIONS"
                :key="item.value"
                :label="item.label"
                :value="item.value"
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
            <el-select v-model="query.status" placeholder="全部状态" clearable @change="search">
              <el-option
                v-for="item in STATUS_OPTIONS"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-select v-model="query.sort" style="width: 110px" @change="search">
              <el-option label="正序" value="asc" />
              <el-option label="倒序" value="desc" />
            </el-select>
          </el-form-item>
        </el-form>
      </div>

      <div v-if="selection.length" class="batch-bar">
        <span>已选择 <span class="batch-count">{{ selection.length }}</span> 道题</span>
        <el-button v-permission="'question:edit'" size="small" @click="batchStatus('on')">
          批量上架
        </el-button>
        <el-button v-permission="'question:edit'" size="small" @click="batchStatus('off')">
          批量下架
        </el-button>
        <el-button v-permission="'question:delete'" size="small" type="danger" plain @click="batchRemove">
          批量删除
        </el-button>
      </div>

      <el-table
        ref="tableRef"
        v-loading="loading"
        :data="rows"
        row-key="id"
        empty-text="暂无匹配的题目，试试调整筛选条件"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="46" />
        <el-table-column label="ID" width="82">
          <template #default="{ $index }">
            <span class="cell-sub">{{ rowIndex($index) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="题目" min-width="300">
          <template #default="{ row }">
            <div class="question-link" @click="openDetail(row)">{{ row.content }}</div>
          </template>
        </el-table-column>
        <el-table-column label="阶段" min-width="170">
          <template #default="{ row }">
            <div class="stage-cell">{{ row.stageName || '—' }}</div>
            <div class="cell-sub">{{ row.subjectName || '—' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="82">
          <template #default="{ row }">
            <el-tag size="small" effect="plain" :type="typeTagType(row.type)">
              {{ typeLabel(row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="难度" width="82">
          <template #default="{ row }">
            <el-tag size="small" effect="plain" :type="difficultyTagType(row.difficulty)">
              {{ difficultyLabel(row.difficulty) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="92">
          <template #default="{ row }">
            <el-tag size="small" effect="light" :type="statusTagType(row.status)">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="160">
          <template #default="{ row }">
            <span class="cell-sub">{{ row.createdAt }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <div class="row-actions">
              <template v-if="row.status === 3">
                <el-button
                  v-permission="'question:audit'"
                  text
                  type="success"
                  size="small"
                  @click="auditPass(row)"
                >
                  通过
                </el-button>
                <el-button
                  v-permission="'question:audit'"
                  text
                  type="danger"
                  size="small"
                  @click="auditReject(row)"
                >
                  不通过
                </el-button>
              </template>
              <el-button
                v-if="row.status === 1"
                v-permission="'question:edit'"
                text
                type="warning"
                size="small"
                @click="toggleStatus(row)"
              >
                下架
              </el-button>
              <el-button
                v-if="row.status === 2"
                v-permission="'question:edit'"
                text
                type="success"
                size="small"
                @click="toggleStatus(row)"
              >
                上架
              </el-button>
              <el-button
                v-if="row.status === 5"
                v-permission="'question:edit'"
                text
                type="primary"
                size="small"
                @click="resubmit(row)"
              >
                重新提交
              </el-button>
              <el-button
                v-permission="'question:edit'"
                text
                type="primary"
                size="small"
                @click="openEdit(row)"
              >
                编辑
              </el-button>
              <el-button
                v-permission="'question:delete'"
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

    <!-- 题目详情 -->
    <el-dialog v-model="detailVisible" title="题目详情" width="760px">
      <div v-loading="detailLoading" class="detail-body">
        <template v-if="detail">
          <div class="detail-tags">
            <el-tag size="small" effect="plain" :type="typeTagType(detail.type)">
              {{ typeLabel(detail.type) }}
            </el-tag>
            <el-tag size="small" effect="plain" :type="difficultyTagType(detail.difficulty)">
              {{ difficultyLabel(detail.difficulty) }}
            </el-tag>
            <el-tag size="small" effect="light" :type="statusTagType(detail.status)">
              {{ statusLabel(detail.status) }}
            </el-tag>
            <span class="cell-sub">{{ detail.subjectName }} / {{ detail.stageName }}</span>
          </div>

          <p class="detail-content">{{ detail.content }}</p>

          <div v-if="detail.options.length" class="detail-options">
            <div
              v-for="option in detail.options"
              :key="option.key"
              class="detail-option"
              :class="{ correct: option.correct }"
            >
              <span class="option-key">{{ option.key }}</span>
              <span>{{ option.text }}</span>
              <el-tag v-if="option.correct" size="small" type="success" effect="plain">正确</el-tag>
            </div>
          </div>

          <div v-else class="answer-card">
            <div class="answer-label">{{ detail.type === 3 ? '判断题答案' : '参考答案' }}</div>
            <pre class="answer-text">{{ detail.answer || '—' }}</pre>
          </div>

          <div v-if="detail.analysis" class="block">
            <div class="block-title">解析</div>
            <p class="block-text">{{ detail.analysis }}</p>
          </div>

          <div v-if="detail.tags" class="block">
            <div class="block-title">标签</div>
            <el-tag size="small" effect="plain">{{ detail.tags }}</el-tag>
          </div>

          <div v-if="detail.status === 5 && detail.rejectReason" class="reject-reason">
            驳回原因：{{ detail.rejectReason }}
          </div>

          <div class="detail-stats">
            <div><span class="cell-sub">答题次数</span><strong>{{ detail.doneCount }}</strong></div>
            <div><span class="cell-sub">正确次数</span><strong>{{ detail.correctCount }}</strong></div>
            <div><span class="cell-sub">正确率</span><strong>{{ detail.accuracyRate }}%</strong></div>
          </div>
        </template>
      </div>

      <template #footer>
        <template v-if="detail">
          <el-button
            v-if="detail.status === 3"
            v-permission="'question:audit'"
            type="success"
            @click="auditPass(detail)"
          >
            通过
          </el-button>
          <el-button
            v-if="detail.status === 3"
            v-permission="'question:audit'"
            type="danger"
            plain
            @click="auditReject(detail)"
          >
            不通过
          </el-button>
          <el-button
            v-if="detail.status === 1"
            v-permission="'question:edit'"
            type="warning"
            @click="toggleStatus(detail)"
          >
            下架
          </el-button>
          <el-button
            v-if="detail.status === 2"
            v-permission="'question:edit'"
            type="success"
            @click="toggleStatus(detail)"
          >
            上架
          </el-button>
          <el-button
            v-if="detail.status === 5"
            v-permission="'question:edit'"
            type="primary"
            @click="resubmit(detail)"
          >
            重新提交
          </el-button>
          <el-button v-permission="'question:edit'" @click="openEdit(detail)">编辑</el-button>
          <el-button v-permission="'question:delete'" type="danger" plain @click="removeQuestion(detail)">
            删除
          </el-button>
        </template>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 新增 / 编辑题目 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="700px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" class="question-form" :model="form" :rules="rules" label-width="92px">
        <div class="form-row">
          <el-form-item label="所属阶段" prop="stageId">
            <el-select v-model="form.stageId" placeholder="请选择阶段" filterable style="width: 100%">
              <el-option-group
                v-for="group in stageGroups"
                :key="group.subjectId"
                :label="group.subjectName"
              >
                <el-option
                  v-for="stage in group.stages"
                  :key="stage.id"
                  :label="stage.status === 0 ? `${stage.name}（已停用）` : stage.name"
                  :value="stage.id"
                />
              </el-option-group>
            </el-select>
            <div v-if="!stageGroups.some((group) => group.stages.length)" class="cell-sub form-tip">
              暂无阶段，请先在「学科管理」中添加阶段，或通过导入 Excel 自动创建阶段。
            </div>
          </el-form-item>
          <el-form-item label="题型" prop="type">
            <el-select v-model="form.type" style="width: 100%" @change="handleTypeChange">
              <el-option
                v-for="item in TYPE_OPTIONS"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
        </div>

        <div class="form-row">
          <el-form-item label="难度" prop="difficulty">
            <el-select v-model="form.difficulty" style="width: 100%">
              <el-option
                v-for="item in DIFFICULTY_OPTIONS"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-input :model-value="statusLabel(form.status)" disabled style="width: 100%" />
            <div v-if="dialogMode === 'edit' && form.status === 5" class="cell-sub form-tip">
              保存后将重新进入待审核
            </div>
          </el-form-item>
        </div>

        <el-form-item label="题目内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="3"
            maxlength="10000"
            show-word-limit
            placeholder="请输入题干，可包含代码或换行"
          />
        </el-form-item>

        <template v-if="isChoice">
          <el-form-item label="选项与答案">
            <div class="option-editor">
              <el-radio-group
                v-if="isSingle"
                v-model="form.singleCorrectIndex"
                class="option-list"
              >
                <div v-for="(option, index) in form.options" :key="index" class="option-row">
                  <el-radio :value="index" />
                  <span class="option-key">{{ optionLetter(index) }}</span>
                  <el-input
                    v-model="option.text"
                    maxlength="500"
                    :placeholder="`选项 ${optionLetter(index)}`"
                  />
                  <el-button
                    v-if="form.options.length > 2"
                    link
                    type="danger"
                    size="small"
                    @click.prevent="removeOption(index)"
                  >
                    删除
                  </el-button>
                </div>
              </el-radio-group>
              <el-checkbox-group v-else v-model="form.correctIndexes" class="option-list">
                <div v-for="(option, index) in form.options" :key="index" class="option-row">
                  <el-checkbox :value="index" />
                  <span class="option-key">{{ optionLetter(index) }}</span>
                  <el-input
                    v-model="option.text"
                    maxlength="500"
                    :placeholder="`选项 ${optionLetter(index)}`"
                  />
                  <el-button
                    v-if="form.options.length > 2"
                    link
                    type="danger"
                    size="small"
                    @click.prevent="removeOption(index)"
                  >
                    删除
                  </el-button>
                </div>
              </el-checkbox-group>
              <el-button
                v-if="form.options.length < 6"
                link
                type="primary"
                size="small"
                :icon="Plus"
                @click="addOption"
              >
                添加选项
              </el-button>
              <div class="cell-sub form-tip">
                {{ isSingle ? '勾选一个正确答案' : '勾选所有正确答案' }}，至少 2 个选项。
              </div>
            </div>
          </el-form-item>
        </template>

        <el-form-item v-else-if="isJudge" label="正确答案">
          <el-radio-group v-model="form.judgeAnswer">
            <el-radio value="正确">正确</el-radio>
            <el-radio value="错误">错误</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item v-else label="参考答案">
          <el-input
            v-model="form.shortAnswer"
            type="textarea"
            :rows="3"
            maxlength="10000"
            show-word-limit
            placeholder="请输入简答题参考答案全文"
          />
        </el-form-item>

        <el-form-item label="解析" prop="analysis">
          <el-input
            v-model="form.analysis"
            type="textarea"
            :rows="2"
            maxlength="5000"
            show-word-limit
            placeholder="选填，说明题目的解法或考点"
          />
        </el-form-item>

        <el-form-item label="标签" prop="tags">
          <el-input v-model="form.tags" maxlength="255" placeholder="选填，多个标签用逗号分隔" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <!-- Excel 导入 -->
    <el-dialog
      v-model="importVisible"
      title="导入 Excel"
      width="900px"
      :close-on-click-modal="false"
      @closed="resetImport"
    >
      <div class="import-target">
        <span class="import-target-label">目标学科</span>
        <el-select
          v-model="importSubjectId"
          placeholder="请选择导入到哪个学科"
          filterable
          style="width: 260px"
          @change="handleImportSubjectChange"
        >
          <el-option
            v-for="subject in subjectOptions"
            :key="subject.id"
            :label="subject.name"
            :value="subject.id"
          />
        </el-select>
        <el-button link type="primary" @click="downloadTemplate">下载导入模板</el-button>
      </div>
      <p class="import-tip">
        Excel 表头：题干、分类、题型、难度、选项A~F、正确答案、解析、标签。 「分类」会作为所选学科下的阶段名，
        已存在则复用，不存在时在确认导入后自动创建。支持多工作表，单次最多 500 道题。
      </p>

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
        <el-button
          type="primary"
          :loading="importParsing"
          :disabled="!importFile || importSubjectId === undefined"
          @click="parseImport"
        >
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
          v-if="importPreview.newStageNames.length"
          class="import-alert"
          type="info"
          :closable="false"
          show-icon
          :title="`将自动创建 ${importPreview.newStageNames.length} 个阶段：${importPreview.newStageNames.join('、')}`"
        />
        <el-alert
          v-if="importPreview.invalidCount > 0"
          class="import-alert"
          type="warning"
          :closable="false"
          show-icon
          title="无效行不会导入；警告行会按提示中的默认值导入。"
        />

        <el-table :data="importPreview.rows" size="small" max-height="360" class="import-table">
          <el-table-column label="工作表 / 行" width="130">
            <template #default="{ row }">
              <span class="cell-sub">{{ row.sheetName }} · 第 {{ row.row }} 行</span>
            </template>
          </el-table-column>
          <el-table-column label="题干" min-width="220" show-overflow-tooltip>
            <template #default="{ row }">{{ row.content || '—' }}</template>
          </el-table-column>
          <el-table-column label="分类 / 阶段" min-width="190">
            <template #default="{ row }">
              <span>{{ row.stageName || '—' }}</span>
              <el-tag v-if="row.newStage" size="small" type="info" effect="plain" class="new-stage-tag">
                将新建
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="题型" width="76">
            <template #default="{ row }">{{ typeLabel(row.type) }}</template>
          </el-table-column>
          <el-table-column label="难度" width="76">
            <template #default="{ row }">{{ difficultyLabel(row.difficulty) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="78">
            <template #default="{ row }">
              <el-tag size="small" :type="rowStatusType(row.status)">
                {{ rowStatusLabel(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="说明" min-width="230">
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

.question-filter-bar {
  padding: 16px 18px 0;
}

.question-filter-bar :deep(.filter-form .el-select) {
  width: 160px;
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

.stage-cell {
  color: var(--ink-2);
  font-size: 13px;
}

.detail-body {
  min-height: 160px;
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

.detail-options {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.detail-option {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 12px;
  border: 1px solid var(--line);
  border-radius: var(--r-xs);
  background: var(--surface-2);
  font-size: 13px;
  line-height: 1.6;
}

.detail-option.correct {
  border-color: var(--jade);
  background: var(--jade-soft);
}

.option-key {
  flex: none;
  width: 20px;
  color: var(--ink-3);
  font-family: var(--font-mono);
  font-size: 12px;
}

.answer-card {
  padding: 12px 14px;
  border: 1px solid var(--line);
  border-radius: var(--r-xs);
  background: var(--surface-2);
}

.answer-label {
  margin-bottom: 6px;
  color: var(--ink-3);
  font-size: 12px;
}

.answer-text {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: var(--font-mono);
  font-size: 12px;
  line-height: 1.7;
}

.block {
  margin-top: 16px;
}

.block-title {
  margin-bottom: 6px;
  color: var(--ink-3);
  font-size: 12px;
}

.block-text {
  margin: 0;
  color: var(--ink-2);
  font-size: 13px;
  line-height: 1.7;
  white-space: pre-wrap;
}

.reject-reason {
  margin-top: 16px;
  padding: 10px 12px;
  border-radius: var(--r-xs);
  background: var(--red-soft);
  color: var(--red);
  font-size: 12px;
  line-height: 1.6;
}

.detail-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-top: 18px;
  padding: 12px 14px;
  border-radius: var(--r-xs);
  background: var(--surface-2);
}

.detail-stats > div {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.detail-stats strong {
  color: var(--ink);
  font-size: 15px;
}

.question-form :deep(.el-form-item) {
  margin-bottom: 14px;
}

.question-form :deep(.el-form-item:last-child) {
  margin-bottom: 0;
}

.form-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 20px;
}

.form-row :deep(.el-form-item) {
  min-width: 0;
}

.form-row :deep(.el-input),
.form-row :deep(.el-select) {
  width: 100%;
}

.form-tip {
  margin-top: 4px;
  line-height: 1.4;
}

.option-editor {
  width: 100%;
}

.option-list {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 6px;
  width: 100%;
  font-size: 13px;
  line-height: 1.45;
}

.option-row {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.option-row :deep(.el-input) {
  flex: 1;
}

.option-row :deep(.el-radio),
.option-row :deep(.el-checkbox) {
  margin-right: 0;
}

.option-row :deep(.el-radio__label),
.option-row :deep(.el-checkbox__label) {
  display: none;
}

.option-row .option-key {
  width: 18px;
}

.import-target {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}

.import-target-label {
  color: var(--ink-2);
  font-size: 13px;
  font-weight: 600;
}

.import-tip {
  margin: 0 0 12px;
  color: var(--ink-3);
  font-size: 12px;
  line-height: 1.6;
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

.new-stage-tag {
  margin-left: 6px;
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

  .detail-stats {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
