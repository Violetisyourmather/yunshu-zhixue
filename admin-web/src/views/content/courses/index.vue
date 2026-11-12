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
import { Plus, Search, UploadFilled } from '@element-plus/icons-vue'
import AdminPage from '@/components/AdminPage.vue'
import {
  confirmCourseImport,
  createCourse,
  deleteCourse,
  getCourseStats,
  listCourseSubjectOptions,
  listCourses,
  previewCourseImport,
  previewCourseImportText,
  updateCourse,
  updateCourseStatus
} from '@/api/courses'
import type {
  CourseImportPreview,
  CourseImportRow,
  CourseImportRowStatus,
  CourseItem,
  CoursePayload,
  CourseStats,
  SubjectRef
} from '@/types/content'

const SOURCE_BILIBILI = 1
const SOURCE_OTHER = 2
const BV_PATTERN = /BV[0-9A-Za-z]{10}/i

const loading = ref(false)
const rows = ref<CourseItem[]>([])
const total = ref(0)
const stats = ref<CourseStats | null>(null)
const subjectOptions = ref<SubjectRef[]>([])

const query = reactive({
  keyword: '',
  subjectId: undefined as number | undefined,
  source: undefined as number | undefined,
  page: 1,
  pageSize: 10
})

let keywordTimer: ReturnType<typeof setTimeout> | undefined

const statCards = computed(() => [
  { label: '课程总数', value: stats.value?.courseCount ?? 0 },
  { label: '总时长', value: formatHours(stats.value?.totalHours ?? 0) },
  { label: '关联学科数', value: stats.value?.subjectCount ?? 0 }
])

async function load(): Promise<void> {
  loading.value = true
  try {
    const [statsResult, result] = await Promise.all([
      getCourseStats(),
      listCourses({
        keyword: query.keyword.trim() || undefined,
        subjectId: query.subjectId,
        source: query.source,
        page: query.page,
        pageSize: query.pageSize
      })
    ])
    stats.value = statsResult
    rows.value = result.list
    total.value = result.total
  } finally {
    loading.value = false
  }
}

