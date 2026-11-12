<script setup lang="ts">
import { computed, onMounted, ref, watch, type Component } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type MenuInstance } from 'element-plus'
import {
  ArrowDown,
  Bell,
  Collection,
  DataBoard,
  Menu as MenuIcon,
  Setting,
  User
} from '@element-plus/icons-vue'
import { portalPreviewTokens } from '@/api/auth'
import { getQuestionStats } from '@/api/questions'
import { authBypassEnabled } from '@/auth/preview'
import { usePermissionStore } from '@/store/permission'
import { useSiteStore } from '@/store/site'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const permissionStore = usePermissionStore()
const siteStore = useSiteStore()

const sidebarOpen = ref(false)
const pendingQuestionCount = ref(0)

/** 学员端地址：生产与后台同域（02 §2.4），开发环境由 .env 指定独立端口。 */
const portalBase = (import.meta.env.VITE_PORTAL_BASE_URL ?? '').replace(/\/+$/, '')

/** 详情页（如 /content/routes/:id）仍高亮其所属菜单 */
const activeMenu = computed(() => {
  const path = route.path
  let matched = path
  let matchedLength = -1

  for (const group of permissionStore.visibleGroups) {
    for (const entry of group.entries) {
      const isMatch = path === entry.path || path.startsWith(`${entry.path}/`)
      if (isMatch && entry.path.length > matchedLength) {
        matched = entry.path
        matchedLength = entry.path.length
      }
    }
  }

  return matched
})

/** 面包屑：菜单分组 / 菜单名称 / 当前页面（详情页会多出最后一级） */
const breadcrumbs = computed(() => {
  const currentTitle = (route.meta.title as string | undefined) ?? ''
  const items: Array<{ title: string; path?: string }> = []

  for (const group of permissionStore.visibleGroups) {
    const entry = group.entries.find((item) => item.path === activeMenu.value)
    if (!entry) {
      continue
    }
    if (group.title) {
      items.push({ title: group.title })
    }
    items.push({ title: entry.title, path: entry.path })
    break
  }

  if (currentTitle && items[items.length - 1]?.title !== currentTitle) {
    items.push({ title: currentTitle })
  }
  if (items.length === 0) {
    items.push({ title: currentTitle || '管理后台' })
  }

  return items
})

const avatarText = computed(() => userStore.displayName.slice(0, 1))
const roleLabel = computed(() => (userStore.isSuperAdmin ? '超级管理员' : '管理员'))

const menuRef = ref<MenuInstance>()

/** 当前激活菜单所属的分组；顶部入口（如数据看板）不属于任何分组 */
const activeGroupTitle = computed(() => {
  for (const group of permissionStore.visibleGroups) {
    if (!group.title) {
      continue
    }
    if (group.entries.some((entry) => entry.path === activeMenu.value)) {
      return group.title
    }
  }
  return undefined
})

/** 刷新后只展开当前激活菜单所在分组，其余分组折叠 */
const defaultOpenedGroups = computed(() =>
  activeGroupTitle.value ? [activeGroupTitle.value] : []
)

/** 路由切换时自动展开当前分组并收起其他分组 */
watch(activeGroupTitle, (title) => {
  if (!menuRef.value) {
    return
  }
  for (const group of permissionStore.visibleGroups) {
    if (group.title && group.title !== title) {
      menuRef.value.close(group.title)
    }
  }
  if (title) {
    menuRef.value.open(title)
  }
})

/** 一级菜单图标（分组标题 + 顶层入口） */
const MENU_GROUP_ICONS: Record<string, Component> = {
  内容管理: Collection,
  学员管理: User,
  系统管理: Setting
}

const MENU_ITEM_ICONS: Record<string, Component> = {
  '/dashboard': DataBoard
}

function groupIcon(title: string): Component | undefined {
  return MENU_GROUP_ICONS[title]
}

function itemIcon(path: string): Component | undefined {
  return MENU_ITEM_ICONS[path]
}

watch(
  () => route.fullPath,
  () => {
    sidebarOpen.value = false
  }
)

function breadcrumbTarget(index: number): { path: string } | undefined {
  const item = breadcrumbs.value[index]
  if (!item?.path || index === breadcrumbs.value.length - 1) {
    return undefined
  }
  return { path: item.path }
}

