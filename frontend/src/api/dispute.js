// 纠纷接口层：页面只调这里，mock/真实由 VITE_USE_MOCK 切换。
import { post } from '@/utils/request'
import { mockCreateDispute } from '@/mock/dispute'

const useMock = import.meta.env.VITE_USE_MOCK === 'true'

// POST /api/disputes —— 发起纠纷
// data: { orderId, reasonType, statement, evidence }
export function createDispute(data) {
  if (useMock) return mockCreateDispute(data)
  return post('/disputes', data)
}
