<script setup>
// 注册页：表单校验（含手机号/密码格式、两次密码一致）→ 调注册接口 → 跳登录页
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register } from '@/api/auth'
import { CAMPUS_LIST } from '@/constants'

const router = useRouter()

const formRef = ref()
const loading = ref(false)
const form = reactive({
  studentNo: '',
  phone: '',
  password: '',
  confirmPassword: '',
  nickname: '',
  campus: '',
})

const rules = {
  studentNo: [{ required: true, message: '请输入学号', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '手机号格式不正确', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    {
      // 正则释义：(?=.*[A-Za-z]) 至少含一个字母，(?=.*\d) 至少含一个数字，.{8,20} 总长 8~20 位
      pattern: /^(?=.*[A-Za-z])(?=.*\d).{8,20}$/,
      message: '密码 8~20 位，需同时包含字母和数字',
      trigger: 'blur',
    },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      // 自定义校验器：和密码框的当前值对比，callback(错误) = 校验失败，callback() = 通过
      validator: (rule, value, callback) => {
        if (value !== form.password) callback(new Error('两次输入的密码不一致'))
        else callback()
      },
      trigger: 'blur',
    },
  ],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  campus: [{ required: true, message: '请选择校区', trigger: 'change' }],
}

async function onSubmit() {
  await formRef.value.validate()
  loading.value = true
  try {
    // 只发送 API 文档定义的字段（confirmPassword 只是前端自检用，不传给后端）
    await register({
      studentNo: form.studentNo,
      phone: form.phone,
      password: form.password,
      nickname: form.nickname,
      campus: form.campus,
    })
    ElMessage.success('注册成功，请登录')
    router.push('/login')
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
      <h2 class="auth-title">创建账号</h2>
      <p class="auth-sub">加入校园二手平台，让你的闲置继续发光</p>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        class="auth-form"
        @submit.prevent
      >
        <el-form-item label="学号" prop="studentNo">
          <el-input v-model="form.studentNo" placeholder="请输入学号" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="8~20 位，含字母和数字"
            show-password
          />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            placeholder="再次输入密码"
            show-password
          />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" placeholder="同学们看到的名字" />
        </el-form-item>
        <el-form-item label="校区" prop="campus">
          <el-select v-model="form.campus" placeholder="请选择校区" style="width: 100%">
            <el-option v-for="c in CAMPUS_LIST" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" class="submit-btn" :loading="loading" @click="onSubmit">
            注 册
          </el-button>
        </el-form-item>
      </el-form>

      <div class="auth-footer">
        已有账号？
        <el-link type="primary" @click="router.push('/login')">去登录</el-link>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 与登录页同款氛围背景，保持体验连贯 */
.auth-page {
  min-height: calc(100vh - 64px - 132px);
  display: grid;
  place-items: center;
  padding: 48px 20px 56px;
  background:
    radial-gradient(560px 300px at 85% 6%, rgba(52, 211, 153, 0.18), transparent 62%),
    radial-gradient(520px 320px at 6% 92%, rgba(13, 148, 136, 0.14), transparent 62%),
    linear-gradient(180deg, #f0faf5, var(--app-bg-page));
}

.auth-card {
  width: min(460px, 100%);
  background: #fff;
  border: 1px solid #eaf4ef;
  border-radius: 20px;
  box-shadow: var(--app-shadow-lg);
  padding: 34px 34px 24px;
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
  margin: 0 0 22px;
  font-size: 13px;
  text-align: center;
  color: var(--app-text-3);
}

/* 顶部标签的表单更紧凑一些 */
.auth-form :deep(.el-form-item) {
  margin-bottom: 18px;
}

.auth-form :deep(.el-form-item__label) {
  margin-bottom: 4px;
  font-weight: 500;
  color: var(--app-text-2);
}

.submit-btn {
  width: 100%;
  font-weight: 600;
  letter-spacing: 2px;
}

.auth-footer {
  margin-top: 2px;
  text-align: center;
  font-size: 14px;
  color: var(--app-text-3);
}
</style>
