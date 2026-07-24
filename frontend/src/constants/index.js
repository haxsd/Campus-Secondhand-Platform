// 状态枚举映射：与数据库/API 文档中的编码一一对应，页面展示一律从这里取

// 商品状态
export const PRODUCT_STATUS = {
  0: { label: '草稿', type: 'info' },
  1: { label: '待审核', type: 'warning' },
  2: { label: '审核驳回', type: 'danger' },
  3: { label: '在售', type: 'success' },
  4: { label: '已下架', type: 'info' },
  5: { label: '已售罄', type: 'danger' },
}

// 订单状态
export const ORDER_STATUS = {
  0: { label: '待卖家确认', type: 'warning' },
  1: { label: '已确认', type: 'primary' },
  2: { label: '已完成', type: 'success' },
  3: { label: '已取消', type: 'info' },
  4: { label: '超时取消', type: 'info' },
  5: { label: '纠纷中', type: 'danger' },
}

// 商品成色
export const ITEM_CONDITION = {
  0: { label: '全新' },
  1: { label: '几乎全新' },
  2: { label: '轻微使用痕迹' },
  3: { label: '明显使用痕迹' },
}

// 纠纷状态
export const DISPUTE_STATUS = {
  0: { label: '待处理', type: 'warning' },
  1: { label: '待补充材料', type: 'warning' },
  2: { label: '已驳回', type: 'info' },
  3: { label: '维持完成', type: 'success' },
  4: { label: '取消交易', type: 'danger' },
}

// 纠纷原因类型
export const DISPUTE_REASON = {
  0: { label: '货不对板' },
  1: { label: '未履约' },
  2: { label: '其他' },
}

// 校区列表（第一版写死，后续可改为接口下发）
export const CAMPUS_LIST = ['东校区', '西校区', '南校区', '北校区']

// 通用取值帮助函数：statusLabel(PRODUCT_STATUS, 3) -> '在售'
export function statusLabel(map, value) {
  return map[value]?.label ?? '未知'
}

export function statusType(map, value) {
  return map[value]?.type ?? 'info'
}
