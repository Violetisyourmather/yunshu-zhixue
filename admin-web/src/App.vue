<script setup lang="ts">
import { onMounted, watchEffect } from 'vue'
import { useRoute } from 'vue-router'
import { useSiteStore } from '@/store/site'

const route = useRoute()
const siteStore = useSiteStore()

onMounted(() => {
  void siteStore.load()
})

/** 浏览器标题跟随平台名称：设置保存后无需刷新即可生效 */
watchEffect(() => {
  const pageTitle = route.meta.title as string | undefined
  document.title = pageTitle
    ? `${pageTitle} · ${siteStore.siteName}管理后台`
    : `${siteStore.siteName} · 管理后台`
})
</script>

<template>
  <RouterView />
</template>
