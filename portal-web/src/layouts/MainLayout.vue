<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { authBypassEnabled } from '@/auth/preview'
import { useSiteStore } from '@/store/site'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const siteStore = useSiteStore()

const navItems = [
  { to: '/', label: '首页' },
  { to: '/routes', label: '学习路线' },
  { to: '/bank', label: '题库' },
  { to: '/interview', label: 'AI 面试' }
]

const supportLinks = ['帮助中心', '意见反馈', '联系我们']
const aboutLinks = ['关于我们', '用户协议', '隐私政策']

const userMenuOpen = ref(false)
const userMenuRoot = ref<HTMLElement | null>(null)
const currentYear = new Date().getFullYear()
const adminUrl = import.meta.env.VITE_ADMIN_URL || '/admin/'

/** 学员未设置头像时使用后台配置的默认头像。 */
const avatarSrc = computed(() => userStore.profile?.avatar || siteStore.defaultAvatar)
const avatarInitial = computed(() => (userStore.profile?.nickname || userStore.displayName || '云').slice(0, 1))

function isNavActive(path: string): boolean {
  if (path === '/') {
    return route.path === '/'
  }
  if (path === '/bank') {
    return route.path === '/bank' || route.path.startsWith('/practice')
  }
  return route.path === path || route.path.startsWith(`${path}/`)
}

function closeUserMenu(): void {
  userMenuOpen.value = false
}

function handlePointerDown(event: PointerEvent): void {
  if (!userMenuOpen.value) {
    return
  }
  const root = userMenuRoot.value
  if (root && !root.contains(event.target as Node)) {
    closeUserMenu()
  }
}

function handleKeydown(event: KeyboardEvent): void {
  if (event.key === 'Escape') {
    closeUserMenu()
  }
}

function notifyPlaceholder(label: string): void {
  ElMessage.info(`${label}入口正在建设中`)
}

