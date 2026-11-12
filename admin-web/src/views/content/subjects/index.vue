<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { ArrowDown, Plus } from '@element-plus/icons-vue'
import AdminPage from '@/components/AdminPage.vue'
import {
  createStage,
  createSubject,
  deleteStage,
  deleteSubject,
  getSubjectStats,
  listSubjects,
  sortStage,
  sortSubject,
  updateStage,
  updateStageStatus,
  updateSubject
} from '@/api/subjects'
import type { StageItem, StagePayload, SubjectCard, SubjectPayload, SubjectStats } from '@/types/content'

/** 阶段图标留空时的展示默认值（需求 4.3.1：图标留空默认）。 */
const DEFAULT_STAGE_ICON = '🧩'
/** 预置主题色板，与种子数据配色保持一致。 */
const THEME_COLORS = [
  '#0F6B57',
  '#27507A',
  '#8A5A2B',
  '#5B5B7A',
  '#7A4B5B',
  '#3F6B4A',
  '#A15C2E',
  '#6B4B8A'
]

const loading = ref(false)
const subjects = ref<SubjectCard[]>([])
const stats = ref<SubjectStats | null>(null)
/** 阶段列表默认折叠，按学科记录展开状态，卡片高度更整齐。 */
const expandedSubjectIds = ref<Set<number>>(new Set())

function isStagesExpanded(subjectId: number): boolean {
  return expandedSubjectIds.value.has(subjectId)
}

function toggleStages(subjectId: number): void {
  const next = new Set(expandedSubjectIds.value)
  if (next.has(subjectId)) {
    next.delete(subjectId)
  } else {
    next.add(subjectId)
  }
  expandedSubjectIds.value = next
}

/** 新增或编辑阶段时自动展开，方便立即看到操作结果。 */
function expandStages(subjectId: number): void {
  if (expandedSubjectIds.value.has(subjectId)) {
    return
  }
  expandedSubjectIds.value = new Set(expandedSubjectIds.value).add(subjectId)
}

const statCards = computed(() => [
  { label: '总学科数', value: stats.value?.subjectCount ?? 0 },
  { label: '有阶段的学科', value: stats.value?.subjectWithStages ?? 0 },
  { label: '阶段总数', value: stats.value?.stageCount ?? 0 },
  { label: '题目总数', value: stats.value?.questionCount ?? 0 }
])

async function load(): Promise<void> {
  loading.value = true
  try {
    const [statsResult, subjectResult] = await Promise.all([getSubjectStats(), listSubjects()])
    stats.value = statsResult
    subjects.value = subjectResult
  } finally {
    loading.value = false
  }
}

/* ---------- 学科表单 ---------- */
const subjectDialogVisible = ref(false)
const subjectDialogMode = ref<'create' | 'edit'>('create')
const editingSubjectId = ref<number | null>(null)
const subjectFormRef = ref<FormInstance>()
const subjectSubmitting = ref(false)

const subjectForm = reactive<SubjectPayload>({
  name: '',
  icon: '',
  description: '',
  themeColor: THEME_COLORS[0],
  sortOrder: 1
})

const subjectDialogTitle = computed(() =>
  subjectDialogMode.value === 'create' ? '新增学科' : '编辑学科'
)

const subjectRules: FormRules = {
  name: [
    { required: true, message: '请输入学科名称', trigger: 'blur' },
    { max: 50, message: '学科名称不能超过 50 个字符', trigger: 'blur' }
  ],
  icon: [
    { required: true, message: '请输入学科图标（Emoji）', trigger: 'blur' },
    { max: 50, message: '学科图标不能超过 50 个字符', trigger: 'blur' }
  ],
  description: [
    { required: true, message: '请输入学科描述', trigger: 'blur' },
    { max: 255, message: '学科描述不能超过 255 个字符', trigger: 'blur' }
  ],
  themeColor: [{ required: true, message: '请选择主题颜色', trigger: 'change' }],
  sortOrder: [{ required: true, message: '请输入排序号', trigger: 'change' }]
}

