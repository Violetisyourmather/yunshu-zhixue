import { ref } from 'vue'
import { defineStore } from 'pinia'
import { getSiteSettings } from '@/api/site'
import type { SystemSettings } from '@/types/system'

/** 系统设置未配置或读取失败时的兜底品牌，与后端默认值保持一致。 */
export const DEFAULT_SITE_NAME = '云枢智学'
export const DEFAULT_SITE_DESCRIPTION = 'AI 赋能的智能面试训练平台'

/**
 * 平台品牌信息（P-T01）：登录页、侧栏、浏览器标题与默认头像共用。
 * 读取失败不阻断页面，沿用默认品牌。
 */
export const useSiteStore = defineStore('site', () => {
  const siteName = ref(DEFAULT_SITE_NAME)
  const siteDescription = ref(DEFAULT_SITE_DESCRIPTION)
  const logoUrl = ref('')
  const defaultAvatar = ref('')
  const loading = ref(false)
  let loaded = false

  function apply(data: SystemSettings): void {
    siteName.value = data.siteName?.trim() || DEFAULT_SITE_NAME
    siteDescription.value = data.siteDescription ?? ''
    logoUrl.value = data.logoUrl ?? ''
    defaultAvatar.value = data.defaultAvatar ?? ''
  }

  async function load(force = false): Promise<void> {
    if (loaded && !force) {
      return
    }
    loading.value = true
    try {
      apply(await getSiteSettings())
      loaded = true
    } catch {
      // 品牌信息属于展示增强，读取失败时保留默认值
    } finally {
      loading.value = false
    }
  }

  return { siteName, siteDescription, logoUrl, defaultAvatar, loading, load, apply }
})
