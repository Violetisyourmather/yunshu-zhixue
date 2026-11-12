<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Plus, Search } from '@element-plus/icons-vue'
import AdminPage from '@/components/AdminPage.vue'
import {
  addRouteCourses,
  getRouteDetail,
  listRouteCourseOptions,
  removeRouteCourse,
  sortRouteCourses
} from '@/api/routes'
import type {
  RouteCourseItem,
  RouteCourseOption,
  RouteDetail,
  SubjectRef
} from '@/types/content'

const currentRoute = useRoute()
const router = useRouter()
const routeId = computed(() => Number(currentRoute.params.id))

const loading = ref(false)
const detail = ref<RouteDetail | null>(null)

const pageTitle = computed(() => (detail.value ? `${detail.value.name} - 课程管理` : '学习路线'))

async function load(): Promise<void> {
  if (!Number.isFinite(routeId.value) || routeId.value <= 0) {
    detail.value = null
    return
  }
  loading.value = true
  try {
    detail.value = await getRouteDetail(routeId.value)
  } catch {
    detail.value = null
  } finally {
    loading.value = false
  }
}

function goBack(): void {
  void router.push('/content/routes')
}

function formatHours(minutes: number): string {
  const hours = minutes / 60
  return `${Number.isInteger(hours) ? hours : hours.toFixed(1)} 小时`
}

function formatDuration(minutes: number): string {
  const hours = Math.floor(minutes / 60)
  const rest = minutes % 60
  if (hours === 0) {
    return `${rest} 分钟`
  }
  return rest === 0 ? `${hours} 小时` : `${hours} 小时 ${rest} 分`
}

function subjectNames(subjects: SubjectRef[]): string {
  return subjects.length ? subjects.map((subject) => subject.name).join('、') : '其他学科'
}

async function moveCourse(index: number, direction: -1 | 1): Promise<void> {
  if (!detail.value) {
    return
  }
  const target = index + direction
  if (target < 0 || target >= detail.value.courses.length) {
    return
  }
  const ordered = [...detail.value.courses]
  ;[ordered[index], ordered[target]] = [ordered[target], ordered[index]]
  await sortRouteCourses(detail.value.id, ordered.map((course) => course.routeCourseId))
  await load()
}

async function removeCourse(source: unknown): Promise<void> {
  const course = source as RouteCourseItem
  if (!detail.value) {
    return
  }
  const confirmed = await ElMessageBox.confirm(
    `确定从路线中移除课程「${course.name}」吗？`,
    '移除课程',
    { type: 'warning', confirmButtonText: '移除', cancelButtonText: '取消' }
  ).catch(() => false)
  if (!confirmed) {
    return
  }
  await removeRouteCourse(detail.value.id, course.courseId)
  ElMessage.success('课程已移除')
  await load()
}

/* ---------- 从课程库添加 ---------- */
const addVisible = ref(false)
const addLoading = ref(false)
const addSubmitting = ref(false)
const addKeyword = ref('')
const addOptions = ref<RouteCourseOption[]>([])
const selectedCourseIds = ref<number[]>([])
let addSearchTimer: ReturnType<typeof setTimeout> | undefined

const sameSubjectOptions = computed(() => addOptions.value.filter((option) => option.sameSubject))
const crossSubjectOptions = computed(() => addOptions.value.filter((option) => !option.sameSubject))
const selectedOptions = computed(() =>
  addOptions.value.filter((option) => selectedCourseIds.value.includes(option.courseId))
)
const hasOfflineSelected = computed(() =>
  selectedOptions.value.some((option) => option.status === 2)
)

function openAdd(): void {
  addVisible.value = true
  addKeyword.value = ''
  selectedCourseIds.value = []
  addOptions.value = []
  void loadAddOptions()
}

function resetAdd(): void {
  if (addSearchTimer) {
    clearTimeout(addSearchTimer)
    addSearchTimer = undefined
  }
  addKeyword.value = ''
  selectedCourseIds.value = []
  addOptions.value = []
}

async function loadAddOptions(): Promise<void> {
  if (!detail.value) {
    return
  }
  addLoading.value = true
  try {
    addOptions.value = await listRouteCourseOptions(
      detail.value.id,
      addKeyword.value.trim() || undefined
    )
  } finally {
    addLoading.value = false
  }
}

function handleAddKeywordInput(): void {
  if (addSearchTimer) {
    clearTimeout(addSearchTimer)
  }
  addSearchTimer = setTimeout(() => {
    void loadAddOptions()
  }, 300)
}

async function confirmAdd(): Promise<void> {
  if (!detail.value || !selectedCourseIds.value.length) {
    return
  }
  addSubmitting.value = true
  try {
    const added = await addRouteCourses(detail.value.id, selectedCourseIds.value)
    ElMessage.success(added > 0 ? `已添加 ${added} 门课程` : '所选课程已全部在路线中')
    addVisible.value = false
    await load()
  } finally {
    addSubmitting.value = false
  }
}

watch(routeId, () => {
  void load()
})