async function loadSubjectOptions(): Promise<void> {
  subjectOptions.value = await listCourseSubjectOptions()
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

function handleSizeChange(): void {
  query.page = 1
  void load()
}

/* ---------- 展示辅助 ---------- */
function formatHours(hours: number): string {
  return `${Number.isInteger(hours) ? hours : hours.toFixed(1)} 小时`
}

function sourceLabel(source: number): string {
  return source === SOURCE_BILIBILI ? 'B站' : '其他'
}

function displayDuration(minutes: number): string {
  const hours = Math.floor(minutes / 60)
  const rest = minutes % 60
  if (hours === 0) {
    return `${rest} 分钟`
  }
  return rest === 0 ? `${hours} 小时` : `${hours} 小时 ${rest} 分`
}

function subjectNames(source: unknown): string {
  const course = source as CourseItem
  return course.subjects.length ? course.subjects.map((subject) => subject.name).join('、') : '其他学科'
}

function rowIndex(index: number): number {
  return (query.page - 1) * query.pageSize + index + 1
}

/* ---------- 新增 / 编辑课程 ---------- */
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()
const submitting = ref(false)

const form = reactive({
  name: '',
  coverUrl: '',
  intro: '',
  durationText: '2:00',
  source: SOURCE_BILIBILI,
  link: '',
  bvid: '',
  status: 1,
  /** 0 = 其他学科；其余为 subject.id，第一项作为主学科 */
  subjectIds: [] as number[]
})

const previousSubjectIds = ref<number[]>([])

const dialogTitle = computed(() => (dialogMode.value === 'create' ? '新增课程' : '编辑课程'))

function parseDurationText(text: string): number | null {
  const match = /^(\d{1,3}):([0-5]?\d)$/.exec(text.trim())
  if (!match) {
    return null
  }
  return Number(match[1]) * 60 + Number(match[2])
}

function formatDurationText(minutes: number): string {
  const hours = Math.floor(minutes / 60)
  const rest = minutes % 60
  return `${hours}:${String(rest).padStart(2, '0')}`
}

function validateDuration(_rule: unknown, value: string, callback: (error?: Error) => void): void {
  const minutes = parseDurationText(value ?? '')
  if (minutes === null) {
    callback(new Error('时长格式为 小时:分钟，如 2:30'))
    return
  }
  if (minutes <= 0) {
    callback(new Error('课程时长必须大于 0 分钟'))
    return
  }
  callback()
}

function validateUrl(_rule: unknown, value: string, callback: (error?: Error) => void): void {
  const url = (value ?? '').trim()
  if (!url) {
    callback()
    return
  }
  if (!/^https?:\/\/\S+$/i.test(url)) {
    callback(new Error('请输入以 http(s):// 开头的有效地址'))
    return
  }
  callback()
}

function validateLink(_rule: unknown, value: string, callback: (error?: Error) => void): void {
  const url = (value ?? '').trim()
  if (form.source === SOURCE_BILIBILI && !url && !form.bvid.trim()) {
    callback(new Error('B站课程需填写课程链接'))
    return
  }
  if (url && !/^https?:\/\/\S+$/i.test(url)) {
    callback(new Error('请输入以 http(s):// 开头的有效地址'))
    return
  }
  callback()
}

const rules: FormRules = {
  name: [
    { required: true, message: '请输入课程名称', trigger: 'blur' },
    { max: 100, message: '课程名称不能超过 100 个字符', trigger: 'blur' }
  ],
  subjectIds: [{ validator: validateSubjects, trigger: 'change' }],
  durationText: [{ validator: validateDuration, trigger: 'blur' }],
  link: [
    { validator: validateLink, trigger: 'blur' },
    { max: 500, message: '课程链接不能超过 500 个字符', trigger: 'blur' }
  ],
  coverUrl: [
    { validator: validateUrl, trigger: 'blur' },
    { max: 500, message: '封面 URL 不能超过 500 个字符', trigger: 'blur' }
  ],
  intro: [{ max: 500, message: '课程简介不能超过 500 个字符', trigger: 'blur' }]
}

function validateSubjects(_rule: unknown, value: number[], callback: (error?: Error) => void): void {
  if (!value || value.length === 0) {
    callback(new Error('请至少选择一个学科'))
    return
  }
  callback()
}

/** 「其他学科」为互斥兜底项：与具体学科同时勾选时，以最后一次操作为准。 */
function handleSubjectsChange(value: Array<string | number | boolean>): void {
  const numeric = value.filter((item): item is number => typeof item === 'number')
  const hasOther = numeric.includes(0)
  const realIds = numeric.filter((id) => id !== 0)
  if (hasOther && realIds.length > 0) {
    form.subjectIds = previousSubjectIds.value.includes(0) ? realIds : [0]
  }
  previousSubjectIds.value = [...form.subjectIds]
}

function handleSourceChange(): void {
  if (form.source === SOURCE_OTHER) {
    form.bvid = ''
  }
}

function resetCourseForm(): void {
  form.name = ''
  form.coverUrl = ''
  form.intro = ''
  form.durationText = '2:00'
  form.source = SOURCE_BILIBILI
  form.link = ''
  form.bvid = ''
  form.status = 1
  form.subjectIds = []
  previousSubjectIds.value = []
}

function openCreate(): void {
  dialogMode.value = 'create'
  editingId.value = null
  resetCourseForm()
  dialogVisible.value = true
  void nextTick(() => formRef.value?.clearValidate())
}

function openEdit(source: unknown): void {
  const course = source as CourseItem
  dialogMode.value = 'edit'
  editingId.value = course.id
  form.name = course.name
  form.coverUrl = course.coverUrl ?? ''
  form.intro = course.intro ?? ''
  form.durationText = formatDurationText(course.durationMinutes)
  form.source = course.source
  form.link = course.link ?? ''
  form.bvid = course.bvid ?? ''
  form.status = course.status
  form.subjectIds = course.subjectId === null
    ? [0]
    : [
        course.subjectId,
        ...course.subjects
          .filter((subject) => subject.id !== course.subjectId)
          .map((subject) => subject.id)
      ]
  previousSubjectIds.value = [...form.subjectIds]
  dialogVisible.value = true
  void nextTick(() => formRef.value?.clearValidate())
}

async function submitForm(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  const durationMinutes = parseDurationText(form.durationText)
  if (durationMinutes === null) {
    return
  }

  const realSubjectIds = form.subjectIds.filter((id) => id !== 0)
  const isOtherSubject = form.subjectIds.includes(0) || realSubjectIds.length === 0
  const primarySubjectId = isOtherSubject ? null : realSubjectIds[0]
  const link = form.link.trim()
  const matchedBvid = BV_PATTERN.exec(link)
  const rawBvid = matchedBvid ? matchedBvid[0] : form.bvid.trim()
  const bvid = form.source === SOURCE_BILIBILI && rawBvid
    ? `BV${rawBvid.slice(2)}`
    : ''

  const payload: CoursePayload = {
    name: form.name.trim(),
    coverUrl: form.coverUrl.trim(),
    intro: form.intro.trim(),
    durationMinutes,
    source: form.source,
    link,
    bvid,
    status: form.status,
    subjectId: primarySubjectId,
    subjectIds: primarySubjectId === null ? [] : realSubjectIds.slice(1)
  }

  submitting.value = true
  try {
    if (dialogMode.value === 'create') {
      await createCourse(payload)
      ElMessage.success('课程已新增')
    } else if (editingId.value !== null) {
      await updateCourse(editingId.value, payload)
      ElMessage.success('课程已更新')
    }
    dialogVisible.value = false
    await load()
  } finally {
    submitting.value = false
  }
}

/* ---------- 行操作 ---------- */
async function toggleStatus(source: unknown): Promise<void> {
  const course = source as CourseItem
  const nextStatus = course.status === 1 ? 2 : 1
  const action = nextStatus === 1 ? '上架' : '下架'
  const message = nextStatus === 1
    ? `确定上架课程「${course.name}」吗？上架后学员端学习路线可见。`
    : `确定下架课程「${course.name}」吗？下架后学员端学习路线不再展示该课程，后台仍可见。`
  const confirmed = await ElMessageBox.confirm(message, `${action}课程`, {
    type: 'warning',
    confirmButtonText: action,
    cancelButtonText: '取消'
  }).catch(() => false)
  if (!confirmed) {
    return
  }
  await updateCourseStatus(course.id, nextStatus)
  ElMessage.success(`课程已${action}`)
  await load()
}

async function removeCourse(source: unknown): Promise<void> {
  const course = source as CourseItem
  const confirmed = await ElMessageBox.confirm(
    `确定删除课程「${course.name}」吗？被学习路线引用的课程不可删除。`,
    '删除课程',
    { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
  ).catch(() => false)
  if (!confirmed) {
    return
  }
  try {
    await deleteCourse(course.id)
    ElMessage.success('课程已删除')
    await load()
  } catch {
    // 被学习路线引用时后端返回 30007，提示由全局拦截器展示
  }
}

/* ---------- 批量导入（Excel / 粘贴文本） ---------- */
const importVisible = ref(false)
const importMode = ref<'file' | 'text'>('file')
const importFile = ref<File | null>(null)
const importFileList = ref<UploadUserFile[]>([])
const importText = ref('')
const uploadRef = ref<UploadInstance>()
const importParsing = ref(false)
const importConfirming = ref(false)
const importPreview = ref<CourseImportPreview | null>(null)

const importableCount = computed(() =>
  importPreview.value ? importPreview.value.validCount + importPreview.value.warningCount : 0
)

function openImport(): void {
  resetImport()
  importVisible.value = true
}

function resetImport(): void {
  importMode.value = 'file'
  importFile.value = null
  importFileList.value = []
  importText.value = ''
  importPreview.value = null
  uploadRef.value?.clearFiles()
}

function handleImportModeChange(): void {
  importPreview.value = null
  if (importMode.value === 'file') {
    importText.value = ''
  } else {
    importFile.value = null
    importFileList.value = []
    uploadRef.value?.clearFiles()
  }
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
  if (importMode.value === 'text' && !importText.value.trim()) {
    ElMessage.warning('请先粘贴课程文本')
    return
  }
  if (importMode.value === 'file' && !importFile.value) {
    ElMessage.warning('请先选择 Excel 文件')
    return
  }
  importParsing.value = true
  try {
    importPreview.value = importMode.value === 'text'
      ? await previewCourseImportText(importText.value)
      : await previewCourseImport(importFile.value as File)
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
    const result = await confirmCourseImport(token)
    ElMessage.success(`已导入 ${result.importedCount} 门课程`)
    importVisible.value = false
    resetImport()
    await load()
  } finally {
    importConfirming.value = false
  }
}

function rowIssues(source: unknown): string {
  const row = source as CourseImportRow
  return [...row.errors, ...row.warnings]
    .map((issue) => `${issue.field}：${issue.reason}`)
    .join('；')
}

function rowStatusLabel(status: CourseImportRowStatus): string {
  if (status === 'valid') {
    return '有效'
  }
  return status === 'warning' ? '警告' : '无效'
}

function rowStatusType(status: CourseImportRowStatus): 'success' | 'warning' | 'danger' {
  if (status === 'valid') {
    return 'success'
  }
  return status === 'warning' ? 'warning' : 'danger'
}

onMounted(() => {
  void load()
  void loadSubjectOptions()
})
</script>

<template>
  <AdminPage
    title="B站课程库"
    description="课程独立维护，一门课可归属多个学科，可被多条学习路线引用。"
  >
    <template #actions>
      <el-button v-permission="'course:import'" @click="openImport">批量导入</el-button>
      <el-button v-permission="'course:add'" type="primary" :icon="Plus" @click="openCreate">
        新增课程
      </el-button>
    </template>

    <div class="stat-grid">
      <div v-for="card in statCards" :key="card.label" class="stat-card">
        <span class="stat-label">{{ card.label }}</span>
        <strong class="stat-value">{{ card.value }}</strong>
      </div>
    </div>

    <div class="panel table-panel">
      <div class="course-filter-bar">
        <el-form class="filter-form" @submit.prevent="search">
          <el-form-item>
            <el-input
              v-model="query.keyword"
              class="filter-keyword"
              placeholder="搜索课程标题"
              clearable
              :prefix-icon="Search"
              @input="handleKeywordInput"
              @keyup.enter="search"
              @clear="search"
            />
          </el-form-item>
          <el-form-item>
            <el-select v-model="query.subjectId" placeholder="全部学科" clearable @change="search">
              <el-option
                v-for="subject in subjectOptions"
                :key="subject.id"
                :label="subject.name"
                :value="subject.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-select v-model="query.source" placeholder="全部来源" clearable @change="search">
              <el-option label="B站" :value="1" />
              <el-option label="其他" :value="2" />
            </el-select>
          </el-form-item>
        </el-form>
      </div>

      <el-table
        v-loading="loading"
        :data="rows"
        row-key="id"
        empty-text="暂无匹配的课程，试试调整筛选条件"
      >
        <el-table-column label="序号" width="64">
          <template #default="{ $index }">
            <span class="cell-sub">{{ rowIndex($index) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="封面" width="90">
          <template #default="{ row }">
            <div class="cover-thumb">
              <el-image
                v-if="row.coverUrl"
                :src="row.coverUrl"
                fit="cover"
                lazy
                referrerpolicy="no-referrer"
                :preview-src-list="[row.coverUrl]"
                preview-teleported
              >
                <template #error>
                  <span class="cover-fallback">{{ sourceLabel(row.source) }}</span>
                </template>
              </el-image>
              <span v-else class="cover-fallback">{{ sourceLabel(row.source) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="课程名称" min-width="240">
          <template #default="{ row }">
            <div class="course-name">{{ row.name }}</div>
            <div v-if="row.bvid" class="course-bvid">{{ row.bvid }}</div>
          </template>
        </el-table-column>
        <el-table-column label="时长" width="130">
          <template #default="{ row }">{{ displayDuration(row.durationMinutes) }}</template>
        </el-table-column>
        <el-table-column label="学科" min-width="190">
          <template #default="{ row }">
            <span class="subject-cell">{{ subjectNames(row) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="来源" width="90">
          <template #default="{ row }">
            <el-tag size="small" effect="plain" :type="row.source === 1 ? 'primary' : 'info'">
              {{ sourceLabel(row.source) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag size="small" effect="light" :type="row.status === 1 ? 'success' : 'warning'">
              {{ row.status === 1 ? '上架' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <div class="row-actions">
              <el-button
                v-permission="'course:edit'"
                text
                :type="row.status === 1 ? 'warning' : 'success'"
                size="small"
                @click="toggleStatus(row)"
              >
                {{ row.status === 1 ? '下架' : '上架' }}
              </el-button>
              <el-button
                v-permission="'course:edit'"
                text
                type="primary"
                size="small"
                @click="openEdit(row)"
              >
                编辑
              </el-button>
              <el-button
                v-permission="'course:delete'"
                text
                type="danger"
                size="small"
                @click="removeCourse(row)"
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

    <!-- 新增 / 编辑课程 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="720px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-form-item label="课程名称" prop="name">
          <el-input v-model.trim="form.name" maxlength="100" placeholder="请输入课程名称" />
        </el-form-item>

        <div class="form-row">
          <el-form-item label="时长" prop="durationText">
            <el-input v-model.trim="form.durationText" placeholder="如 2:30" />
            <div class="cell-sub form-tip">格式：小时:分钟</div>
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="form.status" style="width: 100%">
              <el-option label="上架" :value="1" />
              <el-option label="下架" :value="2" />
            </el-select>
          </el-form-item>
        </div>

        <div class="form-row">
          <el-form-item label="来源" prop="source">
            <el-select
              v-model="form.source"
              style="width: 100%"
              @change="handleSourceChange"
            >
              <el-option label="B站" :value="1" />
              <el-option label="其他" :value="2" />
            </el-select>
          </el-form-item>
        </div>

        <el-form-item label="所属学科" prop="subjectIds">
          <el-checkbox-group
            v-model="form.subjectIds"
            class="subject-group"
            @change="handleSubjectsChange"
          >
            <el-checkbox
              v-for="subject in subjectOptions"
              :key="subject.id"
              :value="subject.id"
            >
              {{ subject.name }}
            </el-checkbox>
            <el-checkbox :value="0">其他学科（兜底）</el-checkbox>
          </el-checkbox-group>
          <div class="cell-sub form-tip">
            可多选；「其他学科」用于暂不属于任何已有学科的课程。
          </div>
        </el-form-item>

        <div class="form-row">
          <el-form-item label="课程链接" prop="link">
            <el-input
              v-model.trim="form.link"
              maxlength="500"
              placeholder="https://www.bilibili.com/video/BV..."
            />
            <div class="cell-sub form-tip">B站课程可从链接自动识别 BV 号</div>
          </el-form-item>
          <el-form-item label="封面 URL" prop="coverUrl">
            <el-input
              v-model.trim="form.coverUrl"
              maxlength="500"
              placeholder="选填，https://..."
            />
          </el-form-item>
        </div>

        <el-form-item label="课程简介" prop="intro">
          <el-input
            v-model.trim="form.intro"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="选填"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <!-- 批量导入 -->
    <el-dialog
      v-model="importVisible"
      title="批量导入课程"
      width="860px"
      :close-on-click-modal="false"
      @closed="resetImport"
    >
      <el-tabs v-model="importMode" class="import-tabs" @tab-change="handleImportModeChange">
        <el-tab-pane label="Excel 文件" name="file">
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
              <div class="el-upload__tip import-tip">
                仅支持 .xlsx / .xls，单次最多 500 行，文件不超过 10MB。<br />
                表头需包含：课程名称、所属学科、来源、时长（分钟）、BV号、封面、播放链接。
              </div>
            </template>
          </el-upload>
        </el-tab-pane>

        <el-tab-pane label="粘贴文本" name="text">
          <el-input
            v-model="importText"
            type="textarea"
            :rows="10"
            maxlength="1000000"
            show-word-limit
            placeholder="每行一门课程：课程名称 | 时长（分钟） | 所属学科 | 来源 | 课程链接 | 封面URL"
          />
          <div class="el-upload__tip import-tip">
            每行一门课程，字段用 Tab、竖线或逗号分隔；无表头时按上述顺序，多学科用逗号分隔。<br />
            示例：Java 基础 | 120 | Java后端开发 | B站 | https://www.bilibili.com/video/BV1xx411c7mD |<br />
            也可粘贴带表头的 Excel 列（课程名称 / 所属学科 / 来源 / 时长 / BV号 / 封面 / 播放链接）。
          </div>
        </el-tab-pane>
      </el-tabs>

      <div class="import-actions">
        <el-button
          type="primary"
          :loading="importParsing"
          :disabled="importMode === 'file' ? !importFile : !importText.trim()"
          @click="parseImport"
        >
          解析预览
        </el-button>
        <el-button v-if="importPreview" @click="resetImport">重新填写</el-button>
      </div>

      <template v-if="importPreview">
        <div class="import-summary">
          <span class="summary-chip valid">有效 {{ importPreview.validCount }}</span>
          <span class="summary-chip warning">警告 {{ importPreview.warningCount }}</span>
          <span class="summary-chip invalid">无效 {{ importPreview.invalidCount }}</span>
          <span class="cell-sub">共解析 {{ importPreview.totalRows }} 行</span>
        </div>

        <el-alert
          v-if="importPreview.invalidCount > 0"
          class="import-alert"
          type="warning"
          :closable="false"
          show-icon
          title="无效行不会导入；警告行会按提示中的默认值导入。"
        />

        <el-table :data="importPreview.rows" size="small" max-height="360" class="import-table">
          <el-table-column label="行" prop="row" width="56" />
          <el-table-column label="课程名称" prop="name" min-width="160" show-overflow-tooltip />
          <el-table-column label="学科" min-width="150" show-overflow-tooltip>
            <template #default="{ row }">{{ row.subjectNames }}</template>
          </el-table-column>
          <el-table-column label="时长" width="92">
            <template #default="{ row }">
              {{ row.durationMinutes ? `${row.durationMinutes} 分钟` : '—' }}
            </template>
          </el-table-column>
          <el-table-column label="来源" width="76">
            <template #default="{ row }">
              {{ row.source === 1 ? 'B站' : row.source === 2 ? '其他' : '—' }}
            </template>
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
          确认导入 {{ importableCount }} 门
        </el-button>
      </template>
    </el-dialog>
  </AdminPage>
</template>

<style scoped>
.course-filter-bar {
  padding: 16px 18px 0;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
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
  font-size: 13px;
}

.stat-value {
  font-family: var(--font-display);
  font-size: 28px;
  line-height: 1.15;
}

.cover-thumb {
  display: grid;
  place-items: center;
  width: 64px;
  height: 38px;
  overflow: hidden;
  border-radius: var(--r-xs);
  background: var(--paper-2);
}

.cover-thumb :deep(.el-image) {
  width: 100%;
  height: 100%;
}

.cover-fallback {
  display: grid;
  place-items: center;
  width: 100%;
  height: 100%;
  color: var(--ink-4);
  font-family: var(--font-mono);
  font-size: 11px;
}

.course-name {
  color: var(--ink);
  font-weight: 600;
}

.course-bvid {
  margin-top: 2px;
  color: var(--ink-3);
  font-family: var(--font-mono);
  font-size: 11px;
}

.subject-cell {
  color: var(--ink-2);
  font-size: 13px;
}

.form-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 18px;
}

.form-tip {
  margin-top: 4px;
  line-height: 1.4;
}

.subject-group {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 18px;
  width: 100%;
}

.subject-group :deep(.el-checkbox) {
  height: auto;
  margin-right: 0;
}

.subject-group :deep(.el-checkbox__label) {
  font-size: 13px;
  line-height: 1.5;
}

.upload-icon {
  color: var(--ink-4);
  font-size: 46px;
}

.import-tabs {
  margin-top: -6px;
}

.import-tip {
  line-height: 1.6;
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
  margin-bottom: 12px;
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
    grid-template-columns: minmax(0, 1fr);
  }

  .form-row {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
