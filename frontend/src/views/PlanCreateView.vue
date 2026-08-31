<script setup lang="ts">
import { ArrowLeft, Check } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { planApi } from '../api/plans'

const router = useRouter()
const formRef = ref<FormInstance>()
const submitting = ref(false)
const form = reactive({
  title: '',
  city: '上海',
  dateRange: [] as string[],
  budget: undefined as number | undefined,
  interests: '',
  startLocation: '',
})

const rules: FormRules = {
  title: [{ required: true, message: '请输入计划名称', trigger: 'blur' }],
  city: [{ required: true, message: '请输入城市', trigger: 'blur' }],
  dateRange: [{ type: 'array', required: true, len: 2, message: '请选择计划日期', trigger: 'change' }],
}

async function submit() {
  if (!formRef.value || !(await formRef.value.validate().catch(() => false))) return
  submitting.value = true
  try {
    const plan = await planApi.create({
      title: form.title,
      city: form.city,
      startDate: form.dateRange[0],
      endDate: form.dateRange[1],
      budget: form.budget,
      interests: form.interests || undefined,
      startLocation: form.startLocation || undefined,
    })
    ElMessage.success('计划已创建')
    await router.push(`/plans/${plan.id}`)
  } catch {
    ElMessage.error('创建失败，请检查填写内容')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <section class="form-page">
    <RouterLink class="back-link" to="/plans"><ArrowLeft :size="17" />返回计划列表</RouterLink>
    <header class="section-heading">
      <div><p class="eyebrow">建立研学目标与时间范围</p><h1>创建研学计划</h1></div>
    </header>
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="plan-form">
      <div class="plan-form-grid">
        <el-form-item label="计划名称" prop="title"><el-input v-model="form.title" maxlength="100" placeholder="例如：上海近代建筑研学" /></el-form-item>
        <el-form-item label="城市" prop="city"><el-input v-model="form.city" maxlength="50" /></el-form-item>
        <el-form-item label="计划日期" prop="dateRange" class="form-span-2">
          <el-date-picker v-model="form.dateRange" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始日期" end-placeholder="结束日期" />
        </el-form-item>
        <el-form-item label="预算（元）"><el-input-number v-model="form.budget" :min="0" :precision="2" controls-position="right" /></el-form-item>
        <el-form-item label="出发地点"><el-input v-model="form.startLocation" maxlength="255" placeholder="例如：人民广场" /></el-form-item>
        <el-form-item label="兴趣方向" class="form-span-2"><el-input v-model="form.interests" maxlength="255" placeholder="例如：博物馆、历史建筑、非遗" /></el-form-item>
      </div>
      <div class="form-actions">
        <el-button @click="router.push('/plans')">取消</el-button>
        <el-button type="primary" :icon="Check" :loading="submitting" @click="submit">创建计划</el-button>
      </div>
    </el-form>
  </section>
</template>
