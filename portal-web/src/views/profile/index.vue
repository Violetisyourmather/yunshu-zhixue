<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { ApiError } from '@/api/http'
import { updateProfile, uploadAvatar } from '@/api/profile'
import { useSiteStore } from '@/store/site'
import { useUserStore } from '@/store/user'
import type { StudentProfile } from '@/types/api'

const userStore = useUserStore()
const siteStore = useSiteStore()

const loading = ref(true)
const loadError = ref('')
const saving = ref(false)
const uploading = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)
const form = reactive({
  nickname: '',
  email: '',
  gender: 0,
  avatar: ''
})

const emailPattern = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/

const profile = computed(() => userStore.profile)
const avatarSrc = computed(() => form.avatar || siteStore.defaultAvatar)
const avatarInitial = computed(() => (form.nickname || profile.value?.nickname || '云').slice(0, 1))
const stats = computed(
  () => profile.value?.stats ?? { practiceCount: 0, interviewCount: 0, wrongQuestionCount: 0 }
)
const study = computed(
  () => profile.value?.study ?? { totalDays: 0, accuracyRate: 0, completedCourseCount: 0 }
)

const statItems = computed(() => [
  { label: '做题数', value: stats.value.practiceCount },
  { label: '面试数', value: stats.value.interviewCount },
  { label: '错题数', value: stats.value.wrongQuestionCount }
])

const learningRows = computed(() => [
  { label: '已完成题目', value: `${stats.value.practiceCount} 题` },
  { label: '平均正确率', value: formatAccuracy(study.value.accuracyRate) },
  { label: 'AI 面试次数', value: `${stats.value.interviewCount} 次` },
  { label: '已学课程', value: `${study.value.completedCourseCount} 门` }
])

function formatAccuracy(value: number): string {
  return Number.isInteger(value) ? `${value}%` : `${value.toFixed(1)}%`
}

function messageOf(error: unknown, fallback: string): string {
  return error instanceof ApiError ? error.message : fallback
}

function applyProfile(data: StudentProfile): void {
  form.nickname = data.nickname
  form.email = data.email ?? ''
  form.gender = data.gender
  form.avatar = data.avatar ?? ''
}

