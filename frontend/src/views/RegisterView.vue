<script setup>
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
      pattern: /^(?=.*[A-Za-z])(?=.*\d).{8,20}$/,
      message: '密码 8~20 位，需同时包含字母和数字',
      trigger: 'blur',
    },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
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
    <el-card class="auth-card">
      <template #header>
        <div class="auth-title">注册</div>
      </template>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px" @submit.prevent>
        <el-form-item label="学号" prop="studentNo">
          <el-input v-model="form.studentNo" placeholder="学号" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="手机号" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="8~20 位，含字母和数字" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="再次输入密码" show-password />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" placeholder="昵称" />
        </el-form-item>
        <el-form-item label="校区" prop="campus">
          <el-select v-model="form.campus" placeholder="请选择校区" style="width: 100%">
            <el-option v-for="c in CAMPUS_LIST" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="submit-btn" :loading="loading" @click="onSubmit">
            注册
          </el-button>
        </el-form-item>
      </el-form>
      <div class="auth-footer">
        已有账号？
        <el-link type="primary" @click="router.push('/login')">去登录</el-link>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.auth-page {
  display: flex;
  justify-content: center;
  padding-top: 60px;
}

.auth-card {
  width: 440px;
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