async function handleLogout(): Promise<void> {
  closeUserMenu()
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

onMounted(() => {
  document.addEventListener('pointerdown', handlePointerDown)
  document.addEventListener('keydown', handleKeydown)
})

onBeforeUnmount(() => {
  document.removeEventListener('pointerdown', handlePointerDown)
  document.removeEventListener('keydown', handleKeydown)
})

watch(
  () => route.path,
  () => closeUserMenu()
)
</script>

<template>
  <div class="app-shell">
    <header class="site-header">
      <div class="wrap header-inner">
        <RouterLink class="brand" to="/" aria-label="返回首页">
          <img v-if="siteStore.logoUrl" class="brand-logo" :src="siteStore.logoUrl" alt="平台 Logo" />
          <span v-else class="brand-mark" aria-hidden="true">云</span>
          <span class="brand-copy">
            <span class="brand-name">{{ siteStore.siteName }}</span>
            <small class="brand-subtitle">AI 面试训练平台</small>
          </span>
        </RouterLink>

        <nav class="main-nav" aria-label="主导航">
          <RouterLink
            v-for="item in navItems"
            :key="item.to"
            :to="item.to"
            class="nav-link"
            :class="{ active: isNavActive(item.to) }"
          >
            {{ item.label }}
          </RouterLink>
        </nav>

        <div class="header-actions">
          <template v-if="userStore.isLoggedIn">
            <div ref="userMenuRoot" class="user-menu-root">
              <button
                type="button"
                class="user-chip"
                aria-haspopup="menu"
                :aria-expanded="userMenuOpen"
                @click="userMenuOpen = !userMenuOpen"
              >
                <img v-if="avatarSrc" class="user-avatar" :src="avatarSrc" alt="" />
                <span v-else class="user-avatar user-initial" aria-hidden="true">{{ avatarInitial }}</span>
                <span class="user-name">{{ userStore.displayName }}</span>
                <span class="user-chevron" :class="{ open: userMenuOpen }" aria-hidden="true">▾</span>
              </button>

              <Transition name="menu">
                <div v-if="userMenuOpen" class="user-menu" role="menu">
                  <RouterLink class="menu-item" role="menuitem" to="/profile" @click="closeUserMenu">
                    个人中心
                  </RouterLink>
                  <RouterLink class="menu-item" role="menuitem" to="/wrong-book" @click="closeUserMenu">
                    我的错题
                  </RouterLink>
                  <RouterLink class="menu-item" role="menuitem" to="/interview/history" @click="closeUserMenu">
                    面试记录
                  </RouterLink>
                  <div class="menu-divider" role="separator" />
                  <button type="button" class="menu-item menu-item-danger" role="menuitem" @click="handleLogout">
                    退出登录
                  </button>
                </div>
              </Transition>
            </div>
          </template>
          <RouterLink v-else class="btn btn-primary btn-sm" to="/login">登录</RouterLink>
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
        <div class="footer-grid">
          <div class="footer-brand">
            <RouterLink class="brand" to="/">
              <img v-if="siteStore.logoUrl" class="brand-logo" :src="siteStore.logoUrl" alt="平台 Logo" />
              <span v-else class="brand-mark" aria-hidden="true">云</span>
              <span class="brand-copy">
                <span class="brand-name">{{ siteStore.siteName }}</span>
                <small class="brand-subtitle">YUNSHU ACADEMY</small>
              </span>
            </RouterLink>
            <p class="footer-tagline">用一条清晰的学习路线，把「学过」变成「能被验证的掌握」。</p>
          </div>

          <div class="footer-col">
            <p class="footer-title">产品</p>
            <RouterLink to="/routes">学习路线</RouterLink>
            <RouterLink to="/bank">题库中心</RouterLink>
            <RouterLink to="/interview">AI 面试</RouterLink>
          </div>

          <div class="footer-col">
            <p class="footer-title">支持</p>
            <button
              v-for="label in supportLinks"
              :key="label"
              type="button"
              class="footer-link"
              @click="notifyPlaceholder(label)"
            >
              {{ label }}
            </button>
          </div>

          <div class="footer-col">
            <p class="footer-title">关于</p>
            <button
              v-for="label in aboutLinks"
              :key="label"
              type="button"
              class="footer-link"
              @click="notifyPlaceholder(label)"
            >
              {{ label }}
            </button>
          </div>
        </div>

        <div class="footer-bottom">
          <span>
            © {{ currentYear }} {{ siteStore.siteName }}
            <template v-if="siteStore.siteDescription"> · {{ siteStore.siteDescription }}</template>
          </span>
          <a class="admin-entry" :href="adminUrl">管理后台 →</a>
        </div>
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
  z-index: 40;
  background: rgba(245, 242, 235, 0.9);
  border-bottom: 1px solid var(--line);
  backdrop-filter: blur(10px);
}

.header-inner {
  display: flex;
  align-items: center;
  gap: 30px;
  height: 70px;
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  flex: none;
  border-radius: 8px;
}

.brand-mark {
  display: inline-grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border-radius: 10px;
  background: var(--jade);
  color: #fff;
  font-family: var(--font-display);
  font-size: 17px;
  line-height: 1;
}

.brand-logo {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  object-fit: cover;
  flex: none;
}

.brand-copy {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.brand-name {
  font-family: var(--font-display);
  font-size: 17px;
  font-weight: 600;
  line-height: 1.2;
  letter-spacing: 0.01em;
}

.brand-subtitle {
  font-size: 9.5px;
  line-height: 1.2;
  letter-spacing: 0.12em;
  color: var(--ink-3);
  font-weight: 500;
}

.main-nav {
  display: flex;
  align-items: center;
  gap: 4px;
  flex: 1;
  min-width: 0;
  overflow-x: auto;
  scrollbar-width: none;
}

.main-nav::-webkit-scrollbar {
  display: none;
}

.nav-link {
  flex: none;
  padding: 8px 14px;
  border-radius: 8px;
  color: var(--ink-2);
  font-size: 13.5px;
  line-height: 1.4;
  transition: background-color 0.16s ease, color 0.16s ease;
}

.nav-link.active {
  background: var(--ink);
  color: #fff;
  font-weight: 500;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: auto;
  flex: none;
}

.user-menu-root {
  position: relative;
}

.user-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 38px;
  padding: 4px 10px 4px 5px;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: var(--surface);
  color: var(--ink-2);
  transition: border-color 0.16s ease, background-color 0.16s ease;
}

.user-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  object-fit: cover;
  flex: none;
}

