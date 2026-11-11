import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import { registerSessionExpiredHandler } from './auth/session-events'
import './styles/index.css'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })

registerSessionExpiredHandler(() => {
  const current = router.currentRoute.value
  if (current.path === '/login') {
    return
  }
  void router.replace({ path: '/login', query: { redirect: current.fullPath } })
})

app.mount('#app')
