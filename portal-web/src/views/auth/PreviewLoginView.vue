<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { setTokens } from '@/auth/session'
import { useUserStore } from '@/store/user'

/**
 * 管理端「学员端（内测账号）」入口。
 *
 * 管理端通过后端签发学员端双令牌，并以 URL fragment 传给本页
 * （fragment 不会发送到服务端，也不会出现在 Referer 中）。
 * 写入登录态后立即清理地址栏并跳转首页，避免令牌留在历史记录里。
 */
const router = useRouter()
const userStore = useUserStore()
const error = ref('')

onMounted(async () => {
  const params = new URLSearchParams(window.location.hash.replace(/^#/, ''))
  const accessToken = params.get('accessToken')
  const refreshToken = params.get('refreshToken')
  if (!accessToken || !refreshToken) {
    error.value = '内测登录参数缺失，请从管理端重新进入。'
    return
  }
  setTokens({ accessToken, refreshToken }, true)
  try {
    await userStore.loadProfile()
  } catch {
    // 资料接口异常不阻断登录，顶栏会在后续 ensureProfile 时重试
  }
  await router.replace('/')
})
</script>

<template>
  <section class="preview-login">
    <div class="preview-card">
      <p class="eyebrow">Preview Login</p>
      <h1 v-if="error">内测登录失败</h1>
      <h1 v-else>正在使用内测学员账号登录…</h1>
      <p v-if="error" class="preview-tip">
        {{ error }}
        <RouterLink class="preview-link" to="/login">前往登录</RouterLink>
      </p>
      <p v-else class="preview-tip">正在写入登录态并进入首页，请稍候。</p>
    </div>
  </section>
</template>

<style scoped>
.preview-login {
  display: grid;
  place-items: center;
  min-height: 60vh;
  padding: 40px 20px;
}

.preview-card {
  width: min(460px, 100%);
  padding: 32px;
  text-align: center;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r);
  box-shadow: var(--shadow);
}

.preview-card h1 {
  margin-top: 10px;
  font-family: var(--font-display);
  font-size: 24px;
}

.preview-tip {
  margin-top: 12px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--ink-3);
}

.preview-link {
  margin-left: 6px;
  color: var(--jade);
}
</style>
