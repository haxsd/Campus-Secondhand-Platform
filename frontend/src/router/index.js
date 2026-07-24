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

// 全局前置守卫：登录校验 + 管理员校验
router.beforeEach((to) => {
  const userStore = useUserStore()
  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.meta.requiresAdmin && !userStore.isAdmin) {
    ElMessage.error('无权限访问')
    return { path: '/' }
  }
})

export default router
