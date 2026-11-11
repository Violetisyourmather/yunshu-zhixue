<script setup lang="ts">
import { onMounted, watchEffect } from 'vue'
import { useRoute } from 'vue-router'
import { useSiteStore } from '@/store/site'
import { useUserStore } from '@/store/user'

const route = useRoute()
const userStore = useUserStore()
const siteStore = useSiteStore()

onMounted(() => {
  userStore.ensureProfile()
  void siteStore.load()
})

/** 浏览器标题跟随平台名称：设置保存后无需刷新即可生效 */
watchEffect(() => {
  const pageTitle = route.meta.title as string | undefined
  document.title = pageTitle ? `${pageTitle} · ${siteStore.siteName}` : siteStore.siteName
})
</script>

<template>
  <RouterView />
</template>
