// 管理端接口层：商品审核 + 纠纷处理。页面只调这里，mock/真实由 VITE_USE_MOCK 切换。
import { get, post } from '@/utils/request'
import { mockGetPendingProducts, mockReviewProduct } from '@/mock/product'
import { mockGetDisputes, mockHandleDispute } from '@/mock/dispute'

const useMock = import.meta.env.VITE_USE_MOCK === 'true'

// GET /api/admin/products/pending —— 待审核商品列表
export function getPendingProducts() {
  if (useMock) return mockGetPendingProducts()
  return get('/admin/products/pending')
}

// GET /api/admin/products/{id} —— 待审核商品完整资料，仅管理员审核详情页使用。
export function getPendingProductDetail(id) {
  if (useMock) return mockGetPendingProducts().then((res) => res.find((item) => String(item.id) === String(id)))
  return get(`/admin/products/${id}`)
}

// POST /api/admin/products/{id}/review —— 审核商品
// data: { pass: true } 或 { pass: false, reason }
export function reviewProduct(id, data) {
  if (useMock) return mockReviewProduct(id, data)
  return post(`/admin/products/${id}/review`, data)
}

export function getAiReview(id) {
  if (useMock) return Promise.resolve(null)
  return get(`/admin/products/${id}/ai-review`)
}

// GET /api/admin/disputes —— 纠纷列表（params: { status, cursorCreatedAt, cursorId, pageSize }）
export function getDisputes(params) {
  if (useMock) return mockGetDisputes(params)
  return get('/admin/disputes', params)
}

// POST /api/admin/disputes/{id}/handle —— 处理纠纷
// data: { action, restock?, note }
export function handleDispute(id, data) {
  if (useMock) return mockHandleDispute(id, data)
  return post(`/admin/disputes/${id}/handle`, data)
}

// GET /api/admin/disputes/{id} —— 管理员读取纠纷聚合详情
export function getDisputeDetail(id) {
  return get(`/admin/disputes/${id}`)
}
