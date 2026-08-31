<script setup lang="ts">
import { Calendar, Location, Plus, Wallet } from '@element-plus/icons-vue'
import { onMounted, ref } from 'vue'
import { planApi } from '../api/plans'
import type { PlanStatus, StudyPlan } from '../types/api'

const loading = ref(true)
const failed = ref(false)
const plans = ref<StudyPlan[]>([])

const statusLabels: Record<PlanStatus, string> = {
  DRAFT: '草稿',
  IN_PROGRESS: '进行中',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
}

const statusTypes: Record<PlanStatus, 'info' | 'warning' | 'success' | 'danger'> = {
  DRAFT: 'info',
  IN_PROGRESS: 'warning',
  COMPLETED: 'success',
  CANCELLED: 'danger',
}

async function load() {
  loading.value = true
  failed.value = false
  try {
    plans.value = await planApi.list()
  } catch {
    failed.value = true
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <section>
    <header class="section-heading">
      <div>
        <p class="eyebrow">规划下一次城市探索</p>
        <h1>研学计划</h1>
      </div>
      <RouterLink to="/plans/create"><el-button type="primary" :icon="Plus">创建计划</el-button></RouterLink>
    </header>

    <div v-loading="loading" class="resource-stage">
      <el-result v-if="failed" icon="error" title="加载失败" sub-title="请稍后重试">
        <template #extra><el-button type="primary" @click="load">重试</el-button></template>
      </el-result>
      <el-empty v-else-if="!loading && plans.length === 0" description="还没有研学计划">
        <RouterLink to="/plans/create"><el-button type="primary">创建第一个计划</el-button></RouterLink>
      </el-empty>
      <div v-else class="plan-grid">
        <RouterLink v-for="plan in plans" :key="plan.id" class="plan-card" :to="`/plans/${plan.id}`">
          <div class="plan-card-heading">
            <el-tag :type="statusTypes[plan.status]" effect="plain">{{ statusLabels[plan.status] }}</el-tag>
            <span>{{ plan.itemCount }} 个地点</span>
          </div>
          <h2>{{ plan.title }}</h2>
          <div class="plan-card-facts">
            <span><Calendar />{{ plan.startDate }} 至 {{ plan.endDate }}</span>
            <span><Location />{{ plan.city }}<template v-if="plan.startLocation"> · {{ plan.startLocation }}</template></span>
            <span v-if="plan.budget !== undefined"><Wallet />预算 ¥{{ plan.budget }}</span>
          </div>
          <p>{{ plan.interests || '尚未设置兴趣方向' }}</p>
          <el-progress :percentage="plan.progress" :stroke-width="5" :show-text="false" />
        </RouterLink>
      </div>
    </div>
  </section>
</template>
