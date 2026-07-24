// 浏览记录接口层：页面只调这里，mock/真实由 VITE_USE_MOCK 切换。
import { get } from '@/utils/request'
import { mockGetBrowseHistory } from '@/mock/history'

const useMock = import.meta.env.VITE_USE_MOCK === 'true'

// GET /api/browse-history —— 我的浏览记录（按最后浏览时间倒序，分页）
// params: { page, pageSize }
export function getBrowseHistory(params) {
  if (useMock) return mockGetBrowseHistory(params)
  return get('/browse-history', params)
}
