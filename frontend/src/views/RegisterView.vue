<script setup lang="ts">
import type { FormInstance, FormRules } from 'element-plus'
import { ArrowRight, UserFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()
const formRef = ref<FormInstance>()
const loading = ref(false)
const form = reactive({ username: '', nickname: '', password: '' })
const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_]{3,50}$/, message: '使用3-50位字母、数字或下划线', trigger: 'blur' },
  ],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, max: 72, message: '密码长度为8-72位', trigger: 'blur' },
    { pattern: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).+$/, message: '需包含大小写字母、数字和特殊字符', trigger: 'blur' },
  ],
}

async function submit() {
  if (!(await formRef.value?.validate().catch(() => false))) return
  loading.value = true
  try {
    await auth.register(form.username, form.password, form.nickname)
    await router.push('/resources')
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message ?? '注册失败，请稍后重试')
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
        <el-icon :size="24"><UserFilled /></el-icon>
        <h1>注册</h1>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="submit">
        <el-form-item label="用户名" prop="username"><el-input v-model="form.username" autocomplete="username" /></el-form-item>
        <el-form-item label="昵称" prop="nickname"><el-input v-model="form.nickname" autocomplete="nickname" /></el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" autocomplete="new-password" show-password />
        </el-form-item>
        <el-button class="submit-button" type="primary" native-type="submit" :loading="loading">
          注册<el-icon><ArrowRight /></el-icon>
        </el-button>
      </el-form>
      <p class="auth-switch">已有账号？<RouterLink to="/login">登录</RouterLink></p>
    </section>
  </main>
</template>
