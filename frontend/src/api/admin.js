import { get, post } from '@/utils/request'
import { mockGetPendingProducts, mockReviewProduct } from '@/mock/product'
import { mockGetDisputes, mockHandleDispute } from '@/mock/dispute'

const useMock = import.meta.env.VITE_USE_MOCK === 'true'
export function getPendingProducts() { if (useMock) return mockGetPendingProducts(); return get('/admin/products/pending') }
export function getPendingProductDetail(id) { if (useMock) return mockGetPendingProducts().then((res) => res.find((item) => String(item.id) === String(id))); return get(`/admin/products/${id}`) }
export function reviewProduct(id, data) { if (useMock) return mockReviewProduct(id, data); return post(`/admin/products/${id}/review`, data) }
export function getAiReview(id) { if (useMock) return Promise.resolve(null); return get(`/admin/products/${id}/ai-review`) }
export function getDisputes(params) { if (useMock) return mockGetDisputes(params); return get('/admin/disputes', params) }
export function getDisputeDetail(id) { if (useMock) return Promise.resolve(null); return get(`/admin/disputes/${id}`) }
export function handleDispute(id, data) { if (useMock) return mockHandleDispute(id, data); return post(`/admin/disputes/${id}/handle`, data) }
