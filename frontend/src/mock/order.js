// 订单模块 mock。本板块（下单）先实现"创建订单"，订单列表/详情在后续板块补。
// 订单数据存在这个模块级数组里（内存态），刷新页面就重置——mock 的正常现象。
import { ElMessage } from 'element-plus'
import { findRawProduct } from '@/mock/product'

// 当前登录用户（mock 固定小明 id=1），下单时作为买家写进订单
// 导出给评价/纠纷模块判断"当前用户是不是这条订单的买家/卖家"
export const CURRENT_USER = { id: '1', nickname: '小明' }

// 订单内存表；导出给后续订单列表/详情板块复用
export const MOCK_ORDERS = []

// requestId → 已创建订单 的映射，用于"幂等"：同一个 requestId 重复提交只会返回第一次创建的订单
const REQUEST_INDEX = {}

let orderSeq = 100 // 订单自增 id 起点（对齐文档示例）

function delay(data, ms = 300) {
  return new Promise((resolve) => setTimeout(() => resolve(data), ms))
}

function fail(code, message) {
  // 和 request.js / 其它 mock 一致：先弹错误提示再 reject，页面 catch 不用重复弹
  return new Promise((_, reject) =>
    setTimeout(() => {
      ElMessage.error(message)
      reject({ code, message, data: null })
    }, 300),
  )
}

// 生成订单号：日期 + 4 位序号（和后端 orderNo 风格一致，仅用于展示）
function genOrderNo(seq) {
  return `20260724${String(seq).padStart(4, '0')}`
}

