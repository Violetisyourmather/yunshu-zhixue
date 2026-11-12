/** 内容管理（04 §5.3 学科管理 A-S01 ~ A-S11）接口类型。 */

export interface SubjectStats {
  subjectCount: number
  subjectWithStages: number
  stageCount: number
  questionCount: number
}

export interface StageItem {
  id: number
  name: string
  icon: string | null
  sortOrder: number
  /** 1 启用 / 0 停用 */
  status: number
  questionCount: number
}

export interface SubjectCard {
  id: number
  name: string
  icon: string
  description: string
  themeColor: string
  sortOrder: number
  courseCount: number
  routeCount: number
  questionCount: number
  stages: StageItem[]
}

export interface SubjectPayload {
  name: string
  icon: string
  description: string
  themeColor: string
  sortOrder: number
}

export interface StagePayload {
  name: string
  icon: string
  sortOrder: number
}

/* ---------- B站课程库（A-C01 ~ A-C08） ---------- */

export interface CourseStats {
  courseCount: number
  totalMinutes: number
  totalHours: number
  subjectCount: number
}

export interface SubjectRef {
  id: number
  name: string
}

export interface CourseItem {
  id: number
  name: string
  coverUrl: string | null
  intro: string | null
  durationMinutes: number
  /** 1 B站 / 2 其他 */
  source: number
  link: string | null
  bvid: string | null
  /** 1 上架 / 2 下架 */
  status: number
  /** 主学科；null = 其他学科 */
  subjectId: number | null
  /** 全部关联学科，主学科在前 */
  subjects: SubjectRef[]
}

export interface CourseQuery {
  keyword?: string
  subjectId?: number
  source?: number
  status?: number
  page: number
  pageSize: number
}

export interface CoursePayload {
  name: string
  coverUrl: string
  intro: string
  durationMinutes: number
  source: number
  link: string
  bvid: string
  status: number
  subjectId: number | null
  subjectIds: number[]
}

export type CourseImportRowStatus = 'valid' | 'warning' | 'invalid'

export interface CourseImportIssue {
  field: string
  reason: string
}

export interface CourseImportRow {
  row: number
  name: string
  durationMinutes: number | null
  subjectNames: string
  subjectId: number | null
  subjectIds: number[]
  source: number | null
  bvid: string | null
  coverUrl: string | null
  link: string | null
  status: CourseImportRowStatus
  errors: CourseImportIssue[]
  warnings: CourseImportIssue[]
}

export interface CourseImportPreview {
  token: string | null
  totalRows: number
  validCount: number
  warningCount: number
  invalidCount: number
  rows: CourseImportRow[]
}

export interface CourseImportResult {
  importedCount: number
}

/* ---------- 学习路线（A-R01 ~ A-R09） ---------- */

export interface RouteCoursePreview {
  id: number
  name: string
}

export interface RouteCard {
  id: number
  name: string
  icon: string | null
  subjectId: number
  subjectName: string | null
  /** 1 启用 / 0 停用 */
  status: number
  targetEducation: string | null
  description: string | null
  courseCount: number
  totalMinutes: number
  previewCourses: RouteCoursePreview[]
}

export interface RouteCourseItem {
  routeCourseId: number
  courseId: number
  name: string
  bvid: string | null
  durationMinutes: number
  /** 1 上架 / 2 下架 */
  status: number
  subjects: SubjectRef[]
  /** true = 本学科课程；false = 跨学科补充 */
  sameSubject: boolean
  sortOrder: number
}

export interface RouteDetail {
  id: number
  name: string
  icon: string | null
  subjectId: number
  subjectName: string | null
  status: number
  targetEducation: string | null
  description: string | null
  courseCount: number
  totalMinutes: number
  courses: RouteCourseItem[]
}

export interface RouteCourseOption {
  courseId: number
  name: string
  bvid: string | null
  durationMinutes: number
  status: number
  subjects: SubjectRef[]
  sameSubject: boolean
  added: boolean
}

export interface RoutePayload {
  name: string
  subjectId: number
  status: number
  icon: string
  targetEducation: string
  description: string
}

/* ---------- 题库管理（A-Q01 ~ A-Q15） ---------- */

export interface QuestionStats {
  total: number
  online: number
  pending: number
  singleChoice: number
  multipleChoice: number
  judge: number
  shortAnswer: number
}

export interface QuestionOptionItem {
  key: string
  text: string
  correct: boolean
}

export interface QuestionItem {
  id: number
  content: string
  stageId: number
  stageName: string | null
  subjectId: number
  subjectName: string | null
  /** 1 单选 / 2 多选 / 3 判断 / 4 简答 */
  type: number
  /** 1 简单 / 2 中等 / 3 困难 */
  difficulty: number
  /** 1 已上架 / 2 已下架 / 3 待审核 / 5 已驳回 */
  status: number
  rejectReason: string | null
  tags: string | null
  createdAt: string
  doneCount: number
  correctCount: number
  accuracyRate: number
}

export interface QuestionDetail extends QuestionItem {
  answer: string | null
  analysis: string | null
  options: QuestionOptionItem[]
}

export interface QuestionStageOption {
  id: number
  name: string
  status: number
}

export interface QuestionStageGroup {
  subjectId: number
  subjectName: string
  stages: QuestionStageOption[]
}

export interface QuestionQuery {
  keyword?: string
  subjectId?: number
  stageId?: number
  type?: number
  difficulty?: number
  status?: number
  /** ID 正序 asc / 倒序 desc */
  sort?: 'asc' | 'desc'
  page: number
  pageSize: number
}

