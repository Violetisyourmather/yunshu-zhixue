import type { Directive } from 'vue'
import { useUserStore } from '@/store/user'

/**
 * v-permission="'question:audit'" / v-permission="['a', 'b']"
 * 无权限时移除元素；仅界面控制，后端仍会校验（04 §2.8）。
 */
export const permissionDirective: Directive<HTMLElement, string | string[]> = {
  mounted(el, binding) {
    const userStore = useUserStore()
    const codes = Array.isArray(binding.value) ? binding.value : [binding.value]
    if (!codes.some((code) => userStore.hasPermission(code))) {
      el.parentNode?.removeChild(el)
    }
  }
}
