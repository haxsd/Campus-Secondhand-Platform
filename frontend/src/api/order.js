// 订单接口层：页面只调这里的函数，mock/真实由 VITE_USE_MOCK 切换。
import { get, post } from '@/utils/request'
import { mockCreateOrder, mockGetOrders } from '@/mock/order'

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
