import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import router from '@/router'

const instance = axios.create({
  baseURL: '/api',
  timeout: 10000,
})

// 请求拦截器：自动携带 token
instance.interceptors.request.use((config) => {
  const userStore = useUserStore()
  if (userStore.token) {
    config.headers.Authorization = `Bearer ${userStore.token}`
  }
  return config
})

// 响应拦截器：统一处理 { code, message, data } 结构
instance.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body.code === 0) {
      return body.data
    }
    if (body.code === 401) {
      handleUnauthorized()
    } else {
      ElMessage.error(body.message || '请求失败')
    }
    return Promise.reject(body)
  },
  (error) => {
    if (error.response?.status === 401) {
      handleUnauthorized()
    } else {
      ElMessage.error(error.response?.data?.message || '网络异常，请稍后重试')
    }
    return Promise.reject(error)
  },
)

function handleUnauthorized() {
  const userStore = useUserStore()
  userStore.clearLogin()
  ElMessage.warning('登录已过期，请重新登录')
  router.push({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
}

export function get(url, params) {
  return instance.get(url, { params })
}

export function post(url, data) {
  return instance.post(url, data)
}

export function put(url, data) {
  return instance.put(url, data)
}

export function del(url) {
  return instance.delete(url)
}

export default instance
