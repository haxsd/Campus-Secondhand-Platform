// 纠纷模块 mock。发起纠纷会把订单改成"纠纷中(5)"并记一条状态日志，
// 所以要读/改订单内存表。管理员处理纠纷放到后续管理端板块。
import { ElMessage } from 'element-plus'
import { findOrder, CURRENT_USER } from '@/mock/order'

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
    createdAt: nowStr(),
  })
  return delay({ id })
}
