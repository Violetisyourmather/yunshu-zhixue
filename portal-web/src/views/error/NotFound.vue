<script setup lang="ts">
</script>

<template>
  <div class="not-found">
    <p class="eyebrow">404</p>
    <h1>页面不存在</h1>
    <p class="lead">你访问的页面可能已被移动或删除。</p>
    <RouterLink class="btn btn-primary" to="/">回到首页</RouterLink>
  </div>
</template>

<style scoped>
.not-found {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 14px;
  padding: 48px;
  text-align: center;
}

.not-found h1 {
  font-family: var(--font-display);
  font-size: 34px;
}
</style>
