/** 权限码：与 04 附录 A（9 组 35 项）一一对应。 */
export const PERMISSION = {
  SUBJECT_VIEW: 'subject:view',
  SUBJECT_ADD: 'subject:add',
  SUBJECT_EDIT: 'subject:edit',
  SUBJECT_DELETE: 'subject:delete',

  COURSE_VIEW: 'course:view',
  COURSE_ADD: 'course:add',
  COURSE_EDIT: 'course:edit',
  COURSE_DELETE: 'course:delete',
  COURSE_IMPORT: 'course:import',

  QUESTION_VIEW: 'question:view',
  QUESTION_ADD: 'question:add',
  QUESTION_EDIT: 'question:edit',
  QUESTION_DELETE: 'question:delete',
  QUESTION_IMPORT: 'question:import',
  QUESTION_EXPORT: 'question:export',
  QUESTION_AUDIT: 'question:audit',

  INTERVIEWBANK_VIEW: 'interviewbank:view',
  INTERVIEWBANK_UPLOAD: 'interviewbank:upload',
  INTERVIEWBANK_EDIT: 'interviewbank:edit',
  INTERVIEWBANK_DELETE: 'interviewbank:delete',
  INTERVIEWBANK_SEARCH_TEST: 'interviewbank:search_test',

  ROUTE_VIEW: 'route:view',
  ROUTE_ADD: 'route:add',
  ROUTE_EDIT: 'route:edit',
  ROUTE_DELETE: 'route:delete',

  STUDENT_VIEW: 'student:view',
  STUDENT_EDIT: 'student:edit',
  STUDENT_DELETE: 'student:delete',

  USER_VIEW: 'user:view',
  USER_ADD: 'user:add',
  USER_EDIT: 'user:edit',
  USER_DISABLE: 'user:disable',

  SYSTEM_LOG_VIEW: 'system:log_view',
  SYSTEM_SETTING: 'system:setting',

  DASHBOARD_VIEW: 'dashboard:view'
} as const

export type PermissionCode = (typeof PERMISSION)[keyof typeof PERMISSION]

export const ALL_PERMISSION_CODES: readonly PermissionCode[] = Object.values(PERMISSION)
