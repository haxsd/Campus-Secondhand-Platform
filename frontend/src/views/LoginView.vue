<script setup>
// 登录页：表单校验 → 调登录接口 → 把 token/用户信息存进 store → 跳回来源页面
import { ref, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const formRef = ref()
const loading = ref(false)
const form = reactive({
  account: '',
  password: '',
})

// el-form 的校验规则：trigger: 'blur' 表示失去焦点时校验
const rules = {
  account: [{ required: true, message: '请输入学号或手机号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function onSubmit() {
  // 先过前端表单校验（不通过会抛异常中断，不会发请求）
  await formRef.value.validate()
  loading.value = true
  try {
    const data = await login({ account: form.account, password: form.password })
    // 登录态写入 Pinia + localStorage，顶栏会立刻变成头像菜单
    userStore.setLogin(data.token, data.user)
    ElMessage.success('登录成功')
    // 如果是被守卫拦截过来的（URL 带 redirect 参数），登录后跳回原目标页
    router.push(route.query.redirect || '/')
  } catch {
    // 错误提示已由请求层统一弹出
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <el-card class="auth-card">
      <template #header>
        <div class="auth-title">登录</div>
      </template>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="0" @submit.prevent>
        <el-form-item prop="account">
          <el-input v-model="form.account" placeholder="学号或手机号" size="large" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            size="large"
            show-password
            @keyup.enter="onSubmit"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" class="submit-btn" :loading="loading" @click="onSubmit">
            登录
          </el-button>
        </el-form-item>
      </el-form>
      <div class="auth-footer">
        还没有账号？
        <el-link type="primary" @click="router.push('/register')">去注册</el-link>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.auth-page {
  display: flex;
  justify-content: center;
  padding-top: 80px;
}

.auth-card {
  width: 400px;
}

.auth-title {
  font-size: 18px;
  font-weight: bold;
  text-align: center;
}

.submit-btn {
  width: 100%;
}

.auth-footer {
  text-align: center;
  font-size: 14px;
  color: #909399;
}
</style>
