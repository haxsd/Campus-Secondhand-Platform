// 评价模块 mock。评价依附于"已完成订单"，所以要读订单内存表：
// 提交成功后把订单标记为已评价（order.reviewed = true），这样详情页的 canReview 会变 false。
import { ElMessage } from 'element-plus'
import { findOrder, CURRENT_USER } from '@/mock/order'

// 评价内存表（导出以便以后做"卖家收到的评价"列表时复用）
export const MOCK_REVIEWS = []

function delay(data, ms = 300) {
  return new Promise((resolve) => setTimeout(() => resolve(data), ms))
}

// 失败：先弹提示再 reject，和 request.js / 其它 mock 保持一致
function fail(code, message) {
  return new Promise((_, reject) =>
    setTimeout(() => {
      ElMessage.error(message)
      reject({ code, message, data: null })
    }, 300),
  )
}

function nowStr() {
  const d = new Date()
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

// POST /api/reviews —— 提交评价
// data: { orderId, rating(1~5), content, tags(逗号分隔字符串) }
export function mockCreateReview(data) {
  const { orderId, rating, content, tags } = data
  const o = findOrder(orderId)
  if (!o) return fail(404, '订单不存在')
  // 只有"已完成(2)"的订单能评价，且只能买家评价
  if (o.status !== 2) return fail(409, '仅已完成的订单可以评价')
  if (o.buyer.id !== CURRENT_USER.id) return fail(403, '只有买家可以评价')
  // 一单一评：重复评价返回 409（对应后端唯一约束）
  if (o.reviewed) return fail(409, '该订单已评价')

  o.reviewed = true
  MOCK_REVIEWS.push({
    orderId: String(orderId),
    rating,
    content: content || '',
    tags: tags || '',
    createdAt: nowStr(),
  })
  return delay(null)
}
