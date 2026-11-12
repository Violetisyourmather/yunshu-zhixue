<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ApiError } from '@/api/http'
import {
  completeRouteCourse,
  getRouteDetail,
  getRoutes,
  getRouteSubjects,
  uncompleteRouteCourse,
  type RouteCardItem,
  type RouteCourseItem,
  type RouteDetail,
  type RouteSubjectItem
} from '@/api/routes'

const currentRoute = useRoute()

const loading = ref(true)
const loadError = ref('')
const subjects = ref<RouteSubjectItem[]>([])
const routes = ref<RouteCardItem[]>([])
const selectedSubjectId = ref<number | null>(null)
const detail = ref<RouteDetail | null>(null)
const loadingDetail = ref(false)
const detailError = ref('')
const pending = reactive(new Set<number>())
const failedCovers = reactive(new Set<number>())

const selectedSubject = computed(
  () => subjects.value.find((subject) => subject.subjectId === selectedSubjectId.value) ?? null
)
const courses = computed(() => detail.value?.courses ?? [])
const completedCount = computed(() => courses.value.filter((course) => course.completed).length)
const totalMinutes = computed(() =>
  courses.value.reduce((total, course) => total + course.durationMinutes, 0)
)
const progress = computed(() => {
  if (!courses.value.length) {
    return 0
  }
  return Math.round((completedCount.value * 1000) / courses.value.length) / 10
})

function messageOf(error: unknown, fallback: string): string {
  return error instanceof ApiError ? error.message : fallback
}

function pad(value: number): string {
  return String(value).padStart(2, '0')
}

function formatDuration(minutes: number): string {
  if (minutes <= 0) {
    return '0 分钟'
  }
  const hours = Math.floor(minutes / 60)
  const rest = minutes % 60
  if (hours > 0 && rest > 0) {
    return `${hours} 小时 ${rest} 分钟`
  }
  if (hours > 0) {
    return `${hours} 小时`
  }
  return `${rest} 分钟`
}

function formatTotalDuration(minutes: number): string {
  if (minutes <= 0) {
    return '0 小时'
  }
  if (minutes < 60) {
    return `${minutes} 分钟`
  }
  const hours = minutes / 60
  return `${hours.toFixed(1).replace(/\.0$/, '')} 小时`
}

function formatProgress(value: number): string {
  return Number.isInteger(value) ? `${value}%` : `${value.toFixed(1)}%`
}

function bilibiliUrl(bvid: string): string {
  return `https://www.bilibili.com/video/${bvid}`
}

/**
 * B 站封面存在防盗链且历史数据可能为 HTTP：
 * 统一升级为 HTTPS，配合 img 的 noreferrer 策略避免 403 与混合内容拦截。
 */
function normalizeCoverUrl(url: string | null): string {
  if (!url) {
    return ''
  }
  if (/^http:\/\/[^/]*\.hdslb\.com\//.test(url)) {
    return url.replace(/^http:/, 'https:')
  }
  return url
}

function resolveInitialSubject(): number | null {
  const raw = currentRoute.query.subjectId ?? currentRoute.query.s
  const queryId = Number(raw)
  if (Number.isInteger(queryId) && subjects.value.some((subject) => subject.subjectId === queryId)) {
    return queryId
  }
  const withRoute = subjects.value.find((subject) => subject.routeId != null)
  return withRoute?.subjectId ?? subjects.value[0]?.subjectId ?? null
}

async function loadDetail(): Promise<void> {
  detail.value = null
  detailError.value = ''
  const subject = selectedSubject.value
  if (!subject) {
    return
  }
  const route = routes.value.find((item) => item.subjectId === subject.subjectId)
  const routeId = route?.id ?? subject.routeId
  if (!routeId) {
    return
  }

  loadingDetail.value = true
  try {
    detail.value = await getRouteDetail(routeId)
  } catch (error) {
    detailError.value = messageOf(error, '路线详情加载失败，请稍后重试')
  } finally {
    loadingDetail.value = false
  }
}

