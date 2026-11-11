<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { authBypassEnabled } from '@/auth/preview'
import { useSiteStore } from '@/store/site'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()
const siteStore = useSiteStore()

/** 学员未设置头像时使用后台配置的默认头像 */
const avatarSrc = computed(() => userStore.profile?.avatar || siteStore.defaultAvatar)

const navItems = [
  { to: '/', label: '首页' },
  { to: '/bank', label: '题库' },
  { to: '/routes', label: '学习路线' },
  { to: '/wrong-book', label: '错题本' },
  { to: '/interview', label: 'AI 面试' }
]

async function handleLogout(): Promise<void> {
  if (authBypassEnabled) {
    ElMessage.info('当前为开发预览模式，无需登录')
    return
  }
  try {
    await ElMessageBox.confirm('确定退出当前账号吗？', '退出登录', {
      confirmButtonText: '退出',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  await userStore.logout()
  ElMessage.success('已退出登录')
  await router.replace('/')
}
</script>

<template>
  <div class="app-shell">
    <header class="site-header">
      <div class="wrap header-inner">
        <RouterLink class="brand" to="/">
          <img v-if="siteStore.logoUrl" class="brand-logo" :src="siteStore.logoUrl" alt="平台 Logo" />
          <span v-else class="brand-mark">云</span>
          <span class="brand-text">{{ siteStore.siteName }}</span>
        </RouterLink>

        <nav class="main-nav" aria-label="主导航">
          <RouterLink v-for="item in navItems" :key="item.to" :to="item.to" class="nav-link">
            {{ item.label }}
          </RouterLink>
        </nav>

        <div class="header-actions">
          <template v-if="userStore.isLoggedIn">
            <RouterLink to="/profile" class="user-entry">
              <img v-if="avatarSrc" class="user-avatar" :src="avatarSrc" alt="学员头像" />
              <span>{{ userStore.displayName }}</span>
            </RouterLink>
            <button type="button" class="btn btn-ghost btn-sm" @click="handleLogout">退出</button>
          </template>
          <RouterLink v-else to="/login" class="btn btn-primary btn-sm">登录</RouterLink>
        </div>
      </div>
    </header>

    <main class="site-main">
      <div class="wrap">
        <RouterView />
      </div>
    </main>

    <footer class="site-footer">
      <div class="wrap footer-inner">
        <span>
          {{ siteStore.siteName }}
          <template v-if="siteStore.siteDescription"> · {{ siteStore.siteDescription }}</template>
        </span>
        <span class="muted small">学习路线 / 刷题训练 / 错题复盘 / AI 面试</span>
      </div>
    </footer>
  </div>
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.site-header {
  position: sticky;
  top: 0;
  z-index: 20;
  background: rgba(245, 242, 235, 0.86);
  border-bottom: 1px solid var(--line);
  backdrop-filter: blur(10px);
}

.header-inner {
  display: flex;
  align-items: center;
  gap: 32px;
  height: 68px;
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  font-family: var(--font-display);
  font-size: 18px;
  font-weight: 600;
}

.brand-mark {
  display: inline-grid;
  place-items: center;
  width: 32px;
  height: 32px;
  border-radius: 9px;
  background: var(--ink);
  color: var(--surface);
  font-size: 15px;
}

.brand-logo {
  width: 32px;
  height: 32px;
  border-radius: 9px;
  object-fit: cover;
  flex: none;
}

.main-nav {
  display: flex;
  align-items: center;
  gap: 4px;
  flex: 1;
}

.nav-link {
  padding: 7px 12px;
  border-radius: 999px;
  color: var(--ink-2);
  font-size: 13.5px;
  transition: background 0.16s, color 0.16s;
}

.nav-link:hover {
  background: rgba(21, 24, 29, 0.05);
}

.nav-link.router-link-active {
  background: var(--jade-soft);
  color: var(--jade-2);
  font-weight: 600;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-entry {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 6px 10px;
  border-radius: 999px;
  font-size: 13px;
  color: var(--ink-2);
}

.user-avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  object-fit: cover;
  flex: none;
}

.user-entry:hover {
  background: rgba(21, 24, 29, 0.05);
}

.site-main {
  flex: 1;
}

.site-footer {
  border-top: 1px solid var(--line);
  background: var(--paper-2);
}

.footer-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 22px 0;
  font-size: 12.5px;
  color: var(--ink-3);
}

@media (max-width: 860px) {
  .header-inner {
    gap: 16px;
    height: auto;
    padding: 12px 0;
    flex-wrap: wrap;
  }

  .main-nav {
    order: 3;
    width: 100%;
    overflow-x: auto;
  }

  .footer-inner {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
