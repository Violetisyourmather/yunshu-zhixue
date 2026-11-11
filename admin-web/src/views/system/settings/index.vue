<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import {
  ElMessage,
  ElMessageBox,
  type FormInstance,
  type FormRules,
  type UploadFile
} from 'element-plus'
import { Delete, Plus } from '@element-plus/icons-vue'
import AdminPage from '@/components/AdminPage.vue'
import { ApiError } from '@/api/http'
import { getSettings, resetSettings, saveSettings } from '@/api/settings'
import { uploadImage } from '@/api/files'
import { useSiteStore } from '@/store/site'
import type { SystemSettings } from '@/types/system'

const siteStore = useSiteStore()

const loading = ref(true)
const saving = ref(false)
const resetting = ref(false)
const uploadingLogo = ref(false)
const uploadingAvatar = ref(false)

const formRef = ref<FormInstance>()
const form = reactive<SystemSettings>({
  siteName: '',
  siteDescription: '',
  logoUrl: '',
  defaultAvatar: ''
})

const rules: FormRules = {
  siteName: [
    { required: true, message: '请输入平台名称', trigger: 'blur' },
    { max: 100, message: '平台名称不能超过 100 个字符', trigger: 'blur' }
  ],
  siteDescription: [{ max: 500, message: '平台描述不能超过 500 个字符', trigger: 'blur' }]
}

function applySettings(data: SystemSettings): void {
  form.siteName = data.siteName
  form.siteDescription = data.siteDescription ?? ''
  form.logoUrl = data.logoUrl ?? ''
  form.defaultAvatar = data.defaultAvatar ?? ''
  // 同步全局品牌：保存后侧栏、登录页与浏览器标题立即生效
  siteStore.apply({ ...data, siteDescription: data.siteDescription ?? '' })
}

async function load(): Promise<void> {
  loading.value = true
  try {
    applySettings(await getSettings())
  } finally {
    loading.value = false
  }
}

async function handleSave(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  saving.value = true
  try {
    const result = await saveSettings({
      siteName: form.siteName.trim(),
      siteDescription: form.siteDescription.trim(),
      logoUrl: form.logoUrl,
      defaultAvatar: form.defaultAvatar
    })
    applySettings(result)
    ElMessage.success('设置已保存')
  } finally {
    saving.value = false
  }
}

async function handleReset(): Promise<void> {
  const confirmed = await ElMessageBox.confirm(
    '确定恢复默认设置吗？平台名称、描述、Logo 与默认头像将恢复为初始值。',
    '恢复默认设置',
    { type: 'warning', confirmButtonText: '恢复默认', cancelButtonText: '取消' }
  ).catch(() => false)
  if (!confirmed) {
    return
  }
  resetting.value = true
  try {
    applySettings(await resetSettings())
    ElMessage.success('已恢复默认设置')
  } finally {
    resetting.value = false
  }
}

function loadImage(src: string): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const image = new Image()
    image.onload = () => resolve(image)
    image.onerror = () => reject(new Error('图片读取失败'))
    image.src = src
  })
}

/** 等比缩放至不超过目标边长，统一输出 PNG（服务端限制 ≤256×256）。 */
async function resizeImage(file: File, maxSize: number): Promise<File> {
  if (!['image/jpeg', 'image/png'].includes(file.type)) {
    throw new Error('仅支持 JPG/PNG 格式的图片')
  }
  if (file.size > 5 * 1024 * 1024) {
    throw new Error('图片大小不能超过 5MB')
  }

  const objectUrl = URL.createObjectURL(file)
  try {
    const image = await loadImage(objectUrl)
    const scale = Math.min(1, maxSize / Math.max(image.naturalWidth, image.naturalHeight))
    const width = Math.max(1, Math.round(image.naturalWidth * scale))
    const height = Math.max(1, Math.round(image.naturalHeight * scale))

    const canvas = document.createElement('canvas')
    canvas.width = width
    canvas.height = height
    const context = canvas.getContext('2d')
    if (!context) {
      throw new Error('当前浏览器不支持图片处理')
    }
    if (file.type === 'image/jpeg') {
      context.fillStyle = '#ffffff'
      context.fillRect(0, 0, width, height)
    }
    context.drawImage(image, 0, 0, width, height)

    const blob = await new Promise<Blob | null>((resolve) => canvas.toBlob(resolve, 'image/png'))
    if (!blob) {
      throw new Error('图片处理失败')
    }
    return new File([blob], 'image.png', { type: 'image/png' })
  } finally {
    URL.revokeObjectURL(objectUrl)
  }
}

