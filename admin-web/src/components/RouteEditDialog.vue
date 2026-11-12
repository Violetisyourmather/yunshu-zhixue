<script setup lang="ts">
import { computed, nextTick, reactive, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { createRoute, listRouteSubjectOptions, updateRoute } from '@/api/routes'
import type { RouteCard, RouteDetail, RoutePayload, SubjectRef } from '@/types/content'

const props = defineProps<{
  modelValue: boolean
  route?: RouteCard | RouteDetail | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'saved'): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})

const isEdit = computed(() => Boolean(props.route))
const dialogTitle = computed(() => (isEdit.value ? '编辑学习路线' : '新建学习路线'))

const subjectOptions = ref<SubjectRef[]>([])
const formRef = ref<FormInstance>()
const submitting = ref(false)

const EDUCATION_OPTIONS = ['不限', '大专及以上', '本科及以上', '硕士及以上']

const form = reactive({
  name: '',
  subjectId: undefined as number | undefined,
  status: 1,
  icon: '🗺️',
  targetEducation: '不限',
  description: ''
})

const enabled = computed({
  get: () => form.status === 1,
  set: (value: boolean) => {
    form.status = value ? 1 : 0
  }
})

const rules: FormRules = {
  name: [
    { required: true, message: '请输入路线名称', trigger: 'blur' },
    { max: 100, message: '路线名称不能超过 100 个字符', trigger: 'blur' }
  ],
  subjectId: [{ required: true, message: '请选择关联学科', trigger: 'change' }],
  icon: [{ max: 50, message: '路线图标不能超过 50 个字符', trigger: 'blur' }],
  targetEducation: [{ max: 100, message: '适合学历不能超过 100 个字符', trigger: 'blur' }],
  description: [{ max: 1000, message: '路线描述不能超过 1000 个字符', trigger: 'blur' }]
}

async function ensureSubjectOptions(): Promise<void> {
  if (!subjectOptions.value.length) {
    subjectOptions.value = await listRouteSubjectOptions()
  }
}

function fillForm(): void {
  if (props.route) {
    form.name = props.route.name
    form.subjectId = props.route.subjectId
    form.status = props.route.status
    form.icon = props.route.icon ?? '🗺️'
    form.targetEducation = props.route.targetEducation ?? '不限'
    form.description = props.route.description ?? ''
    return
  }
  form.name = ''
  form.subjectId = undefined
  form.status = 1
  form.icon = '🗺️'
  form.targetEducation = '不限'
  form.description = ''
}

watch(
  () => props.modelValue,
  async (open) => {
    if (!open) {
      return
    }
    try {
      await ensureSubjectOptions()
    } catch {
      // 学科下拉加载失败由全局拦截器提示，表单仍可展示
    }
    fillForm()
    await nextTick(() => formRef.value?.clearValidate())
  }
)

async function submit(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid || form.subjectId === undefined) {
    return
  }
  const payload: RoutePayload = {
    name: form.name.trim(),
    subjectId: form.subjectId,
    status: form.status,
    icon: form.icon.trim(),
    targetEducation: form.targetEducation.trim(),
    description: form.description.trim()
  }

  submitting.value = true
  try {
    if (props.route) {
      await updateRoute(props.route.id, payload)
      ElMessage.success('路线已更新')
    } else {
      await createRoute(payload)
      ElMessage.success('路线已创建')
    }
    emit('saved')
    visible.value = false
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <el-dialog
    v-model="visible"
    :title="dialogTitle"
    width="560px"
    :close-on-click-modal="false"
  >
    <p class="dialog-tip">路线课程在保存后进入详情页添加、排序与移除。</p>

    <el-form ref="formRef" :model="form" :rules="rules" label-width="88px">
      <div class="form-row">
        <el-form-item label="路线名称" prop="name">
          <el-input v-model.trim="form.name" maxlength="100" placeholder="请输入路线名称" />
        </el-form-item>
        <el-form-item label="关联学科" prop="subjectId">
          <el-select
            v-model="form.subjectId"
            placeholder="请选择学科"
            filterable
            style="width: 100%"
          >
            <el-option
              v-for="subject in subjectOptions"
              :key="subject.id"
              :label="subject.name"
              :value="subject.id"
            />
          </el-select>
        </el-form-item>
      </div>

      <div class="form-row">
        <el-form-item label="适合学历" prop="targetEducation">
          <el-select
            v-model="form.targetEducation"
            filterable
            allow-create
            default-first-option
            placeholder="请选择或输入"
            style="width: 100%"
          >
            <el-option v-for="item in EDUCATION_OPTIONS" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="路线图标" prop="icon">
          <el-input v-model.trim="form.icon" maxlength="50" placeholder="如 🗺️" />
        </el-form-item>
      </div>

      <el-form-item label="路线描述" prop="description">
        <el-input
          v-model.trim="form.description"
          type="textarea"
          :rows="4"
          maxlength="1000"
          show-word-limit
          placeholder="选填，描述路线目标与适合人群"
        />
      </el-form-item>

      <el-form-item label="状态">
        <el-checkbox v-model="enabled">启用该路线</el-checkbox>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">保存</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.dialog-tip {
  margin: -4px 0 14px;
  color: var(--ink-3);
  font-size: 12px;
}

.form-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

@media (max-width: 560px) {
  .form-row {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