async function handleLogout(): Promise<void> {
  if (authBypassEnabled) {
    ElMessage.info('当前为开发预览模式，无需登录')
    return
  }
  const confirmed = await ElMessageBox.confirm('确认退出登录？', '提示', {
    type: 'warning',
    confirmButtonText: '退出',
    cancelButtonText: '取消'
  }).catch(() => false)
  if (!confirmed) {
    return
  }
  await userStore.logout()
  ElMessage.success('已退出登录')
  await router.replace('/login')
}

/**
 * 进入学员端内测账号：后端签发学员端令牌后，以 URL fragment 传给学员端
 * （fragment 不会发送到服务端），学员端写入登录态即完成免登录。
 * 同步先开新标签页，避免 await 之后被浏览器拦截弹窗。
 */
async function openPortalPreview(): Promise<void> {
  const tab = window.open('', '_blank')
  try {
    const tokens = await portalPreviewTokens()
    const origin = portalBase || window.location.origin
    const hash = `#accessToken=${encodeURIComponent(tokens.accessToken)}&refreshToken=${encodeURIComponent(tokens.refreshToken)}`
    const url = `${origin}/preview-login${hash}`
    if (tab) {
      tab.opener = null
      tab.location.replace(url)
    } else {
      window.location.href = url
    }
  } catch {
    // 错误提示由 HTTP 拦截器统一弹出
    tab?.close()
  }
}

/** 通知铃铛：角标为待审核题目数，点击进入题库并筛选「待审核」。 */
async function loadPendingQuestions(): Promise<void> {
  if (!userStore.hasPermission('question:view')) {
    return
  }
  try {
    const stats = await getQuestionStats()
    pendingQuestionCount.value = stats.pending
  } catch {
    pendingQuestionCount.value = 0
  }
}

function goPendingQuestions(): void {
  void router.push({ path: '/content/questions', query: { status: '3' } })
}

function openPortalLogin(): void {
  window.open(`${portalBase || window.location.origin}/login`, '_blank', 'noopener')
}

function handleCommand(command: string): void {
  if (command === 'portal') {
    void openPortalPreview()
  } else if (command === 'portal-login') {
    openPortalLogin()
  } else if (command === 'logout') {
    void handleLogout()
  }
}

onMounted(() => {
  void loadPendingQuestions()
})
</script>

