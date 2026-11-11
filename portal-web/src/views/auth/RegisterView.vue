<script setup lang="ts">
import AuthShell from '@/components/AuthShell.vue'
</script>

<template>
  <AuthShell>
    <h2>注册</h2>
    <p class="muted small">注册功能将随认证模块一并实现。</p>
    <div class="form-error" style="background: var(--jade-soft); color: var(--jade-2)">
      基础工程已就绪：路由、请求层、状态管理与样式令牌已接入。
    </div>
    <p class="lead">注册需要手机号、短信验证码与 6~20 位密码（04 P-A02）。</p>
    <div class="auth-links">
      <RouterLink to="/login">返回登录</RouterLink>
      <RouterLink to="/">回到首页</RouterLink>
    </div>
  </AuthShell>
</template>
