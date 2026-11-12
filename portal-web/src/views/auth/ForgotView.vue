<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import AuthShell from '@/components/AuthShell.vue'
import { resetPassword, sendSmsCode, verifyResetCode } from '@/api/auth'
import { ApiError } from '@/api/http'
import { useCountdown } from '@/composables/useCountdown'

const { remaining: smsRemaining, running: smsRunning, start: startSmsCountdown } = useCountdown()

const step = ref<1 | 2 | 3>(1)
const sendingSms = ref(false)
const submitting = ref(false)
const errorMessage = ref('')
const smsHint = ref('')
const resetToken = ref('')
const form = reactive({
  mobile: '',
  code: '',
  password: '',
  confirmPassword: ''
})

function messageOf(error: unknown, fallback: string): string {
  return error instanceof ApiError ? error.message : fallback
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
    const result = await sendSmsCode({ mobile, type: 'reset' })
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

async function handleVerify(): Promise<void> {
  if (!/^1[3-9]\d{9}$/.test(form.mobile.trim())) {
    errorMessage.value = '请输入正确的 11 位手机号'
    return
  }
  if (!/^\d{6}$/.test(form.code.trim())) {
    errorMessage.value = '请输入 6 位验证码'
    return
  }
  submitting.value = true
  errorMessage.value = ''
  try {
    const result = await verifyResetCode({ mobile: form.mobile.trim(), code: form.code.trim() })
    resetToken.value = result.resetToken
    step.value = 2
  } catch (error) {
    errorMessage.value = messageOf(error, '验证失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

async function handleReset(): Promise<void> {
  if (form.password.length < 6 || form.password.length > 20) {
    errorMessage.value = '密码长度为 6~20 位'
    return
  }
  if (form.password !== form.confirmPassword) {
    errorMessage.value = '两次输入的密码不一致'
    return
  }
  submitting.value = true
  errorMessage.value = ''
  try {
    await resetPassword({ resetToken: resetToken.value, newPassword: form.password })
    step.value = 3
    ElMessage.success('密码已重置')
  } catch (error) {
    errorMessage.value = messageOf(error, '重置失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

function backToVerify(): void {
  step.value = 1
  errorMessage.value = ''
  form.password = ''
  form.confirmPassword = ''
}
</script>

<template>
  <AuthShell>
    <h2>找回密码</h2>
    <p class="muted small">通过手机号验证后重置密码。</p>

    <ol class="auth-steps" aria-label="找回密码流程">
      <li class="auth-step" :class="{ active: step >= 1 }"><span class="n">1</span>验证手机</li>
      <li class="auth-step" :class="{ active: step >= 2 }"><span class="n">2</span>重置密码</li>
      <li class="auth-step" :class="{ active: step >= 3 }"><span class="n">3</span>完成</li>
    </ol>

    <p v-if="errorMessage" class="form-error">{{ errorMessage }}</p>

    <template v-if="step === 1">
      <div class="field">
        <label for="forgot-mobile">手机号</label>
        <input
          id="forgot-mobile"
          v-model.trim="form.mobile"
          class="input"
          inputmode="numeric"
          maxlength="11"
          autocomplete="tel"
          placeholder="请输入注册手机号"
        />
      </div>

      <div class="field">
        <label for="forgot-code">验证码</label>
        <div class="auth-code-row">
          <input
            id="forgot-code"
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

      <button type="button" class="btn btn-primary btn-lg btn-block" :disabled="submitting" @click="handleVerify">
        {{ submitting ? '验证中…' : '下一步' }}
      </button>
    </template>

    <template v-else-if="step === 2">
      <div class="field">
        <label for="forgot-password">新密码</label>
        <input
          id="forgot-password"
          v-model="form.password"
          class="input"
          type="password"
          autocomplete="new-password"
          placeholder="6~20 位新密码"
        />
      </div>

      <div class="field">
        <label for="forgot-confirm">确认密码</label>
        <input
          id="forgot-confirm"
          v-model="form.confirmPassword"
          class="input"
          type="password"
          autocomplete="new-password"
          placeholder="再次输入新密码"
        />
      </div>

      <button type="button" class="btn btn-primary btn-lg btn-block" :disabled="submitting" @click="handleReset">
        {{ submitting ? '提交中…' : '重置密码' }}
      </button>
      <button type="button" class="auth-text-button" @click="backToVerify">返回重新验证</button>
    </template>

    <template v-else>
      <div class="auth-success">
        <span class="auth-success-mark" aria-hidden="true">✓</span>
        <h3>密码已重置</h3>
        <p class="muted small">请使用新密码重新登录。</p>
      </div>
      <RouterLink class="btn btn-primary btn-lg btn-block" to="/login">立即登录</RouterLink>
    </template>

    <p class="auth-switch"><RouterLink to="/login">返回登录</RouterLink></p>
  </AuthShell>
</template>

<style scoped>
.auth-success {
  margin: 10px 0 22px;
  padding: 28px 20px;
  border: 1px solid var(--line);
  border-radius: var(--r);
  background: var(--surface-2);
  text-align: center;
}

.auth-success-mark {
  display: inline-grid;
  place-items: center;
  width: 44px;
  height: 44px;
  margin-bottom: 10px;
  border-radius: 50%;
  background: var(--jade-soft);
  color: var(--jade);
  font-size: 20px;
  font-weight: 600;
}

.auth-success h3 {
  font-size: 16px;
  margin-bottom: 6px;
}

.auth-text-button {
  display: block;
  width: 100%;
  margin-top: 12px;
  padding: 6px;
  border: 0;
  background: transparent;
  color: var(--ink-3);
  font-size: 12.5px;
}

.auth-text-button:hover {
  color: var(--jade);
}
</style>
