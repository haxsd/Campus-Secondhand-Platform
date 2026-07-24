// 认证模块 mock 数据：模拟后端的登录/注册逻辑，字段严格对齐 docs/API接口文档.md。
// 内置两个账号：普通用户 20230001 / Abc12345，管理员 admin / Admin123。
// 注意：mock 数据存在 JS 内存里，刷新页面就重置（新注册的账号会消失），这是正常现象。

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

// 模拟网络延迟：真实接口不会瞬间返回，加 300ms 让 loading 效果能被看到
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

// 登录：account 可以是学号或手机号（和 API 文档约定一致）
export function mockLogin({ account, password }) {
  const found = mockUsers.find(
    (u) => (u.studentNo === account || u.phone === account) && u.password === password,
  )
  if (!found) return fail('账号或密码错误')
  // 解构去掉 password 字段：返回给页面的用户信息不应包含密码
  const { password: _pwd, ...safe } = found
  return delay({
    token: `mock-token-${found.id}`,
    user: { id: safe.id, nickname: safe.nickname, avatar: safe.avatar, campus: safe.campus, role: safe.role },
  })
}

// 注册：模拟后端的唯一性校验（学号/手机号不能重复）
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
