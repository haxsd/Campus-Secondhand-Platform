// 认证接口层：页面只调这里的函数，不关心数据来自 mock 还是真实后端。
// 每个函数的模式固定：useMock 为 true 走 src/mock/ 里的假数据，否则发真实 HTTP 请求。
// 将来后端写好后，只需把 .env.development 里 VITE_USE_MOCK 改成 false 即可联调，页面代码零改动。
import { get, post } from '@/utils/request'
import { mockLogin, mockRegister, mockLogout, mockMe } from '@/mock/auth'

const useMock = import.meta.env.VITE_USE_MOCK === 'true'

// POST /api/auth/login  { account, password } -> { token, user }
export function login(data) {
  if (useMock) return mockLogin(data)
  return post('/auth/login', data)
}

// POST /api/auth/register  { studentNo, phone, password, nickname, campus } -> null
export function register(data) {
  if (useMock) return mockRegister(data)
  return post('/auth/register', data)
}

// POST /api/auth/logout -> null
export function logout() {
  if (useMock) return mockLogout()
  return post('/auth/logout')
}

// GET /api/auth/me -> 当前用户信息
export function me() {
  if (useMock) return mockMe()
  return get('/auth/me')
}