function openCreateSubject(): void {
  subjectDialogMode.value = 'create'
  editingSubjectId.value = null
  subjectForm.name = ''
  subjectForm.icon = '🧩'
  subjectForm.description = ''
  subjectForm.themeColor = THEME_COLORS[0]
  subjectForm.sortOrder = subjects.value.length + 1
  subjectDialogVisible.value = true
  void nextTick(() => subjectFormRef.value?.clearValidate())
}

function openEditSubject(subject: SubjectCard): void {
  subjectDialogMode.value = 'edit'
  editingSubjectId.value = subject.id
  subjectForm.name = subject.name
  subjectForm.icon = subject.icon
  subjectForm.description = subject.description
  subjectForm.themeColor = subject.themeColor
  subjectForm.sortOrder = subject.sortOrder
  subjectDialogVisible.value = true
  void nextTick(() => subjectFormRef.value?.clearValidate())
}

async function submitSubject(): Promise<void> {
  const valid = await subjectFormRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  const payload: SubjectPayload = {
    name: subjectForm.name.trim(),
    icon: subjectForm.icon.trim(),
    description: subjectForm.description.trim(),
    themeColor: subjectForm.themeColor,
    sortOrder: subjectForm.sortOrder
  }

  subjectSubmitting.value = true
  try {
    if (subjectDialogMode.value === 'create') {
      await createSubject(payload)
      ElMessage.success('学科已新增')
    } else if (editingSubjectId.value !== null) {
      await updateSubject(editingSubjectId.value, payload)
      ElMessage.success('学科已更新')
    }
    subjectDialogVisible.value = false
    await load()
  } finally {
    subjectSubmitting.value = false
  }
}

/* ---------- 阶段表单 ---------- */
const stageDialogVisible = ref(false)
const stageDialogMode = ref<'create' | 'edit'>('create')
const stageSubject = ref<SubjectCard | null>(null)
const editingStageId = ref<number | null>(null)
const stageFormRef = ref<FormInstance>()
const stageSubmitting = ref(false)

const stageForm = reactive<StagePayload>({
  name: '',
  icon: '',
  sortOrder: 1
})

const stageDialogTitle = computed(() =>
  stageDialogMode.value === 'create' ? '添加阶段' : '编辑阶段'
)

const stageRules: FormRules = {
  name: [
    { required: true, message: '请输入阶段名称', trigger: 'blur' },
    { max: 50, message: '阶段名称不能超过 50 个字符', trigger: 'blur' }
  ],
  icon: [{ max: 50, message: '阶段图标不能超过 50 个字符', trigger: 'blur' }],
  sortOrder: [{ required: true, message: '请输入排序号', trigger: 'change' }]
}

function openCreateStage(subject: SubjectCard): void {
  expandStages(subject.id)
  stageDialogMode.value = 'create'
  stageSubject.value = subject
  editingStageId.value = null
  stageForm.name = ''
  stageForm.icon = ''
  stageForm.sortOrder = subject.stages.length + 1
  stageDialogVisible.value = true
  void nextTick(() => stageFormRef.value?.clearValidate())
}

function openEditStage(subject: SubjectCard, stage: StageItem): void {
  expandStages(subject.id)
  stageDialogMode.value = 'edit'
  stageSubject.value = subject
  editingStageId.value = stage.id
  stageForm.name = stage.name
  stageForm.icon = stage.icon ?? ''
  stageForm.sortOrder = stage.sortOrder
  stageDialogVisible.value = true
  void nextTick(() => stageFormRef.value?.clearValidate())
}

async function submitStage(): Promise<void> {
  const valid = await stageFormRef.value?.validate().catch(() => false)
  if (!valid || !stageSubject.value) {
    return
  }
  const payload: StagePayload = {
    name: stageForm.name.trim(),
    icon: stageForm.icon.trim(),
    sortOrder: stageForm.sortOrder
  }

  stageSubmitting.value = true
  try {
    if (stageDialogMode.value === 'create') {
      await createStage(stageSubject.value.id, payload)
      ElMessage.success('阶段已添加')
    } else if (editingStageId.value !== null) {
      await updateStage(stageSubject.value.id, editingStageId.value, payload)
      ElMessage.success('阶段已更新')
    }
    stageDialogVisible.value = false
    await load()
  } finally {
    stageSubmitting.value = false
  }
}

