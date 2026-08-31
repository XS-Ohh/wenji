<script setup lang="ts">
import type { FormInstance, FormRules } from 'element-plus'
import { ArrowRight, Lock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const formRef = ref<FormInstance>()
const loading = ref(false)
const form = reactive({ username: '', password: '' })
const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function submit() {
  if (!(await formRef.value?.validate().catch(() => false))) return
  loading.value = true
  try {
    await auth.login(form.username, form.password)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/resources'
    await router.push(redirect)
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message ?? '登录失败，请稍后重试')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="auth-page">
    <section class="auth-panel">
      <RouterLink class="auth-brand" to="/resources"><span class="brand-mark">文</span>文迹</RouterLink>
      <div class="auth-heading">
        <el-icon :size="24"><Lock /></el-icon>
        <h1>登录</h1>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="submit">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" autocomplete="username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" autocomplete="current-password" show-password placeholder="请输入密码" />
        </el-form-item>
        <el-button class="submit-button" type="primary" native-type="submit" :loading="loading">
          登录<el-icon><ArrowRight /></el-icon>
        </el-button>
      </el-form>
      <p class="auth-switch">还没有账号？<RouterLink to="/register">注册</RouterLink></p>
    </section>
  </main>
</template>
