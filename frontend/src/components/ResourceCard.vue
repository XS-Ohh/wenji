<script setup lang="ts">
import { Clock, Location, Star, StarFilled } from '@element-plus/icons-vue'
import type { CultureResource } from '../types/api'

defineProps<{ resource: CultureResource }>()
</script>

<template>
  <RouterLink class="resource-card" :to="`/resources/${resource.id}`">
    <div class="resource-cover" :class="`category-${resource.categoryId}`">
      <img
        v-if="resource.coverImage"
        :src="resource.coverImage"
        :alt="resource.name"
        @error="($event.target as HTMLImageElement).style.display = 'none'"
      />
      <span>{{ resource.categoryName }}</span>
      <el-icon v-if="resource.favorited" class="favorite-mark" :size="18" title="已收藏"><StarFilled /></el-icon>
    </div>
    <div class="resource-body">
      <div class="resource-meta">
        <span>{{ resource.categoryName }}</span>
        <span><Star :size="15" />{{ Number(resource.averageRating).toFixed(1) }}</span>
      </div>
      <h2>{{ resource.name }}</h2>
      <p>{{ resource.summary || '等待补充资源简介' }}</p>
      <footer>
        <span><Location :size="16" />{{ resource.district || resource.city }}</span>
        <span><Clock :size="16" />{{ resource.recommendedMinutes }} 分钟</span>
      </footer>
    </div>
  </RouterLink>
</template>
