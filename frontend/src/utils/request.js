// Axios 请求封装：全项目唯一发 HTTP 请求的地方。
// 页面/接口层不直接 import axios，而是用这里导出的 get/post/put/del，
// 这样"带 token、统一错误提示、401 跳登录"这些逻辑只需要写一次。
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import router from '@/router'

// 创建独立的 axios 实例（不污染全局默认配置）
const instance = axios.create({
  baseURL: '/api', // 所有请求自动加 /api 前缀，如 post('/auth/login') 实际请求 /api/auth/login
  timeout: 10000, // 10 秒超时，防止请求卡死页面一直转圈
})

// ===== 请求拦截器：每个请求发出前都会经过这里 =====
// 作用：已登录时自动在请求头带上 token，后端据此识别用户身份
instance.interceptors.request.use((config) => {
  const userStore = useUserStore()
  if (userStore.token) {
    // API 文档约定的认证格式：Authorization: Bearer <token>
    config.headers.Authorization = `Bearer ${userStore.token}`
  }
  return config
})

// ===== 响应拦截器：每个响应到达页面前都会经过这里 =====
// 后端统一返回结构 { code, message, data }：
//   code === 0   业务成功 → 只把 data 给页面（页面拿到的直接就是数据本身）
//   code === 401 未登录/token 过期 → 清登录态并跳登录页
//   其他 code    业务失败 → 统一弹错误提示，页面 catch 后无需再弹
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
    // reject 让调用方的 catch 能感知失败（如按钮取消 loading 状态）
    return Promise.reject(body)
  },
  (error) => {
    // 进到这里说明 HTTP 层面就失败了（网络断开、后端没起、404/500 等）
    if (error.response?.status === 401) {
      handleUnauthorized()
    } else {
      ElMessage.error(error.response?.data?.message || '网络异常，请稍后重试')
    }
    return Promise.reject(error)
  },
)

// 401 统一处理：清空本地登录态 → 跳登录页，并把当前页面地址放进 redirect 参数，
// 这样登录成功后能自动跳回用户原来想去的页面
function handleUnauthorized() {
  const userStore = useUserStore()
  userStore.clearLogin()
  ElMessage.warning('登录已过期，请重新登录')
  router.push({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
}

// 对外暴露 4 个语义化方法，接口层（src/api/）只用这几个
export function get(url, params) {
  return instance.get(url, { params }) // params 会拼成 ?key=value 查询串
}

export function post(url, data) {
  return instance.post(url, data) // data 作为 JSON 请求体
}

export function put(url, data) {
  return instance.put(url, data)
}

export function del(url) {
  return instance.delete(url)
}

export default instance
