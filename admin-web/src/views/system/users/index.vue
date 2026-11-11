<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import AdminPage from '@/components/AdminPage.vue'
import {
  batchDeleteUsers,
  batchUpdateUserStatus,
  createUser,
  deleteUser,
  getUserDetail,
  listRoleOptions,
  listUsers,
  resetUserPassword,
  updateUser
} from '@/api/users'
import { useUserStore } from '@/store/user'
import type { RoleOption, UserDetail, UserItem, UserPayload } from '@/types/system'

const userStore = useUserStore()

const loading = ref(false)
const rows = ref<UserItem[]>([])
const total = ref(0)
const roleOptions = ref<RoleOption[]>([])
const selection = ref<UserItem[]>([])
const tableRef = ref<{ clearSelection: () => void }>()

const query = reactive({
  keyword: '',
  roleId: undefined as number | undefined,
  status: undefined as number | undefined,
  page: 1,
  pageSize: 10
})

const selectableRoles = computed(() => roleOptions.value.filter((role) => role.code !== 'super_admin'))

async function load(): Promise<void> {
  loading.value = true
  try {
    const result = await listUsers({
      keyword: query.keyword.trim() || undefined,
      roleId: query.roleId,
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

async function loadRoleOptions(): Promise<void> {
  roleOptions.value = await listRoleOptions()
}

function search(): void {
  query.page = 1
  void load()
}

function resetFilters(): void {
  query.keyword = ''
  query.roleId = undefined
  query.status = undefined
  query.page = 1
  void load()
}

function handleSizeChange(): void {
  query.page = 1
  void load()
}

function handleSelectionChange(value: UserItem[]): void {
  selection.value = value
}

/** 当前登录账号本人不可禁用/删除，避免运营人员自锁。 */
function isSelf(source: unknown): boolean {
  const row = source as UserItem
  return userStore.isSelf(row.id)
}

/** 超级管理员账号：列表仍可见（需求允许按角色筛选），但详情与维护入口仅超管可见。 */
function isSuperAdminRow(source: unknown): boolean {
  const row = source as UserItem
  return Boolean(row.builtin) || row.roleCode === 'super_admin'
}

function canSelect(row: UserItem): boolean {
  return !isSuperAdminRow(row) && !isSelf(row)
}

function canManage(source: unknown): boolean {
  return userStore.isSuperAdmin || !isSuperAdminRow(source)
}

function canViewDetail(source: unknown): boolean {
  return userStore.isSuperAdmin || !isSuperAdminRow(source)
}

/* ---------- 新增 / 编辑 ---------- */
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const editing = ref<UserItem | null>(null)
const formRef = ref<FormInstance>()
const submitting = ref(false)
const form = reactive({
  username: '',
  mobile: '',
  email: '',
  roleId: undefined as number | undefined,
  password: '',
  status: 1 as number
})

const dialogTitle = computed(() => (dialogMode.value === 'create' ? '新增用户' : '编辑用户'))
const editingBuiltin = computed(() => Boolean(editing.value?.builtin))
const editingSelf = computed(() => Boolean(editing.value && userStore.isSelf(editing.value.id)))
const formRoleOptions = computed(() =>
  editingBuiltin.value ? roleOptions.value : selectableRoles.value
)
/**
 * 角色分级：编辑时仅超级管理员可调整角色；任何角色都不可修改自己的角色；
 * 内置超管角色只读。新增时仍需选择角色（超管角色已被过滤）。
 */
const roleLocked = computed(
  () =>
    dialogMode.value === 'edit' &&
    (editingBuiltin.value || editingSelf.value || !userStore.isSuperAdmin)
)
const roleTip = computed(() => {
  if (editingBuiltin.value) {
    return '内置超级管理员账号的角色不可修改'
  }
  if (editingSelf.value) {
    return '不能修改当前登录账号的角色'
  }
  if (dialogMode.value === 'edit' && !userStore.isSuperAdmin) {
    return '仅超级管理员可调整用户角色'
  }
  return ''
})

/** 内置超级管理员部署期可能未配置手机号（03 §4.5），编辑时允许留空。 */
function validateMobile(_rule: unknown, value: string, callback: (error?: Error) => void): void {
  const mobile = (value ?? '').trim()
  if (!mobile) {
    if (editingBuiltin.value) {
      callback()
      return
    }
    callback(new Error('请输入手机号'))
    return
  }
  if (!/^1[3-9]\d{9}$/.test(mobile)) {
    callback(new Error('手机号格式不正确'))
    return
  }
  callback()
}

function validatePassword(_rule: unknown, value: string, callback: (error?: Error) => void): void {
  if (dialogMode.value === 'create') {
    if (!value) {
      callback(new Error('请输入密码'))
      return
    }
    if (value.length < 6 || value.length > 20) {
      callback(new Error('密码长度需为 6~20 位'))
      return
    }
    callback()
    return
  }
  if (value && (value.length < 6 || value.length > 20)) {
    callback(new Error('密码长度需为 6~20 位'))
    return
  }
  callback()
}

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { max: 50, message: '用户名不能超过 50 个字符', trigger: 'blur' }
  ],
  mobile: [{ validator: validateMobile, trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
  roleId: [{ required: true, message: '请选择角色', trigger: 'change' }],
  password: [{ validator: validatePassword, trigger: 'blur' }]
}

function resetForm(): void {
  form.username = ''
  form.mobile = ''
  form.email = ''
  form.roleId = undefined
  form.password = ''
  form.status = 1
}

function openCreate(): void {
  dialogMode.value = 'create'
  editing.value = null
  resetForm()
  dialogVisible.value = true
  void nextTick(() => formRef.value?.clearValidate())
}

function openEdit(source: unknown): void {
  const row = source as UserItem
  dialogMode.value = 'edit'
  editing.value = row
  form.username = row.username
  form.mobile = row.mobile ?? ''
  form.email = row.email ?? ''
  form.roleId = row.roleId
  form.password = ''
  form.status = row.status
  dialogVisible.value = true
  void nextTick(() => formRef.value?.clearValidate())
}

async function submitForm(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid || !form.roleId) {
    return
  }
  const payload: UserPayload = {
    username: form.username.trim(),
    mobile: form.mobile.trim(),
    email: form.email.trim(),
    roleId: form.roleId,
    status: form.status
  }
  if (form.password) {
    payload.password = form.password
  }

  submitting.value = true
  try {
    if (dialogMode.value === 'create') {
      await createUser(payload)
      ElMessage.success('用户已创建')
    } else if (editing.value) {
      await updateUser(editing.value.id, payload)
      ElMessage.success('用户信息已更新')
    }
    dialogVisible.value = false
    await load()
  } finally {
    submitting.value = false
  }
}

/* ---------- 行操作 ---------- */
async function toggleStatus(source: unknown): Promise<void> {
  const row = source as UserItem
  if (row.builtin) {
    return
  }
  const nextStatus = row.status === 1 ? 0 : 1
  const actionText = nextStatus === 1 ? '启用' : '禁用'
  const confirmed = await ElMessageBox.confirm(
    `确定${actionText}用户「${row.username}」吗？`,
    `${actionText}用户`,
    { type: 'warning', confirmButtonText: actionText, cancelButtonText: '取消' }
  ).catch(() => false)
  if (!confirmed) {
    return
  }
  await batchUpdateUserStatus([row.id], nextStatus)
  ElMessage.success(`已${actionText}用户「${row.username}」`)
  await load()
}

async function removeUser(source: unknown): Promise<void> {
  const row = source as UserItem
  const confirmed = await ElMessageBox.confirm(
    `确定删除用户「${row.username}」吗？删除后该账号将无法登录。`,
    '删除用户',
    { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
  ).catch(() => false)
  if (!confirmed) {
    return
  }
  await deleteUser(row.id)
  ElMessage.success('用户已删除')
  await load()
}

async function resetPassword(source: unknown): Promise<void> {
  const row = source as UserItem
  const confirmed = await ElMessageBox.confirm(
    `确定将用户「${row.username}」的密码重置为系统默认密码吗？`,
    '重置密码',
    { type: 'warning', confirmButtonText: '重置', cancelButtonText: '取消' }
  ).catch(() => false)
  if (!confirmed) {
    return
  }
  const result = await resetUserPassword(row.id)
  await ElMessageBox.alert(`新密码：${result.password}`, '密码已重置', {
    type: 'success',
    confirmButtonText: '我已记录'
  }).catch(() => undefined)
}

async function batchStatus(status: number): Promise<void> {
  if (!selection.value.length) {
    return
  }
  const actionText = status === 1 ? '启用' : '禁用'
  const confirmed = await ElMessageBox.confirm(
    `确定${actionText}选中的 ${selection.value.length} 个用户吗？`,
    `批量${actionText}`,
    { type: 'warning', confirmButtonText: actionText, cancelButtonText: '取消' }
  ).catch(() => false)
  if (!confirmed) {
    return
  }
  await batchUpdateUserStatus(selection.value.map((item) => item.id), status)
  ElMessage.success(`已${actionText} ${selection.value.length} 个用户`)
  tableRef.value?.clearSelection()
  await load()
}

async function batchRemove(): Promise<void> {
  if (!selection.value.length) {
    return
  }
  const count = selection.value.length
  const confirmed = await ElMessageBox.confirm(
    `确定删除选中的 ${count} 个用户吗？删除后这些账号将无法登录。`,
    '批量删除',
    { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
  ).catch(() => false)
  if (!confirmed) {
    return
  }
  await batchDeleteUsers(selection.value.map((item) => item.id))
  ElMessage.success(`已删除 ${count} 个用户`)
  tableRef.value?.clearSelection()
  await load()
}

/* ---------- 详情 ---------- */
const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<UserDetail | null>(null)

async function openDetail(source: unknown): Promise<void> {
  const row = source as UserItem
  if (!canViewDetail(row)) {
    ElMessage.warning('超级管理员账号仅超级管理员可查看')
    return
  }
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    detail.value = await getUserDetail(row.id)
  } finally {
    detailLoading.value = false
  }
}

onMounted(() => {
  void load()
  void loadRoleOptions()
})
</script>

<template>
  <AdminPage title="用户管理" description="管理后台内部账号：新增、编辑、启停、重置密码与查看最近操作。">
    <template #actions>
      <el-button v-permission="'user:add'" type="primary" :icon="Plus" @click="openCreate">
        新增用户
      </el-button>
    </template>

    <div class="panel filter-panel">
      <el-form class="filter-form" @submit.prevent="search">
        <el-form-item>
          <el-input
            v-model="query.keyword"
            class="filter-keyword"
            placeholder="搜索用户名 / 手机号"
            clearable
            :prefix-icon="Search"
            @keyup.enter="search"
            @clear="search"
          />
        </el-form-item>
        <el-form-item>
          <el-select v-model="query.roleId" placeholder="全部角色" clearable @change="search">
            <el-option v-for="role in roleOptions" :key="role.id" :label="role.name" :value="role.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-select v-model="query.status" placeholder="全部状态" clearable @change="search">
            <el-option label="已启用" :value="1" />
            <el-option label="已禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button :icon="Refresh" @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="panel table-panel">
      <div v-if="selection.length" class="batch-bar">
        <span>已选择 <span class="batch-count">{{ selection.length }}</span> 个用户</span>
        <el-button v-permission="'user:edit'" size="small" @click="batchStatus(1)">批量启用</el-button>
        <el-button v-permission="'user:edit'" size="small" @click="batchStatus(0)">批量禁用</el-button>
        <el-button v-permission="'user:disable'" size="small" type="danger" plain @click="batchRemove">
          批量删除
        </el-button>
      </div>

      <el-table
        ref="tableRef"
        v-loading="loading"
        :data="rows"
        row-key="id"
        empty-text="没有找到匹配的用户"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="46" :selectable="canSelect" />
        <el-table-column label="用户" min-width="190">
          <template #default="{ row }">
            <div class="user-cell">
              <span class="user-initial">{{ row.username.slice(0, 1).toUpperCase() }}</span>
              <div>
                <div class="cell-strong">{{ row.username }}</div>
                <div class="cell-sub">{{ row.email || '未设置邮箱' }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="手机号" min-width="130">
          <template #default="{ row }">{{ row.mobile || '—' }}</template>
        </el-table-column>
        <el-table-column label="角色" min-width="120">
          <template #default="{ row }">
            <el-tag effect="plain" type="info">{{ row.roleName || '未分配' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="light">
              {{ row.status === 1 ? '已启用' : '已禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="最后登录" min-width="160">
          <template #default="{ row }">
            <span class="cell-sub">{{ row.lastLoginAt || '从未登录' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <div class="row-actions">
              <el-button
                v-if="canViewDetail(row)"
                text
                type="primary"
                size="small"
                @click="openDetail(row)"
              >
                详情
              </el-button>
              <el-button
                v-if="canManage(row)"
                v-permission="'user:edit'"
                text
                type="primary"
                size="small"
                @click="openEdit(row)"
              >
                编辑
              </el-button>
              <el-tooltip
                v-if="!row.builtin"
                :disabled="!isSelf(row)"
                content="不能禁用当前登录账号"
                placement="top"
              >
                <span>
                  <el-button
                    v-permission="'user:edit'"
                    text
                    :type="row.status === 1 ? 'warning' : 'success'"
                    size="small"
                    :disabled="isSelf(row)"
                    @click="toggleStatus(row)"
                  >
                    {{ row.status === 1 ? '禁用' : '启用' }}
                  </el-button>
                </span>
              </el-tooltip>
              <el-button
                v-if="canManage(row)"
                v-permission="'user:edit'"
                text
                type="primary"
                size="small"
                @click="resetPassword(row)"
              >
                重置密码
              </el-button>
              <el-tooltip
                v-if="!row.builtin"
                :disabled="!isSelf(row)"
                content="不能删除当前登录账号"
                placement="top"
              >
                <span>
                  <el-button
                    v-permission="'user:disable'"
                    text
                    type="danger"
                    size="small"
                    :disabled="isSelf(row)"
                    @click="removeUser(row)"
                  >
                    删除
                  </el-button>
                </span>
              </el-tooltip>
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

    <!-- 新增 / 编辑 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="520px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="88px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model.trim="form.username" maxlength="50" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="手机号" prop="mobile">
          <el-input v-model.trim="form.mobile" maxlength="11" placeholder="请输入手机号" />
          <div v-if="editingBuiltin" class="cell-sub form-tip">内置超级管理员账号手机号可留空</div>
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model.trim="form.email" maxlength="100" placeholder="选填" />
        </el-form-item>
        <el-form-item label="角色" prop="roleId">
          <el-select
            v-model="form.roleId"
            placeholder="请选择角色"
            :disabled="roleLocked"
            style="width: 100%"
          >
            <el-option v-for="role in formRoleOptions" :key="role.id" :label="role.name" :value="role.id" />
          </el-select>
          <div v-if="roleTip" class="cell-sub form-tip">{{ roleTip }}</div>
        </el-form-item>
        <el-form-item :label="dialogMode === 'create' ? '密码' : '新密码'" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            maxlength="20"
            :placeholder="dialogMode === 'create' ? '6~20 位密码' : '留空则不修改密码'"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch
            v-model="form.status"
            :active-value="1"
            :inactive-value="0"
            active-text="已启用"
            inactive-text="已禁用"
            :disabled="editingBuiltin || editingSelf"
          />
          <div v-if="editingSelf && !editingBuiltin" class="cell-sub form-tip">
            不能禁用当前登录账号
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <!-- 用户详情 -->
    <el-drawer v-model="detailVisible" title="用户详情" size="440px">
      <div v-loading="detailLoading" class="detail-body">
        <template v-if="detail">
          <div class="detail-profile">
            <span class="detail-avatar">{{ detail.username.slice(0, 1).toUpperCase() }}</span>
            <div>
              <h3>{{ detail.username }}</h3>
              <p class="cell-sub">
                {{ detail.roleName || '未分配角色' }} · {{ detail.status === 1 ? '已启用' : '已禁用' }}
                <el-tag v-if="detail.builtin" size="small" type="warning" effect="plain" class="builtin-tag">
                  内置
                </el-tag>
              </p>
            </div>
          </div>

          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="手机号">{{ detail.mobile || '—' }}</el-descriptions-item>
            <el-descriptions-item label="邮箱">{{ detail.email || '—' }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ detail.createdAt }}</el-descriptions-item>
            <el-descriptions-item label="最后登录">{{ detail.lastLoginAt || '从未登录' }}</el-descriptions-item>
          </el-descriptions>

          <h4 class="detail-section-title">最近操作</h4>
          <div v-if="detail.recentOperations.length" class="recent-list">
            <div v-for="item in detail.recentOperations" :key="item.id" class="recent-item">
              <div class="recent-main">
                <el-tag size="small" effect="plain">{{ item.action }}</el-tag>
                <span>{{ item.targetType }} · {{ item.targetName }}</span>
              </div>
              <div class="cell-sub">{{ item.ip }} · {{ item.createdAt }}</div>
            </div>
          </div>
          <el-empty v-else description="暂无操作记录" :image-size="72" />
        </template>
      </div>
    </el-drawer>
  </AdminPage>
</template>

<style scoped>
.form-tip {
  margin-top: 4px;
  line-height: 1.4;
}

.detail-body {
  min-height: 200px;
}

.detail-profile {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 20px;
}

.detail-profile h3 {
  font-family: var(--font-display);
  font-size: 20px;
}

.detail-avatar {
  display: inline-grid;
  place-items: center;
  width: 52px;
  height: 52px;
  border-radius: 50%;
  background: var(--jade-soft);
  color: var(--jade-2);
  font-family: var(--font-display);
  font-size: 22px;
  font-weight: 600;
}

.builtin-tag {
  margin-left: 6px;
}

.detail-section-title {
  margin: 24px 0 12px;
  font-size: 14px;
}

.recent-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.recent-item {
  padding: 10px 12px;
  border: 1px solid var(--line);
  border-radius: var(--r-xs);
  background: var(--surface-2);
}

.recent-main {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 3px;
  font-size: 13px;
}
</style>