export interface QuestionOptionPayload {
  key: string
  text: string
}

export interface QuestionPayload {
  stageId: number
  type: number
  difficulty: number
  content: string
  analysis: string
  tags: string
  options: QuestionOptionPayload[]
  answerKeys: string[]
  answer: string
}

export interface QuestionBatchResult {
  updated: number
  skipped: number
}

export type QuestionImportRowStatus = 'valid' | 'warning' | 'invalid'

export interface QuestionImportIssue {
  field: string
  reason: string
}

export interface QuestionImportOption {
  key: string
  text: string
  correct: boolean
}

export interface QuestionImportRow {
  sheetNo: number
  sheetName: string
  row: number
  content: string | null
  stageName: string | null
  stageId: number | null
  newStage: boolean
  type: number | null
  difficulty: number | null
  options: QuestionImportOption[]
  answerKeys: string[]
  answer: string | null
  analysis: string | null
  tags: string | null
  status: QuestionImportRowStatus
  errors: QuestionImportIssue[]
  warnings: QuestionImportIssue[]
}

export interface QuestionImportPreview {
  token: string | null
  subjectId: number
  subjectName: string
  totalRows: number
  validCount: number
  warningCount: number
  invalidCount: number
  newStageNames: string[]
  rows: QuestionImportRow[]
}

export interface QuestionImportResult {
  importedCount: number
  createdStageCount: number
}

/* ---------- 面试题库（A-I01 ~ A-I13） ---------- */

export interface InterviewStats {
  total: number
  directionCount: number
  unclassifiedCount: number
  todayAdded: number
}

export interface InterviewSubjectOption {
  id: number
  name: string
}

export interface InterviewQuestionItem {
  id: number
  content: string
  subjectId: number
  subjectName: string | null
  topic: string | null
  difficulty: number
  tags: string | null
  /** 1 文档解析 / 2 手工录入 / 3 手动导入 */
  sourceType: number
  sourceDocName: string | null
  sourceSeq: number | null
  status: number
  createdAt: string
}

export interface InterviewQuestionDetail extends InterviewQuestionItem {
  referenceAnswer: string | null
}

export interface InterviewTopicItem {
  id: number
  name: string
  subjectId: number | null
  subjectName: string | null
  questionCount: number
  sortOrder: number
}

export interface InterviewQuestionQuery {
  keyword?: string
  subjectId?: number
  topicId?: number
  difficulty?: number
  unclassified?: boolean
  page: number
  pageSize: number
}

export interface InterviewQuestionCreatePayload {
  subjectId: number
  content: string
  referenceAnswer: string
  topic: string
  difficulty: number
  tags: string
}

export interface InterviewQuestionUpdatePayload {
  topic: string
  difficulty: number
}

export interface InterviewTopicPayload {
  name: string
  subjectId: number | null
}

export type InterviewImportRowStatus = 'valid' | 'warning' | 'invalid'

export interface InterviewImportIssue {
  field: string
  reason: string
}

export interface InterviewImportRow {
  sheetNo: number
  sheetName: string
  row: number
  subjectName: string | null
  subjectId: number | null
  content: string | null
  difficulty: number | null
  topic: string | null
  newTopic: boolean
  tags: string | null
  referenceAnswer: string | null
  status: InterviewImportRowStatus
  errors: InterviewImportIssue[]
  warnings: InterviewImportIssue[]
}

export interface InterviewImportPreview {
  token: string | null
  fileName: string
  totalRows: number
  validCount: number
  warningCount: number
  invalidCount: number
  newTopics: string[]
  rows: InterviewImportRow[]
}

export interface InterviewImportResult {
  importedCount: number
  createdTopicCount: number
}

/* ---------- 面试题 Markdown 导入 / 检索测试 ---------- */

export interface InterviewMarkdownSample {
  seq: number
  title: string
  answerPreview: string
  line: number
}

export interface InterviewMarkdownPreview {
  token: string
  fileName: string
  fileSize: number
  questionCount: number
  samples: InterviewMarkdownSample[]
}

export interface InterviewMarkdownConfirmPayload {
  token: string
  subjectId: number
  difficulty: number
  topic?: string
  tags?: string
}

export interface InterviewMarkdownResult {
  importedCount: number
}

export interface InterviewSearchResultItem {
  id: number
  content: string
  referenceAnswer: string | null
  subjectId: number
  subjectName: string | null
  topic: string | null
  difficulty: number
  sourceDocName: string | null
  sourceSeq: number | null
}

/* ---------- 学员管理（A-U01 ~ A-U08） ---------- */

export interface StudentItem {
  id: number
  nickname: string
  mobile: string
  avatar: string | null
  /** 1 已启用 / 0 已禁用 */
  status: number
  createdAt: string
  lastLoginAt: string | null
  learningDays: number
  practiceCount: number
  interviewCount: number
}

export interface StudentDetail {
  id: number
  nickname: string
  mobile: string
  email: string | null
  avatar: string | null
  gender: number
  status: number
  createdAt: string
  lastLoginAt: string | null
  learningDays: number
  completedCourseCount: number
  practiceCount: number
  accuracyRate: number
  interviewCount: number
}

export interface StudentInterviewItem {
  id: number
  subjectId: number
  subjectName: string | null
  difficulty: number
  questionCount: number
  /** 0–10 */
  overallScore: number | null
  /** 秒 */
  totalDuration: number | null
  startedAt: string
  completedAt: string
}

export interface StudentQuery {
  keyword?: string
  status?: number
  page: number
  pageSize: number
}
