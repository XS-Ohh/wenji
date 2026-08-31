<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { resourceApi } from '../api/resources'
import ResourceCard from '../components/ResourceCard.vue'
import type { CultureResource } from '../types/api'

const loading = ref(true)
const failed = ref(false)
const resources = ref<CultureResource[]>([])

async function load() {
  loading.value = true
  failed.value = false
  try {
    resources.value = await resourceApi.favorites()
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
        <p class="eyebrow">个人研学收藏夹</p>
        <h1>我的收藏</h1>
      </div>
      <span class="result-count">{{ resources.length }} 处</span>
    </header>

    <div v-loading="loading" class="resource-stage favorites-stage">
      <el-result v-if="failed" icon="error" title="加载失败" sub-title="请稍后重试">
        <template #extra><el-button type="primary" @click="load">重试</el-button></template>
      </el-result>
      <el-empty v-else-if="!loading && resources.length === 0" description="暂未收藏文化资源">
        <RouterLink to="/resources"><el-button type="primary">浏览文化资源</el-button></RouterLink>
      </el-empty>
      <div v-else class="resource-grid">
        <ResourceCard v-for="resource in resources" :key="resource.id" :resource="resource" />
      </div>
    </div>
  </section>
</template>
