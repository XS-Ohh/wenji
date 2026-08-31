<script setup lang="ts">
import { Medal, User } from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
</script>

<template>
  <section class="profile-page">
    <header class="section-heading"><div><p class="eyebrow">个人资料</p><h1>{{ auth.user?.nickname }}</h1></div></header>
    <div class="profile-facts">
      <div><User :size="22" /><span>用户名</span><strong>{{ auth.user?.username }}</strong></div>
      <div><Medal :size="22" /><span>积分与等级</span><strong>{{ auth.user?.points }} 分 · Lv.{{ auth.user?.level }}</strong></div>
    </div>
  </section>
</template>
