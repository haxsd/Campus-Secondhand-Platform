// 用户登录态 store（Pinia）：全局唯一的"当前登录用户"数据源。
// 顶栏显示、路由守卫、请求拦截器都从这里读登录状态。
// 同时同步到 localStorage，让刷新页面后登录态不丢失。
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

// localStorage 的 key 加 ct_ 前缀（campus trade），避免和其他本地项目冲突
const TOKEN_KEY = 'ct_token'
const USER_KEY = 'ct_user'

export const useUserStore = defineStore('user', () => {
  // 初始化时从 localStorage 恢复（页面刷新后 JS 内存清空，但 localStorage 还在）
  const token = ref(localStorage.getItem(TOKEN_KEY) || '')
  const user = ref(JSON.parse(localStorage.getItem(USER_KEY) || 'null'))

  // 计算属性：有 token 即视为已登录（token 是否真的有效由后端校验，401 时会被动登出）
  const isLoggedIn = computed(() => !!token.value)
  // role: 0=普通用户 1=管理员（与数据库 user.role 编码一致）
  const isAdmin = computed(() => user.value?.role === 1)

  // 登录成功后调用：同时写内存（响应式，界面立刻变化）和 localStorage（刷新不丢）
  function setLogin(newToken, newUser) {
    token.value = newToken
    user.value = newUser
    localStorage.setItem(TOKEN_KEY, newToken)
    localStorage.setItem(USER_KEY, JSON.stringify(newUser))
  }

  // 个人资料保存后调用：不改变 token，只同步内存和 localStorage 中的用户展示信息。
  // 合并而非直接覆盖，能兼容后端个人资料接口未来只返回部分可修改字段的情况。
  function updateUser(newUser) {
    user.value = { ...user.value, ...newUser }
    localStorage.setItem(USER_KEY, JSON.stringify(user.value))
  }

  // 退出登录 / token 过期时调用：两边都清掉
  function clearLogin() {
    token.value = ''
    user.value = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }

  return { token, user, isLoggedIn, isAdmin, setLogin, updateUser, clearLogin }
})
