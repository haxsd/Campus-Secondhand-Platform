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