<template>
  <el-container class="admin-shell">
    <el-aside :class="['admin-aside', { 'is-open': sidebarOpen }]" width="232px">
      <RouterLink class="admin-brand" to="/dashboard">
        <img v-if="siteStore.logoUrl" class="brand-logo" :src="siteStore.logoUrl" alt="平台 Logo" />
        <span v-else class="brand-mark">云</span>
        <span class="brand-text">
          {{ siteStore.siteName }}
          <small>ADMIN CONSOLE</small>
        </span>
      </RouterLink>

      <div class="admin-menu-scroll">
        <el-menu
          ref="menuRef"
          :default-active="activeMenu"
          :default-openeds="defaultOpenedGroups"
          unique-opened
          router
          class="admin-menu"
          background-color="#15181d"
          text-color="#a8adb4"
          active-text-color="#ffffff"
          @select="sidebarOpen = false"
        >
          <template v-for="group in permissionStore.visibleGroups" :key="group.title || 'root'">
            <el-sub-menu v-if="group.title" :index="group.title" class="admin-menu-group">
              <template #title>
                <el-icon v-if="groupIcon(group.title)" class="menu-first-icon">
                  <component :is="groupIcon(group.title)" />
                </el-icon>
                <span class="menu-group-title">{{ group.title }}</span>
              </template>
              <el-menu-item v-for="item in group.entries" :key="item.path" :index="item.path">
                {{ item.title }}
              </el-menu-item>
            </el-sub-menu>
            <template v-else>
              <el-menu-item v-for="item in group.entries" :key="item.path" :index="item.path">
                <el-icon v-if="itemIcon(item.path)" class="menu-first-icon">
                  <component :is="itemIcon(item.path)" />
                </el-icon>
                <span>{{ item.title }}</span>
              </el-menu-item>
            </template>
          </template>
        </el-menu>
      </div>

      <div class="admin-aside-foot">{{ siteStore.siteName }} · 管理后台</div>
    </el-aside>

    <div v-if="sidebarOpen" class="admin-mask" @click="sidebarOpen = false" />

    <el-container class="admin-body">
      <el-header class="admin-header">
        <div class="header-left">
          <el-button
            class="sidebar-toggle"
            text
            :icon="MenuIcon"
            aria-label="展开菜单"
            @click="sidebarOpen = true"
          />
          <el-breadcrumb separator="/" class="admin-breadcrumb">
            <el-breadcrumb-item
              v-for="(item, index) in breadcrumbs"
              :key="`${item.title}-${index}`"
              :to="breadcrumbTarget(index)"
            >
              {{ item.title }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="header-right">
          <el-badge
            v-if="userStore.hasPermission('question:view')"
            :value="pendingQuestionCount"
            :max="99"
            :hidden="pendingQuestionCount === 0"
            class="admin-bell"
          >
            <el-button
              class="bell-button"
              text
              circle
              :icon="Bell"
              aria-label="通知：待审核题目"
              @click="goPendingQuestions"
            />
          </el-badge>

          <el-dropdown trigger="click" @command="handleCommand">
            <span class="admin-user">
              <img
                v-if="siteStore.defaultAvatar"
                class="admin-avatar"
                :src="siteStore.defaultAvatar"
                alt="头像"
              />
              <span v-else class="admin-avatar">{{ avatarText }}</span>
              <span class="admin-user-meta">
                <span class="admin-user-name">{{ userStore.displayName }}</span>
                <span class="admin-user-role">{{ roleLabel }}</span>
              </span>
              <el-icon class="admin-user-arrow"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="portal-login">返回学员前台</el-dropdown-item>
                <el-dropdown-item command="portal">学员端（内测账号）</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="admin-main">
        <RouterView v-slot="{ Component }">
          <Transition name="page-fade" mode="out-in">
            <component :is="Component" :key="route.path" />
          </Transition>
        </RouterView>
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.admin-shell {
  min-height: 100vh;
}

.admin-aside {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: var(--ink);
  position: sticky;
  top: 0;
  height: 100vh;
  /* 滚动交给内部菜单区，品牌与底部说明始终可见 */
  overflow: hidden;
}

.admin-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: none;
  height: 68px;
  padding: 0 20px;
  color: #fff;
  font-family: var(--font-display);
  font-size: 17px;
  font-weight: 600;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.admin-brand .brand-mark {
  display: inline-grid;
  place-items: center;
  width: 32px;
  height: 32px;
  border-radius: 9px;
  background: var(--jade);
  color: #fff;
  font-size: 15px;
  flex: none;
}

.admin-brand .brand-logo {
  width: 32px;
  height: 32px;
  border-radius: 9px;
  object-fit: cover;
  flex: none;
}

.admin-brand .brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.25;
}

.admin-brand small {
  margin-top: 2px;
  font-family: var(--font-ui);
  font-size: 9px;
  font-weight: 400;
  color: var(--ink-4);
  letter-spacing: 0.14em;
}

/* 菜单区独立滚动：缩放/矮屏时只滚动菜单，品牌与底栏不被裁切 */
.admin-menu-scroll {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overscroll-behavior: contain;
  scrollbar-width: thin;
  scrollbar-color: rgba(255, 255, 255, 0.18) transparent;
  /* 底部渐隐，提示下方仍有菜单项，避免半截行看起来像被裁切 */
  -webkit-mask-image: linear-gradient(to bottom, #000 calc(100% - 18px), transparent);
  mask-image: linear-gradient(to bottom, #000 calc(100% - 18px), transparent);
}

.admin-menu-scroll::-webkit-scrollbar {
  width: 6px;
}

.admin-menu-scroll::-webkit-scrollbar-track {
  background: transparent;
}

.admin-menu-scroll::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.18);
}

.admin-menu-scroll::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.3);
}

.admin-menu {
  padding: 10px 0 20px;
  border-right: 0;
}