async function handleImageSelect(file: UploadFile, kind: 'logo' | 'avatar'): Promise<void> {
  const raw = file.raw
  if (!raw) {
    return
  }
  const flag = kind === 'logo' ? uploadingLogo : uploadingAvatar
  flag.value = true
  try {
    const processed = await resizeImage(raw, kind === 'logo' ? 256 : 200)
    const result = await uploadImage(processed)
    if (kind === 'logo') {
      form.logoUrl = result.url
    } else {
      form.defaultAvatar = result.url
    }
    ElMessage.success(kind === 'logo' ? 'Logo 已上传' : '默认头像已上传')
  } catch (error) {
    if (error instanceof ApiError) {
      return
    }
    ElMessage.error(error instanceof Error ? error.message : '图片上传失败')
  } finally {
    flag.value = false
  }
}

function onLogoChange(file: UploadFile): void {
  void handleImageSelect(file, 'logo')
}

function onAvatarChange(file: UploadFile): void {
  void handleImageSelect(file, 'avatar')
}

onMounted(load)
</script>

<template>
  <AdminPage title="系统设置" description="配置平台基本信息与展示内容，保存后应用于管理后台与学员端。">
    <div v-loading="loading" class="panel settings-panel">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="settings-form">
        <el-form-item label="平台名称" prop="siteName">
          <el-input
            v-model.trim="form.siteName"
            maxlength="100"
            show-word-limit
            placeholder="显示在浏览器标题与 Logo 旁"
          />
        </el-form-item>

        <el-form-item label="平台描述" prop="siteDescription">
          <el-input
            v-model="form.siteDescription"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="一句话介绍平台定位"
          />
        </el-form-item>

        <el-form-item label="Logo">
          <div class="uploader-row">
            <el-upload
              class="image-uploader"
              :show-file-list="false"
              :auto-upload="false"
              accept="image/png,image/jpeg"
              :on-change="onLogoChange"
            >
              <div v-loading="uploadingLogo" class="uploader-box">
                <img v-if="form.logoUrl" :src="form.logoUrl" alt="平台 Logo" />
                <div v-else class="uploader-empty">
                  <el-icon :size="20"><Plus /></el-icon>
                  <span>上传 Logo</span>
                </div>
              </div>
            </el-upload>
            <el-button
              v-if="form.logoUrl"
              text
              type="danger"
              size="small"
              :icon="Delete"
              @click="form.logoUrl = ''"
            >
              移除
            </el-button>
          </div>
          <div class="uploader-hint">建议 256×256，支持 JPG/PNG，不超过 5MB；超出尺寸将自动等比缩小。</div>
        </el-form-item>

        <el-form-item label="默认头像">
          <div class="uploader-row">
            <el-upload
              class="image-uploader"
              :show-file-list="false"
              :auto-upload="false"
              accept="image/png,image/jpeg"
              :on-change="onAvatarChange"
            >
              <div v-loading="uploadingAvatar" class="uploader-box avatar-box">
                <img v-if="form.defaultAvatar" :src="form.defaultAvatar" alt="默认头像" />
                <div v-else class="uploader-empty">
                  <el-icon :size="20"><Plus /></el-icon>
                  <span>上传头像</span>
                </div>
              </div>
            </el-upload>
            <el-button
              v-if="form.defaultAvatar"
              text
              type="danger"
              size="small"
              :icon="Delete"
              @click="form.defaultAvatar = ''"
            >
              移除
            </el-button>
          </div>
          <div class="uploader-hint">用户未设置头像时显示，建议 200×200，支持 JPG/PNG。</div>
        </el-form-item>

        <div class="settings-actions">
          <el-button :loading="resetting" @click="handleReset">重置</el-button>
          <el-button type="primary" :loading="saving" @click="handleSave">保存设置</el-button>
        </div>
      </el-form>
    </div>
  </AdminPage>
</template>

<style scoped>
.settings-panel {
  max-width: 860px;
  padding: 26px 28px;
}

.settings-form {
  max-width: 640px;
}

.uploader-row {
  display: flex;
  align-items: center;
  width: 100%;
  gap: 14px;
}

.uploader-box {
  display: grid;
  place-items: center;
  width: 112px;
  height: 112px;
  overflow: hidden;
  border: 1px dashed var(--line-2);
  border-radius: var(--r-sm);
  background: var(--surface-2);
  cursor: pointer;
  transition: border-color 0.16s ease, background-color 0.16s ease;
}

.uploader-box:hover {
  border-color: var(--jade);
  background: var(--jade-soft);
}

.uploader-box img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.avatar-box {
  border-radius: 50%;
}

.avatar-box img {
  border-radius: 50%;
  object-fit: cover;
}

.uploader-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  color: var(--ink-3);
  font-size: 12px;
}

.uploader-hint {
  flex-basis: 100%;
  margin-top: 8px;
  color: var(--ink-3);
  font-size: 12px;
  line-height: 1.5;
}

.settings-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 26px;
  padding-top: 18px;
  border-top: 1px dashed var(--line);
}
</style>
