<script setup lang="ts">
import AdminPage from '@/components/AdminPage.vue'

withDefaults(
  defineProps<{
    title: string
    description?: string
    emptyText?: string
  }>(),
  {
    description: '',
    emptyText: '页面布局已就绪，业务功能将在后续迭代中实现'
  }
)
</script>

<template>
  <AdminPage :title="title" :description="description">
    <template v-if="$slots.actions" #actions>
      <slot name="actions" />
    </template>

    <el-card shadow="never" class="placeholder-card">
      <el-empty :description="emptyText" />
    </el-card>

    <slot />
  </AdminPage>
</template>

<style scoped>
.placeholder-card {
  background: var(--surface);
}

.placeholder-card :deep(.el-empty) {
  padding: 48px 0;
}
</style>
