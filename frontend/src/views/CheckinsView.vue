<script setup lang="ts">
import { Camera, CircleCheck, Clock, Location, Plus, RefreshRight } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { checkinApi, type CheckinPayload } from '../api/checkins'
import { planApi } from '../api/plans'
import { resourceApi } from '../api/resources'
import type { Checkin, CheckinStatus, CultureResource, StudyPlan } from '../types/api'

const route = useRoute()
const loading = ref(true)
const saving = ref(false)
const dialogVisible = ref(false)
const checkins = ref<Checkin[]>([])
const plans = ref<StudyPlan[]>([])
const resources = ref<CultureResource[]>([])
const selectedFile = ref<File>()
const previewUrl = ref('')
const fileInput = ref<HTMLInputElement>()
const form = reactive<CheckinPayload>({ resourceId: 0, content: '' })

const statusMeta: Record<CheckinStatus, { label: string; type: 'warning' | 'success' | 'danger' }> = {
  PENDING: { label: '待审核', type: 'warning' },
  APPROVED: { label: '已通过', type: 'success' },
  REJECTED: { label: '已驳回', type: 'danger' },
}

const availableResources = computed(() => {
  if (!form.planId) return resources.value
  const ids = new Set(plans.value.find((plan) => plan.id === form.planId)?.items.map((item) => item.resourceId) || [])
  return resources.value.filter((resource) => ids.has(resource.id))
})

watch(() => form.planId, () => {
  if (form.resourceId && !availableResources.value.some((resource) => resource.id === form.resourceId)) {
    form.resourceId = 0
  }
})

async function load() {
  loading.value = true
  try {
    const [mine, planList, resourcePage] = await Promise.all([
      checkinApi.listMine(),
      planApi.list(),
      resourceApi.list({ page: 1, pageSize: 100 }),
    ])
    checkins.value = mine
    plans.value = planList
    resources.value = resourcePage.records
  } catch {
    ElMessage.error('打卡记录加载失败')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  clearPreview()
  Object.assign(form, { planId: undefined, resourceId: 0, content: '' })
  dialogVisible.value = true
}

function chooseFile() {
  fileInput.value?.click()
}

function selectFile(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file) return
  if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
    ElMessage.warning('仅支持 JPG、PNG 或 WebP 图片')
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('图片大小不能超过 5MB')
    return
  }
  clearPreview()
  selectedFile.value = file
  previewUrl.value = URL.createObjectURL(file)
}

function clearPreview() {
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
  previewUrl.value = ''
  selectedFile.value = undefined
  if (fileInput.value) fileInput.value.value = ''
}

async function submit() {
  if (!form.resourceId) {
    ElMessage.warning('请选择文化地点')
    return
  }
  if (!selectedFile.value && !form.content?.trim()) {
    ElMessage.warning('请上传图片或填写打卡文字')
    return
  }
  saving.value = true
  try {
    const image = selectedFile.value ? await checkinApi.uploadImage(selectedFile.value) : undefined
    await checkinApi.create({
      planId: form.planId,
      resourceId: form.resourceId,
      content: form.content?.trim() || undefined,
      imageUrl: image?.url,
    })
    dialogVisible.value = false
    clearPreview()
    await load()
    ElMessage.success('打卡已提交，等待管理员审核')
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '打卡提交失败')
  } finally {
    saving.value = false
  }
}

function formatDate(value: string) {
  return new Date(value).toLocaleString('zh-CN', { hour12: false })
}

onMounted(async () => {
  await load()
  const planId = Number(route.query.planId)
  const resourceId = Number(route.query.resourceId)
  if (planId || resourceId) {
    Object.assign(form, {
      planId: Number.isFinite(planId) && planId > 0 ? planId : undefined,
      resourceId: Number.isFinite(resourceId) && resourceId > 0 ? resourceId : 0,
      content: '',
    })
    dialogVisible.value = true
  }
})

onBeforeUnmount(clearPreview)
</script>

<template>
  <section v-loading="loading" class="checkin-page">
    <header class="section-heading checkin-heading">
      <div><p class="eyebrow">CULTURAL FOOTPRINT</p><h1>我的文化打卡</h1><p>记录每一次到访，审核通过后获得 10 积分并更新计划进度。</p></div>
      <el-button type="primary" :icon="Plus" @click="openCreate">发起打卡</el-button>
    </header>

    <el-empty v-if="!loading && checkins.length === 0" description="还没有打卡记录">
      <el-button type="primary" @click="openCreate">完成第一次打卡</el-button>
    </el-empty>

    <div v-else class="checkin-grid">
      <article v-for="item in checkins" :key="item.id" class="checkin-card">
        <el-image v-if="item.imageUrl" class="checkin-image" :src="item.imageUrl" :preview-src-list="[item.imageUrl]" fit="cover" />
        <div v-else class="checkin-image checkin-image-placeholder"><Camera /></div>
        <div class="checkin-card-body">
          <div class="checkin-card-title">
            <h2>{{ item.resourceName }}</h2>
            <el-tag :type="statusMeta[item.status].type" effect="plain">{{ statusMeta[item.status].label }}</el-tag>
          </div>
          <p v-if="item.content" class="checkin-content">{{ item.content }}</p>
          <div class="checkin-meta">
            <span><Location />{{ item.resourceAddress }}</span>
            <span><Clock />{{ formatDate(item.checkinTime) }}</span>
            <span v-if="item.planTitle"><CircleCheck />{{ item.planTitle }}</span>
          </div>
          <el-alert v-if="item.auditComment" class="audit-comment" :type="item.status === 'REJECTED' ? 'error' : 'success'" :closable="false" show-icon>
            <template #title>审核意见：{{ item.auditComment }}</template>
          </el-alert>
          <el-button v-if="item.status === 'REJECTED'" class="resubmit-button" :icon="RefreshRight" @click="Object.assign(form, { planId: item.planId, resourceId: item.resourceId, content: item.content || '' }); dialogVisible = true">修改后重新提交</el-button>
        </div>
      </article>
    </div>

    <el-dialog v-model="dialogVisible" title="提交文化打卡" width="min(600px, 92vw)" @closed="clearPreview">
      <el-form label-position="top">
        <el-form-item label="关联研学计划（可选）">
          <el-select v-model="form.planId" clearable class="full-control" placeholder="不关联计划">
            <el-option v-for="plan in plans" :key="plan.id" :label="plan.title" :value="plan.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="文化地点" required>
          <el-select v-model="form.resourceId" filterable class="full-control" placeholder="选择打卡地点">
            <el-option v-for="resource in availableResources" :key="resource.id" :label="resource.name" :value="resource.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="打卡图片（JPG / PNG / WebP，最大 5MB）">
          <input ref="fileInput" class="hidden-file-input" type="file" accept="image/jpeg,image/png,image/webp" @change="selectFile" />
          <div class="image-picker" @click="chooseFile">
            <img v-if="previewUrl" :src="previewUrl" alt="待上传图片预览" />
            <template v-else><Camera /><span>选择一张打卡图片</span></template>
          </div>
        </el-form-item>
        <el-form-item label="打卡感想">
          <el-input v-model="form.content" type="textarea" :rows="4" maxlength="500" show-word-limit placeholder="写下你的观察、收获或感受" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">提交审核</el-button>
      </template>
    </el-dialog>
  </section>
</template>