/* ---------- 排序 / 启停 / 删除 ---------- */
async function moveSubject(subject: SubjectCard, move: 'up' | 'down'): Promise<void> {
  await sortSubject(subject.id, move)
  await load()
}

async function moveStage(subject: SubjectCard, stage: StageItem, move: 'up' | 'down'): Promise<void> {
  await sortStage(subject.id, stage.id, move)
  await load()
}

async function toggleStage(subject: SubjectCard, stage: StageItem): Promise<void> {
  const nextStatus = stage.status === 1 ? 0 : 1
  await updateStageStatus(subject.id, stage.id, nextStatus)
  ElMessage.success(nextStatus === 1 ? `已启用阶段「${stage.name}」` : `已停用阶段「${stage.name}」`)
  await load()
}

/** 删除学科的三种提示（需求 4.3.1）：有阶段与题目 / 仅有阶段 / 无阶段。 */
function subjectRelationText(subject: SubjectCard): string {
  const parts: string[] = []
  if (subject.stages.length > 0 && subject.questionCount > 0) {
    parts.push(`该学科下有 ${subject.stages.length} 个阶段与 ${subject.questionCount} 道题`)
  } else if (subject.stages.length > 0) {
    parts.push(`该学科下有 ${subject.stages.length} 个阶段`)
  } else if (subject.questionCount > 0) {
    parts.push(`该学科下有 ${subject.questionCount} 道题`)
  }
  if (subject.courseCount > 0) {
    parts.push(`关联 ${subject.courseCount} 门课程`)
  }
  if (subject.routeCount > 0) {
    parts.push(`关联 ${subject.routeCount} 条学习路线`)
  }
  return parts.join('、')
}

async function removeSubject(subject: SubjectCard): Promise<void> {
  const related = subjectRelationText(subject)
  const message = related
    ? `${related}。请先处理关联内容；确认后系统仍会再次校验并阻止删除。`
    : `确定删除学科「${subject.name}」吗？删除后不可恢复。`
  const confirmed = await ElMessageBox.confirm(message, '删除学科', {
    type: 'warning',
    confirmButtonText: related ? '仍要尝试' : '删除',
    cancelButtonText: '取消'
  }).catch(() => false)
  if (!confirmed) {
    return
  }
  try {
    await deleteSubject(subject.id)
    ElMessage.success('学科已删除')
    await load()
  } catch {
    // 关联内容仍存在时后端返回 30007，错误提示由全局拦截器展示
  }
}

async function removeStage(subject: SubjectCard, stage: StageItem): Promise<void> {
  const related = stage.questionCount > 0
  const message = related
    ? `该阶段关联 ${stage.questionCount} 道题，请先处理题目；确认后系统仍会再次校验并阻止删除。`
    : `确定删除阶段「${stage.name}」吗？删除后不可恢复。`
  const confirmed = await ElMessageBox.confirm(message, '删除阶段', {
    type: 'warning',
    confirmButtonText: related ? '仍要尝试' : '删除',
    cancelButtonText: '取消'
  }).catch(() => false)
  if (!confirmed) {
    return
  }
  try {
    await deleteStage(subject.id, stage.id)
    ElMessage.success('阶段已删除')
    await load()
  } catch {
    // 阶段下仍有题目时后端返回 30007，错误提示由全局拦截器展示
  }
}

onMounted(() => {
  void load()
})
</script>

