<script setup lang="ts">
/**
 * 管理端页面统一容器：负责页头（标题、描述、操作区）与内容区的布局，
 * 业务页面只需填充内容，避免各页重复编写页头结构。
 */
defineProps<{
  title: string
  description?: string
}>()
</script>

<template>
  <section class="admin-page">
    <header class="admin-page-head">
      <div class="admin-page-heading">
        <h2>{{ title }}</h2>
        <p v-if="description">{{ description }}</p>
      </div>
      <div v-if="$slots.actions" class="admin-page-actions">
        <slot name="actions" />
      </div>
    </header>

    <div class="admin-page-body">
      <slot />
    </div>
  </section>
</template>

<style scoped>
.admin-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
  min-width: 0;
}

.admin-page-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.admin-page-heading {
  min-width: 0;
}

.admin-page-heading h2 {
  font-family: var(--font-display);
  font-size: 24px;
  line-height: 1.3;
}

.admin-page-heading p {
  margin-top: 6px;
  color: var(--ink-3);
  font-size: 13px;
}

.admin-page-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.admin-page-body {
  min-width: 0;
}

@media (max-width: 640px) {
  .admin-page-head {
    align-items: flex-start;
  }

  .admin-page-heading h2 {
    font-size: 21px;
  }
}
</style>
