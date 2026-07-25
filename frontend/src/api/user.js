// 个人中心接口层：页面只调用语义化函数，不直接处理 /users/me 的 HTTP 细节。
// 当 VITE_USE_MOCK=true 时仍走 mock，实现与真实后端相同的数据结构。
import { get, put } from '@/utils/request'
import { mockChangePassword, mockGetProfile, mockUpdateProfile } from '@/mock/auth'

const useMock = import.meta.env.VITE_USE_MOCK === 'true'

// GET /api/users/me -> 当前用户可查看的完整资料（含只读的学号、手机号）
export function getMyProfile() {
  if (useMock) return mockGetProfile()
  return get('/users/me')
}

// PUT /api/users/me -> { nickname, campus, avatar }
export function updateMyProfile(data) {
  if (useMock) return mockUpdateProfile(data)
  return put('/users/me', data)
}

// PUT /api/users/me/password -> { oldPassword, newPassword }
export function changeMyPassword(data) {
  if (useMock) return mockChangePassword(data)
  return put('/users/me/password', data)
}
