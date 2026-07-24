// 状态枚举映射：数据库里状态存的是数字（如订单 status=0），
// 页面展示需要中文和颜色，全部集中在这里维护，避免每个页面各写一份、改起来遗漏。
// label = 显示文案；type = Element Plus 的 el-tag 颜色类型
// （success 绿 / warning 黄 / danger 红 / info 灰 / primary 蓝）

// 商品状态（对应 product.status，与数据库设计文档一致）
export const PRODUCT_STATUS = {
  0: { label: '草稿', type: 'info' },
  1: { label: '待审核', type: 'warning' },
  2: { label: '审核驳回', type: 'danger' },
  3: { label: '在售', type: 'success' },
  4: { label: '已下架', type: 'info' },
  5: { label: '已售罄', type: 'danger' },
}

// 订单状态（对应 trade_order.status）
export const ORDER_STATUS = {
  0: { label: '待卖家确认', type: 'warning' },
  1: { label: '已确认', type: 'primary' },
  2: { label: '已完成', type: 'success' },
  3: { label: '已取消', type: 'info' },
  4: { label: '超时取消', type: 'info' },
  5: { label: '纠纷中', type: 'danger' },
}

// 商品成色（对应 product.item_condition；condition 是 MySQL 保留字所以字段叫 item_condition）
export const ITEM_CONDITION = {
  0: { label: '全新' },
  1: { label: '几乎全新' },
  2: { label: '轻微使用痕迹' },
  3: { label: '明显使用痕迹' },
}

// 纠纷状态（对应 dispute.status）
export const DISPUTE_STATUS = {
  0: { label: '待处理', type: 'warning' },
  1: { label: '待补充材料', type: 'warning' },
  2: { label: '已驳回', type: 'info' },
  3: { label: '维持完成', type: 'success' },
  4: { label: '取消交易', type: 'danger' },
}

// 纠纷原因类型（对应 dispute.reason_type）
export const DISPUTE_REASON = {
  0: { label: '货不对板' },
  1: { label: '未履约' },
  2: { label: '其他' },
}

// 校区列表（第一版前端写死；以后如果要动态配置，可改成从接口下发）
export const CAMPUS_LIST = ['东校区', '西校区', '南校区', '北校区']

// 帮助函数：statusLabel(ORDER_STATUS, 3) -> '已取消'
// ?. 防止传入未知编码时报错，兜底返回'未知'
export function statusLabel(map, value) {
  return map[value]?.label ?? '未知'
}

// 帮助函数：statusType(ORDER_STATUS, 3) -> 'info'（给 el-tag 的 type 属性用）
export function statusType(map, value) {
  return map[value]?.type ?? 'info'
}
