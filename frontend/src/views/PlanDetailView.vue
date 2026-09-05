<script setup lang="ts">
import { ArrowDown, ArrowLeft, ArrowUp, Camera, Clock, Delete, Edit, Location, MagicStick, Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { planApi, type AiRoutePayload, type PlanItemPayload, type PlanPayload } from '../api/plans'
import { resourceApi } from '../api/resources'
import type { AiRouteResult, CultureResource, PlanItem, PlanStatus, StudyPlan } from '../types/api'

const route = useRoute()
const router = useRouter()
const planId = Number(route.params.id)
const loading = ref(true)
const failed = ref(false)
const saving = ref(false)
const generating = ref(false)
const plan = ref<StudyPlan>()
const resources = ref<CultureResource[]>([])
const itemDialog = ref(false)
const planDialog = ref(false)
const aiRouteDialog = ref(false)
const generatedRoute = ref<AiRouteResult>()
const editingItemId = ref<number>()
const editDateRange = ref<string[]>([])

const itemForm = reactive<PlanItemPayload>({ resourceId: 0 })
const editPlanForm = reactive<PlanPayload>({ title: '', city: '', startDate: '', endDate: '' })
const aiRouteForm = reactive<AiRoutePayload>({
  desiredPlaces: 4,
  dailyStartTime: '09:00:00',
  dailyEndTime: '17:00:00',
})

const statusOptions: Array<{ value: PlanStatus; label: string }> = [
  { value: 'DRAFT', label: '草稿' },
  { value: 'IN_PROGRESS', label: '进行中' },
  { value: 'COMPLETED', label: '已完成' },
  { value: 'CANCELLED', label: '已取消' },
]

async function load() {
  loading.value = true
  failed.value = false
  try {
    plan.value = await planApi.detail(planId)
    const result = await resourceApi.list({ page: 1, pageSize: 100, city: plan.value.city })
    resources.value = result.records
  } catch {
    failed.value = true
  } finally {
    loading.value = false
  }
}

async function changeStatus(status: PlanStatus) {
  if (!plan.value) return
  try {
    plan.value = await planApi.changeStatus(planId, status)
    ElMessage.success('计划状态已更新')
  } catch {
    ElMessage.error('状态更新失败')
  }
}

function openCreateItem() {
  editingItemId.value = undefined
  Object.assign(itemForm, {
    resourceId: 0,
    visitDate: plan.value?.startDate,
    startTime: undefined,
    endTime: undefined,
    sortOrder: undefined,
    transportation: '',
    reason: '',
  })
  itemDialog.value = true
}

function openEditItem(item: PlanItem) {
  editingItemId.value = item.id
  Object.assign(itemForm, {
    resourceId: item.resourceId,
    visitDate: item.visitDate,
    startTime: item.startTime,
    endTime: item.endTime,
    sortOrder: item.sortOrder,
    transportation: item.transportation || '',
    reason: item.reason || '',
  })
  itemDialog.value = true
}

async function saveItem() {
  if (!itemForm.resourceId) {
    ElMessage.warning('请选择文化资源')
    return
  }
  if (itemForm.startTime && itemForm.endTime && itemForm.startTime >= itemForm.endTime) {
    ElMessage.warning('结束时间必须晚于开始时间')
    return
  }
  saving.value = true
  try {
    if (editingItemId.value) {
      await planApi.updateItem(planId, editingItemId.value, itemForm)
    } else {
      await planApi.addItem(planId, itemForm)
    }
    itemDialog.value = false
    await load()
    ElMessage.success(editingItemId.value ? '路线节点已更新' : '已加入路线')
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function removeItem(item: PlanItem) {
  await ElMessageBox.confirm(`确认从路线中移除“${item.resourceName}”？`, '移除节点', { type: 'warning' })
  await planApi.removeItem(planId, item.id)
  await load()
  ElMessage.success('节点已移除')
}

async function moveItem(index: number, offset: number) {
  if (!plan.value) return
  const target = index + offset
  if (target < 0 || target >= plan.value.items.length) return
  const ordered = [...plan.value.items]
  const [item] = ordered.splice(index, 1)
  ordered.splice(target, 0, item)
  try {
    plan.value.items = await planApi.reorder(planId, ordered.map((entry) => entry.id))
  } catch {
    ElMessage.error('排序失败')
  }
}

function openEditPlan() {
  if (!plan.value) return
  Object.assign(editPlanForm, {
    title: plan.value.title,
    city: plan.value.city,
    startDate: plan.value.startDate,
    endDate: plan.value.endDate,
    budget: plan.value.budget,
    interests: plan.value.interests || '',
    startLocation: plan.value.startLocation || '',
  })
  editDateRange.value = [plan.value.startDate, plan.value.endDate]
  planDialog.value = true
}

async function savePlan() {
  if (!editPlanForm.title.trim() || !editPlanForm.city.trim() || editDateRange.value.length !== 2) {
    ElMessage.warning('请完整填写计划名称、城市和日期')
    return
  }
  saving.value = true
  try {
    editPlanForm.startDate = editDateRange.value[0]
    editPlanForm.endDate = editDateRange.value[1]
    plan.value = await planApi.update(planId, editPlanForm)
    planDialog.value = false
    ElMessage.success('计划信息已更新')
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function removePlan() {
  if (!plan.value) return
  await ElMessageBox.confirm(`确认删除“${plan.value.title}”？该操作不可撤销。`, '删除计划', { type: 'warning' })
  await planApi.remove(planId)
  ElMessage.success('计划已删除')
  await router.push('/plans')
}

function openAiRoute() {
  aiRouteDialog.value = true
}

function startCheckin(item: PlanItem) {
  router.push({ path: '/checkins', query: { planId, resourceId: item.resourceId } })
}

async function generateAiRoute() {
  if (aiRouteForm.dailyStartTime >= aiRouteForm.dailyEndTime) {
    ElMessage.warning('结束时间必须晚于开始时间')
    return
  }
  if (plan.value?.items.length) {
    await ElMessageBox.confirm('生成新路线后，当前路线节点将被替换。是否继续？', '替换当前路线', {
      type: 'warning',
      confirmButtonText: '继续生成',
    })
  }
  generating.value = true
  try {
    const result = await planApi.generateAiRoute(planId, aiRouteForm)
    generatedRoute.value = result
    plan.value = result.plan
    aiRouteDialog.value = false
    ElMessage.success(result.fallback ? '已使用规则推荐生成路线' : '智能路线已生成')
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '路线生成失败')
  } finally {
    generating.value = false
  }
}

onMounted(load)
</script>

<template>
  <section v-loading="loading" class="plan-detail-page">
    <RouterLink class="back-link" to="/plans"><ArrowLeft :size="17" />返回计划列表</RouterLink>
    <el-result v-if="failed" icon="error" title="计划加载失败" sub-title="计划可能不存在或不属于当前用户" />
    <template v-else-if="plan">
      <header class="plan-detail-heading">
        <div><p class="eyebrow">{{ plan.city }}文化研学</p><h1>{{ plan.title }}</h1></div>
        <div class="plan-heading-actions">
          <el-select :model-value="plan.status" class="status-select" @change="changeStatus">
            <el-option v-for="option in statusOptions" :key="option.value" :label="option.label" :value="option.value" />
          </el-select>
          <el-button :icon="Edit" @click="openEditPlan">编辑计划</el-button>
          <el-button :icon="Delete" @click="removePlan">删除</el-button>
        </div>
      </header>

      <div class="plan-overview">
        <div><span>计划日期</span><strong>{{ plan.startDate }} 至 {{ plan.endDate }}</strong></div>
        <div><span>预算</span><strong>{{ plan.budget === undefined ? '未设置' : `¥${plan.budget}` }}</strong></div>
        <div><span>出发地点</span><strong>{{ plan.startLocation || '未设置' }}</strong></div>
        <div><span>兴趣方向</span><strong>{{ plan.interests || '未设置' }}</strong></div>
      </div>

      <section class="route-section">
        <header class="route-heading">
          <div><h2>路线节点</h2><span>{{ plan.items.length }} 个文化地点</span></div>
          <div class="route-heading-actions">
            <el-button :icon="MagicStick" @click="openAiRoute">智能生成</el-button>
            <el-button type="primary" :icon="Plus" @click="openCreateItem">添加地点</el-button>
          </div>
        </header>

        <div v-if="generatedRoute" class="ai-route-result">
          <div><strong>{{ generatedRoute.title }}</strong><span>{{ generatedRoute.fallback ? '规则推荐' : 'AI 推荐' }}</span></div>
          <p>{{ generatedRoute.summary }}</p>
          <small v-if="generatedRoute.tips.length">{{ generatedRoute.tips.join(' · ') }}</small>
        </div>

        <el-empty v-if="plan.items.length === 0" description="路线中还没有文化地点" />
        <div v-else class="route-list">
          <article v-for="(item, index) in plan.items" :key="item.id" class="route-item">
            <div class="route-order">{{ index + 1 }}</div>
            <div class="route-item-main">
              <h3>{{ item.resourceName }}</h3>
              <div class="route-item-meta">
                <span><Location />{{ item.resourceAddress }}</span>
                <span v-if="item.visitDate"><Clock />{{ item.visitDate }} {{ item.startTime?.slice(0, 5) }}<template v-if="item.endTime"> - {{ item.endTime.slice(0, 5) }}</template></span>
              </div>
              <p v-if="item.transportation || item.reason">{{ item.transportation }}<template v-if="item.transportation && item.reason"> · </template>{{ item.reason }}</p>
            </div>
            <div class="route-actions">
              <el-tooltip content="打卡"><el-button circle :icon="Camera" @click="startCheckin(item)" /></el-tooltip>
              <el-tooltip content="上移"><el-button circle :icon="ArrowUp" :disabled="index === 0" @click="moveItem(index, -1)" /></el-tooltip>
              <el-tooltip content="下移"><el-button circle :icon="ArrowDown" :disabled="index === plan.items.length - 1" @click="moveItem(index, 1)" /></el-tooltip>
              <el-tooltip content="编辑"><el-button circle :icon="Edit" @click="openEditItem(item)" /></el-tooltip>
              <el-tooltip content="移除"><el-button circle :icon="Delete" @click="removeItem(item)" /></el-tooltip>
            </div>
          </article>
        </div>
      </section>
    </template>

    <el-dialog v-model="aiRouteDialog" title="生成文化研学路线" width="min(520px, 92vw)">
      <el-alert v-if="plan?.items.length" title="生成后将替换当前路线节点" type="warning" :closable="false" show-icon />
      <el-form class="ai-route-form" label-position="top">
        <el-form-item label="地点数量">
          <el-radio-group v-model="aiRouteForm.desiredPlaces">
            <el-radio-button :value="3">3 个地点</el-radio-button>
            <el-radio-button :value="4">4 个地点</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <div class="dialog-form-grid">
          <el-form-item label="每日开始时间">
            <el-time-picker v-model="aiRouteForm.dailyStartTime" value-format="HH:mm:ss" format="HH:mm" />
          </el-form-item>
          <el-form-item label="每日结束时间">
            <el-time-picker v-model="aiRouteForm.dailyEndTime" value-format="HH:mm:ss" format="HH:mm" />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="aiRouteDialog = false">取消</el-button>
        <el-button type="primary" :icon="MagicStick" :loading="generating" @click="generateAiRoute">生成路线</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="itemDialog" :title="editingItemId ? '编辑路线节点' : '添加路线节点'" width="min(620px, 92vw)">
      <el-form label-position="top">
        <el-form-item label="文化资源" required>
          <el-select v-model="itemForm.resourceId" filterable placeholder="选择文化地点" class="full-control">
            <el-option v-for="resource in resources" :key="resource.id" :label="resource.name" :value="resource.id" />
          </el-select>
        </el-form-item>
        <div class="dialog-form-grid">
          <el-form-item label="参观日期"><el-date-picker v-model="itemForm.visitDate" value-format="YYYY-MM-DD" /></el-form-item>
          <el-form-item label="交通方式"><el-input v-model="itemForm.transportation" maxlength="50" placeholder="步行、地铁等" /></el-form-item>
          <el-form-item label="开始时间"><el-time-picker v-model="itemForm.startTime" value-format="HH:mm:ss" format="HH:mm" /></el-form-item>
          <el-form-item label="结束时间"><el-time-picker v-model="itemForm.endTime" value-format="HH:mm:ss" format="HH:mm" /></el-form-item>
        </div>
        <el-form-item label="推荐理由"><el-input v-model="itemForm.reason" type="textarea" :rows="3" maxlength="255" show-word-limit /></el-form-item>
      </el-form>
      <template #footer><el-button @click="itemDialog = false">取消</el-button><el-button type="primary" :loading="saving" @click="saveItem">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="planDialog" title="编辑计划" width="min(620px, 92vw)">
      <el-form label-position="top">
        <div class="dialog-form-grid">
          <el-form-item label="计划名称" required><el-input v-model="editPlanForm.title" maxlength="100" /></el-form-item>
          <el-form-item label="城市" required><el-input v-model="editPlanForm.city" maxlength="50" /></el-form-item>
          <el-form-item label="计划日期" class="form-span-2" required><el-date-picker v-model="editDateRange" type="daterange" value-format="YYYY-MM-DD" /></el-form-item>
          <el-form-item label="预算"><el-input-number v-model="editPlanForm.budget" :min="0" :precision="2" /></el-form-item>
          <el-form-item label="出发地点"><el-input v-model="editPlanForm.startLocation" maxlength="255" /></el-form-item>
          <el-form-item label="兴趣方向" class="form-span-2"><el-input v-model="editPlanForm.interests" maxlength="255" /></el-form-item>
        </div>
      </el-form>
      <template #footer><el-button @click="planDialog = false">取消</el-button><el-button type="primary" :loading="saving" @click="savePlan">保存</el-button></template>
    </el-dialog>
  </section>
</template>