async function load(): Promise<void> {
  loading.value = true
  loadError.value = ''
  try {
    const [subjectData, routeData] = await Promise.all([getRouteSubjects(), getRoutes()])
    subjects.value = subjectData
    routes.value = routeData
    selectedSubjectId.value = resolveInitialSubject()
    await loadDetail()
  } catch (error) {
    loadError.value = messageOf(error, '学习路线加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

function selectSubject(subjectId: number): void {
  if (selectedSubjectId.value === subjectId) {
    return
  }
  selectedSubjectId.value = subjectId
  void loadDetail()
}

function syncRouteProgress(): void {
  const route = routes.value.find((item) => item.id === detail.value?.id)
  if (!route) {
    return
  }
  route.completedCourseCount = completedCount.value
  route.progress = progress.value
}

async function toggleCourse(course: RouteCourseItem, event: Event): Promise<void> {
  const target = event.target as HTMLInputElement
  const nextCompleted = target.checked
  if (pending.has(course.routeCourseId)) {
    target.checked = course.completed
    return
  }

  pending.add(course.routeCourseId)
  try {
    if (nextCompleted) {
      await completeRouteCourse(course.routeCourseId, course.courseId)
    } else {
      await uncompleteRouteCourse(course.routeCourseId)
    }
    course.completed = nextCompleted
    syncRouteProgress()
    ElMessage.success(nextCompleted ? '已标记完成' : '已取消完成')
  } catch (error) {
    target.checked = course.completed
    ElMessage.error(messageOf(error, '操作失败，请稍后重试'))
  } finally {
    pending.delete(course.routeCourseId)
  }
}

onMounted(() => {
  void load()
})
</script>

<template>
  <section class="routes-page">
    <header class="page-head">
      <div>
        <p class="eyebrow">Learning Route</p>
        <h1>学习路线</h1>
        <p class="lead">选一个方向，按顺序把课程走完。课程都来自 B 站，点击即跳转。</p>
      </div>
      <RouterLink class="btn" to="/bank">去刷题 →</RouterLink>
    </header>

    <div v-if="loading" class="state-panel">正在加载学习路线…</div>

    <div v-else-if="loadError" class="state-panel state-error">
      <p>{{ loadError }}</p>
      <button type="button" class="btn btn-sm" @click="load">重新加载</button>
    </div>

    <template v-else>
      <div v-if="subjects.length" class="subject-grid" role="list" aria-label="选择学科">
        <button
          v-for="subject in subjects"
          :key="subject.subjectId"
          type="button"
          class="subject-tile"
          :class="{ active: subject.subjectId === selectedSubjectId }"
          role="listitem"
          :aria-pressed="subject.subjectId === selectedSubjectId"
          @click="selectSubject(subject.subjectId)"
        >
          <span class="subject-icon" aria-hidden="true">{{ subject.icon }}</span>
          <span class="subject-name">{{ subject.subjectName }}</span>
          <span class="subject-count">{{ subject.courseCount }} 门</span>
        </button>
      </div>

      <div v-else class="empty-panel">请选择一个学科方向</div>

      <template v-if="selectedSubject">
        <div v-if="loadingDetail" class="state-panel">正在加载路线…</div>

        <div v-else-if="detailError" class="state-panel state-error">
          <p>{{ detailError }}</p>
          <button type="button" class="btn btn-sm" @click="loadDetail">重新加载</button>
        </div>

        <section v-else-if="detail" class="route-head">
          <div class="route-head-main">
            <div class="route-title">
              <span class="badge badge-jade">按顺序学习</span>
              <h2>{{ detail.name }}</h2>
              <p class="route-meta">
                <span v-if="detail.targetEducation">适合人群：{{ detail.targetEducation }}</span>
                <span v-if="detail.description">{{ detail.description }}</span>
              </p>
            </div>
            <div class="progress-line">
              <div class="progress-track">
                <i :style="{ width: `${progress}%` }" />
              </div>
              <span class="small muted">{{ formatProgress(progress) }}</span>
            </div>
          </div>
          <div class="route-stats">
            <div>
              <div class="v">{{ courses.length }}</div>
              <div class="k">门课程</div>
            </div>
            <div>
              <div class="v">{{ formatTotalDuration(totalMinutes) }}</div>
              <div class="k">总计时长</div>
            </div>
            <div>
              <div class="v">{{ completedCount }}</div>
              <div class="k">已完成</div>
            </div>
          </div>
        </section>

        <div v-else class="empty-panel">
          {{ selectedSubject.subjectName }}暂无学习路线，课程正在筹备中。
        </div>

        <div v-if="detail && courses.length" class="course-list">
          <article
            v-for="(course, index) in courses"
            :key="course.routeCourseId"
            class="course-row"
            :class="{ 'is-done': course.completed }"
          >
            <span class="course-index" aria-hidden="true">{{ pad(index + 1) }}</span>

            <div class="course-cover">
              <img
                v-if="course.coverUrl && !failedCovers.has(course.courseId)"
                :src="normalizeCoverUrl(course.coverUrl)"
                alt=""
                loading="lazy"
                referrerpolicy="no-referrer"
                @error="failedCovers.add(course.courseId)"
              />
              <span v-else class="cover-fallback">{{ course.source === 1 ? 'B站' : '课程' }}</span>
            </div>

            <div class="course-copy">
              <h3 :title="course.name">{{ course.name }}</h3>
              <p class="course-meta">
                <span v-if="course.bvid">{{ course.bvid }}</span>
                <span>{{ formatDuration(course.durationMinutes) }}</span>
              </p>
            </div>

            <div class="course-actions">
              <a
                v-if="course.bvid"
                class="btn btn-sm"
                :href="bilibiliUrl(course.bvid)"
                target="_blank"
                rel="noopener noreferrer"
              >
                去B站学习 ↗
              </a>
              <button v-else type="button" class="btn btn-sm" disabled>暂无链接</button>

              <label class="course-check">
                <input
                  type="checkbox"
                  :checked="course.completed"
                  :disabled="pending.has(course.routeCourseId)"
                  @change="toggleCourse(course, $event)"
                />
                标记完成
              </label>
            </div>
          </article>
        </div>

        <div v-else-if="detail" class="empty-panel">课程筹备中，敬请期待。</div>

        <p v-if="detail" class="route-note">
          路线按列表顺序学习；平台不记录观看进度，只记录你是否勾选完成。
        </p>
      </template>
    </template>
  </section>
</template>

<style scoped>
.routes-page {
  padding: 40px 0 24px;
}

.page-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
}

.page-head h1 {
  margin-top: 6px;
  font-family: var(--font-display);
  font-size: clamp(30px, 3.2vw, 38px);
  line-height: 1.2;
  letter-spacing: 0;
}

.page-head .lead {
  max-width: 620px;
  margin-top: 8px;
  color: var(--ink-2);
  font-size: 14px;
  line-height: 1.75;
  text-wrap: pretty;
}

.state-panel {
  margin-top: 22px;
  padding: 24px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r);
  color: var(--ink-3);
  font-size: 13px;
}

.state-error {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  color: var(--red);
}

.empty-panel {
  margin-top: 18px;
  padding: 36px 24px;
  background: var(--surface-2);
  border: 1px dashed var(--line-2);
  border-radius: var(--r);
  color: var(--ink-3);
  font-size: 13px;
  text-align: center;
}

/* ---------- 学科选择 ---------- */
.subject-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(112px, 1fr));
  gap: 12px;
  margin-top: 24px;
}