async function load(): Promise<void> {
  loading.value = true
  loadError.value = ''
  try {
    await userStore.loadProfile()
    if (userStore.profile) {
      applyProfile(userStore.profile)
    } else if (!userStore.isLoggedIn) {
      loadError.value = '登录状态已失效，请重新登录'
    }
  } catch (error) {
    loadError.value = messageOf(error, '个人资料加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

async function handleSave(): Promise<void> {
  const nickname = form.nickname.trim()
  if (!nickname || nickname.length > 20) {
    ElMessage.warning('用户名长度为 1~20 个字符')
    return
  }
  const email = form.email.trim()
  if (email && !emailPattern.test(email)) {
    ElMessage.warning('邮箱格式不正确')
    return
  }

  saving.value = true
  try {
    await updateProfile({
      nickname,
      email: email || null,
      gender: form.gender,
      avatar: form.avatar || null
    })
    ElMessage.success('资料已保存')
    try {
      await userStore.loadProfile()
      if (userStore.profile) {
        applyProfile(userStore.profile)
      }
    } catch {
      // 保存已成功，刷新失败不阻断本地表单
    }
  } catch (error) {
    ElMessage.error(messageOf(error, '保存失败，请稍后重试'))
  } finally {
    saving.value = false
  }
}

function pickAvatar(): void {
  fileInput.value?.click()
}

async function handleAvatarChange(event: Event): Promise<void> {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) {
    return
  }
  if (!['image/jpeg', 'image/png'].includes(file.type)) {
    ElMessage.warning('仅支持 JPG / PNG 图片')
    return
  }

  uploading.value = true
  try {
    const result = await uploadAvatar(file)
    form.avatar = result.url
    ElMessage.success('头像已上传，保存后生效')
  } catch (error) {
    ElMessage.error(messageOf(error, '头像上传失败，请稍后重试'))
  } finally {
    uploading.value = false
  }
}

onMounted(() => {
  void load()
})
</script>

<template>
  <section class="profile-page">
    <header class="page-head">
      <p class="eyebrow">Profile</p>
      <h1>个人中心</h1>
    </header>

    <div v-if="loading" class="state-panel">正在加载个人资料…</div>

    <div v-else-if="loadError" class="state-panel state-error">
      <p>{{ loadError }}</p>
      <button type="button" class="btn btn-sm" @click="load">重新加载</button>
    </div>

    <template v-else>
      <section class="profile-card">
        <div class="profile-identity">
          <img v-if="avatarSrc" class="profile-avatar" :src="avatarSrc" alt="用户头像" />
          <span v-else class="profile-avatar avatar-fallback" aria-hidden="true">{{ avatarInitial }}</span>
          <div class="identity-copy">
            <h2>{{ form.nickname || profile?.nickname }}</h2>
            <div class="identity-meta">
              <span class="badge badge-jade">学员</span>
              <span class="muted small">{{ profile?.mobile }}</span>
            </div>
          </div>
        </div>

        <div class="profile-stats">
          <div v-for="item in statItems" :key="item.label">
            <div class="v">{{ item.value }}</div>
            <div class="k">{{ item.label }}</div>
          </div>
        </div>
      </section>

      <div class="profile-grid">
        <section class="panel">
          <header class="panel-head">
            <h3>基本信息</h3>
            <span class="small muted">手机号不可修改</span>
          </header>
          <div class="panel-body">
            <div class="form-grid">
              <div class="field">
                <label for="profile-nickname">用户名</label>
                <input
                  id="profile-nickname"
                  v-model.trim="form.nickname"
                  class="input"
                  maxlength="20"
                  autocomplete="nickname"
                  placeholder="请输入用户名"
                />
              </div>
              <div class="field">
                <label for="profile-gender">性别</label>
                <select id="profile-gender" v-model.number="form.gender" class="select">
                  <option :value="0">保密</option>
                  <option :value="1">男</option>
                  <option :value="2">女</option>
                </select>
              </div>
              <div class="field">
                <label for="profile-mobile">手机号</label>
                <input id="profile-mobile" class="input" :value="profile?.mobile" disabled />
              </div>
              <div class="field">
                <label for="profile-email">邮箱</label>
                <input
                  id="profile-email"
                  v-model.trim="form.email"
                  class="input"
                  autocomplete="email"
                  placeholder="选填，用于接收通知"
                />
              </div>
            </div>

            <div class="field avatar-field">
              <label>头像</label>
              <div class="avatar-row">
                <img v-if="avatarSrc" class="avatar-preview" :src="avatarSrc" alt="头像预览" />
                <span v-else class="avatar-preview avatar-fallback" aria-hidden="true">{{ avatarInitial }}</span>
                <input
                  ref="fileInput"
                  class="visually-hidden"
                  type="file"
                  accept="image/jpeg,image/png"
                  @change="handleAvatarChange"
                />
                <button type="button" class="btn btn-sm" :disabled="uploading" @click="pickAvatar">
                  {{ uploading ? '上传中…' : '上传头像' }}
                </button>
                <span class="small muted">JPG / PNG，尺寸不限，建议 400×400</span>
              </div>
            </div>

            <button type="button" class="btn btn-primary" :disabled="saving" @click="handleSave">
              {{ saving ? '保存中…' : '保存修改' }}
            </button>
          </div>
        </section>

        <section class="panel">
          <header class="panel-head">
            <h3>学习数据</h3>
          </header>
          <div class="panel-body">
            <div class="learning-list">
              <div v-for="row in learningRows" :key="row.label" class="learning-row">
                <span class="muted small">{{ row.label }}</span>
                <strong>{{ row.value }}</strong>
              </div>
            </div>
            <div class="divider" />
            <p class="small muted">统计口径：作答记录与面试提交记录，仅统计服务端完成的数据。</p>
          </div>
        </section>
      </div>
    </template>
  </section>
</template>

<style scoped>
.profile-page {
  padding: 40px 0 24px;
}

.page-head h1 {
  margin-top: 6px;
  font-family: var(--font-display);
  font-size: clamp(30px, 3vw, 36px);
  letter-spacing: 0;
}

.state-panel {
  margin-top: 24px;
  padding: 28px;
  border: 1px solid var(--line);
  border-radius: var(--r);
  background: var(--surface);
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

.profile-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 28px;
  margin-top: 24px;
  padding: 26px 30px;
  border: 1px solid var(--line);
  border-radius: var(--r);
  background: var(--surface);
  box-shadow: var(--shadow);
}

.profile-identity {
  display: flex;
  align-items: center;
  gap: 18px;
  min-width: 0;
}

.profile-avatar {
  width: 64px;
  height: 64px;
  border: 1px solid var(--line);
  border-radius: 50%;
  background: var(--paper-2);
  object-fit: cover;
  flex: none;
}

.avatar-fallback {
  display: inline-grid;
  place-items: center;
  background: var(--jade-soft);
  color: var(--jade-2);
  font-family: var(--font-display);
  font-size: 24px;
}

.identity-copy {
  min-width: 0;
}

.identity-copy h2 {
  font-family: var(--font-display);
  font-size: 26px;
  line-height: 1.3;
  letter-spacing: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.identity-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 7px;
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

.profile-stats {
  display: flex;
  gap: 36px;
  flex: none;
  text-align: right;
}

.profile-stats .v {
  font-family: var(--font-display);
  font-size: 28px;
  line-height: 1.1;
  letter-spacing: -0.01em;
  font-variant-numeric: tabular-nums;
}

.profile-stats .k {
  margin-top: 3px;
  color: var(--ink-3);
  font-size: 11.5px;
}

.profile-grid {
  display: grid;
  grid-template-columns: 1.4fr 1fr;
  gap: 18px;
  margin-top: 18px;
  align-items: start;
}

.panel {
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r);
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 16px 22px;
  border-bottom: 1px solid var(--line);
}

.panel-head h3 {
  font-size: 14.5px;
  letter-spacing: 0;
}

.panel-body {
  padding: 22px;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 18px;
}

.avatar-field {
  margin-top: 2px;
}

.avatar-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.avatar-preview {
  width: 48px;
  height: 48px;
  border: 1px solid var(--line);
  border-radius: 50%;
  background: var(--paper-2);
  object-fit: cover;
  flex: none;
  font-family: var(--font-display);
  font-size: 18px;
}

.visually-hidden {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

.learning-list {
  display: grid;
  gap: 15px;
}

.learning-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 16px;
}

.learning-row strong {
  font-family: var(--font-display);
  font-size: 17px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.divider {
  height: 1px;
  margin: 20px 0 16px;
  background: var(--line);
}

@media (max-width: 960px) {
  .profile-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 700px) {
  .profile-page {
    padding-top: 24px;
  }

  .profile-card {
    flex-direction: column;
    align-items: flex-start;
    padding: 22px;
  }

  .profile-stats {
    width: 100%;
    justify-content: space-between;
    gap: 16px;
    text-align: left;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .panel-body {
    padding: 18px;
  }
}
</style>
