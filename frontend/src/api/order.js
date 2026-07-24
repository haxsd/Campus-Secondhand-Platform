// 订单接口层：页面只调这里的函数，mock/真实由 VITE_USE_MOCK 切换。
import { get, post } from '@/utils/request'
import {
  mockCreateOrder,
  mockGetOrders,
  mockGetOrderDetail,
  mockConfirmOrder,
  mockCancelOrder,
  mockCompleteOrder,
} from '@/mock/order'

const useMock = import.meta.env.VITE_USE_MOCK === 'true'

// POST /api/orders —— 创建订单（下单）
// data: { productId, quantity, tradeTime, tradePlace, remark, requestId }
// 返回：{ id, orderNo, status, confirmDeadline }
export function createOrder(data) {
  if (useMock) return mockCreateOrder(data)
  return post('/orders', data)
}

// GET /api/orders —— 我的订单列表
// params: { role: 'buyer'|'seller', status, page, pageSize }
// 返回：{ list, total, page, pageSize }
export function getOrders(params) {
  if (useMock) return mockGetOrders(params)
  return get('/orders', params)
}

// GET /api/orders/{id} —— 订单详情（含快照、状态日志、can* 操作标记）
export function getOrderDetail(id) {
  if (useMock) return mockGetOrderDetail(id)
  return get(`/orders/${id}`)
}

// POST /api/orders/{id}/confirm —— 卖家确认（0→1）
export function confirmOrder(id) {
  if (useMock) return mockConfirmOrder(id)
  return post(`/orders/${id}/confirm`)
}

// POST /api/orders/{id}/cancel —— 买/卖家取消（0或1→3），body: { reason }
export function cancelOrder(id, reason) {
  if (useMock) return mockCancelOrder(id, reason)
  return post(`/orders/${id}/cancel`, { reason })
}

// POST /api/orders/{id}/complete —— 买家确认完成（1→2）
export function completeOrder(id) {
  if (useMock) return mockCompleteOrder(id)
  return post(`/orders/${id}/complete`)
}
