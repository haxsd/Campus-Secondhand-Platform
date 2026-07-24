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

// POST /api/admin/products/{id}/review —— 审核商品
// data: { pass: true } 或 { pass: false, reason }
export function reviewProduct(id, data) {
  if (useMock) return mockReviewProduct(id, data)
  return post(`/admin/products/${id}/review`, data)
}

// GET /api/admin/disputes —— 纠纷列表（params: { status, page, pageSize }）
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
