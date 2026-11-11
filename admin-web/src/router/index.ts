import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/store/user'
import { PERMISSION, type PermissionCode } from '@/types/permissions'

declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    requiresAuth?: boolean
    publicOnly?: boolean
    permission?: PermissionCode
    superAdminOnly?: boolean
  }
}

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', publicOnly: true }
  },
  {
    path: '/',
    component: () => import('@/layouts/AdminLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '数据看板', requiresAuth: true, permission: PERMISSION.DASHBOARD_VIEW }
      },
      {
        path: 'content/subjects',
        name: 'subjects',
        component: () => import('@/views/content/subjects/index.vue'),
        meta: { title: '学科管理', requiresAuth: true, permission: PERMISSION.SUBJECT_VIEW }
      },
      {
        path: 'content/courses',
        name: 'courses',
        component: () => import('@/views/content/courses/index.vue'),
        meta: { title: '课程库', requiresAuth: true, permission: PERMISSION.COURSE_VIEW }
      },
      {
        path: 'content/questions',
        name: 'questions',
        component: () => import('@/views/content/questions/index.vue'),
        meta: { title: '题库管理', requiresAuth: true, permission: PERMISSION.QUESTION_VIEW }
      },
      {
        path: 'content/interview-bank',
        name: 'interview-bank',
        component: () => import('@/views/content/interview-bank/index.vue'),
        meta: { title: '面试题库', requiresAuth: true, permission: PERMISSION.INTERVIEWBANK_VIEW }
      },
      {
        path: 'content/routes',
        name: 'routes',
        component: () => import('@/views/content/routes/index.vue'),
        meta: { title: '学习路线', requiresAuth: true, permission: PERMISSION.ROUTE_VIEW }
      },
      {
        path: 'content/routes/:id',
        name: 'route-detail',
        component: () => import('@/views/content/routes/detail.vue'),
        meta: { title: '路线详情', requiresAuth: true, permission: PERMISSION.ROUTE_VIEW }
      },
      {
        path: 'students',
        name: 'students',
        component: () => import('@/views/student/index.vue'),
        meta: { title: '学员管理', requiresAuth: true, permission: PERMISSION.STUDENT_VIEW }
      },
      {
        path: 'system/users',
        name: 'system-users',
        component: () => import('@/views/system/users/index.vue'),
        meta: { title: '用户管理', requiresAuth: true, permission: PERMISSION.USER_VIEW }
      },
      {
        path: 'system/roles',
        name: 'system-roles',
        component: () => import('@/views/system/roles/index.vue'),
        meta: { title: '角色权限', requiresAuth: true, superAdminOnly: true }
      },
      {
        path: 'system/logs',
        name: 'system-logs',
        component: () => import('@/views/system/logs/index.vue'),
        meta: { title: '操作日志', requiresAuth: true, permission: PERMISSION.SYSTEM_LOG_VIEW }
      },
      {
        path: 'system/settings',
        name: 'system-settings',
        component: () => import('@/views/system/settings/index.vue'),
        meta: { title: '系统设置', requiresAuth: true, permission: PERMISSION.SYSTEM_SETTING }
      }
    ]
  },
  {
    path: '/403',
    name: 'forbidden',
    component: () => import('@/views/error/Forbidden.vue'),
    meta: { title: '无权限', requiresAuth: true }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/views/error/NotFound.vue'),
    meta: { title: '页面不存在', requiresAuth: true }
  }
]

const router = createRouter({
  history: createWebHistory('/admin'),
  routes,
  scrollBehavior(_to, _from, savedPosition) {
    return savedPosition ?? { top: 0 }
  }
})

router.beforeEach((to) => {
  const userStore = useUserStore()

  if (to.meta.publicOnly && userStore.isLoggedIn) {
    return { path: '/dashboard' }
  }
  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.meta.superAdminOnly && !userStore.isSuperAdmin) {
    return { path: '/403' }
  }
  if (to.meta.permission && !userStore.hasPermission(to.meta.permission)) {
    return { path: '/403' }
  }
  return true
})

export default router