<template>
  <AdminPage
    title="学科管理"
    description="学科是内容体系的第一层，阶段、题目、课程与学习路线都挂在学科下。"
  >
    <template #actions>
      <el-button v-permission="'subject:add'" type="primary" :icon="Plus" @click="openCreateSubject">
        新增学科
      </el-button>
    </template>

    <div class="stat-grid">
      <div v-for="card in statCards" :key="card.label" class="stat-card">
        <span class="stat-label">{{ card.label }}</span>
        <strong class="stat-value">{{ card.value }}</strong>
      </div>
    </div>

    <div v-loading="loading" class="subject-grid-wrap">
      <div v-if="subjects.length" class="subject-grid">
        <article v-for="(subject, subjectIndex) in subjects" :key="subject.id" class="subject-card">
          <header class="subject-head">
            <span class="subject-icon">{{ subject.icon }}</span>
            <div class="subject-title">
              <h3>{{ subject.name }}</h3>
              <p class="cell-sub">
                排序 {{ subject.sortOrder }} · {{ subject.stages.length }} 个阶段 ·
                {{ subject.questionCount }} 道题 · {{ subject.courseCount }} 门课程 ·
                {{ subject.routeCount }} 条路线
              </p>
            </div>
            <div class="subject-actions">
              <el-button
                v-permission="'subject:edit'"
                link
                size="small"
                :disabled="subjectIndex === 0"
                @click="moveSubject(subject, 'up')"
              >
                上移
              </el-button>
              <el-button
                v-permission="'subject:edit'"
                link
                size="small"
                :disabled="subjectIndex === subjects.length - 1"
                @click="moveSubject(subject, 'down')"
              >
                下移
              </el-button>
              <el-button
                v-permission="'subject:edit'"
                link
                type="primary"
                size="small"
                @click="openEditSubject(subject)"
              >
                编辑
              </el-button>
              <el-button
                v-permission="'subject:delete'"
                link
                type="danger"
                size="small"
                @click="removeSubject(subject)"
              >
                删除
              </el-button>
            </div>
          </header>

          <p class="subject-desc">{{ subject.description }}</p>

          <section class="stage-section">
            <button
              v-if="subject.stages.length"
              type="button"
              class="stage-toggle"
              :aria-expanded="isStagesExpanded(subject.id)"
              @click="toggleStages(subject.id)"
            >
              <span class="stage-toggle-main">
                阶段
                <span class="stage-badge">{{ subject.stages.length }}</span>
              </span>
              <span class="stage-toggle-action">
                {{ isStagesExpanded(subject.id) ? '收起' : '展开' }}
                <el-icon class="stage-chevron" :class="{ open: isStagesExpanded(subject.id) }">
                  <ArrowDown />
                </el-icon>
              </span>
            </button>
            <div v-else class="stage-head-static">
              <span>阶段</span>
              <span class="stage-badge muted">0</span>
            </div>

            <div
              v-if="subject.stages.length"
              v-show="isStagesExpanded(subject.id)"
              class="stage-list"
            >
              <div
                v-for="(stage, stageIndex) in subject.stages"
                :key="stage.id"
                class="stage-row"
                :class="{ disabled: stage.status === 0 }"
              >
                <span class="stage-icon">{{ stage.icon || DEFAULT_STAGE_ICON }}</span>
                <div class="stage-main">
                  <span class="stage-name">{{ stage.name }}</span>
                  <span class="stage-count">{{ stage.questionCount }} 题</span>
                  <el-tag v-if="stage.status === 0" size="small" type="info" effect="plain">
                    已停用
                  </el-tag>
                </div>
                <div class="stage-actions">
                  <el-button
                    v-permission="'subject:edit'"
                    link
                    size="small"
                    :disabled="stageIndex === 0"
                    @click="moveStage(subject, stage, 'up')"
                  >
                    上移
                  </el-button>
                  <el-button
                    v-permission="'subject:edit'"
                    link
                    size="small"
                    :disabled="stageIndex === subject.stages.length - 1"
                    @click="moveStage(subject, stage, 'down')"
                  >
                    下移
                  </el-button>
                  <el-button
                    v-permission="'subject:edit'"
                    link
                    type="primary"
                    size="small"
                    @click="openEditStage(subject, stage)"
                  >
                    编辑
                  </el-button>
                  <el-button
                    v-permission="'subject:edit'"
                    link
                    size="small"
                    :type="stage.status === 1 ? 'warning' : 'success'"
                    @click="toggleStage(subject, stage)"
                  >
                    {{ stage.status === 1 ? '停用' : '启用' }}
                  </el-button>
                  <el-button
                    v-permission="'subject:delete'"
                    link
                    type="danger"
                    size="small"
                    @click="removeStage(subject, stage)"
                  >
                    删除
                  </el-button>
                </div>
              </div>
            </div>
            <p v-else class="stage-empty">暂无阶段，点击下方按钮添加</p>

            <el-button
              v-permission="'subject:add'"
              class="add-stage"
              text
              type="primary"
              :icon="Plus"
              @click="openCreateStage(subject)"
            >
              添加阶段
            </el-button>
          </section>
        </article>
      </div>
      <el-empty v-else description="暂无学科，点击右上角「新增学科」开始配置" />
    </div>

    <!-- 新增 / 编辑学科 -->
    <el-dialog
      v-model="subjectDialogVisible"
      :title="subjectDialogTitle"
      width="620px"
      :close-on-click-modal="false"
    >
      <el-form ref="subjectFormRef" :model="subjectForm" :rules="subjectRules" label-width="88px">
        <div class="form-row">
          <el-form-item label="学科名称" prop="name">
            <el-input v-model.trim="subjectForm.name" maxlength="50" placeholder="请输入学科名称" />
          </el-form-item>
          <el-form-item label="学科图标" prop="icon">
            <el-input v-model.trim="subjectForm.icon" maxlength="50" placeholder="如 🤖" />
          </el-form-item>
        </div>
        <div class="form-row">
          <el-form-item label="排序号" prop="sortOrder">
            <el-input-number
              v-model="subjectForm.sortOrder"
              :min="0"
              :max="9999"
              controls-position="right"
            />
          </el-form-item>
          <el-form-item label="主题颜色" prop="themeColor">
            <div class="color-picker">
              <div class="color-palette">
                <button
                  v-for="color in THEME_COLORS"
                  :key="color"
                  type="button"
                  class="color-swatch"
                  :class="{ active: subjectForm.themeColor === color }"
                  :style="{ background: color }"
                  :title="color"
                  @click="subjectForm.themeColor = color"
                />
              </div>
              <div
                class="color-preview"
                :style="{
                  background: `${subjectForm.themeColor}18`,
                  color: subjectForm.themeColor
                }"
              >
                <span class="color-preview-icon" :style="{ background: subjectForm.themeColor }">
                  {{ subjectForm.icon || '🧩' }}
                </span>
                <span class="color-preview-text">
                  <strong>{{ subjectForm.name.trim() || '学科名称' }}</strong>
                  <span>主题色预览 · {{ subjectForm.themeColor }}</span>
                </span>
              </div>
            </div>
          </el-form-item>
        </div>
        <el-form-item label="学科描述" prop="description">
          <el-input
            v-model.trim="subjectForm.description"
            type="textarea"
            :rows="3"
            maxlength="255"
            show-word-limit
            placeholder="一句话说明该学科的学习方向"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="subjectDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="subjectSubmitting" @click="submitSubject">保存</el-button>
      </template>
    </el-dialog>

    <!-- 添加 / 编辑阶段 -->
    <el-dialog
      v-model="stageDialogVisible"
      :title="stageDialogTitle"
      width="480px"
      :close-on-click-modal="false"
    >
      <div v-if="stageSubject" class="stage-dialog-subject">
        所属学科：{{ stageSubject.icon }} {{ stageSubject.name }}
      </div>
      <el-form ref="stageFormRef" :model="stageForm" :rules="stageRules" label-width="88px">
        <el-form-item label="阶段名称" prop="name">
          <el-input v-model.trim="stageForm.name" maxlength="50" placeholder="请输入阶段名称" />
        </el-form-item>
        <div class="form-row">
          <el-form-item label="图标" prop="icon">
            <el-input v-model.trim="stageForm.icon" maxlength="50" placeholder="留空默认" />
          </el-form-item>
          <el-form-item label="排序号" prop="sortOrder">
            <el-input-number
              v-model="stageForm.sortOrder"
              :min="0"
              :max="9999"
              controls-position="right"
            />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="stageDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="stageSubmitting" @click="submitStage">保存</el-button>
      </template>
    </el-dialog>
  </AdminPage>
