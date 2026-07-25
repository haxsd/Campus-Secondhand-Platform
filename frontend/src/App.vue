<script setup>
// 全局布局组件：所有页面共用的"顶栏 + 内容区"。
// 顶栏：Logo、登录/注册按钮（未登录）或 发布按钮+头像下拉菜单（已登录）。
//       （搜索统一放在首页筛选栏，顶栏不再单独放全局搜索框）
// 内容区：<router-view> 根据当前 URL 渲染对应的页面组件。
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { logout } from '@/api/auth'

const router = useRouter()
const userStore = useUserStore()

// 头像下拉菜单的点击处理：command 要么是路由地址（直接跳转），要么是 'logout'
function onCommand(command) {
  if (command === 'logout') {
    ElMessageBox.confirm('确定退出登录吗？', '提示', { type: 'warning' }).then(async () => {
      // 先通知后端使 token 失效（失败也不阻塞本地退出），再清本地登录态
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
        <div class="logo" @click="router.push('/')">校园二手平台</div>

        <!-- 显式首页按钮：用户进入发布、订单或管理页面后可随时返回商品首页。 -->
        <el-button class="home-button" text @click="router.push('/')">首页</el-button>

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

.home-button {
  color: #606266;
}

.home-button:hover {
  color: #409eff;
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
