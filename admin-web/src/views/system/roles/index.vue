<script setup lang="ts">
import { nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import AdminPage from '@/components/AdminPage.vue'
import {
  createRole,
  deleteRole,
  getPermissionTree,
  listRoles,
  saveRolePermissions
} from '@/api/roles'
import type { PermissionNode, RoleCard } from '@/types/system'

const loading = ref(false)
const roles = ref<RoleCard[]>([])

async function load(): Promise<void> {
  loading.value = true
  try {
    roles.value = await listRoles()
  } finally {
    loading.value = false
  }
}

function tagPreview(role: RoleCard): string[] {
  return role.permissionNames.slice(0, 4)
}

function deleteDisabled(role: RoleCard): boolean {
  return role.builtin || role.userCount > 0
}

function deleteReason(role: RoleCard): string {
  if (role.builtin) {
    return '超级管理员角色不可删除'
  }
  if (role.userCount > 0) {
    return '该角色下存在用户，不可删除'
  }
  return ''
}

/* ---------- 新增角色 ---------- */
const createVisible = ref(false)
const createFormRef = ref<FormInstance>()
const creating = ref(false)
const createForm = reactive({ name: '', code: '', description: '' })

const createRules: FormRules = {
  name: [
    { required: true, message: '请输入角色名称', trigger: 'blur' },
    { max: 50, message: '角色名称不能超过 50 个字符', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入角色编码', trigger: 'blur' },
    {
      pattern: /^[a-zA-Z][a-zA-Z0-9_]{1,49}$/,
      message: '以字母开头，仅含字母、数字与下划线，长度 2~50',
      trigger: 'blur'
    }
  ],
  description: [{ max: 255, message: '描述不能超过 255 个字符', trigger: 'blur' }]
}

function openCreate(): void {
  createForm.name = ''
  createForm.code = ''
  createForm.description = ''
  createVisible.value = true
  void nextTick(() => createFormRef.value?.clearValidate())
}

async function submitCreate(): Promise<void> {
  const valid = await createFormRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  creating.value = true
  try {
    await createRole({
      name: createForm.name.trim(),
      code: createForm.code.trim(),
      description: createForm.description.trim()
    })
    ElMessage.success('角色已创建')
    createVisible.value = false
    await load()
  } finally {
    creating.value = false
  }
}

/* ---------- 删除角色 ---------- */
async function removeRole(role: RoleCard): Promise<void> {
  if (deleteDisabled(role)) {
    return
  }
  const confirmed = await ElMessageBox.confirm(
    `确定删除角色「${role.name}」吗？该角色的权限配置将一并清除。`,
    '删除角色',
    { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
  ).catch(() => false)
  if (!confirmed) {
    return
  }
  await deleteRole(role.id)
  ElMessage.success('角色已删除')
  await load()
}

/* ---------- 权限树 ---------- */
const permissionVisible = ref(false)
const permissionLoading = ref(false)
const permissionSaving = ref(false)
const permissionRole = ref<RoleCard | null>(null)
const permissionTree = ref<PermissionNode[]>([])
const treeRef = ref<{
  setCheckedKeys: (keys: number[]) => void
  getCheckedKeys: (leafOnly?: boolean) => number[]
}>()

function collectCheckedLeaves(nodes: PermissionNode[], result: number[] = []): number[] {
  for (const node of nodes) {
    if (node.type === 2 && node.checked) {
      result.push(node.id)
    }
    if (node.children?.length) {
      collectCheckedLeaves(node.children, result)
    }
  }
  return result
}

async function openPermissions(role: RoleCard): Promise<void> {
  if (role.builtin) {
    ElMessage.info('超级管理员默认拥有全部权限，不可编辑')
    return
  }
  permissionRole.value = role
  permissionTree.value = []
  permissionVisible.value = true
  permissionLoading.value = true
  try {
    permissionTree.value = await getPermissionTree(role.id)
    const checked = collectCheckedLeaves(permissionTree.value)
    await nextTick()
    treeRef.value?.setCheckedKeys(checked)
  } finally {
    permissionLoading.value = false
  }
}

async function submitPermissions(): Promise<void> {
  if (!permissionRole.value) {
    return
  }
  const checkedKeys = treeRef.value?.getCheckedKeys(false) ?? []
  permissionSaving.value = true
  try {
    await saveRolePermissions(permissionRole.value.id, checkedKeys)
    ElMessage.success('权限已保存')
    permissionVisible.value = false
    await load()
  } finally {
    permissionSaving.value = false
  }
}

onMounted(load)
</script>

<template>
  <AdminPage title="角色权限" description="角色卡片、权限树与全量覆盖保存。角色管理仅超级管理员可访问。">
    <template #actions>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增角色</el-button>
    </template>

    <div v-loading="loading" class="role-grid-wrap">
      <div v-if="roles.length" class="role-grid">
        <article
          v-for="role in roles"
          :key="role.id"
          class="role-card"
          :class="{ builtin: role.builtin }"
          @click="openPermissions(role)"
        >
          <header class="role-head">
            <div class="role-title">
              <h3>{{ role.name }}</h3>
              <code class="role-code">{{ role.code }}</code>
            </div>
            <el-tag v-if="role.builtin" type="warning" effect="plain" size="small">内置</el-tag>
          </header>

          <p class="role-desc">{{ role.description || '暂无描述' }}</p>

          <div class="role-meta">
            <span>{{ role.userCount }} 位用户</span>
            <span class="dot">·</span>
            <span>{{ role.permissionCount }} 项权限</span>
          </div>

          <div class="role-tags">
            <template v-if="role.builtin">
              <el-tag size="small" type="success" effect="plain">全部权限</el-tag>
            </template>
            <template v-else-if="role.permissionNames.length">
              <el-tag
                v-for="label in tagPreview(role)"
                :key="label"
                size="small"
                type="info"
                effect="plain"
              >
                {{ label }}
              </el-tag>
              <el-tag v-if="role.permissionNames.length > 4" size="small" type="info" effect="plain">
                +{{ role.permissionNames.length - 4 }}
              </el-tag>
            </template>
            <el-tag v-else size="small" type="info" effect="plain">暂无权限</el-tag>
          </div>

          <footer class="role-actions" @click.stop>
            <el-button
              size="small"
              text
              type="primary"
              :disabled="role.builtin"
              @click="openPermissions(role)"
            >
              编辑权限
            </el-button>
            <el-tooltip
              :disabled="!deleteDisabled(role)"
              :content="deleteReason(role)"
              placement="top"
            >
              <span>
                <el-button
                  size="small"
                  text
                  type="danger"
                  :disabled="deleteDisabled(role)"
                  @click="removeRole(role)"
                >
                  删除
                </el-button>
              </span>
            </el-tooltip>
          </footer>
        </article>
      </div>

      <el-empty v-else-if="!loading" description="暂无角色数据，点击右上角新增角色" />
    </div>

    <!-- 新增角色 -->
    <el-dialog v-model="createVisible" title="新增角色" width="480px" :close-on-click-modal="false">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="88px">
        <el-form-item label="角色名称" prop="name">
          <el-input v-model.trim="createForm.name" maxlength="50" placeholder="如：内容审核" />
        </el-form-item>
        <el-form-item label="角色编码" prop="code">
          <el-input v-model.trim="createForm.code" maxlength="50" placeholder="如：content_reviewer" />
        </el-form-item>
        <el-form-item label="角色描述" prop="description">
          <el-input
            v-model.trim="createForm.description"
            type="textarea"
            :rows="3"
            maxlength="255"
            show-word-limit
            placeholder="选填"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="submitCreate">保存</el-button>
      </template>
    </el-dialog>

    <!-- 编辑权限 -->
    <el-dialog
      v-model="permissionVisible"
      :title="permissionRole ? `编辑权限 - ${permissionRole.name}` : '编辑权限'"
      width="620px"
      :close-on-click-modal="false"
    >
      <div v-loading="permissionLoading" class="permission-body">
        <p class="permission-tip">勾选该角色可使用的操作权限，保存后立即生效；再次登录后令牌权限同步更新。</p>
        <el-tree
          ref="treeRef"
          :data="permissionTree"
          node-key="id"
          show-checkbox
          default-expand-all
          :props="{ label: 'name', children: 'children' }"
        >
          <template #default="{ data }">
            <span class="tree-node">
              <span>{{ data.name }}</span>
              <code v-if="data.type === 1" class="tree-code">{{ data.code }}</code>
            </span>
          </template>
        </el-tree>
      </div>
      <template #footer>
        <el-button @click="permissionVisible = false">取消</el-button>
        <el-button type="primary" :loading="permissionSaving" @click="submitPermissions">保存</el-button>
      </template>
    </el-dialog>
  </AdminPage>
</template>

<style scoped>
.role-grid-wrap {
  min-height: 240px;
}

.role-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}

.role-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 20px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r);
  box-shadow: var(--shadow);
  cursor: pointer;
  transition: transform 0.16s ease, border-color 0.16s ease, box-shadow 0.16s ease;
}

