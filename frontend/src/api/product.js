// 商品接口层：页面只调这里的函数，不关心数据来自 mock 还是真实后端。
// 将来后端写好后，把 .env.development 里的 VITE_USE_MOCK 改成 false 即可联调，页面代码零改动。
import { get } from '@/utils/request'
import { mockGetProducts, mockGetProductDetail } from '@/mock/product'

const useMock = import.meta.env.VITE_USE_MOCK === 'true'

// GET /api/products —— 商品列表（只返回在售且 stock>0）
// params: { keyword, categoryId, campus, minPrice, maxPrice, page, pageSize }
// 返回：{ list, total, page, pageSize }
export function getProducts(params) {
  if (useMock) return mockGetProducts(params)
  return get('/products', params)
}

// GET /api/products/{id} —— 商品公开详情（含卖家信用摘要、最近评价）
export function getProductDetail(id) {
  if (useMock) return mockGetProductDetail(id)
  return get(`/products/${id}`)
}
