// 路由配置：定义"URL 路径 → 页面组件"的映射，并用全局守卫做登录/权限拦截。
// component: () => import(...) 是"懒加载"：访问到该页面时才下载对应 JS，首屏更快。
// meta 是自定义标记：requiresAuth=需登录，requiresAdmin=需管理员，由下方守卫统一检查。
import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/login', name: 'login', component: () => import('@/views/LoginView.vue') },
    { path: '/register', name: 'register', component: () => import('@/views/RegisterView.vue') },
    { path: '/', name: 'home', component: () => import('@/views/HomeView.vue') },
    {
      path: '/products/:id',
      name: 'product-detail',
      component: () => import('@/views/ProductDetailView.vue'),
    },
    {
      path: '/publish',
      name: 'product-publish',
      component: () => import('@/views/ProductEditView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/products/:id/edit',
      name: 'product-edit',
      component: () => import('@/views/ProductEditView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/my/products',
      name: 'my-products',
      component: () => import('@/views/MyProductsView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/orders/create',
      name: 'order-create',
      component: () => import('@/views/OrderCreateView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/my/orders',
      name: 'my-orders',
      component: () => import('@/views/MyOrdersView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/orders/:id',
      name: 'order-detail',
      component: () => import('@/views/OrderDetailView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/my/history',
      name: 'browse-history',
      component: () => import('@/views/BrowseHistoryView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/admin/products',
      name: 'admin-products',
      component: () => import('@/views/AdminProductReviewView.vue'),
      meta: { requiresAuth: true, requiresAdmin: true },
    },
    {
      path: '/admin/disputes',
      name: 'admin-disputes',
      component: () => import('@/views/AdminDisputeView.vue'),
      meta: { requiresAuth: true, requiresAdmin: true },
    },
  ],
})

// 全局前置守卫：每次路由跳转前都会执行。
// 返回一个路由对象 = 重定向到该地址；不返回任何东西 = 放行。
// 注意：这只是前端体验层的拦截，真正的权限校验在后端（前端代码用户可篡改，不可信）。
router.beforeEach((to) => {
  const userStore = useUserStore()
  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    // redirect 带上目标地址，登录成功后 LoginView 会跳回这里
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.meta.requiresAdmin && !userStore.isAdmin) {
    ElMessage.error('无权限访问')
    return { path: '/' }
  }
})

export default router
