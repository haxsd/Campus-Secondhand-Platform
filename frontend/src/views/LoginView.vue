<script setup>
// 登录页：表单校验 → 调登录接口 → 把 token/用户信息存进 store → 跳回来源页面
import { ref, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
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
    <div class="auth-card">
      <!-- 品牌标识 -->
      <div class="brand">
        <span class="brand-mark">
          <svg viewBox="0 0 64 64" width="24" height="24" aria-hidden="true">
            <path
              d="M20 27h24l-2.6 16.2a4 4 0 0 1-4 3.8H26.6a4 4 0 0 1-4-3.8L20 27z"
              fill="none"
              stroke="#fff"
              stroke-width="5"
              stroke-linejoin="round"
            />
            <path
              d="M25.5 27v-3.5a6.5 6.5 0 0 1 13 0V27"
              fill="none"
              stroke="#fff"
              stroke-width="5"
              stroke-linecap="round"
            />
          </svg>
        </span>
      </div>
      <h2 class="auth-title">欢迎回来</h2>
      <p class="auth-sub">登录校园二手平台，继续你的淘好物之旅</p>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="0" @submit.prevent>
        <el-form-item prop="account">
          <el-input
            v-model="form.account"
            placeholder="学号或手机号"
            size="large"
            :prefix-icon="User"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            size="large"
            :prefix-icon="Lock"
            show-password
            @keyup.enter="onSubmit"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            class="submit-btn"
            :loading="loading"
            @click="onSubmit"
          >
            登 录
          </el-button>
        </el-form-item>
      </el-form>

      <div class="auth-footer">
        还没有账号？
        <el-link type="primary" @click="router.push('/register')">去注册</el-link>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 整页氛围背景：两团柔和的绿色光晕 */
.auth-page {
  min-height: calc(100vh - 64px - 132px);
  display: grid;
  place-items: center;
  padding: 56px 20px 64px;
  background:
    radial-gradient(560px 300px at 85% 6%, rgba(52, 211, 153, 0.18), transparent 62%),
    radial-gradient(520px 320px at 6% 92%, rgba(13, 148, 136, 0.14), transparent 62%),
    linear-gradient(180deg, #f0faf5, var(--app-bg-page));
}

.auth-card {
  width: min(420px, 100%);
  background: #fff;
  border: 1px solid #eaf4ef;
  border-radius: 20px;
  box-shadow: var(--app-shadow-lg);
  padding: 36px 34px 26px;
}

.brand {
  display: flex;
  justify-content: center;
  margin-bottom: 14px;
}

.brand-mark {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: 14px;
  background: var(--app-gradient);
  box-shadow: 0 6px 16px rgba(16, 185, 129, 0.35);
}

.auth-title {
  margin: 0 0 6px;
  font-size: 22px;
  font-weight: 700;
  text-align: center;
  color: var(--app-text-1);
}

.auth-sub {
  margin: 0 0 26px;
  font-size: 13px;
  text-align: center;
  color: var(--app-text-3);
}

.submit-btn {
  width: 100%;
  font-weight: 600;
  letter-spacing: 2px;
}

.auth-footer {
  margin-top: 6px;
  text-align: center;
  font-size: 14px;
  color: var(--app-text-3);
}
</style>