</template>

<style scoped>
.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
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
  font-size: 30px;
  line-height: 1.15;
}

.subject-grid-wrap {
  min-height: 220px;
  margin-top: 16px;
}

.subject-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.subject-card {
  display: flex;
  flex-direction: column;
  gap: 0;
  padding: 20px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r);
  box-shadow: var(--shadow);
}

.subject-head {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.subject-icon {
  display: grid;
  place-items: center;
  flex: none;
  width: 42px;
  height: 42px;
  border-radius: 11px;
  background: var(--paper);
  font-size: 20px;
}

.subject-title {
  flex: 1;
  min-width: 0;
}

.subject-title h3 {
  font-family: var(--font-display);
  font-size: 15px;
  line-height: 1.4;
}

.subject-title p {
  margin-top: 3px;
}

.subject-desc {
  margin: 10px 0 6px;
  color: var(--ink-3);
  font-size: 12px;
  line-height: 1.6;
}

.subject-actions {
  display: flex;
  align-items: center;
  flex: none;
  gap: 4px;
  flex-wrap: wrap;
  margin-left: 8px;
}

.subject-actions .el-button + .el-button,
.stage-actions .el-button + .el-button {
  margin-left: 0;
}

.stage-section {
  margin-top: 2px;
}

.stage-toggle {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  margin: 0 0 8px;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--ink-2);
  font-family: inherit;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.stage-toggle:hover {
  color: var(--jade);
}

.stage-toggle-main,
.stage-toggle-action {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.stage-toggle-action {
  color: var(--ink-3);
  font-size: 12px;
  font-weight: 400;
}

.stage-badge {
  display: inline-grid;
  place-items: center;
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  border-radius: 999px;
  background: var(--jade-soft);
  color: var(--jade-2);
  font-size: 11px;
  font-weight: 600;
}

.stage-badge.muted {
  background: var(--paper-2);
  color: var(--ink-3);
}

.stage-head-static {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
  color: var(--ink-2);
  font-size: 13px;
  font-weight: 600;
}

.stage-chevron {
  font-size: 12px;
  color: var(--ink-3);
  transform: rotate(-90deg);
  transition: transform 0.16s ease;
}

.stage-chevron.open {
  transform: rotate(0);
}

.stage-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.stage-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border: 1px solid var(--line);
  border-radius: var(--r-xs);
  background: var(--surface-2);
}

.stage-row.disabled {
  opacity: 0.66;
}

.stage-icon {
  flex: none;
  width: 24px;
  text-align: center;
}

.stage-main {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
}

.stage-name {
  overflow: hidden;
  color: var(--ink);
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.stage-count {
  color: var(--ink-3);
  font-size: 11px;
  white-space: nowrap;
}

.stage-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
}

.stage-empty {
  margin: 4px 0 0;
  color: var(--ink-4);
  font-size: 12px;
}

.add-stage {
  margin-top: 8px;
}

.stage-dialog-subject {
  margin: -8px 0 14px;
  color: var(--ink-3);
  font-size: 13px;
}

.form-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.color-picker {
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 100%;
}

.color-palette {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  padding: 4px 3px 3px;
}

.color-swatch {
  display: inline-grid;
  place-items: center;
  width: 28px;
  height: 28px;
  padding: 0;
  border: 2px solid transparent;
  border-radius: 9px;
  box-shadow: inset 0 0 0 1px rgba(21, 24, 29, 0.08);
  cursor: pointer;
  transition: transform 0.12s ease;
}

.color-swatch:hover {
  transform: translateY(-1px);
}

/* 选中态：外圈描边 + 勾选，深色色块上也能辨认 */
.color-swatch.active {
  outline: 2px solid var(--ink);
  outline-offset: 2px;
}

.color-swatch.active::after {
  content: '✓';
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  line-height: 1;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.45);
}

.color-preview {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 8px 10px;
  border-radius: var(--r-xs);
}

.color-preview-icon {
  display: inline-grid;
  place-items: center;
  flex: none;
  width: 26px;
  height: 26px;
  border-radius: 8px;
  font-size: 15px;
}

.color-preview-text {
  display: flex;
  flex-direction: column;
  min-width: 0;
  line-height: 1.35;
}

.color-preview-text strong {
  overflow: hidden;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.color-preview-text span {
  font-size: 11px;
  opacity: 0.8;
}

@media (max-width: 900px) {
  .stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .subject-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (max-width: 560px) {
  .stat-grid,
  .form-row {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