// 当前时间 + 24 小时，格式化成 'YYYY-MM-DD HH:mm:ss'（作为卖家确认截止时间）
function confirmDeadline() {
  const d = new Date(Date.now() + 24 * 3600 * 1000)
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

// POST /api/orders —— 创建订单（下单）
// data: { productId, quantity, tradeTime, tradePlace, remark, requestId }
export function mockCreateOrder(data) {
  const { productId, quantity, tradeTime, tradePlace, remark, requestId } = data

  // 1) 幂等：同一个 requestId 已创建过，直接返回第一次的订单（不重复下单）
  if (requestId && REQUEST_INDEX[requestId]) {
    const existed = REQUEST_INDEX[requestId]
    return delay({
      id: existed.id,
      orderNo: existed.orderNo,
      status: existed.status,
      confirmDeadline: existed.confirmDeadline,
    })
  }

  const product = findRawProduct(productId)
  if (!product) return fail(404, '商品不存在或已下架')

  // 2) 不能买自己发布的商品（买家=卖家）
  if (product.seller.id === CURRENT_USER.id) {
    return fail(400, '不能购买自己发布的商品')
  }

  // 3) 库存/状态校验：只有在售且库存足够才能下单，否则 409（对应后端条件更新影响行数为 0）
  if (product.status !== 3 || product.stock < quantity) {
    return fail(409, '库存不足或商品已变化')
  }

  // 4) 扣库存（模拟后端条件更新）；扣到 0 则商品变"已售罄"
  product.stock -= quantity
  if (product.stock === 0) product.status = 5

  // 5) 生成订单：带下单时的商品快照（后续商品改价/改信息都不影响已生成的订单）
  const id = String(++orderSeq)
  const order = {
    id,
    orderNo: genOrderNo(orderSeq),
    productId: String(productId), // 记住对应商品 id，取消订单时回补库存用（后端 order 表也有 product_id）
    status: 0, // 0=待卖家确认
    quantity,
    unitPrice: product.price,
    totalAmount: (Number(product.price) * quantity).toFixed(2),
    tradeTime,
    tradePlace,
    remark: remark || '',
    confirmDeadline: confirmDeadline(),
    finishedAt: null,
    createdAt: nowStr(),
    buyer: { ...CURRENT_USER },
    seller: { id: product.seller.id, nickname: product.seller.nickname },
    snapshot: {
      title: product.title,
      description: product.description,
      price: product.price,
      itemCondition: product.itemCondition,
      campus: product.campus,
      tradePlace: product.tradePlace,
      images: product.images,
    },
    logs: [],
  }

  MOCK_ORDERS.push(order)
  if (requestId) REQUEST_INDEX[requestId] = order

  return delay({
    id: order.id,
    orderNo: order.orderNo,
    status: order.status,
    confirmDeadline: order.confirmDeadline,
  })
}

// 当前时间字符串
function nowStr() {
  const d = new Date()
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

// ---- 以下为订单列表/详情板块用的 mock ----

// 造一条种子订单塞进 MOCK_ORDERS。
// asRole='buyer' 表示"我买到的"（我是买家）；asRole='seller' 表示"我卖出的"（我是卖家）。
// other 是交易对方的简要信息。快照取自对应商品当前信息（模拟下单当时的快照）。
function seedOrder({
  productId,
  status,
  quantity = 1,
  asRole,
  other,
  createdAt,
  finishedAt = null,
}) {
  const product = findRawProduct(productId)
  if (!product) return
  const seq = ++orderSeq
  const buyer = asRole === 'buyer' ? { ...CURRENT_USER } : other
  const seller =
    asRole === 'seller'
      ? { ...CURRENT_USER }
      : { id: product.seller.id, nickname: product.seller.nickname }
  MOCK_ORDERS.push({
    id: String(seq),
    orderNo: genOrderNo(seq),
    productId: String(productId),
    status,
    quantity,
    unitPrice: product.price,
    totalAmount: (Number(product.price) * quantity).toFixed(2),
    tradeTime: '2026-07-25 18:00:00',
    tradePlace: product.tradePlace,
    remark: '',
    confirmDeadline: '2026-07-25 09:00:00',
    finishedAt,
    createdAt,
    buyer,
    seller,
    snapshot: {
      title: product.title,
      description: product.description,
      price: product.price,
      itemCondition: product.itemCondition,
      campus: product.campus,
      tradePlace: product.tradePlace,
      images: product.images,
    },
    logs: [],
  })
}

// 我买到的（我是买家，向别的卖家下单），覆盖不同状态
seedOrder({ productId: '1', status: 0, asRole: 'buyer', createdAt: '2026-07-23 20:10:00' })
seedOrder({ productId: '5', status: 1, asRole: 'buyer', createdAt: '2026-07-22 15:30:00' })
seedOrder({
  productId: '9',
  status: 2,
  asRole: 'buyer',
  createdAt: '2026-07-20 11:00:00',
  finishedAt: '2026-07-21 12:00:00',
})
seedOrder({ productId: '13', status: 3, asRole: 'buyer', createdAt: '2026-07-19 09:00:00' })
seedOrder({ productId: '17', status: 5, asRole: 'buyer', createdAt: '2026-07-18 14:00:00' })

// 我卖出的（我是卖家，别人买我的商品）
const OTHER_BUYER = { id: '3', nickname: '爱看书的阿May' }
seedOrder({
  productId: 'm4',
  status: 0,
  asRole: 'seller',
  other: OTHER_BUYER,
  createdAt: '2026-07-23 21:00:00',
})
seedOrder({
  productId: 'm4',
  status: 1,
  asRole: 'seller',
  other: OTHER_BUYER,
  createdAt: '2026-07-22 10:00:00',
})
seedOrder({
  productId: 'm4',
  status: 2,
  asRole: 'seller',
  other: OTHER_BUYER,
  createdAt: '2026-07-21 16:00:00',
  finishedAt: '2026-07-22 09:00:00',
})

// GET /api/orders —— 我的订单列表
// params: { role: 'buyer'|'seller', status 可选, page, pageSize }
export function mockGetOrders(params = {}) {
  const { role = 'buyer', status, page = 1, pageSize = 10 } = params
  // 按角色过滤：卖出的看 seller 是我，买到的看 buyer 是我
  let filtered = MOCK_ORDERS.filter((o) =>
    role === 'seller' ? o.seller.id === CURRENT_USER.id : o.buyer.id === CURRENT_USER.id,
  )
  if (status !== undefined && status !== null && status !== '') {
    filtered = filtered.filter((o) => o.status === Number(status))
  }
  // 按创建时间倒序
  filtered = filtered.slice().sort((a, b) => (a.createdAt < b.createdAt ? 1 : -1))

  const total = filtered.length
  const start = (page - 1) * pageSize
  const list = filtered.slice(start, start + pageSize)
  return delay({ list, total, page, pageSize })
}

// 在内存表里按 id 找订单（导出给评价/纠纷模块复用，它们要读/改同一条订单）
export function findOrder(id) {
  return MOCK_ORDERS.find((o) => o.id === String(id))
}

// 计算当前登录用户对这条订单能做哪些操作（对应后端返回的 can* 字段）。
// 真实后端会按"当前用户身份 + 订单状态"算好给前端，前端据此显示按钮，后端仍做最终校验。
function canFlags(o) {
  const isBuyer = o.buyer.id === CURRENT_USER.id
  const isSeller = o.seller.id === CURRENT_USER.id
  return {
    canConfirm: isSeller && o.status === 0, // 卖家确认：待确认(0)
    canCancel: (isBuyer || isSeller) && (o.status === 0 || o.status === 1), // 待确认/已确认都能取消
    canComplete: isBuyer && o.status === 1, // 买家确认完成：已确认(1)
    canReview: isBuyer && o.status === 2 && !o.reviewed, // 已完成且未评价
    canDispute: (isBuyer || isSeller) && (o.status === 1 || o.status === 2) && !o.disputed, // 已确认/已完成且未发起过纠纷
  }
}

// 往订单状态日志里追加一条流转记录
// operatorType: 0=买家 1=卖家 2=系统 3=管理员（与后端编码一致）
function pushLog(o, fromStatus, toStatus, operatorType, reason = null) {
  o.logs.push({ fromStatus, toStatus, operatorType, reason, createdAt: nowStr() })
}

// 当前用户在这条订单里的操作方类型（用于写状态日志）
function operatorTypeOf(o) {
  return o.seller.id === CURRENT_USER.id ? 1 : 0
}

// GET /api/orders/{id} —— 订单详情（含快照、状态日志、can* 操作标记）
export function mockGetOrderDetail(id) {
  const o = findOrder(id)
  if (!o) return fail(404, '订单不存在')
  // 展开一层返回，附上 can* 标记；不直接返回内存对象引用，避免页面误改内存态
  return delay({ ...o, ...canFlags(o) })
}

// POST /api/orders/{id}/confirm —— 卖家确认（0→1）
export function mockConfirmOrder(id) {
  const o = findOrder(id)
  if (!o) return fail(404, '订单不存在')
  // 状态机校验：只有"待卖家确认(0)"能确认，否则 409（对应后端条件更新影响行数为 0）
  if (o.status !== 0) return fail(409, '订单状态已变化，请刷新')
  o.status = 1
  pushLog(o, 0, 1, 1)
  return delay(null)
}

// POST /api/orders/{id}/cancel —— 买/卖家取消（0或1→3），并回补库存
export function mockCancelOrder(id, reason) {
  const o = findOrder(id)
  if (!o) return fail(404, '订单不存在')
  if (o.status !== 0 && o.status !== 1) return fail(409, '订单状态已变化，请刷新')
  const from = o.status
  o.status = 3
  // 回补库存：把下单时扣掉的数量还给商品；若商品曾因售罄(5)下线，有货后恢复在售(3)
  const product = findRawProduct(o.productId)
  if (product) {
    product.stock += o.quantity
    if (product.status === 5 && product.stock > 0) product.status = 3
  }
  pushLog(o, from, 3, operatorTypeOf(o), reason || null)
  return delay(null)
}

// POST /api/orders/{id}/complete —— 买家确认完成（1→2）
export function mockCompleteOrder(id) {
  const o = findOrder(id)
  if (!o) return fail(404, '订单不存在')
  if (o.status !== 1) return fail(409, '订单状态已变化，请刷新')
  o.status = 2
  o.finishedAt = nowStr()
  pushLog(o, 1, 2, 0)
  return delay(null)
}
