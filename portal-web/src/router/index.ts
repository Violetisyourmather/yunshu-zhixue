import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/store/user'

declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    /** 登录后才能访问（公开区不设置） */
    requiresAuth?: boolean
    /** 仅未登录可访问（登录/注册/找回密码） */
    publicOnly?: boolean
  }
}

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    children: [
      {
        path: '',
        name: 'home',
        component: () => import('@/views/home/index.vue'),
        meta: { title: '首页' }
      },
      {
        path: 'bank',
        name: 'question-bank',
        component: () => import('@/views/questions/index.vue'),
        meta: { title: '题库中心' }
      },
      {
        path: 'practice',
        name: 'practice',
        component: () => import('@/views/practice/index.vue'),
        meta: { title: '刷题练习', requiresAuth: true }
      },
      {
        path: 'wrong-book',
        name: 'wrong-book',
        component: () => import('@/views/wrong-book/index.vue'),
        meta: { title: '错题本', requiresAuth: true }
      },
      {
        path: 'routes',
        name: 'routes',
        component: () => import('@/views/routes/index.vue'),
        meta: { title: '学习路线', requiresAuth: true }
      },
      {
        path: 'interview',
        name: 'interview-config',
        component: () => import('@/views/interview/ConfigView.vue'),
        meta: { title: 'AI 面试', requiresAuth: true }
      },
      {
        path: 'interview/session',
        name: 'interview-session',
        component: () => import('@/views/interview/SessionView.vue'),
        meta: { title: '面试进行中', requiresAuth: true }
      },
      {
        path: 'interview/report/:id',
        name: 'interview-report',
        component: () => import('@/views/interview/ReportView.vue'),
        meta: { title: '面试报告', requiresAuth: true }
      },
      {
        path: 'interview/history',
        name: 'interview-history',
        component: () => import('@/views/interview/HistoryView.vue'),
        meta: { title: '面试记录', requiresAuth: true }
      },
      {
        path: 'profile',
        name: 'profile',
        component: () => import('@/views/profile/index.vue'),
        meta: { title: '个人中心', requiresAuth: true }
      }
    ]
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/auth/LoginView.vue'),
    meta: { title: '登录', publicOnly: true }
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('@/views/auth/RegisterView.vue'),
    meta: { title: '注册', publicOnly: true }
  },
  {
    path: '/forgot',
    name: 'forgot-password',
    component: () => import('@/views/auth/ForgotView.vue'),
    meta: { title: '找回密码', publicOnly: true }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/views/error/NotFound.vue'),
    meta: { title: '页面不存在' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(_to, _from, savedPosition) {
    return savedPosition ?? { top: 0 }
  }
})

router.beforeEach((to) => {
  const userStore = useUserStore()

  if (to.meta.publicOnly && userStore.isLoggedIn) {
    return { path: '/' }
  }
  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  return true
})

export default router