.user-initial {
  display: inline-grid;
  place-items: center;
  background: var(--jade-soft);
  color: var(--jade-2);
  font-size: 12px;
  font-weight: 600;
}

.user-name {
  max-width: 96px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
}

.user-chevron {
  font-size: 10px;
  color: var(--ink-4);
  transition: transform 0.16s ease;
}

.user-chevron.open {
  transform: rotate(180deg);
}

.user-menu {
  position: absolute;
  right: 0;
  top: calc(100% + 8px);
  width: 188px;
  padding: 6px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: 12px;
  box-shadow: var(--shadow-lg);
  z-index: 60;
}

.menu-item {
  display: block;
  width: 100%;
  padding: 9px 12px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: var(--ink-2);
  font-size: 13px;
  line-height: 1.5;
  text-align: left;
  transition: background-color 0.14s ease, color 0.14s ease;
}

.menu-item-danger {
  color: var(--red);
}

.menu-divider {
  height: 1px;
  margin: 6px 0;
  background: var(--line);
}

.menu-enter-active,
.menu-leave-active {
  transition: opacity 0.14s ease, transform 0.14s ease;
}

.menu-enter-from,
.menu-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

.site-main {
  flex: 1;
  min-height: 55vh;
}

.site-footer {
  margin-top: 72px;
  padding: 48px 0 30px;
  border-top: 1px solid var(--line);
  background: rgba(237, 232, 222, 0.34);
}

.footer-grid {
  display: grid;
  grid-template-columns: 1.5fr 1fr 1fr 1fr;
  gap: 36px;
}

.footer-brand .brand {
  margin-bottom: 14px;
}

.footer-tagline {
  max-width: 280px;
  color: var(--ink-3);
  font-size: 12.5px;
  line-height: 1.75;
}

.footer-title {
  margin: 0 0 12px;
  color: var(--ink-3);
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.1em;
}

.footer-col {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
}

.footer-col a,
.footer-link {
  display: block;
  padding: 4px 0;
  border: 0;
  background: transparent;
  color: var(--ink-2);
  font-size: 12.5px;
  line-height: 1.6;
  text-align: left;
  transition: color 0.14s ease;
}

.footer-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  margin-top: 34px;
  padding-top: 20px;
  border-top: 1px solid var(--line);
  color: var(--ink-3);
  font-size: 12px;
}

.admin-entry {
  color: var(--jade);
  font-weight: 500;
}

:where(.nav-link, .user-chip, .menu-item, .footer-col a, .footer-link, .admin-entry):focus-visible {
  outline: 2px solid var(--jade);
  outline-offset: 2px;
}

@media (hover: hover) {
  .nav-link:hover {
    background: rgba(21, 24, 29, 0.05);
  }

  .nav-link.active:hover {
    background: var(--ink);
  }

  .user-chip:hover {
    border-color: var(--line-2);
    background: var(--surface-2);
  }

  .menu-item:hover {
    background: var(--paper-2);
    color: var(--ink);
  }

  .menu-item-danger:hover {
    background: var(--red-soft);
    color: var(--red);
  }

  .footer-col a:hover,
  .footer-link:hover,
  .admin-entry:hover {
    color: var(--jade);
  }
}

@media (max-width: 900px) {
  .header-inner {
    flex-wrap: wrap;
    gap: 10px 16px;
    height: auto;
    padding: 12px 0 10px;
  }

  .main-nav {
    order: 3;
    width: 100%;
    flex: 0 0 100%;
  }

  .header-actions {
    margin-left: auto;
  }

  .footer-grid {
    grid-template-columns: 1.3fr 1fr 1fr;
  }

  .footer-brand {
    grid-column: 1 / -1;
  }

  .footer-tagline {
    max-width: 420px;
  }
}

@media (max-width: 560px) {
  .brand-subtitle {
    display: none;
  }

  .user-name {
    display: none;
  }

  .user-chip {
    padding-right: 8px;
  }

  .footer-grid {
    grid-template-columns: 1fr 1fr;
    gap: 28px 24px;
  }

  .footer-bottom {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .nav-link,
  .user-chip,
  .user-chevron,
  .menu-item,
  .menu-enter-active,
  .menu-leave-active,
  .footer-col a,
  .footer-link {
    transition: none;
  }
}
</style>
