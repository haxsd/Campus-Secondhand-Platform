// 纠纷接口层：页面只调这里，mock/真实由 VITE_USE_MOCK 切换。
import { get, post } from '@/utils/request'
import { mockCreateDispute } from '@/mock/dispute'

const useMock = import.meta.env.VITE_USE_MOCK === 'true'

// POST /api/disputes —— 发起纠纷
// data: { orderId, reasonType, statement, evidence }
export function createDispute(data) {
  if (useMock) return mockCreateDispute(data)
  return post('/disputes', data)
}

// GET /api/disputes/{id} —— 当事人查看自己的纠纷详情
export function getDispute(id) {
  return get(`/disputes/${id}`)
}

// GET /api/disputes/by-order/{orderId} —— 订单详情读取关联纠纷
export function getDisputeByOrder(orderId) {
  return get(`/disputes/by-order/${orderId}`)
}

// POST /api/disputes/{id}/evidence —— 追加文字和图片证据
export function appendDisputeEvidence(id, data) {
  return post(`/disputes/${id}/evidence`, data)
}
