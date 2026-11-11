<script setup lang="ts">
withDefaults(
  defineProps<{
    eyebrow?: string
    title: string
    description?: string
  }>(),
  {
    eyebrow: '云枢智学',
    description: ''
  }
)
</script>

<template>
  <section class="page">
    <div class="page-head">
      <p class="eyebrow">{{ eyebrow }}</p>
      <h1>{{ title }}</h1>
      <p v-if="description" class="lead">{{ description }}</p>
    </div>
    <div class="placeholder-card">
      <span class="placeholder-dot" />
      <p>基础工程已就绪，业务功能将在后续迭代中实现。</p>
    </div>
    <slot />
  </section>
</template>

<style scoped>
.page {
  padding: 56px 0 80px;
}

.page-head h1 {
  margin-top: 10px;
  font-family: var(--font-display);
  font-size: clamp(28px, 3vw, 38px);
}

.page-head .lead {
  margin-top: 12px;
  max-width: 720px;
}

.placeholder-card {
  margin-top: 36px;
  padding: 40px;
  display: flex;
  align-items: center;
  gap: 14px;
  background: var(--surface);
  border: 1px dashed var(--line-2);
  border-radius: var(--r);
  color: var(--ink-3);
}

.placeholder-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: var(--jade);
  box-shadow: 0 0 0 5px var(--jade-soft);
}
</style>