onMounted(() => {
  void load()
})
</script>

<template>
  <AdminPage
    :title="pageTitle"
    description="调整课程顺序即调整学员的学习顺序。"
  >
    <template #actions>
      <el-button :icon="ArrowLeft" @click="goBack">返回列表</el-button>
      <el-button
        v-if="detail"
        v-permission="'route:edit'"
        type="primary"
        :icon="Plus"
        @click="openAdd"
      >
        从课程库添加
      </el-button>
    </template>

    <div v-loading="loading" class="detail-wrap">
      <template v-if="detail">
        <div class="panel route-summary">
          <div class="summary-grid">
            <div class="summary-item">
              <span class="cell-sub">关联学科</span>
              <strong>{{ detail.subjectName || '—' }}</strong>
            </div>
            <div class="summary-item">
              <span class="cell-sub">状态</span>
              <el-tag
                size="small"
                effect="light"
                :type="detail.status === 1 ? 'success' : 'info'"
              >
                {{ detail.status === 1 ? '已启用' : '已停用' }}
              </el-tag>
            </div>
            <div class="summary-item">
              <span class="cell-sub">课程数 / 总时长</span>
              <strong>{{ detail.courseCount }} 门 / {{ formatHours(detail.totalMinutes) }}</strong>
            </div>
            <div class="summary-item">
              <span class="cell-sub">适合学历</span>
              <strong>{{ detail.targetEducation || '不限' }}</strong>
            </div>
          </div>

          <p class="summary-desc">{{ detail.description || '暂无描述' }}</p>
        </div>

        <div class="panel table-panel">
          <el-table
            :data="detail.courses"
            row-key="routeCourseId"
            empty-text="暂无课程，点击右上角从课程库添加"
          >
            <el-table-column label="序号" width="64">
              <template #default="{ $index }">
                <span class="cell-sub">{{ String($index + 1).padStart(2, '0') }}</span>
              </template>
            </el-table-column>
            <el-table-column label="BV号" width="130">
              <template #default="{ row }">
                <span class="course-bvid">{{ row.bvid || '—' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="课程名称" min-width="260">
              <template #default="{ row }">
                <div class="course-name-row">
                  <span class="course-name">{{ row.name }}</span>
                  <el-tag v-if="row.status === 2" size="small" type="warning" effect="plain">
                    已下架
                  </el-tag>
                </div>
                <div class="course-subjects">{{ subjectNames(row.subjects) }}</div>
              </template>
            </el-table-column>
            <el-table-column label="时长" width="120">
              <template #default="{ row }">{{ formatDuration(row.durationMinutes) }}</template>
            </el-table-column>
            <el-table-column label="归属" width="100">
              <template #default="{ row }">
                <el-tag
                  size="small"
                  effect="plain"
                  :type="row.sameSubject ? 'success' : 'info'"
                >
                  {{ row.sameSubject ? '本学科' : '跨学科' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="排序" width="120">
              <template #default="{ $index }">
                <div class="sort-actions">
                  <el-button
                    v-permission="'route:edit'"
                    link
                    size="small"
                    :disabled="$index === 0"
                    @click="moveCourse($index, -1)"
                  >
                    ↑
                  </el-button>
                  <el-button
                    v-permission="'route:edit'"
                    link
                    size="small"
                    :disabled="$index === detail.courses.length - 1"
                    @click="moveCourse($index, 1)"
                  >
                    ↓
                  </el-button>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="90" fixed="right">
              <template #default="{ row }">
                <el-button
                  v-permission="'route:edit'"
                  link
                  type="danger"
                  size="small"
                  @click="removeCourse(row)"
                >
                  移除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </template>

      <el-empty v-else-if="!loading" description="学习路线不存在或已删除">
        <el-button type="primary" @click="goBack">返回列表</el-button>
      </el-empty>
    </div>

    <el-dialog
      v-model="addVisible"
      title="从课程库添加"
      width="820px"
      :close-on-click-modal="false"
      @closed="resetAdd"
    >
      <el-input
        v-model="addKeyword"
        class="add-search"
        placeholder="搜索课程名称 / BV号"
        clearable
        :prefix-icon="Search"
        @input="handleAddKeywordInput"
        @keyup.enter="loadAddOptions"
        @clear="loadAddOptions"
      />
      <p class="add-tip">
        课程按「本学科 / 跨学科补充」分组；下架课程可选，但学员端暂不展示，恢复上架后可见。
        单次最多展示 300 门课程，可用搜索查找更多。
      </p>

      <div v-loading="addLoading" class="course-option-groups">
        <section v-if="sameSubjectOptions.length" class="option-group">
          <h4 class="option-title">本学科课程</h4>
          <el-checkbox-group v-model="selectedCourseIds">
            <el-checkbox
              v-for="option in sameSubjectOptions"
              :key="option.courseId"
              :value="option.courseId"
              :disabled="option.added"
              class="course-option"
              :class="{ added: option.added }"
            >
              <div class="option-main">
                <div class="option-name">
                  <span class="option-title-text">{{ option.name }}</span>
                  <el-tag v-if="option.status === 2" size="small" type="warning" effect="plain">
                    已下架
                  </el-tag>
                  <el-tag v-if="option.added" size="small" type="info" effect="plain">
                    已添加
                  </el-tag>
                </div>
                <div class="option-sub">
                  {{ option.bvid || '—' }} · {{ formatDuration(option.durationMinutes) }} ·
                  {{ subjectNames(option.subjects) }}
                </div>
              </div>
            </el-checkbox>
          </el-checkbox-group>
        </section>

        <section v-if="crossSubjectOptions.length" class="option-group">
          <h4 class="option-title">其他学科课程（跨学科补充）</h4>
          <el-checkbox-group v-model="selectedCourseIds">
            <el-checkbox
              v-for="option in crossSubjectOptions"
              :key="option.courseId"
              :value="option.courseId"
              :disabled="option.added"
              class="course-option"
              :class="{ added: option.added }"
            >
              <div class="option-main">
                <div class="option-name">
                  <span class="option-title-text">{{ option.name }}</span>
                  <el-tag v-if="option.status === 2" size="small" type="warning" effect="plain">
                    已下架
                  </el-tag>
                  <el-tag v-if="option.added" size="small" type="info" effect="plain">
                    已添加
                  </el-tag>
                </div>
                <div class="option-sub">
                  {{ option.bvid || '—' }} · {{ formatDuration(option.durationMinutes) }} ·
                  {{ subjectNames(option.subjects) }}
                </div>
              </div>
            </el-checkbox>
          </el-checkbox-group>
        </section>

        <el-empty
          v-if="!addLoading && !addOptions.length"
          description="没有匹配的课程，试试调整搜索关键词"
          :image-size="72"
        />
      </div>

      <el-alert
        v-if="hasOfflineSelected"
        class="add-alert"
        type="warning"
        :closable="false"
        show-icon
        title="已选课程中包含已下架课程，学员端暂不展示。"
      />

      <template #footer>
        <span class="add-count">已选 {{ selectedCourseIds.length }} 门</span>
        <el-button @click="addVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="addSubmitting"
          :disabled="!selectedCourseIds.length"
          @click="confirmAdd"
        >
          添加所选
        </el-button>
      </template>
    </el-dialog>

  </AdminPage>
</template>

<style scoped>
.detail-wrap {
  min-height: 220px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.route-summary {
  padding: 18px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.summary-item {
  display: flex;
  flex-direction: column;
  gap: 5px;
  min-width: 0;
}

.summary-item strong {
  color: var(--ink);
  font-size: 14px;
  font-weight: 600;
}

.summary-desc {
  margin: 14px 0 0;
  color: var(--ink-2);
  font-size: 13px;
  line-height: 1.7;
}

.course-name-row {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.course-name {
  color: var(--ink);
  font-weight: 600;
}

.course-bvid {
  color: var(--ink-3);
  font-family: var(--font-mono);
  font-size: 11px;
}

.course-subjects {
  margin-top: 2px;
  color: var(--ink-3);
  font-size: 12px;
}

.sort-actions {
  display: flex;
  align-items: center;
}

.sort-actions .el-button + .el-button {
  margin-left: 2px;
}

.add-search {
  margin-bottom: 8px;
}

.add-tip {
  margin: 0 0 10px;
  color: var(--ink-3);
  font-size: 12px;
  line-height: 1.6;
}

.course-option-groups {
  max-height: 420px;
  min-height: 120px;
  overflow: auto;
  padding-right: 4px;
}

.option-group {
  min-width: 0;
}

.option-title {
  margin: 14px 0 6px;
  color: var(--ink-2);
  font-size: 13px;
  font-weight: 600;
  line-height: 1.5;
}

.option-group:first-child .option-title {
  margin-top: 4px;
}

.course-option {
  display: flex;
  align-items: flex-start;
  width: 100%;
  height: auto;
  margin-right: 0;
  padding: 9px 4px;
  border-bottom: 1px dashed var(--line);
  box-sizing: border-box;
  white-space: normal;
}

.course-option.added {
  opacity: 0.66;
}

.course-option :deep(.el-checkbox__label) {
  flex: 1;
  min-width: 0;
  overflow: visible;
  white-space: normal;
  line-height: 1.45;
}

.option-main {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.option-name {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  flex-wrap: wrap;
  color: var(--ink);
  font-size: 13px;
  line-height: 1.45;
}

.option-title-text {
  flex: 0 1 auto;
  min-width: 0;
  overflow-wrap: anywhere;
}

.option-name :deep(.el-tag) {
  flex: none;
}

.option-sub {
  color: var(--ink-3);
  font-size: 11px;
  line-height: 1.45;
  overflow-wrap: anywhere;
  white-space: normal;
}

.add-alert {
  margin-top: 12px;
}

.add-count {
  margin-right: 12px;
  color: var(--ink-3);
  font-size: 12px;
}

@media (max-width: 900px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 560px) {
  .summary-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