.role-card:hover {
  transform: translateY(-2px);
  border-color: var(--jade);
  box-shadow: var(--shadow-lg);
}

.role-card.builtin {
  background: linear-gradient(180deg, var(--amber-soft), var(--surface) 46%);
  cursor: default;
}

.role-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.role-title h3 {
  font-family: var(--font-display);
  font-size: 19px;
  line-height: 1.3;
}

.role-code {
  display: inline-block;
  margin-top: 4px;
  padding: 1px 7px;
  border-radius: 999px;
  background: var(--paper-2);
  color: var(--ink-3);
  font-family: var(--font-mono);
  font-size: 11px;
}

.role-desc {
  min-height: 40px;
  color: var(--ink-3);
  font-size: 13px;
  line-height: 1.55;
}

.role-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--ink-2);
  font-size: 12.5px;
}

.role-meta .dot {
  color: var(--ink-4);
}

.role-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  min-height: 24px;
}

.role-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 4px;
  padding-top: 12px;
  margin-top: auto;
  border-top: 1px dashed var(--line);
}

.permission-body {
  min-height: 260px;
}

/* 权限树较高时在弹窗内滚动，保证底部“保存”按钮始终可见 */
.permission-body :deep(.el-tree) {
  max-height: 52vh;
  overflow-y: auto;
  padding-right: 6px;
}

.permission-tip {
  margin-bottom: 14px;
  padding: 9px 12px;
  border-radius: var(--r-xs);
  background: var(--jade-soft);
  color: var(--jade-2);
  font-size: 12.5px;
}

.tree-node {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.tree-code {
  color: var(--ink-4);
  font-family: var(--font-mono);
  font-size: 11px;
}

:deep(.el-tree-node__content) {
  height: 34px;
  border-radius: var(--r-xs);
}

:deep(.el-tree-node:focus > .el-tree-node__content) {
  background: var(--jade-soft);
}
</style>
