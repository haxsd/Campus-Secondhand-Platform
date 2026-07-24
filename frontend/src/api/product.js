// 商品接口层：页面只调这里的函数，不关心数据来自 mock 还是真实后端。
// 将来后端写好后，把 .env.development 里的 VITE_USE_MOCK 改成 false 即可联调，页面代码零改动。
import { get, post, put } from '@/utils/request'
import {
  mockGetProducts,
  mockGetProductDetail,
  mockCreateProduct,
  mockUpdateProduct,
} from '@/mock/product'

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

// POST /api/products —— 发布商品（创建草稿）
// data: { title, description, price, stock, itemCondition, categoryId, campus, tradePlace, images }
// 返回：{ id }
export function createProduct(data) {
  if (useMock) return mockCreateProduct(data)
  return post('/products', data)
}

// PUT /api/products/{id} —— 编辑商品（仅草稿/驳回/已下架状态可编辑，需带 version 做乐观锁）
// version 不匹配后端返回 409（商品已被其他请求修改），页面提示刷新后重试
export function updateProduct(id, data) {
  if (useMock) return mockUpdateProduct(id, data)
  return put(`/products/${id}`, data)
}
