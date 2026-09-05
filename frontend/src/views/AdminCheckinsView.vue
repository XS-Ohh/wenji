<script setup lang="ts">
import { Check, Clock, Close, Location, Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { onMounted, ref } from 'vue'
import { checkinApi } from '../api/checkins'
import type { Checkin, CheckinStatus } from '../types/api'

const loading = ref(true)
const reviewingId = ref<number>()
const status = ref<CheckinStatus | undefined>('PENDING')
const checkins = ref<Checkin[]>([])

const statusMeta: Record<CheckinStatus, { label: string; type: 'warning' | 'success' | 'danger' }> = {
  PENDING: { label: '待审核', type: 'warning' },
  APPROVED: { label: '已通过', type: 'success' },
  REJECTED: { label: '已驳回', type: 'danger' },
}

async function load() {
  loading.value = true
  try {
    checkins.value = await checkinApi.listForReview(status.value)
  } catch {
    ElMessage.error('审核列表加载失败')
  } finally {
    loading.value = false
  }
}

async function approve(item: Checkin) {
  await ElMessageBox.confirm(`确认通过“${item.userNickname}”在“${item.resourceName}”的打卡？`, '通过打卡', {
    type: 'success',
    confirmButtonText: '确认通过',
  })
  reviewingId.value = item.id
  try {
    await checkinApi.review(item.id, 'APPROVED', '打卡内容符合要求')
    await load()
    ElMessage.success('审核通过，积分和计划进度已更新')
  } finally {
    reviewingId.value = undefined
  }
}

async function reject(item: Checkin) {
  try {
    const result = await ElMessageBox.prompt('请说明驳回原因，用户可修改后重新提交。', `驳回 ${item.resourceName} 打卡`, {
      inputPlaceholder: '填写审核意见',
      inputValidator: (value) => Boolean(value.trim()) || '审核意见不能为空',
      confirmButtonText: '确认驳回',
    })
    reviewingId.value = item.id
    await checkinApi.review(item.id, 'REJECTED', result.value.trim())
    await load()
    ElMessage.success('已驳回该打卡')
  } catch (error: any) {
    if (error !== 'cancel' && error !== 'close') ElMessage.error(error.response?.data?.message || '审核失败')
  } finally {
    reviewingId.value = undefined
  }
}

function formatDate(value: string) {
  return new Date(value).toLocaleString('zh-CN', { hour12: false })
}

onMounted(load)
</script>

<template>
  <section class="admin-checkin-page">
    <header class="section-heading checkin-heading">
      <div><p class="eyebrow">REVIEW CENTER</p><h1>打卡审核</h1><p>审核用户的文化足迹，通过后系统将自动发放积分并更新计划进度。</p></div>
      <div class="review-filters">
        <el-select v-model="status" clearable placeholder="全部状态" @change="load">
          <el-option label="待审核" value="PENDING" />
          <el-option label="已通过" value="APPROVED" />
          <el-option label="已驳回" value="REJECTED" />
        </el-select>
        <el-button circle :icon="Refresh" aria-label="刷新" @click="load" />
      </div>
    </header>

    <div v-loading="loading" class="review-list">
      <el-empty v-if="!loading && checkins.length === 0" description="当前没有需要处理的打卡" />
      <article v-for="item in checkins" :key="item.id" class="review-card">
        <el-image v-if="item.imageUrl" class="review-image" :src="item.imageUrl" :preview-src-list="[item.imageUrl]" fit="cover" />
        <div v-else class="review-image checkin-image-placeholder">无图片</div>
        <div class="review-main">
          <div class="checkin-card-title">
            <div><span class="review-user">{{ item.userNickname }}</span><h2>{{ item.resourceName }}</h2></div>
            <el-tag :type="statusMeta[item.status].type" effect="plain">{{ statusMeta[item.status].label }}</el-tag>
          </div>
          <p v-if="item.content" class="checkin-content">{{ item.content }}</p>
          <div class="checkin-meta">
            <span><Location />{{ item.resourceAddress }}</span>
            <span><Clock />{{ formatDate(item.checkinTime) }}</span>
            <span v-if="item.planTitle">关联计划：{{ item.planTitle }}</span>
          </div>
          <p v-if="item.auditComment" class="review-comment">审核意见：{{ item.auditComment }}</p>
        </div>
        <div v-if="item.status === 'PENDING'" class="review-actions">
          <el-button type="success" plain :icon="Check" :loading="reviewingId === item.id" @click="approve(item)">通过</el-button>
          <el-button type="danger" plain :icon="Close" :disabled="reviewingId === item.id" @click="reject(item)">驳回</el-button>
        </div>
      </article>
    </div>
  </section>
</template>
