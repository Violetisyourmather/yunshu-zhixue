<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AuthShell from '@/components/AuthShell.vue'
import { sendSmsCode } from '@/api/auth'
import { ApiError } from '@/api/http'
import { useCountdown } from '@/composables/useCountdown'
import { useUserStore } from '@/store/user'

type LoginMode = 'password' | 'sms'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const { remaining: smsRemaining, running: smsRunning, start: startSmsCountdown } = useCountdown()

const mode = ref<LoginMode>('password')
const submitting = ref(false)
const sendingSms = ref(false)
const remember = ref(true)
const errorMessage = ref('')
const smsHint = ref('')
const form = reactive({
  account: '',
  password: '',
  mobile: '',
  code: ''
})

function switchMode(next: LoginMode): void {
  mode.value = next
  errorMessage.value = ''
  smsHint.value = ''
}

function validate(): string | null {
  if (mode.value === 'password') {
    if (!form.account.trim()) {
      return '请输入手机号或邮箱'
    }
    if (form.password.length < 6 || form.password.length > 20) {
      return '密码长度为 6~20 位'
    }
    return null
  }
  if (!/^1[3-9]\d{9}$/.test(form.mobile.trim())) {
    return '请输入正确的 11 位手机号'
  }
  if (!/^\d{6}$/.test(form.code.trim())) {
    return '请输入 6 位验证码'
  }
  return null
}

function messageOf(error: unknown, fallback: string): string {
  return error instanceof ApiError ? error.message : fallback
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

async function handleSendCode(): Promise<void> {
  const mobile = form.mobile.trim()
  if (!/^1[3-9]\d{9}$/.test(mobile)) {
    errorMessage.value = '请输入正确的 11 位手机号'
    return
  }
  sendingSms.value = true
  errorMessage.value = ''
  try {
    const result = await sendSmsCode({ mobile, type: 'login' })
    startSmsCountdown(60)
    smsHint.value = result.devCode
      ? `验证码已发送，开发环境验证码：${result.devCode}`
      : '验证码已发送'
    ElMessage.success('验证码已发送')
  } catch (error) {
    errorMessage.value = messageOf(error, '验证码发送失败，请稍后重试')
  } finally {
    sendingSms.value = false
  }
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
    if (mode.value === 'password') {
      await userStore.loginByPassword(
        { account: form.account.trim(), password: form.password },
        remember.value
      )
    } else {
      await userStore.loginBySms(
        { mobile: form.mobile.trim(), code: form.code.trim() },
        remember.value
      )
    }
    ElMessage.success('登录成功')
    await router.replace(resolveTarget())
  } catch (error) {
    errorMessage.value = messageOf(error, '登录失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <AuthShell>
    <h2>欢迎回来</h2>
    <p class="muted small">登录后继续你的学习路线与面试记录。</p>

    <div class="auth-tabs" role="tablist" aria-label="登录方式">
      <button
        type="button"
        class="auth-tab"
        :class="{ active: mode === 'password' }"
        role="tab"
        :aria-selected="mode === 'password'"
        @click="switchMode('password')"
      >
        账号密码登录
      </button>
      <button
        type="button"
        class="auth-tab"
        :class="{ active: mode === 'sms' }"
        role="tab"
        :aria-selected="mode === 'sms'"
        @click="switchMode('sms')"
      >
        手机验证码登录
      </button>
    </div>

    <form novalidate @submit.prevent="handleSubmit">
      <p v-if="errorMessage" class="form-error">{{ errorMessage }}</p>

      <template v-if="mode === 'password'">
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
      </template>

      <template v-else>
        <div class="field">
          <label for="login-mobile">手机号</label>
          <input
            id="login-mobile"
            v-model.trim="form.mobile"
            class="input"
            inputmode="numeric"
            maxlength="11"
            autocomplete="tel"
            placeholder="请输入 11 位手机号"
          />
        </div>
        <div class="field">
          <label for="login-code">验证码</label>
          <div class="auth-code-row">
            <input
              id="login-code"
              v-model.trim="form.code"
              class="input"
              inputmode="numeric"
              maxlength="6"
              autocomplete="one-time-code"
              placeholder="6 位验证码"
            />
            <button
              type="button"
              class="btn auth-code-button"
              :disabled="sendingSms || smsRunning"
              @click="handleSendCode"
            >
              {{ sendingSms ? '发送中…' : smsRunning ? `${smsRemaining}s 后重发` : '获取验证码' }}
            </button>
          </div>
          <p v-if="smsHint" class="password-hint" style="margin-top: 6px">{{ smsHint }}</p>
        </div>
      </template>

      <div class="auth-between">
        <label class="auth-check">
          <input v-model="remember" type="checkbox" />
          记住我
        </label>
        <RouterLink class="auth-inline-link" to="/forgot">忘记密码？</RouterLink>
      </div>

      <button type="submit" class="btn btn-primary btn-lg btn-block" :disabled="submitting">
        {{ submitting ? '登录中…' : '登录' }}
      </button>
    </form>

    <p class="auth-switch">还没有账号？<RouterLink to="/register">立即注册</RouterLink></p>
    <p class="auth-hint">登录后可使用刷题、错题本、学习路线与 AI 面试。</p>
  </AuthShell>
</template>

<style scoped>
.auth-inline-link {
  color: var(--jade);
  font-size: 12.5px;
  font-weight: 500;
}
</style>
