// 分类接口层：页面只调这里的函数，不关心数据来自 mock 还是真实后端。
// useMock 为 true 时走 src/mock/category.js 的假数据，否则发真实 HTTP 请求。
import { get } from '@/utils/request'
import { mockGetCategories } from '@/mock/category'

const useMock = import.meta.env.VITE_USE_MOCK === 'true'

// GET /api/categories -> [{ id, name }]（启用中的分类列表，未登录也能拿）
export function getCategories() {
  if (useMock) return mockGetCategories()
  return get('/categories')
}
