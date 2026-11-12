<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AuthShell from '@/components/AuthShell.vue'
import { sendSmsCode } from '@/api/auth'
import { ApiError } from '@/api/http'
import { useCountdown } from '@/composables/useCountdown'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()
const { remaining: smsRemaining, running: smsRunning, start: startSmsCountdown } = useCountdown()

const submitting = ref(false)
const sendingSms = ref(false)
const errorMessage = ref('')
const smsHint = ref('')
const form = reactive({
  mobile: '',
  code: '',
  password: '',
  confirmPassword: '',
  agree: false
})

const strength = computed(() => {
  const value = form.password
  let score = 0
  if (value.length >= 6) {
    score += 1
  }
  if (/[A-Z]/.test(value) && /[a-z]/.test(value)) {
    score += 1
  }
  if (/\d/.test(value) && /[^A-Za-z0-9]/.test(value)) {
    score += 1
  }
  return score
})

const strengthLabel = computed(() => {
  if (!form.password) {
    return '待输入'
  }
  return ['弱', '弱', '中', '强'][strength.value]
})

function messageOf(error: unknown, fallback: string): string {
  return error instanceof ApiError ? error.message : fallback
}

function validate(): string | null {
  if (!/^1[3-9]\d{9}$/.test(form.mobile.trim())) {
    return '请输入正确的 11 位手机号'
  }
  if (!/^\d{6}$/.test(form.code.trim())) {
    return '请输入 6 位验证码'
  }
  if (form.password.length < 6 || form.password.length > 20) {
    return '密码长度为 6~20 位'
  }
  if (form.password !== form.confirmPassword) {
    return '两次输入的密码不一致'
  }
  if (!form.agree) {
    return '请先阅读并同意《用户协议》和《隐私政策》'
  }
  return null
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
    const result = await sendSmsCode({ mobile, type: 'register' })
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
    await userStore.registerAndLogin({
      mobile: form.mobile.trim(),
      code: form.code.trim(),
      password: form.password
    })
    ElMessage.success('注册成功，请完善个人资料')
    await router.replace('/profile')
  } catch (error) {
    errorMessage.value = messageOf(error, '注册失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <AuthShell>
    <h2>创建账号</h2>
    <p class="muted small">注册后会自动进入个人中心，补齐资料再开始训练。</p>

    <ol class="auth-steps" aria-label="注册流程">
      <li class="auth-step active"><span class="n">1</span>创建账号</li>
      <li class="auth-step"><span class="n">2</span>完善资料</li>
      <li class="auth-step"><span class="n">3</span>开始训练</li>
    </ol>

    <form novalidate @submit.prevent="handleSubmit">
      <p v-if="errorMessage" class="form-error">{{ errorMessage }}</p>

      <div class="field">
        <label for="register-mobile">手机号</label>
        <input
          id="register-mobile"
          v-model.trim="form.mobile"
          class="input"
          inputmode="numeric"
          maxlength="11"
          autocomplete="tel"
          placeholder="请输入 11 位手机号"
        />
      </div>

      <div class="field">
        <label for="register-code">验证码</label>
        <div class="auth-code-row">
          <input
            id="register-code"
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

      <div class="field">
        <label for="register-password">设置密码</label>
        <input
          id="register-password"
          v-model="form.password"
          class="input"
          type="password"
          autocomplete="new-password"
          placeholder="6~20 位密码"
        />
        <div class="password-meter" :class="`strength-${strength}`" aria-hidden="true">
          <i /><i /><i />
        </div>
        <p class="password-hint">强度：{{ strengthLabel }}，建议包含大小写字母、数字与特殊字符</p>
      </div>

      <div class="field">
        <label for="register-confirm">确认密码</label>
        <input
          id="register-confirm"
          v-model="form.confirmPassword"
          class="input"
          type="password"
          autocomplete="new-password"
          placeholder="再次输入密码"
        />
      </div>

      <label class="auth-check" style="margin-bottom: 18px">
        <input v-model="form.agree" type="checkbox" />
        我已阅读并同意《用户协议》和《隐私政策》
      </label>

      <button type="submit" class="btn btn-primary btn-lg btn-block" :disabled="submitting">
        {{ submitting ? '创建中…' : '创建账号' }}
      </button>
    </form>

    <p class="auth-switch">已有账号？<RouterLink to="/login">去登录</RouterLink></p>
  </AuthShell>
</template>
