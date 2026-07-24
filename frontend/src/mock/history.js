// 浏览记录 mock。真实后端是"打开商品详情接口时由服务端顺手写一条浏览记录"，
// 前端不单独调写接口。这里同样在 mockGetProductDetail 里调用 recordBrowse 来模拟写入。
// 记录按"最后浏览时间"倒序，同一商品重复浏览只更新时间并挪到最前（去重）。

// 浏览记录内存表（每项含卡片展示所需字段 + 商品 id + 最后浏览时间）
export const MOCK_HISTORY = []

function delay(data, ms = 300) {
  return new Promise((resolve) => setTimeout(() => resolve(data), ms))
}

function nowStr() {
  const d = new Date()
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

// 记录一次浏览：由商品详情 mock 调用，传入完整商品对象。
// 已存在同一商品 → 先移除旧记录，再把新记录插到最前（等价于"更新最后浏览时间并置顶"）。
export function recordBrowse(product) {
  const idx = MOCK_HISTORY.findIndex((h) => h.productId === product.id)
  if (idx >= 0) MOCK_HISTORY.splice(idx, 1)
  MOCK_HISTORY.unshift({
    // ProductCard 需要的字段（id/cover/title/price/itemCondition/campus）
    id: product.id,
    productId: product.id,
    title: product.title,
    cover: product.cover,
    price: product.price,
    itemCondition: product.itemCondition,
    campus: product.campus,
    lastViewTime: nowStr(),
  })
}

// GET /api/browse-history —— 我的浏览记录（已按最后浏览时间倒序，分页）
export function mockGetBrowseHistory(params = {}) {
  const { page = 1, pageSize = 12 } = params
  const total = MOCK_HISTORY.length
  const start = (page - 1) * pageSize
  const list = MOCK_HISTORY.slice(start, start + pageSize)
  return delay({ list, total, page, pageSize })
}
