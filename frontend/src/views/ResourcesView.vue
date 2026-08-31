<script setup lang="ts">
import { Search } from '@element-plus/icons-vue'
import { onMounted, reactive, ref } from 'vue'
import { resourceApi } from '../api/resources'
import ResourceCard from '../components/ResourceCard.vue'
import type { Category, CultureResource } from '../types/api'

const loading = ref(false)
const failed = ref(false)
const resources = ref<CultureResource[]>([])
const categories = ref<Category[]>([])
const total = ref(0)
const filters = reactive({ page: 1, pageSize: 12, keyword: '', categoryId: undefined as number | undefined, city: '上海' })

async function load() {
  loading.value = true
  failed.value = false
  try {
    const result = await resourceApi.list(filters)
    resources.value = result.records
    total.value = result.total
  } catch {
    failed.value = true
  } finally {
    loading.value = false
  }
}

function search() {
  filters.page = 1
  load()
}

onMounted(async () => {
  const [categoryResult] = await Promise.allSettled([resourceApi.categories(), load()])
  if (categoryResult.status === 'fulfilled') categories.value = categoryResult.value
})
</script>

<template>
  <section class="resource-page">
    <header class="section-heading">
      <div>
        <p class="eyebrow">上海文化资源库</p>
        <h1>文化资源</h1>
      </div>
      <span class="result-count">{{ total }} 处</span>
    </header>

    <form class="filter-bar" @submit.prevent="search">
      <el-input v-model="filters.keyword" clearable placeholder="搜索名称或简介" :prefix-icon="Search" />
      <el-select v-model="filters.categoryId" clearable placeholder="全部分类">
        <el-option v-for="category in categories" :key="category.id" :label="category.name" :value="category.id" />
      </el-select>
      <el-input v-model="filters.city" placeholder="城市" />
      <el-button type="primary" native-type="submit" :icon="Search">查询</el-button>
    </form>

    <div v-loading="loading" class="resource-stage">
      <el-result v-if="failed" icon="error" title="加载失败" sub-title="请检查后端服务后重试">
        <template #extra><el-button type="primary" @click="load">重试</el-button></template>
      </el-result>
      <el-empty v-else-if="!loading && resources.length === 0" description="暂无符合条件的文化资源" />
      <div v-else class="resource-grid">
        <ResourceCard v-for="item in resources" :key="item.id" :resource="item" />
      </div>
    </div>
    <el-pagination
      v-if="total > filters.pageSize"
      v-model:current-page="filters.page"
      :page-size="filters.pageSize"
      :total="total"
      layout="prev, pager, next"
      @current-change="load"
    />
  </section>
</template>
