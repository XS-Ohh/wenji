<script setup lang="ts">
import { Calendar, Camera, Compass, Finished, MapLocation, Star, SwitchButton, User } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()

function logout() {
  auth.logout()
  router.push('/login')
}
</script>

<template>
  <div class="app-shell">
    <header class="topbar">
      <RouterLink class="brand" to="/resources">
        <span class="brand-mark">文</span>
        <span>文迹</span>
      </RouterLink>
      <nav class="primary-nav" aria-label="主导航">
        <RouterLink to="/resources"><Compass :size="18" />文化资源</RouterLink>
        <RouterLink to="/map"><MapLocation :size="18" />文化地图</RouterLink>
        <RouterLink v-if="auth.isAuthenticated" to="/favorites"><Star :size="18" />我的收藏</RouterLink>
        <RouterLink v-if="auth.isAuthenticated" to="/plans"><Calendar :size="18" />研学计划</RouterLink>
        <RouterLink v-if="auth.user?.role === 'STUDENT'" to="/checkins"><Camera :size="18" />我的打卡</RouterLink>
        <RouterLink v-if="auth.user?.role === 'ADMIN'" to="/admin/checkins"><Finished :size="18" />打卡审核</RouterLink>
      </nav>
      <div class="account-actions">
        <template v-if="auth.isAuthenticated">
          <RouterLink class="account-link" to="/profile"><User :size="18" />{{ auth.user?.nickname }}</RouterLink>
          <el-button circle :icon="SwitchButton" title="退出登录" aria-label="退出登录" @click="logout" />
        </template>
        <RouterLink v-else class="account-link" to="/login"><User :size="18" />登录</RouterLink>
      </div>
    </header>
    <main class="page-content">
      <RouterView />
    </main>
  </div>
</template>
