import { PERMISSION, type PermissionCode } from '@/types/permissions'

export interface MenuEntry {
  path: string
  title: string
  permission?: PermissionCode
  /** 角色管理仅超级管理员可见（04 §2.8、A-L01~A-L05） */
  superAdminOnly?: boolean
}

export interface MenuGroup {
  title: string
  entries: MenuEntry[]
}

/** 菜单按权限码动态渲染（02 §3.3、D7）。 */
export const MENU_GROUPS: MenuGroup[] = [
  {
    title: '',
    entries: [{ path: '/dashboard', title: '数据看板', permission: PERMISSION.DASHBOARD_VIEW }]
  },
  {
    title: '内容管理',
    entries: [
      { path: '/content/subjects', title: '学科管理', permission: PERMISSION.SUBJECT_VIEW },
      { path: '/content/courses', title: '课程库', permission: PERMISSION.COURSE_VIEW },
      { path: '/content/questions', title: '题库管理', permission: PERMISSION.QUESTION_VIEW },
      { path: '/content/interview-bank', title: '面试题库', permission: PERMISSION.INTERVIEWBANK_VIEW },
      { path: '/content/routes', title: '学习路线', permission: PERMISSION.ROUTE_VIEW }
    ]
  },
  {
    title: '学员管理',
    entries: [{ path: '/students', title: '学员列表', permission: PERMISSION.STUDENT_VIEW }]
  },
  {
    title: '系统管理',
    entries: [
      { path: '/system/users', title: '用户管理', permission: PERMISSION.USER_VIEW },
      { path: '/system/roles', title: '角色权限', superAdminOnly: true },
      { path: '/system/logs', title: '操作日志', permission: PERMISSION.SYSTEM_LOG_VIEW },
      { path: '/system/settings', title: '系统设置', permission: PERMISSION.SYSTEM_SETTING }
    ]
  }
]
