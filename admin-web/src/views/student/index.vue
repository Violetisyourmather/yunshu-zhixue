<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download, Refresh, Search } from '@element-plus/icons-vue'
import AdminPage from '@/components/AdminPage.vue'
import {
  batchDeleteStudents,
  batchUpdateStudentStatus,
  deleteStudent,
  exportStudents,
  getStudentDetail,
  listStudentInterviews,
  listStudents,
  updateStudentStatus
} from '@/api/students'
import type { StudentDetail, StudentInterviewItem, StudentItem } from '@/types/content'

const DIFFICULTY_LABELS: Record<number, string> = {
  1: '简单',
  2: '中等',
  3: '困难'
}

const loading = ref(false)
const rows = ref<StudentItem[]>([])
const total = ref(0)
const selection = ref<StudentItem[]>([])
const tableRef = ref<{ clearSelection: () => void }>()

const query = reactive({
  keyword: '',
  status: undefined as number | undefined,
  page: 1,
  pageSize: 10
})

let keywordTimer: ReturnType<typeof setTimeout> | undefined

async function load(): Promise<void> {
  loading.value = true
  try {
    const result = await listStudents({
      keyword: query.keyword.trim() || undefined,
      status: query.status,
      page: query.page,
      pageSize: query.pageSize
    })
    rows.value = result.list
    total.value = result.total
  } finally {
    loading.value = false
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

function resetFilters(): void {
  query.keyword = ''
  query.status = undefined
  search()
}

function handleSizeChange(): void {
  query.page = 1
  void load()
}

function handleSelectionChange(value: unknown[]): void {
  selection.value = value as StudentItem[]
}

function studentInitial(source: unknown): string {
  const item = source as { nickname?: string } | null
  return item?.nickname?.slice(0, 1) || '学'
}

function statusLabel(status: number): string {
  return status === 1 ? '已启用' : '已禁用'
}

function statusTagType(status: number): 'success' | 'info' {
  return status === 1 ? 'success' : 'info'
}

function difficultyLabel(difficulty: number | null | undefined): string {
  return DIFFICULTY_LABELS[difficulty ?? 0] ?? '—'
}

function difficultyTagType(difficulty: number): 'success' | 'info' | 'warning' {
  if (difficulty === 1) {
    return 'success'
  }
  return difficulty === 2 ? 'info' : 'warning'
}

/* ---------- 详情与面试记录 ---------- */
const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<StudentDetail | null>(null)
const interviews = ref<StudentInterviewItem[]>([])
const interviewLoading = ref(false)
const interviewPage = ref(1)
const interviewPageSize = 5
const interviewTotal = ref(0)

const detailStatItems = computed(() => {
  const item = detail.value
  return item
    ? [
        { label: '累计学习天数', value: `${item.learningDays} 天` },
        { label: '已学课程', value: `${item.completedCourseCount} 门` },
        { label: '刷题总数', value: `${item.practiceCount} 题` },
        { label: '正确率', value: `${item.accuracyRate}%` },
        { label: '累计面试', value: `${item.interviewCount} 次` }
      ]
    : []
})

async function loadInterviews(studentId: number, page: number): Promise<void> {
  interviewLoading.value = true
  try {
    const result = await listStudentInterviews(studentId, page, interviewPageSize)
    interviews.value = result.list
    interviewTotal.value = result.total
    interviewPage.value = page
  } finally {
    interviewLoading.value = false
  }
}

async function openDetail(source: unknown): Promise<void> {
  const row = source as StudentItem
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  interviews.value = []
  interviewTotal.value = 0
  interviewPage.value = 1
  try {
    const [detailResult, interviewResult] = await Promise.all([
      getStudentDetail(row.id),
      listStudentInterviews(row.id, 1, interviewPageSize)
    ])
    detail.value = detailResult
    interviews.value = interviewResult.list
    interviewTotal.value = interviewResult.total
  } finally {
    detailLoading.value = false
  }
}

function handleInterviewPageChange(page: number): void {
  if (!detail.value) {
    return
  }
  void loadInterviews(detail.value.id, page)
}

/* ---------- 启停 / 删除 / 批量 ---------- */
async function toggleStatus(source: unknown): Promise<void> {
  const row = source as StudentItem
  const nextStatus = row.status === 1 ? 0 : 1
  const action = nextStatus === 1 ? '启用' : '禁用'
  const message = nextStatus === 1
    ? `确定启用学员「${row.nickname}」吗？`
    : `确定禁用学员「${row.nickname}」吗？禁用后该学员将无法登录学员端。`
  const confirmed = await ElMessageBox.confirm(message, `${action}学员`, {
    type: 'warning',
    confirmButtonText: action,
    cancelButtonText: '取消'
  }).catch(() => false)
  if (!confirmed) {
    return
  }
  await updateStudentStatus(row.id, nextStatus)
  ElMessage.success(`已${action}学员「${row.nickname}」`)
  detailVisible.value = false
  await load()
}

async function removeStudent(source: unknown): Promise<void> {
  const row = source as StudentItem
  const confirmed = await ElMessageBox.confirm(
    `确定删除学员「${row.nickname}」吗？逻辑删除，学员的历史作答与面试记录会保留归档。`,
    '删除学员',
    { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
  ).catch(() => false)
  if (!confirmed) {
    return
  }
  await deleteStudent(row.id)
  ElMessage.success('学员已删除')
  detailVisible.value = false
  await load()
}

async function batchStatus(status: number): Promise<void> {
  if (!selection.value.length) {
    return
  }
  const action = status === 1 ? '启用' : '禁用'
  const confirmed = await ElMessageBox.confirm(
    `确定${action}选中的 ${selection.value.length} 名学员吗？`,
    `批量${action}`,
    { type: 'warning', confirmButtonText: action, cancelButtonText: '取消' }
  ).catch(() => false)
  if (!confirmed) {
    return
  }
  const updated = await batchUpdateStudentStatus(
    selection.value.map((item) => item.id),
    status
  )
  ElMessage.success(`已${action} ${updated} 名学员`)
  tableRef.value?.clearSelection()
  await load()
}

async function batchRemove(): Promise<void> {
  if (!selection.value.length) {
    return
  }
  const count = selection.value.length
  const confirmed = await ElMessageBox.confirm(
    `确定删除选中的 ${count} 名学员吗？逻辑删除，历史记录会保留归档。`,
    '批量删除',
    { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
  ).catch(() => false)
  if (!confirmed) {
    return
  }
  await batchDeleteStudents(selection.value.map((item) => item.id))
  ElMessage.success(`已删除 ${count} 名学员`)
  tableRef.value?.clearSelection()
  await load()
}

/* ---------- 导出 ---------- */
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

async function handleExport(): Promise<void> {
  const blob = await exportStudents({
    keyword: query.keyword.trim() || undefined,
    status: query.status,
    page: 1,
    pageSize: query.pageSize
  })
  const date = new Date().toISOString().slice(0, 10).replace(/-/g, '')
  saveBlob(blob, `学员列表_${date}.xlsx`)
}

onMounted(() => {
  void load()
})
</script>

<template>
  <AdminPage
    title="学员管理"
    description="学员由前台注册产生，后台只做查看、禁用、导出与删除。"
  >
    <template #actions>
      <el-button v-permission="'student:view'" :icon="Download" @click="handleExport">
        导出学员
      </el-button>
    </template>

    <div class="panel table-panel">
      <div class="student-filter-bar">
        <el-form class="filter-form" @submit.prevent="search">
          <el-form-item>
            <el-input
              v-model="query.keyword"
              class="filter-keyword"
              placeholder="搜索昵称 / 手机号"
              clearable
              :prefix-icon="Search"
              @input="handleKeywordInput"
              @keyup.enter="search"
              @clear="search"
            />
          </el-form-item>
          <el-form-item>
            <el-select v-model="query.status" placeholder="全部状态" clearable @change="search">
              <el-option label="已启用" :value="1" />
              <el-option label="已禁用" :value="0" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button :icon="Refresh" @click="resetFilters">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <div v-if="selection.length" class="batch-bar">
        <span>已选择 <span class="batch-count">{{ selection.length }}</span> 名学员</span>
        <el-button v-permission="'student:edit'" size="small" @click="batchStatus(1)">批量启用</el-button>
        <el-button v-permission="'student:edit'" size="small" @click="batchStatus(0)">批量禁用</el-button>
        <el-button
          v-permission="'student:delete'"
          size="small"
          type="danger"
          plain
          @click="batchRemove"
        >
          批量删除
        </el-button>
      </div>

      <el-table
        ref="tableRef"
        v-loading="loading"
        :data="rows"
        row-key="id"
        empty-text="没有找到匹配的学员"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="46" :reserve-selection="true" />
        <el-table-column label="学员" min-width="190">
          <template #default="{ row }">
            <div class="student-cell">
              <span class="student-avatar">{{ studentInitial(row) }}</span>
              <span class="cell-strong">{{ row.nickname }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="手机号" min-width="130">
          <template #default="{ row }">
            <span class="cell-sub">{{ row.mobile }}</span>
          </template>
        </el-table-column>
        <el-table-column label="注册时间" min-width="160">
          <template #default="{ row }">
            <span class="cell-sub">{{ row.createdAt }}</span>
          </template>
        </el-table-column>
        <el-table-column label="累计学习天数" width="130">
          <template #default="{ row }">{{ row.learningDays }} 天</template>
        </el-table-column>
        <el-table-column label="刷题 / 面试" min-width="140">
          <template #default="{ row }">
            {{ row.practiceCount }} 题 / {{ row.interviewCount }} 次
          </template>
        </el-table-column>
        <el-table-column label="状态" width="92">
          <template #default="{ row }">
            <el-tag size="small" effect="light" :type="statusTagType(row.status)">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <div class="row-actions">
              <el-button text type="primary" size="small" @click="openDetail(row)">查看详情</el-button>
              <el-button
                v-permission="'student:edit'"
                text
                :type="row.status === 1 ? 'warning' : 'success'"
                size="small"
                @click="toggleStatus(row)"
              >
                {{ row.status === 1 ? '禁用' : '启用' }}
              </el-button>
              <el-button
                v-permission="'student:delete'"
                text
                type="danger"
                size="small"
                @click="removeStudent(row)"
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

    <!-- 学员详情 -->
    <el-drawer v-model="detailVisible" title="学员详情" size="560px">
      <div v-loading="detailLoading" class="detail-body">
        <template v-if="detail">
          <div class="detail-profile">
            <el-avatar
              :size="56"
              :src="detail.avatar || undefined"
              @error="detail.avatar = null"
            >
              {{ studentInitial(detail) }}
            </el-avatar>
            <div class="profile-text">
              <h3>{{ detail.nickname }}</h3>
              <div class="profile-meta">
                <el-tag size="small" effect="light" :type="statusTagType(detail.status)">
                  {{ statusLabel(detail.status) }}
                </el-tag>
                <span class="cell-sub">注册于 {{ detail.createdAt }}</span>
              </div>
            </div>
          </div>

          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="手机号">{{ detail.mobile }}</el-descriptions-item>
            <el-descriptions-item label="邮箱">{{ detail.email || '—' }}</el-descriptions-item>
            <el-descriptions-item label="注册时间">{{ detail.createdAt }}</el-descriptions-item>
            <el-descriptions-item label="最后登录">
              {{ detail.lastLoginAt || '从未登录' }}
            </el-descriptions-item>
          </el-descriptions>

          <h4 class="detail-section-title">学习统计</h4>
          <div class="detail-stats">
            <div v-for="item in detailStatItems" :key="item.label" class="detail-stat">
              <span class="cell-sub">{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </div>
          </div>

          <h4 class="detail-section-title">面试记录</h4>
          <el-table
            v-loading="interviewLoading"
            :data="interviews"
            size="small"
            empty-text="暂无面试记录"
          >
            <el-table-column label="方向" min-width="150">
              <template #default="{ row }">
                <div>{{ row.subjectName || '—' }}</div>
                <div class="cell-sub">{{ row.questionCount }} 题</div>
              </template>
            </el-table-column>
            <el-table-column label="难度" width="78">
              <template #default="{ row }">
                <el-tag size="small" effect="plain" :type="difficultyTagType(row.difficulty)">
                  {{ difficultyLabel(row.difficulty) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="评分" width="86">
              <template #default="{ row }">
                {{ row.overallScore == null ? '—' : `${row.overallScore} / 10` }}
              </template>
            </el-table-column>
            <el-table-column label="完成时间" min-width="150">
              <template #default="{ row }">
                <span class="cell-sub">{{ row.completedAt }}</span>
              </template>
            </el-table-column>
          </el-table>
          <div v-if="interviewTotal > 0" class="interview-pagination">
            <el-pagination
              v-model:current-page="interviewPage"
              :page-size="interviewPageSize"
              :total="interviewTotal"
              layout="prev, pager, next"
              small
              background
              @current-change="handleInterviewPageChange"
            />
          </div>

          <p class="detail-note">
            说明：「已学课程」取学员在路线中手动勾选「标记完成」的课程，不含观看进度与时长；
            后台不展示面试作答原文。
          </p>
        </template>
      </div>
    </el-drawer>
  </AdminPage>
</template>

<style scoped>
.student-filter-bar {
  padding: 16px 18px 0;
}

.student-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.student-avatar {
  display: inline-grid;
  place-items: center;
  flex: none;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: var(--jade-soft);
  color: var(--jade-2);
  font-family: var(--font-display);
  font-size: 13px;
  font-weight: 600;
}

.detail-body {
  min-height: 220px;
}

.detail-profile {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 20px;
}

.profile-text h3 {
  font-family: var(--font-display);
  font-size: 20px;
}

.profile-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 6px;
}

.detail-section-title {
  margin: 22px 0 10px;
  font-size: 14px;
}

.detail-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.detail-stat {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 10px 12px;
  border: 1px solid var(--line);
  border-radius: var(--r-xs);
  background: var(--surface-2);
}

.detail-stat strong {
  color: var(--ink);
  font-size: 15px;
}

.interview-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 10px;
}

.detail-note {
  margin: 18px 0 0;
  color: var(--ink-3);
  font-size: 12px;
  line-height: 1.6;
}

@media (max-width: 560px) {
  .detail-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
