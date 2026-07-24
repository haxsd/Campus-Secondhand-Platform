// 分类模块 mock 数据：对应后端 GET /api/categories（启用中的分类列表）。
// 字段严格对齐 docs/API接口文档.md：分类返回 [{ id, name }]。
// 这 6 条与 deploy/sql/init.sql 里的初始分类数据一一对应，联调时后端会返回同样的分类。

// 模拟网络延迟：真实接口不会瞬间返回，加一点延迟让页面 loading 效果能被看到
function delay(data, ms = 200) {
  return new Promise((resolve) => setTimeout(() => resolve(data), ms))
}

// 注意：id 用字符串。后端主键是 BIGINT，JS 的 Number 最大安全整数是 2^53，
// 大 id 会精度丢失，所以全项目约定 id 一律当字符串处理（和 API 文档一致）。
export const MOCK_CATEGORIES = [
  { id: '1', name: '数码电子' },
  { id: '2', name: '图书教材' },
  { id: '3', name: '生活用品' },
  { id: '4', name: '运动户外' },
  { id: '5', name: '服饰美妆' },
  { id: '6', name: '其他' },
]

// GET /api/categories 的 mock 实现
export function mockGetCategories() {
  return delay(MOCK_CATEGORIES)
}
