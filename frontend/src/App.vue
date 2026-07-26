<script setup>
// 全局布局组件：所有页面共用的"顶栏 + 内容区 + 页脚"。
// 顶栏：Logo、首页入口、登录/注册按钮（未登录）或 发布按钮+头像下拉菜单（已登录）。
//       （搜索统一放在首页筛选栏，顶栏不再单独放全局搜索框）
// 内容区：<router-view> 根据当前 URL 渲染对应的页面组件。
// 视觉说明：顶栏做了"毛玻璃吸顶"效果；页面版心宽度由各页面的 .page 类自行约束，
//          这样首页横幅、登录页背景等可以铺满整个视口宽度。
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import {
  Plus,
  ArrowDown,
  User,
  Goods,
  Tickets,
  Clock,
  Checked,
  Warning,
  SwitchButton,
} from '@element-plus/icons-vue'
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
    <el-header class="app-header" height="64px">
      <div class="header-inner">
        <!-- 品牌 Logo：渐变图标 + 站名 -->
        <div class="logo" @click="router.push('/')">
          <span class="logo-mark">
            <svg viewBox="0 0 64 64" width="20" height="20" aria-hidden="true">
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
          <span class="logo-text">校园二手平台</span>
        </div>

        <!-- 显式首页按钮：用户进入发布、订单或管理页面后可随时返回商品首页。 -->
        <button class="nav-link" @click="router.push('/')">首页</button>

        <div class="header-right">
          <template v-if="userStore.isLoggedIn">
            <el-button type="primary" round :icon="Plus" @click="router.push('/publish')">
              发布闲置
            </el-button>
            <el-dropdown @command="onCommand">
              <span class="user-entry">
                <el-avatar :size="34" :src="userStore.user?.avatar || undefined">
                  {{ userStore.user?.nickname?.[0] || '我' }}
                </el-avatar>
                <span class="nickname">{{ userStore.user?.nickname }}</span>
                <el-icon class="caret"><ArrowDown /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="/my/profile" :icon="User">个人中心</el-dropdown-item>
                  <el-dropdown-item command="/my/products" :icon="Goods">我的商品</el-dropdown-item>
                  <el-dropdown-item command="/my/orders" :icon="Tickets">我的订单</el-dropdown-item>
                  <el-dropdown-item command="/my/history" :icon="Clock">浏览记录</el-dropdown-item>
                  <el-dropdown-item
                    v-if="userStore.isAdmin"
                    command="/admin/products"
                    :icon="Checked"
                    divided
                  >
                    商品审核
                  </el-dropdown-item>
                  <el-dropdown-item
                    v-if="userStore.isAdmin"
                    command="/admin/disputes"
                    :icon="Warning"
                  >
                    纠纷处理
                  </el-dropdown-item>
                  <el-dropdown-item command="logout" :icon="SwitchButton" divided>
                    退出登录
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button text class="login-btn" @click="router.push('/login')">登录</el-button>
            <el-button type="primary" round @click="router.push('/register')">注册</el-button>
          </template>
        </div>
      </div>
    </el-header>

    <el-main class="app-main">
      <router-view />
    </el-main>

    <!-- 页脚：品牌信息 + 一句话简介 -->
    <footer class="app-footer">
      <div class="footer-inner">
        <div class="footer-brand">
          <span class="footer-dot"></span>
          校园二手平台
        </div>
        <p class="footer-slogan">让闲置好物在校园里继续发光 · 线上撮合 · 线下面交</p>
        <p class="footer-meta">仅供校内学习交流使用 · Campus Secondhand Platform</p>
      </div>
    </footer>
  </el-container>
</template>

<style scoped>
.app-container {
  min-height: 100vh;
}

/* 顶栏：毛玻璃吸顶 */
.app-header {
  position: sticky;
  top: 0;
  z-index: 100;
  padding: 0 20px;
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: saturate(180%) blur(14px);
  -webkit-backdrop-filter: saturate(180%) blur(14px);
  border-bottom: 1px solid rgba(31, 66, 52, 0.07);
}

.header-inner {
  max-width: 1200px;
  margin: 0 auto;
  height: 64px;
  display: flex;
  align-items: center;
  gap: 20px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  white-space: nowrap;
  user-select: none;
}

.logo-mark {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 10px;
  background: var(--app-gradient);
  box-shadow: 0 3px 10px rgba(16, 185, 129, 0.35);
}

.logo-text {
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 0.5px;
  background: var(--app-gradient);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

/* 顶栏导航项：小胶囊 */
.nav-link {
  border: none;
  background: transparent;
  padding: 7px 14px;
  border-radius: 999px;
  font-size: 14px;
  font-family: inherit;
  color: var(--app-text-2);
  cursor: pointer;
  transition: all 0.2s ease;
}

.nav-link:hover {
  color: var(--el-color-primary-dark-2);
  background: var(--app-bg-soft);
}

.header-right {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 14px;
}

.login-btn {
  color: var(--app-text-2);
  font-size: 14px;
}

.user-entry {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 10px 4px 4px;
  border-radius: 999px;
  cursor: pointer;
  outline: none;
  transition: background 0.2s ease;
}

.user-entry:hover {
  background: var(--app-bg-soft);
}

.nickname {
  font-size: 14px;
  font-weight: 500;
  color: var(--app-text-1);
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.caret {
  font-size: 12px;
  color: var(--app-text-3);
}

/* 内容区：不限宽（版心由各页面的 .page 控制），去掉默认内边距 */
.app-main {
  padding: 0;
  width: 100%;
}

/* 页脚 */
.app-footer {
  margin-top: auto;
  border-top: 1px solid var(--app-border);
  background: #fbfdfc;
}

.footer-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 28px 20px 32px;
  text-align: center;
}

.footer-brand {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: var(--app-text-1);
}

.footer-dot {
  width: 10px;
  height: 10px;
  border-radius: 999px;
  background: var(--app-gradient);
}

.footer-slogan {
  margin: 8px 0 4px;
  font-size: 13px;
  color: var(--app-text-2);
}

.footer-meta {
  margin: 0;
  font-size: 12px;
  color: var(--app-text-3);
}
</style>
