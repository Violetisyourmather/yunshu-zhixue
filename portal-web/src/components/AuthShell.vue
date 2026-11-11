<script setup lang="ts">
import { useSiteStore } from '@/store/site'

const siteStore = useSiteStore()
</script>

<template>
  <div class="auth-shell">
    <aside class="auth-aside">
      <RouterLink class="auth-brand" to="/">
        <img v-if="siteStore.logoUrl" class="brand-logo" :src="siteStore.logoUrl" alt="平台 Logo" />
        <span v-else class="brand-mark">云</span>
        <span>{{ siteStore.siteName }}</span>
      </RouterLink>
      <div>
        <p class="eyebrow">AI Interview Training</p>
        <h1>把每一次练习，<br />变成面试的底气。</h1>
        <p class="lead">学习路线 · 刷题训练 · 错题复盘 · AI 面试</p>
      </div>
      <p class="muted small">© {{ siteStore.siteName }}</p>
    </aside>
    <section class="auth-card">
      <slot />
    </section>
  </div>
</template>

<style scoped>
.auth-brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  font-family: var(--font-display);
  font-size: 18px;
  font-weight: 600;
}

.brand-mark {
  display: inline-grid;
  place-items: center;
  width: 32px;
  height: 32px;
  border-radius: 9px;
  background: var(--ink);
  color: var(--surface);
  font-size: 15px;
}

.brand-logo {
  width: 32px;
  height: 32px;
  border-radius: 9px;
  object-fit: cover;
  flex: none;
}
</style>
