<script setup lang="ts">
import { ArrowLeft, Calendar, Clock, Location, Star, StarFilled, View } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { planApi } from '../api/plans'
import { resourceApi } from '../api/resources'
import { useAuthStore } from '../stores/auth'
import type { CultureResource, StudyPlan } from '../types/api'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const loading = ref(true)
const favoriteLoading = ref(false)
const planLoading = ref(false)
const addPlanDialog = ref(false)
const selectedPlanId = ref<number>()
const plans = ref<StudyPlan[]>([])
const failed = ref(false)
const resource = ref<CultureResource>()

async function toggleFavorite() {
  if (!auth.isAuthenticated) {
    await router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  if (!resource.value || favoriteLoading.value) return

  favoriteLoading.value = true
  try {
    if (resource.value.favorited) {
      await resourceApi.unfavorite(resource.value.id)
      resource.value.favorited = false
      resource.value.favoriteCount = Math.max(0, resource.value.favoriteCount - 1)
      ElMessage.success('已取消收藏')
    } else {
      await resourceApi.favorite(resource.value.id)
      resource.value.favorited = true
      resource.value.favoriteCount += 1
      ElMessage.success('已加入收藏')
    }
  } catch {
    ElMessage.error('操作失败，请稍后重试')
  } finally {
    favoriteLoading.value = false
  }
}

async function openPlanPicker() {
  if (!auth.isAuthenticated) {
    await router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  planLoading.value = true
  try {
    plans.value = (await planApi.list()).filter((plan) => plan.status === 'DRAFT' || plan.status === 'IN_PROGRESS')
    if (plans.value.length === 0) {
      ElMessage.info('请先创建一个可编辑的研学计划')
      await router.push('/plans/create')
      return
    }
    selectedPlanId.value = plans.value[0].id
    addPlanDialog.value = true
  } catch {
    ElMessage.error('计划列表加载失败')
  } finally {
    planLoading.value = false
  }
}

async function addToPlan() {
  if (!resource.value || !selectedPlanId.value) return
  planLoading.value = true
  try {
    await planApi.addItem(selectedPlanId.value, { resourceId: resource.value.id })
    addPlanDialog.value = false
    ElMessage.success('已加入研学计划')
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '加入计划失败')
  } finally {
    planLoading.value = false
  }
}

onMounted(async () => {
  try {
    resource.value = await resourceApi.detail(Number(route.params.id))
  } catch {
    failed.value = true
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <section v-loading="loading" class="detail-page">
    <RouterLink class="back-link" to="/resources"><ArrowLeft :size="17" />返回资源库</RouterLink>
    <el-result v-if="failed" icon="error" title="未找到资源" sub-title="资源可能尚未发布或已下线" />
    <template v-else-if="resource">
      <div class="detail-cover" :class="`category-${resource.categoryId}`">
        <img v-if="resource.coverImage" :src="resource.coverImage" :alt="resource.name" />
        <span>{{ resource.categoryName }}</span>
      </div>
      <div class="detail-content">
        <p class="eyebrow">{{ resource.categoryName }} · {{ resource.city }}</p>
        <div class="detail-title-row">
          <h1>{{ resource.name }}</h1>
          <div class="detail-title-actions">
            <el-button :icon="Calendar" :loading="planLoading" @click="openPlanPicker">加入计划</el-button>
            <el-button
              :type="resource.favorited ? 'primary' : 'default'"
              :loading="favoriteLoading"
              :icon="resource.favorited ? StarFilled : Star"
              @click="toggleFavorite"
            >
              {{ resource.favorited ? '已收藏' : '收藏' }}
            </el-button>
          </div>
        </div>
        <div class="detail-stats">
          <span><Location :size="17" />{{ resource.district }} {{ resource.address }}</span>
          <span><Clock :size="17" />建议 {{ resource.recommendedMinutes }} 分钟</span>
          <span><Star :size="17" />{{ Number(resource.averageRating).toFixed(1) }}</span>
          <span><StarFilled :size="17" />{{ resource.favoriteCount }} 人收藏</span>
          <span><View :size="17" />{{ resource.viewCount }}</span>
        </div>
        <p class="lead">{{ resource.summary }}</p>
        <div class="detail-section"><h2>资源介绍</h2><p>{{ resource.description || '暂无详细介绍' }}</p></div>
        <div class="detail-facts">
          <div><span>开放时间</span><strong>{{ resource.openingHours || '以场馆公告为准' }}</strong></div>
          <div><span>参观信息</span><strong>{{ resource.ticketInfo || '以场馆公告为准' }}</strong></div>
        </div>
      </div>
    </template>

    <el-dialog v-model="addPlanDialog" title="加入研学计划" width="min(480px, 92vw)">
      <el-form label-position="top">
        <el-form-item label="选择计划">
          <el-select v-model="selectedPlanId" class="full-control">
            <el-option v-for="item in plans" :key="item.id" :label="`${item.title}（${item.startDate}）`" :value="item.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="addPlanDialog = false">取消</el-button><el-button type="primary" :loading="planLoading" @click="addToPlan">确认加入</el-button></template>
    </el-dialog>
  </section>
</template>
