import { computed } from 'vue'
import { defineStore } from 'pinia'
import { MENU_GROUPS, type MenuEntry } from '@/router/menus'
import { useUserStore } from '@/store/user'

/** 菜单与按钮可见性：仅做展示控制，接口权限始终由后端校验。 */
export const usePermissionStore = defineStore('permission', () => {
  const userStore = useUserStore()

  function canSee(entry: MenuEntry): boolean {
    if (entry.superAdminOnly && !userStore.isSuperAdmin) {
      return false
    }
    return !entry.permission || userStore.hasPermission(entry.permission)
  }

  const visibleGroups = computed(() =>
    MENU_GROUPS.map((group) => ({
      title: group.title,
      entries: group.entries.filter(canSee)
    })).filter((group) => group.entries.length > 0)
  )

  function can(code: string): boolean {
    return userStore.hasPermission(code)
  }

  return { visibleGroups, canSee, can }
})
