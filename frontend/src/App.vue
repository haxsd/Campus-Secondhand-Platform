<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { logout } from '@/api/auth'

const router = useRouter()
const userStore = useUserStore()
const keyword = ref('')

function onSearch() {
  router.push({ path: '/', query: keyword.value ? { keyword: keyword.value } : {} })
}

function onCommand(command) {
  if (command === 'logout') {
    ElMessageBox.confirm('确定退出登录吗？', '提示', { type: 'warning' }).then(async () => {
      await logout().catch(() => {})
      userStore.clearLogin()
      router.push('/')
    })
  } else {
    router.push(command)
  }
}
</script>

<template>
  <el-container class="app-container">
    <el-header class="app-header">
      <div class="header-inner">
        <div class="logo" @click="router.push('/')">校园二手</div>

        <el-input
          v-model="keyword"
          class="search-input"
          placeholder="搜索你想要的宝贝"
          clearable
          @keyup.enter="onSearch"
        >
          <template #append>
            <el-button :icon="Search" @click="onSearch" />
          </template>
        </el-input>

        <div class="header-right">
          <template v-if="userStore.isLoggedIn">
            <el-button type="primary" @click="router.push('/publish')">发布闲置</el-button>
            <el-dropdown @command="onCommand">
              <span class="user-entry">
                <el-avatar :size="32">{{ userStore.user?.nickname?.[0] || '我' }}</el-avatar>
                <span class="nickname">{{ userStore.user?.nickname }}</span>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="/my/products">我的商品</el-dropdown-item>
                  <el-dropdown-item command="/my/orders">我的订单</el-dropdown-item>
                  <el-dropdown-item command="/my/history">浏览记录</el-dropdown-item>
                  <el-dropdown-item v-if="userStore.isAdmin" command="/admin/products" divided>
                    商品审核
                  </el-dropdown-item>
                  <el-dropdown-item v-if="userStore.isAdmin" command="/admin/disputes">
                    纠纷处理
                  </el-dropdown-item>
                  <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button @click="router.push('/login')">登录</el-button>
            <el-button type="primary" @click="router.push('/register')">注册</el-button>
          </template>
        </div>
      </div>
    </el-header>

    <el-main class="app-main">
      <router-view />
    </el-main>
  </el-container>
</template>

<style scoped>
.app-container {
  min-height: 100vh;
}

.app-header {
  border-bottom: 1px solid #e4e7ed;
  background: #fff;
}

.header-inner {
  max-width: 1200px;
  margin: 0 auto;
  height: 60px;
  display: flex;
  align-items: center;
  gap: 24px;
}

.logo {
  font-size: 20px;
  font-weight: bold;
  color: #409eff;
  cursor: pointer;
  white-space: nowrap;
}

.search-input {
  max-width: 420px;
}

.header-right {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-entry {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.nickname {
  font-size: 14px;
  color: #303133;
}

.app-main {
  max-width: 1200px;
  width: 100%;
  margin: 0 auto;
}
</style>
