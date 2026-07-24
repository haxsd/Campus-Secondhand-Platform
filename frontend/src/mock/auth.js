// 认证模块 mock 数据：字段严格对齐 docs/API接口文档.md
// 内置两个账号：普通用户 20230001 / Abc12345，管理员 admin / Admin123

import { ElMessage } from 'element-plus'

const mockUsers = [
  {
    id: '1',
    studentNo: '20230001',
    phone: '13812345678',
    password: 'Abc12345',
    nickname: '小明',
    avatar: null,
    campus: '东校区',
    role: 0,
  },
  {
    id: '2',
    studentNo: 'admin',
    phone: '13900000000',
    password: 'Admin123',
    nickname: '管理员',
    avatar: null,
    campus: '东校区',
    role: 1,
  },
]

function delay(data, ms = 300) {
  return new Promise((resolve) => setTimeout(() => resolve(data), ms))
}

// 模拟 request.js 对失败响应的处理：弹出错误提示后 reject
function fail(message) {
  return new Promise((_, reject) =>
    setTimeout(() => {
      ElMessage.error(message)
      reject({ code: 400, message, data: null })
    }, 300),
  )
}

export function mockLogin({ account, password }) {
  const found = mockUsers.find(
    (u) => (u.studentNo === account || u.phone === account) && u.password === password,
  )
  if (!found) return fail('账号或密码错误')
  const { password: _pwd, ...safe } = found
  return delay({
    token: `mock-token-${found.id}`,
    user: { id: safe.id, nickname: safe.nickname, avatar: safe.avatar, campus: safe.campus, role: safe.role },
  })
}

export function mockRegister({ studentNo, phone, password, nickname, campus }) {
  if (mockUsers.some((u) => u.studentNo === studentNo || u.phone === phone)) {
    return fail('学号或手机号已注册')
  }
  mockUsers.push({
    id: String(mockUsers.length + 1),
    studentNo,
    phone,
    password,
    nickname,
    avatar: null,
    campus,
    role: 0,
  })
  return delay(null)
}

export function mockLogout() {
  return delay(null)
}

export function mockMe() {
  const { password: _pwd, ...safe } = mockUsers[0]
  return delay(safe)
}
