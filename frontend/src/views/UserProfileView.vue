<script setup>
// 个人中心页面：资料编辑与密码修改放在同一页面的两个标签中。
// 学号、手机号仅展示，不提供可编辑控件；身份类字段必须由后端维护。
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { CAMPUS_LIST } from '@/constants'
import { changeMyPassword, getMyProfile, updateMyProfile } from '@/api/user'
import { useUserStore } from '@/stores/user'
import ImageUploader from '@/components/ImageUploader.vue'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const savingProfile = ref(false)
const changingPassword = ref(false)
const profileFormRef = ref()
const passwordFormRef = ref()

// profile 是服务端原始展示数据；profileForm 只保存可修改字段，避免误把只读身份字段提交出去。
const profile = ref(null)
const profileForm = reactive({
  nickname: '',
  campus: '',
})
// ImageUploader 使用 URL 数组模型。个人头像最多一张，提交时取数组第一个元素即可。
const avatarImages = ref([])

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const profileRules = {
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { max: 30, message: '昵称不能超过 30 个字符', trigger: 'blur' },
  ],
  campus: [{ required: true, message: '请选择校区', trigger: 'change' }],
}

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    {
      pattern: /^(?=.*[A-Za-z])(?=.*\d).{8,20}$/,
      message: '密码 8~20 位，需同时包含字母和数字',
      trigger: 'blur',
    },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) =>
        value === passwordForm.newPassword ? callback() : callback(new Error('两次输入的新密码不一致')),
      trigger: 'blur',
    },
  ],
}

// 加载后把服务端数据拆到“展示数据”和“可编辑表单”中，防止页面直接修改原始对象。
async function loadProfile() {
  loading.value = true
  try {
    const data = await getMyProfile()
    profile.value = data
    profileForm.nickname = data.nickname || ''
    profileForm.campus = data.campus || ''
    avatarImages.value = data.avatar ? [data.avatar] : []
  } finally {
    loading.value = false
  }
}

// 资料保存成功后，同步 Pinia，顶栏昵称和头像无需刷新页面即可更新。
async function saveProfile() {
  const valid = await profileFormRef.value.validate().catch(() => false)
  if (!valid) return

  savingProfile.value = true
  try {
    const data = await updateMyProfile({
      nickname: profileForm.nickname,
      campus: profileForm.campus,
      avatar: avatarImages.value[0] || '',
    })
    profile.value = data
    userStore.updateUser(data)
    ElMessage.success('个人资料已保存')
  } finally {
    savingProfile.value = false
  }
}

// 密码变更成功后，后端已使当前 token 失效；前端同步清理本地状态并要求重新登录。
async function savePassword() {
  const valid = await passwordFormRef.value.validate().catch(() => false)
  if (!valid) return

  changingPassword.value = true
  try {
    await changeMyPassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
    })
    userStore.clearLogin()
    ElMessage.success('密码已修改，请使用新密码重新登录')
    router.replace('/login')
  } finally {
    changingPassword.value = false
  }
}

onMounted(loadProfile)
</script>

<template>
  <div v-loading="loading" class="profile-page">
    <el-card shadow="never">
      <template #header>个人中心</template>

      <el-tabs>
        <el-tab-pane label="个人资料">
          <!-- 标签宽度留出余量，防止浏览器缩放时“确认新密码”等中文标签折行。 -->
          <el-form ref="profileFormRef" :model="profileForm" :rules="profileRules" label-width="110px" class="profile-form">
            <el-form-item label="学号">
              <el-input :model-value="profile?.studentNo || ''" disabled />
              <div class="field-tip">学号是登录账号，暂不支持修改。</div>
            </el-form-item>

            <el-form-item label="手机号">
              <el-input :model-value="profile?.phone || ''" disabled />
              <div class="field-tip">手机号是登录账号，暂不支持修改。</div>
            </el-form-item>

            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="profileForm.nickname" maxlength="30" show-word-limit placeholder="请输入昵称" />
            </el-form-item>

            <el-form-item label="所在校区" prop="campus">
              <el-select v-model="profileForm.campus" placeholder="请选择校区" class="form-control">
                <el-option v-for="campus in CAMPUS_LIST" :key="campus" :label="campus" :value="campus" />
              </el-select>
            </el-form-item>

            <el-form-item label="头像">
              <!-- 复用商品图片上传组件，限制为 1 张；删除图片即可恢复系统默认首字母头像。 -->
              <ImageUploader v-model="avatarImages" :max="1" />
              <div class="field-tip">支持 jpg、png、webp，单张不超过 5MB；删除图片可恢复默认头像。</div>
            </el-form-item>

            <el-form-item>
              <el-button type="primary" :loading="savingProfile" @click="saveProfile">保存资料</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="修改密码">
          <el-alert
            title="修改密码后，当前设备会退出登录，需要使用新密码重新登录。"
            type="warning"
            :closable="false"
            show-icon
            class="password-alert"
          />
          <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="110px" class="password-form">
            <el-form-item label="当前密码" prop="oldPassword">
              <el-input v-model="passwordForm.oldPassword" type="password" show-password autocomplete="current-password" />
            </el-form-item>

            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="passwordForm.newPassword" type="password" show-password autocomplete="new-password" />
            </el-form-item>

            <el-form-item label="确认新密码" prop="confirmPassword">
              <el-input v-model="passwordForm.confirmPassword" type="password" show-password autocomplete="new-password" />
            </el-form-item>

            <el-form-item>
              <el-button type="primary" :loading="changingPassword" @click="savePassword">修改密码</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<style scoped>
.profile-page {
  max-width: 760px;
  margin: 0 auto;
  padding: 16px 0;
}

.profile-form,
.password-form {
  max-width: 560px;
  margin-top: 16px;
}

.form-control {
  width: 100%;
}

.field-tip {
  margin-top: 4px;
  color: #909399;
  font-size: 12px;
  line-height: 1.5;
}

.password-alert {
  max-width: 560px;
  margin-top: 16px;
}

/* Element Plus 会从标签宽度中扣除右侧间距；禁止折行可避免中文字段名在缩放时变成两行。 */
:deep(.el-form-item__label) {
  white-space: nowrap;
}

/* 窄屏时改为“标签在上、控件在下”，避免输入框被过度压缩。 */
@media (max-width: 600px) {
  .profile-page {
    padding: 12px 0;
  }

  :deep(.el-form-item) {
    display: block;
  }

  :deep(.el-form-item__label) {
    display: block;
    width: 100% !important;
    padding-bottom: 6px;
    line-height: 1.5;
    text-align: left;
  }

  :deep(.el-form-item__content) {
    display: block;
    margin-left: 0 !important;
  }
}
</style>
