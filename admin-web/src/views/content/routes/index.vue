<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import AdminPage from '@/components/AdminPage.vue'
import RouteEditDialog from '@/components/RouteEditDialog.vue'
import RoutePreviewDialog from '@/components/RoutePreviewDialog.vue'
import { deleteRoute, getRouteDetail, listRoutes, updateRouteStatus } from '@/api/routes'
import type { RouteCard, RouteDetail } from '@/types/content'

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const routes = ref<RouteCard[]>([])

const editVisible = ref(false)
const editingRoute = ref<RouteCard | null>(null)

const previewVisible = ref(false)
const previewRoute = ref<RouteDetail | null>(null)

async function load(): Promise<void> {
  loading.value = true
  try {
    routes.value = await listRoutes()
  } finally {
    loading.value = false
  }
}

function formatHours(minutes: number): string {
  const hours = minutes / 60
  return `${Number.isInteger(hours) ? hours : hours.toFixed(1)} 小时`
}

function openCreate(): void {
  editingRoute.value = null
  editVisible.value = true
}

function openEdit(route: RouteCard): void {
  editingRoute.value = route
  editVisible.value = true
}

function goDetail(route: RouteCard): void {
  void router.push(`/content/routes/${route.id}`)
}

async function openPreview(route: RouteCard): Promise<void> {
  previewRoute.value = await getRouteDetail(route.id)
  previewVisible.value = true
}

async function toggleStatus(route: RouteCard): Promise<void> {
  const nextStatus = route.status === 1 ? 0 : 1
  const action = nextStatus === 1 ? '启用' : '停用'
  const confirmed = await ElMessageBox.confirm(
    nextStatus === 1
      ? `确定启用路线「${route.name}」吗？启用后学员端可见。`
      : `确定停用路线「${route.name}」吗？停用后学员端不再展示该路线。`,
    `${action}学习路线`,
    { type: 'warning', confirmButtonText: action, cancelButtonText: '取消' }
  ).catch(() => false)
  if (!confirmed) {
    return
  }
  await updateRouteStatus(route.id, nextStatus)
  ElMessage.success(`路线已${action}`)
  await load()
}

async function removeRoute(route: RouteCard): Promise<void> {
  const confirmed = await ElMessageBox.confirm(
    `确定删除路线「${route.name}」吗？删除后学员端将不再展示该路线。`,
    '删除学习路线',
    { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
  ).catch(() => false)
  if (!confirmed) {
    return
  }
  await deleteRoute(route.id)
  ElMessage.success('学习路线已删除')
  await load()
}

onMounted(async () => {
  await load()
  // 数据看板「创建新路线」入口：自动打开新建弹窗，并清掉 query 避免刷新/返回时重复弹出。
  if (route.query.new === '1') {
    openCreate()
    await router.replace({ path: '/content/routes' })
  }
})
</script>

<template>
  <AdminPage
    title="学习路线"
    description="一条路线是一组有序的 B 站课程；课程来自课程库，可跨学科补充。"
  >
    <template #actions>
      <el-button v-permission="'route:add'" type="primary" :icon="Plus" @click="openCreate">
        新建路线
      </el-button>
    </template>

    <div v-loading="loading" class="route-grid-wrap">
      <div v-if="routes.length" class="route-grid">
        <article v-for="route in routes" :key="route.id" class="route-card">
          <header class="route-head">
            <span class="route-icon">{{ route.icon || '🗺️' }}</span>
            <div class="route-title">
              <h3>{{ route.name }}</h3>
              <p class="cell-sub">
                {{ route.subjectName || '—' }} · {{ route.targetEducation || '不限' }} ·
                {{ route.courseCount }} 门课 · {{ formatHours(route.totalMinutes) }}
              </p>
            </div>
            <el-tag
              size="small"
              effect="light"
              :type="route.status === 1 ? 'success' : 'info'"
            >
              {{ route.status === 1 ? '已启用' : '已停用' }}
            </el-tag>
          </header>

          <p class="route-desc">{{ route.description || '暂无描述' }}</p>

          <div class="route-preview">
            <span class="preview-label">课程预览</span>
            <span v-if="route.previewCourses.length" class="preview-text">
              {{ route.previewCourses.map((course) => course.name).join('、') }}
              <template v-if="route.courseCount > 4"> 等</template>
            </span>
            <span v-else class="cell-sub">暂无课程</span>
          </div>

          <footer class="route-actions">
            <el-button size="small" @click="goDetail(route)">进入详情</el-button>
            <el-button
              v-permission="'route:edit'"
              size="small"
              @click="openEdit(route)"
            >
              编辑
            </el-button>
            <el-button
              v-permission="'route:edit'"
              size="small"
              @click="toggleStatus(route)"
            >
              {{ route.status === 1 ? '停用' : '启用' }}
            </el-button>
            <el-button size="small" @click="openPreview(route)">预览</el-button>
            <el-button
              v-permission="'route:delete'"
              size="small"
              type="danger"
              plain
              @click="removeRoute(route)"
            >
              删除
            </el-button>
          </footer>
        </article>
      </div>
      <el-empty v-else description="暂无学习路线，点击上方「新建路线」创建一条" />
    </div>

    <RouteEditDialog v-model="editVisible" :route="editingRoute" @saved="load" />
    <RoutePreviewDialog v-model="previewVisible" :route="previewRoute" />
  </AdminPage>
</template>

<style scoped>
.route-grid-wrap {
  min-height: 220px;
}

.route-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.route-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 20px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r);
  box-shadow: var(--shadow);
}

.route-head {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.route-icon {
  display: grid;
  place-items: center;
  flex: none;
  width: 42px;
  height: 42px;
  border-radius: 11px;
  background: var(--paper);
  font-size: 20px;
}

.route-title {
  flex: 1;
  min-width: 0;
}

.route-title h3 {
  font-family: var(--font-display);
  font-size: 15px;
  line-height: 1.4;
}

.route-title p {
  margin-top: 3px;
}

.route-desc {
  margin: 0;
  color: var(--ink-3);
  font-size: 12px;
  line-height: 1.6;
}

.route-preview {
  color: var(--ink-3);
  font-size: 12px;
  line-height: 1.6;
}

.preview-label {
  margin-right: 8px;
  color: var(--ink-4);
}

.preview-text {
  color: var(--ink-3);
}

.route-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 4px;
}

.route-actions .el-button + .el-button {
  margin-left: 0;
}

@media (max-width: 900px) {
  .route-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
