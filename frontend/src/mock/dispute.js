// 纠纷模块 mock。发起纠纷会把订单改成"纠纷中(5)"并记一条状态日志，
// 所以要读/改订单内存表。管理员处理纠纷放到后续管理端板块。
import { ElMessage } from 'element-plus'
import { findOrder, CURRENT_USER, MOCK_ORDERS } from '@/mock/order'
import { findRawProduct } from '@/mock/product'

// 纠纷内存表（导出以便以后做纠纷详情/管理端列表复用）
export const MOCK_DISPUTES = []

let disputeSeq = 200 // 纠纷自增 id 起点

function delay(data, ms = 300) {
  return new Promise((resolve) => setTimeout(() => resolve(data), ms))
}

function fail(code, message) {
  return new Promise((_, reject) =>
    setTimeout(() => {
      ElMessage.error(message)
      reject({ code, message, data: null })
    }, 300),
  )
}

function nowStr() {
  const d = new Date()
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

// POST /api/disputes —— 发起纠纷
// data: { orderId, reasonType(0货不对板/1未履约/2其他), statement, evidence:[图片url] }
export function mockCreateDispute(data) {
  const { orderId, reasonType, statement, evidence } = data
  const o = findOrder(orderId)
  if (!o) return fail(404, '订单不存在')
  // 只有"已确认(1)/已完成(2)"能发起纠纷；当事双方（买/卖家）才能发起
  if (o.status !== 1 && o.status !== 2) return fail(409, '当前订单状态不可发起纠纷')
  const isParty = o.buyer.id === CURRENT_USER.id || o.seller.id === CURRENT_USER.id
  if (!isParty) return fail(403, '只有交易双方可以发起纠纷')
  // 同一订单只能有一个进行中的纠纷
  if (o.disputed) return fail(409, '该订单已存在纠纷')

  const id = String(++disputeSeq)
  const from = o.status
  o.disputed = true
  o.status = 5 // 纠纷中
  // 记一条状态日志：operatorType 0=买家 1=卖家
  const operatorType = o.seller.id === CURRENT_USER.id ? 1 : 0
  o.logs.push({
    fromStatus: from,
    toStatus: 5,
    operatorType,
    reason: statement,
    createdAt: nowStr(),
  })

  MOCK_DISPUTES.push({
    id,
    orderId: String(orderId),
    reasonType,
    statement,
    evidence: evidence || [],
    status: 0, // 0=待处理
    orderPrevStatus: from, // 记住发起纠纷前的订单状态，管理员"驳回"时用来恢复
    createdAt: nowStr(),
  })
  return delay({ id })
}

// ===== 种子数据：给一条"纠纷中(5)"的订单补一条纠纷记录，方便管理端有数据可看 =====
const seedDisputedOrder = MOCK_ORDERS.find((o) => o.status === 5)
if (seedDisputedOrder) {
  seedDisputedOrder.disputed = true
  MOCK_DISPUTES.push({
    id: String(++disputeSeq),
    orderId: seedDisputedOrder.id,
    reasonType: 0, // 货不对板
    statement: '收到的实物成色比页面图片差很多，明显不符，申请退货。',
    evidence: [],
    status: 0, // 待处理
    orderPrevStatus: 1, // 假设由"已确认"转入纠纷
    createdAt: '2026-07-20 10:30:00',
  })
}

// ===== 管理端：纠纷处理 =====

// GET /api/admin/disputes —— 纠纷列表（可按 status 筛选 + (createdAt, id) 游标分页）
// 列表项额外带上订单号/商品标题/买卖双方，方便管理员一眼看清是哪单。
export function mockGetDisputes(params = {}) {
  const { status, cursorCreatedAt, cursorId, pageSize = 10 } = params
  let filtered = MOCK_DISPUTES.slice()
  if (status !== undefined && status !== null && status !== '') {
    filtered = filtered.filter((d) => d.status === Number(status))
  }
  // createdAt 相同时按 id 倒序，必须与后端 SQL 的稳定排序规则一致。
  filtered.sort((a, b) => {
    if (a.createdAt !== b.createdAt) return a.createdAt < b.createdAt ? 1 : -1
    return Number(b.id) - Number(a.id)
  })
  if (cursorCreatedAt && cursorId) {
    filtered = filtered.filter(
      (d) =>
        d.createdAt < cursorCreatedAt ||
        (d.createdAt === cursorCreatedAt && Number(d.id) < Number(cursorId)),
    )
  }
  // 多取一条只用于判断 hasNext，不返回给页面。
  const rows = filtered.slice(0, Number(pageSize) + 1)
  const hasNext = rows.length > pageSize
  const currentRows = hasNext ? rows.slice(0, pageSize) : rows
  const list = currentRows.map((d) => {
    const o = findOrder(d.orderId)
    return {
      ...d,
      orderNo: o?.orderNo ?? '',
      productTitle: o?.snapshot?.title ?? '',
      buyerName: o?.buyer?.nickname ?? '',
      sellerName: o?.seller?.nickname ?? '',
      orderStatus: o?.status,
    }
  })
  const last = currentRows[currentRows.length - 1]
  return delay({
    list,
    hasNext,
    nextCursorCreatedAt: hasNext ? last.createdAt : null,
    nextCursorId: hasNext ? last.id : null,
  })
}

// POST /api/admin/disputes/{id}/handle —— 管理员处理
// data: { action, restock?, note }
// action 白名单：
//   REJECT         驳回纠纷 → 纠纷状态2，订单恢复发起前状态
//   KEEP_COMPLETED 维持完成 → 纠纷状态3，订单置为已完成(2)
//   CANCEL_TRADE   取消交易 → 纠纷状态4，订单置为已取消(3)，restock=true 时回补库存
//   NEED_MORE      待补材料 → 纠纷状态1，订单仍为纠纷中(5)
export function mockHandleDispute(id, data) {
  const d = MOCK_DISPUTES.find((x) => x.id === String(id))
  if (!d) return fail(404, '纠纷不存在')
  // 只有"待处理(0)/待补材料(1)"的纠纷可以继续处理
  if (d.status !== 0 && d.status !== 1) return fail(409, '该纠纷已处理')
  const o = findOrder(d.orderId)
  const { action, restock, note } = data

  // 给订单追加一条"管理员(operatorType=3)"的状态日志
  const log = (toStatus) => {
    if (o) {
      o.logs.push({
        fromStatus: 5,
        toStatus,
        operatorType: 3,
        reason: note || null,
        createdAt: nowStr(),
      })
    }
  }

  switch (action) {
    case 'REJECT':
      d.status = 2
      if (o) {
        o.status = d.orderPrevStatus // 恢复到发起纠纷前的状态
        log(o.status)
      }
      break
    case 'KEEP_COMPLETED':
      d.status = 3
      if (o) {
        o.status = 2
        o.finishedAt = o.finishedAt || nowStr()
        log(2)
      }
      break
    case 'CANCEL_TRADE':
      d.status = 4
      if (o) {
        o.status = 3
        // 取消交易且需要退货时，把库存补回给商品
        if (restock) {
          const product = findRawProduct(o.productId)
          if (product) {
            product.stock += o.quantity
            if (product.status === 5 && product.stock > 0) product.status = 3
          }
        }
        log(3)
      }
      break
    case 'NEED_MORE':
      d.status = 1 // 待补材料，订单仍为纠纷中，不动订单状态
      break
    default:
      return fail(400, '未知的处理动作')
  }
  return delay(null)
}
