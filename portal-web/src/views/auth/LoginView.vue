<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AuthShell from '@/components/AuthShell.vue'
import { ApiError } from '@/api/http'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const submitting = ref(false)
const errorMessage = ref('')
const form = reactive({ account: '', password: '' })

function validate(): string | null {
  if (!form.account.trim()) {
    return '请输入手机号或邮箱'
  }
  if (form.password.length < 6 || form.password.length > 20) {
    return '密码长度为 6~20 位'
  }
  return null
}

/** 仅接受站内回跳地址；登录/注册/找回密码页不作为落地页 */
function resolveTarget(): string {
  const redirect = route.query.redirect
  if (typeof redirect !== 'string' || !redirect.startsWith('/') || redirect.startsWith('//')) {
    return '/'
  }
  if (router.resolve(redirect).meta.publicOnly) {
    return '/'
  }
  return redirect
}

async function handleSubmit(): Promise<void> {
  const problem = validate()
  if (problem) {
    errorMessage.value = problem
    return
  }

  submitting.value = true
  errorMessage.value = ''
  try {
    await userStore.loginByPassword({
      account: form.account.trim(),
      password: form.password
    })
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
  <AuthShell>
    <h2>登录</h2>
    <p class="muted small">学员端与管理端账号体系独立，请使用学员账号登录。</p>

    <form novalidate @submit.prevent="handleSubmit">
      <p v-if="errorMessage" class="form-error">{{ errorMessage }}</p>

      <div class="field">
        <label for="login-account">手机号 / 邮箱</label>
        <input
          id="login-account"
          v-model.trim="form.account"
          class="input"
          autocomplete="username"
          placeholder="请输入手机号或邮箱"
        />
      </div>
      <div class="field">
        <label for="login-password">密码</label>
        <input
          id="login-password"
          v-model="form.password"
          class="input"
          type="password"
          autocomplete="current-password"
          placeholder="6~20 位密码"
        />
      </div>

      <button type="submit" class="btn btn-primary btn-lg btn-block" :disabled="submitting">
        {{ submitting ? '登录中…' : '登录' }}
      </button>
    </form>

    <div class="auth-links">
      <RouterLink to="/register">注册账号</RouterLink>
      <RouterLink to="/forgot">忘记密码</RouterLink>
    </div>

    <p class="auth-hint">登录后可使用刷题、错题本、学习路线与 AI 面试。</p>
  </AuthShell>
</template>
