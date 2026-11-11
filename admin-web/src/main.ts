import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import { onTokensChanged } from './auth/session'
import { registerSessionExpiredHandler } from './auth/session-events'
import { permissionDirective } from './directives/permission'
import { useUserStore } from './store/user'
import './styles/index.css'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)
app.use(ElementPlus, { locale: zhCn })
app.directive('permission', permissionDirective)

const userStore = useUserStore(pinia)
// 刷新令牌轮换后同步最新权限码
onTokensChanged(() => userStore.syncPermissions())

registerSessionExpiredHandler(() => {
  const current = router.currentRoute.value
  if (current.path === '/login') {
    return
  }
  void router.replace({ path: '/login', query: { redirect: current.fullPath } })
})

app.mount('#app')