.admin-menu :deep(.el-menu-item) {
  height: 42px;
  margin: 2px 10px;
  border-radius: 9px;
  font-size: 14px;
}

.admin-menu :deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.06) !important;
}

.admin-menu :deep(.el-menu-item.is-active) {
  background: var(--jade) !important;
}

.admin-menu :deep(.el-sub-menu__title) {
  height: 40px;
  line-height: 40px;
  margin: 6px 10px 2px;
  padding: 0 14px 0 20px !important;
  border-radius: 9px;
  color: var(--ink-4);
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.04em;
}

.admin-menu :deep(.el-sub-menu__title:hover) {
  background: rgba(255, 255, 255, 0.06) !important;
  color: #d7dade;
}

.admin-menu :deep(.el-sub-menu__icon-arrow) {
  right: 14px;
  font-size: 12px;
  color: var(--ink-4);
}

.admin-menu :deep(.menu-first-icon) {
  margin-right: 9px;
  font-size: 16px;
  color: inherit;
  flex: none;
}

.admin-menu :deep(.el-sub-menu .el-menu) {
  background: transparent;
}

.admin-menu :deep(.el-sub-menu .el-menu-item) {
  padding-right: 16px;
  /* 一级标题文字位于 55px（10px 外边距 + 20px 内边距 + 16px 图标 + 9px 间距），
     子项再右移 16px，落在标题文字右下方，体现层级归属 */
  padding-left: 61px !important;
}

.admin-aside-foot {
  flex: none;
  padding: 14px 20px 18px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  color: var(--ink-4);
  font-size: 11px;
  letter-spacing: 0.04em;
}

.admin-mask {
  display: none;
}

.admin-body {
  min-width: 0;
}

.admin-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  height: 68px;
  padding: 0 28px;
  background: var(--surface);
  border-bottom: 1px solid var(--line);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.sidebar-toggle {
  display: none;
  font-size: 18px;
  color: var(--ink-2);
}

.admin-breadcrumb {
  min-width: 0;
}

.admin-breadcrumb :deep(.el-breadcrumb__inner) {
  font-size: 13.5px;
  color: var(--ink-3);
}

.admin-breadcrumb :deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) {
  color: var(--ink);
  font-weight: 600;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 14px;
}

.admin-bell {
  display: inline-flex;
}

.bell-button {
  color: var(--ink-3);
}

.bell-button:hover {
  color: var(--jade);
}

.admin-user {
  display: inline-flex;
  align-items: center;
  gap: 9px;
  padding: 5px 10px 5px 6px;
  border-radius: 999px;
  cursor: pointer;
  color: var(--ink-2);
  transition: background 0.16s;
}

.admin-user:hover {
  background: var(--paper-2);
}

.admin-avatar {
  display: inline-grid;
  place-items: center;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: var(--jade-soft);
  color: var(--jade-2);
  font-size: 13px;
  font-weight: 600;
  flex: none;
}

img.admin-avatar {
  display: block;
  object-fit: cover;
}

.admin-user-meta {
  display: flex;
  flex-direction: column;
  line-height: 1.3;
}

.admin-user-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--ink);
}

.admin-user-role {
  font-size: 11px;
  color: var(--ink-3);
}

.admin-user-arrow {
  font-size: 12px;
  color: var(--ink-3);
}

.admin-main {
  padding: 24px 28px 36px;
  background: var(--paper);
}

@media (max-width: 960px) {
  .admin-aside {
    position: fixed;
    inset: 0 auto 0 0;
    z-index: 1001;
    width: 232px;
    transform: translateX(-100%);
    transition: transform 0.22s ease;
  }

  .admin-aside.is-open {
    transform: translateX(0);
    box-shadow: var(--shadow-lg);
  }

  .admin-mask {
    display: block;
    position: fixed;
    inset: 0;
    z-index: 1000;
    background: rgba(21, 24, 29, 0.42);
  }

  .sidebar-toggle {
    display: inline-flex;
  }

  .admin-header {
    padding: 0 16px;
  }

  .admin-main {
    padding: 18px 16px 32px;
  }
}

@media (max-width: 640px) {
  .admin-user-meta {
    display: none;
  }

  .admin-header {
    gap: 8px;
  }
}
</style>
