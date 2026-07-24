// 评价接口层：页面只调这里，mock/真实由 VITE_USE_MOCK 切换。
import { post } from '@/utils/request'
import { mockCreateReview } from '@/mock/review'

const useMock = import.meta.env.VITE_USE_MOCK === 'true'

// POST /api/reviews —— 提交评价
// data: { orderId, rating, content, tags }
export function createReview(data) {
  if (useMock) return mockCreateReview(data)
  return post('/reviews', data)
}
