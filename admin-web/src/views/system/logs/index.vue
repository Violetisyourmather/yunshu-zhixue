<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download, Delete, Refresh, Search } from '@element-plus/icons-vue'
import AdminPage from '@/components/AdminPage.vue'
import { clearLogs, exportLogs, getLog, listLogs } from '@/api/logs'
import type { OperationLogItem } from '@/types/system'

const OPERATION_TYPES = ['登录', '登出', '新增', '修改', '删除', '禁用启用', '导出', '其他']

const loading = ref(false)
const exporting = ref(false)
const rows = ref<OperationLogItem[]>([])
const total = ref(0)
const dateRange = ref<string[]>([])

const query = reactive({
  keyword: '',
  operationType: '',
  page: 1,
  pageSize: 10
})

function filterParams(): {
  keyword?: string
  operationType?: string
  startDate?: string
  endDate?: string
} {
  return {
    keyword: query.keyword.trim() || undefined,
    operationType: query.operationType || undefined,
    startDate: dateRange.value[0],
    endDate: dateRange.value[1]
  }
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const result = await listLogs({
      ...filterParams(),
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
  query.page = 1
  void load()
}

function resetFilters(): void {
  query.keyword = ''
  query.operationType = ''
  dateRange.value = []
  query.page = 1
  void load()
}

function handleSizeChange(): void {
  query.page = 1
  void load()
}

function actionTagType(action: string): 'primary' | 'success' | 'info' | 'warning' | 'danger' {
  switch (action) {
    case '新增':
    case '登录':
      return 'success'
    case '修改':
      return 'primary'
    case '删除':
      return 'danger'
    case '禁用启用':
      return 'warning'
    default:
      return 'info'
  }
}

/* ---------- 详情 ---------- */
const detailVisible = ref(false)
const detail = ref<OperationLogItem | null>(null)

async function openDetail(row: OperationLogItem): Promise<void> {
  detail.value = null
  detailVisible.value = true
  detail.value = await getLog(row.id)
}

/* ---------- 导出 / 清空 ---------- */
async function handleExport(): Promise<void> {
  exporting.value = true
  try {
    const blob = await exportLogs(filterParams())
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    const stamp = new Date()
      .toISOString()
      .slice(0, 19)
      .replace(/[-:T]/g, '')
    link.href = url
    link.download = `操作日志_${stamp}.csv`
    link.click()
    URL.revokeObjectURL(url)
    ElMessage.success('日志已导出')
  } finally {
    exporting.value = false
  }
}

async function handleClear(): Promise<void> {
  const confirmed = await ElMessageBox.confirm(
    '确定清空全部操作日志吗？清空后不可恢复，仅保留本次清空记录。',
    '清空日志',
    { type: 'warning', confirmButtonText: '清空', cancelButtonText: '取消' }
  ).catch(() => false)
  if (!confirmed) {
    return
  }
  const count = await clearLogs()
  ElMessage.success(`已清空 ${count} 条日志`)
  query.page = 1
  await load()
}

onMounted(load)
</script>

<template>
  <AdminPage title="操作日志" description="记录管理端写操作与登录登出，支持筛选、详情、导出与清空。">
    <template #actions>
      <el-button
        v-permission="'system:log_view'"
        :icon="Download"
        :loading="exporting"
        @click="handleExport"
      >
        导出日志
      </el-button>
      <el-button
        v-permission="'system:log_view'"
        type="danger"
        plain
        :icon="Delete"
        @click="handleClear"
      >
        清空日志
      </el-button>
    </template>

    <div class="panel filter-panel">
      <el-form class="filter-form" @submit.prevent="search">
        <el-form-item>
          <el-input
            v-model="query.keyword"
            class="filter-keyword"
            placeholder="搜索用户 / 操作 / 目标"
            clearable
            :prefix-icon="Search"
            @keyup.enter="search"
            @clear="search"
          />
        </el-form-item>
        <el-form-item>
          <el-select v-model="query.operationType" placeholder="全部操作类型" clearable @change="search">
            <el-option v-for="type in OPERATION_TYPES" :key="type" :label="type" :value="type" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-date-picker
            v-model="dateRange"
            class="filter-date"
            type="daterange"
            value-format="YYYY-MM-DD"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            @change="search"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button :icon="Refresh" @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="panel table-panel">
      <el-table
        v-loading="loading"
        :data="rows"
        row-key="id"
        empty-text="暂无匹配的日志记录"
        class="log-table"
        @row-click="openDetail"
      >
        <el-table-column label="用户" min-width="120">
          <template #default="{ row }">
            <span class="cell-strong">{{ row.username }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="110">
          <template #default="{ row }">
            <el-tag :type="actionTagType(row.action)" effect="light" size="small">
              {{ row.action }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="目标" min-width="240">
          <template #default="{ row }">
            <span class="cell-sub">{{ row.targetType }}</span>
            <span class="target-sep">·</span>
            <span>{{ row.targetName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="IP地址" width="150">
          <template #default="{ row }">
            <span class="mono cell-sub">{{ row.ip }}</span>
          </template>
        </el-table-column>
        <el-table-column label="时间" width="180">
          <template #default="{ row }">
            <span class="cell-sub">{{ row.createdAt }}</span>
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

    <el-dialog v-model="detailVisible" title="日志详情" width="500px">
      <el-descriptions v-if="detail" :column="1" border size="small">
        <el-descriptions-item label="日志ID">
          <span class="mono">{{ detail.id }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="操作用户">{{ detail.username }}</el-descriptions-item>
        <el-descriptions-item label="操作类型">
          <el-tag :type="actionTagType(detail.action)" effect="light" size="small">
            {{ detail.action }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="操作对象">
          {{ detail.targetType }} · {{ detail.targetName }}
        </el-descriptions-item>
        <el-descriptions-item label="IP地址">
          <span class="mono">{{ detail.ip }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="操作时间">{{ detail.createdAt }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button type="primary" @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </AdminPage>
</template>

<style scoped>
.filter-form :deep(.filter-date.el-date-editor) {
  width: 260px;
}

.log-table :deep(.el-table__row) {
  cursor: pointer;
}

.target-sep {
  margin: 0 6px;
  color: var(--ink-4);
}
</style>