.subject-tile {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 14px 10px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r);
  transition: transform 0.16s ease, border-color 0.16s ease, background-color 0.16s ease,
    color 0.16s ease;
}

.subject-tile.active {
  background: var(--ink);
  border-color: var(--ink);
  color: #fff;
}

.subject-icon {
  font-size: 20px;
  line-height: 1.2;
}

.subject-name {
  max-width: 100%;
  font-size: 12.5px;
  line-height: 1.4;
  text-align: center;
}

.subject-count {
  color: var(--ink-3);
  font-size: 11px;
}

.subject-tile.active .subject-count {
  color: rgba(255, 255, 255, 0.62);
}

/* ---------- 路线头 ---------- */
.route-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 28px;
  margin-top: 18px;
  padding: 26px 28px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r);
  box-shadow: var(--shadow);
}

.route-head-main {
  flex: 1;
  min-width: 0;
}

.route-title h2 {
  margin-top: 10px;
  font-family: var(--font-display);
  font-size: clamp(24px, 2.5vw, 30px);
  line-height: 1.35;
  letter-spacing: 0;
}

.badge {
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 8px;
  border-radius: 6px;
  background: var(--paper-2);
  font-size: 11.5px;
  font-weight: 500;
}

.badge-jade {
  background: var(--jade-soft);
  color: var(--jade-2);
}

