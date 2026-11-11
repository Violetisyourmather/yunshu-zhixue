<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { ApiError } from '@/api/http'
import { usePermissionStore } from '@/store/permission'
import { useSiteStore } from '@/store/site'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const permissionStore = usePermissionStore()
const siteStore = useSiteStore()

const formRef = ref<FormInstance>()
const submitting = ref(false)
const errorMessage = ref('')
const form = reactive({ account: '', password: '' })

/** 学员端根地址：开发端口不同，生产与后台同域（见 .env.*） */
const portalBase = (import.meta.env.VITE_PORTAL_BASE_URL ?? '').replace(/\/+$/, '')
const portalLoginUrl = `${portalBase}/login`

const rules: FormRules = {
  account: [{ required: true, message: '请输入用户名或手机号', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为 6~20 位', trigger: 'blur' }
  ]
}

/** 无指定回跳地址（或回跳页无权限）时，落到该账号可见的第一个菜单。 */
function defaultTarget(): string {
  return permissionStore.visibleGroups[0]?.entries[0]?.path ?? '/dashboard'
}

function resolveTarget(): string {
  const redirect = route.query.redirect
  if (typeof redirect !== 'string' || !redirect.startsWith('/')) {
    return defaultTarget()
  }
  const { meta } = router.resolve(redirect)
  if (meta.permission && !userStore.hasPermission(meta.permission)) {
    return defaultTarget()
  }
  if (meta.superAdminOnly && !userStore.isSuperAdmin) {
    return defaultTarget()
  }
  return redirect
}

async function handleSubmit(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }

  submitting.value = true
  errorMessage.value = ''
  try {
    await userStore.login({ account: form.account.trim(), password: form.password })
    ElMessage.success('登录成功')
    await router.replace(resolveTarget())
  } catch (error) {
    errorMessage.value = error instanceof ApiError ? error.message : '登录失败，请稍后重试'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="login-shell">
    <div class="login-card">
      <div class="login-brand">
        <img v-if="siteStore.logoUrl" class="brand-logo" :src="siteStore.logoUrl" alt="平台 Logo" />
        <span v-else class="brand-mark">云</span>
        <span>{{ siteStore.siteName }}</span>
      </div>
      <h1>管理后台</h1>
      <p class="login-subtitle">请使用后台管理员账号登录</p>

      <p v-if="errorMessage" class="login-error">{{ errorMessage }}</p>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="handleSubmit">
        <el-form-item label="用户名 / 手机号" prop="account">
          <el-input v-model.trim="form.account" size="large" autocomplete="username" placeholder="请输入用户名或手机号" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            size="large"
            type="password"
            show-password
            autocomplete="current-password"
            placeholder="6~20 位密码"
            @keyup.enter="handleSubmit()"
          />
        </el-form-item>
        <el-button type="primary" size="large" class="login-submit" :loading="submitting" @click="handleSubmit()">
          登录
        </el-button>
      </el-form>

      <a class="login-switch" :href="portalLoginUrl">前往学员端登录</a>

      <p class="login-footer">学员端与管理端账号体系独立，请勿使用学员账号登录</p>
    </div>
  </div>
</template>

<style scoped>
.login-submit {
  width: 100%;
  margin-top: 6px;
}

.login-switch {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 40px;
  margin-top: 14px;
  border: 1px solid var(--line);
  border-radius: 10px;
  color: var(--jade);
  font-size: 14px;
  font-weight: 600;
  transition: border-color 0.16s, background 0.16s;
}

.login-switch:hover {
  border-color: var(--jade);
  background: var(--jade-soft);
}
</style>
