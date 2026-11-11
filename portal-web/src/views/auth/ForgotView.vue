<script setup lang="ts">
import AuthShell from '@/components/AuthShell.vue'
</script>

<template>
  <AuthShell>
    <h2>找回密码</h2>
    <p class="muted small">找回密码将随认证模块一并实现。</p>
    <div class="form-error" style="background: var(--jade-soft); color: var(--jade-2)">
      流程：验证手机号与验证码 → 获取一次性重置令牌 → 设置新密码（04 P-A05/P-A06）。
    </div>
    <div class="auth-links">
      <RouterLink to="/login">返回登录</RouterLink>
      <RouterLink to="/">回到首页</RouterLink>
    </div>
  </AuthShell>
</template>