.route-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-top: 8px;
  color: var(--ink-3);
  font-size: 12.5px;
  line-height: 1.7;
}

.route-stats {
  display: flex;
  gap: 28px;
  flex: none;
  text-align: right;
}

.route-stats .v {
  font-family: var(--font-display);
  font-size: 26px;
  line-height: 1.15;
  letter-spacing: -0.01em;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}

.route-stats .k {
  margin-top: 2px;
  color: var(--ink-3);
  font-size: 11.5px;
  white-space: nowrap;
}

.progress-line {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 18px;
}

.progress-track {
  flex: 1;
  max-width: 280px;
  height: 6px;
  overflow: hidden;
  border-radius: 99px;
  background: var(--paper-2);
}

.progress-track i {
  display: block;
  height: 100%;
  border-radius: 99px;
  background: var(--jade);
  transition: width 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}

/* ---------- 课程列表 ---------- */
.course-list {
  display: grid;
  gap: 10px;
  margin-top: 14px;
}

.course-row {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 18px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r);
  transition: border-color 0.16s ease, transform 0.16s ease;
}

.course-index {
  width: 24px;
  flex: none;
  color: var(--ink-4);
  font-family: var(--font-mono);
  font-size: 12px;
}

.course-cover {
  display: grid;
  place-items: center;
  width: 78px;
  height: 54px;
  flex: none;
  overflow: hidden;
  background: var(--paper-2);
  border-radius: 8px;
}

.course-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-fallback {
  color: var(--jade-2);
  font-size: 11px;
  font-weight: 500;
}

.course-copy {
  flex: 1;
  min-width: 0;
}

.course-copy h3 {
  display: -webkit-box;
  overflow: hidden;
  font-size: 14.5px;
  line-height: 1.5;
  letter-spacing: 0;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.course-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 4px;
  color: var(--ink-3);
  font-size: 11.5px;
}

.course-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: none;
}

.course-check {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--ink-2);
  font-size: 12.5px;
  white-space: nowrap;
  cursor: pointer;
}

.course-check input {
  width: 15px;
  height: 15px;
  accent-color: var(--jade);
}

.is-done .course-index {
  color: var(--jade);
}

.is-done .course-copy h3 {
  color: var(--ink-3);
}

.route-note {
  margin-top: 14px;
  color: var(--ink-3);
  font-size: 11.5px;
  line-height: 1.7;
}

:where(.subject-tile, .course-check, .state-error .btn, .page-head .btn):focus-visible {
  outline: 2px solid var(--jade);
  outline-offset: 3px;
}

@media (hover: hover) {
  .subject-tile:hover {
    transform: translateY(-2px);
    border-color: var(--line-2);
  }

  .subject-tile.active:hover {
    border-color: var(--ink);
  }

  .course-row:hover {
    transform: translateY(-1px);
    border-color: var(--line-2);
  }
}

@media (max-width: 900px) {
  .route-head {
    flex-direction: column;
    gap: 18px;
  }

  .route-stats {
    width: 100%;
    justify-content: space-between;
    gap: 16px;
    text-align: left;
  }

  .progress-track {
    max-width: none;
  }
}

@media (max-width: 760px) {
  .routes-page {
    padding-top: 24px;
  }

  .page-head {
    flex-direction: column;
    align-items: flex-start;
    gap: 14px;
  }

  .subject-grid {
    grid-template-columns: repeat(3, 1fr);
    gap: 10px;
  }

  .subject-tile {
    padding: 12px 8px;
  }

  .course-row {
    flex-wrap: wrap;
    gap: 12px;
  }

  .course-cover {
    width: 64px;
    height: 48px;
  }

  .course-copy {
    flex: 1 1 150px;
  }

  .course-actions {
    width: 100%;
    justify-content: flex-end;
  }

  .state-error {
    flex-direction: column;
    align-items: flex-start;
  }
}

@media (max-width: 520px) {
  .subject-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .course-actions {
    justify-content: space-between;
  }
}

@media (prefers-reduced-motion: reduce) {
  .subject-tile,
  .course-row,
  .progress-track i {
    transition: none;
  }
}
</style>
