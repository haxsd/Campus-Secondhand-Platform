// 商品模块 mock 数据：模拟后端的商品列表 / 商品详情接口，字段严格对齐 docs/API接口文档.md。
// 说明（给新手）：
//   1) 这里造了 24 条商品，覆盖 6 个分类、4 个校区、不同价格段和成色，方便首页筛选/分页都能测到；
//   2) id 一律用字符串（后端主键是 BIGINT，超过 JS 安全整数会精度丢失，全项目统一按字符串处理）；
//   3) 列表接口只返回卡片需要的少量字段，详情接口才返回完整字段——这和真实后端一致，能少传数据；
//   4) 封面图用 picsum.photos 的随机占位图（按 id 取种子，保证每次刷新同一商品图不变），第一版没有真实图片。

import { MOCK_CATEGORIES } from './category'

// categoryId -> categoryName 的快速查找表，避免每条商品都手写分类名
const CATEGORY_NAME = Object.fromEntries(MOCK_CATEGORIES.map((c) => [c.id, c.name]))

// 几个卖家信息（商品详情里要展示卖家信用摘要），多条商品可共用同一个卖家
const SELLERS = {
  s1: {
    id: '2',
    nickname: '数码小王',
    avatar: null,
    creditScore: 98,
    dealCount: 23,
    avgRating: 4.9,
    goodReviewRate: 0.96,
  },
  s2: {
    id: '3',
    nickname: '爱看书的阿May',
    avatar: null,
    creditScore: 95,
    dealCount: 12,
    avgRating: 4.7,
    goodReviewRate: 0.92,
  },
  s3: {
    id: '4',
    nickname: '毕业甩卖菌',
    avatar: null,
    creditScore: 90,
    dealCount: 8,
    avgRating: 4.5,
    goodReviewRate: 0.88,
  },
  // s0 = 当前登录用户"小明"（mock 用户 id=1），用于"我的商品"页展示自己发布的商品
  s0: {
    id: '1',
    nickname: '小明',
    avatar: null,
    creditScore: 100,
    dealCount: 5,
    avgRating: 5.0,
    goodReviewRate: 1.0,
  },
}

// 商品原始种子数据：只写"有区分度"的字段，其余（封面、创建时间、状态等）由下面的构建函数统一生成。
// 字段含义：title 标题 / categoryId 分类 / price 价格(字符串两位小数) / itemCondition 成色(0全新~3明显使用)
//          / campus 校区 / stock 库存 / sellerKey 对应上面的卖家 / desc 描述
const SEED = [
  {
    title: '95新 iPad Air 5 64G 深空灰',
    categoryId: '1',
    price: '2680.00',
    itemCondition: 1,
    campus: '东校区',
    stock: 1,
    sellerKey: 's1',
    desc: '去年双十一买的，平时上课记笔记用，屏幕无划痕，配原装充电器和保护套。',
  },
  {
    title: '罗技 MX Master 3S 无线鼠标',
    categoryId: '1',
    price: '480.00',
    itemCondition: 1,
    campus: '东校区',
    stock: 2,
    sellerKey: 's1',
    desc: '手感一流，静音按键，充电口 Type-C，几乎全新。',
  },
  {
    title: '小米蓝牙耳机 Air4 Pro',
    categoryId: '1',
    price: '260.00',
    itemCondition: 2,
    campus: '西校区',
    stock: 1,
    sellerKey: 's1',
    desc: '主动降噪，音质不错，有轻微使用痕迹，功能一切正常。',
  },
  {
    title: 'Kindle Paperwhite 4 电子书',
    categoryId: '1',
    price: '520.00',
    itemCondition: 2,
    campus: '南校区',
    stock: 1,
    sellerKey: 's2',
    desc: '看书神器，护眼墨水屏，8G 容量，含原装保护壳。',
  },
  {
    title: '机械键盘 Keychron K2 青轴',
    categoryId: '1',
    price: '330.00',
    itemCondition: 1,
    campus: '北校区',
    stock: 1,
    sellerKey: 's1',
    desc: '87 键紧凑布局，蓝牙+有线双模，键帽无磨损。',
  },
  {
    title: '大学英语四级真题 12 套',
    categoryId: '2',
    price: '25.00',
    itemCondition: 2,
    campus: '东校区',
    stock: 3,
    sellerKey: 's2',
    desc: '刷过一半，答案完整，笔记较少，考完就出。',
  },
  {
    title: '《算法导论》第三版 中文',
    categoryId: '2',
    price: '60.00',
    itemCondition: 2,
    campus: '西校区',
    stock: 1,
    sellerKey: 's2',
    desc: '经典教材，八成新，无笔记划线，适合考研/秋招刷题。',
  },
  {
    title: '考研数学 张宇 1000 题',
    categoryId: '2',
    price: '35.00',
    itemCondition: 3,
    campus: '南校区',
    stock: 1,
    sellerKey: 's2',
    desc: '做了大半，有笔记有答案，便宜出，仅供参考。',
  },
  {
    title: '《深入理解计算机系统》CSAPP',
    categoryId: '2',
    price: '75.00',
    itemCondition: 1,
    campus: '东校区',
    stock: 1,
    sellerKey: 's2',
    desc: '几乎全新，塑封拆了没怎么看，计算机专业必备。',
  },
  {
    title: '宿舍小台灯 护眼 可调光',
    categoryId: '3',
    price: '40.00',
    itemCondition: 2,
    campus: '北校区',
    stock: 4,
    sellerKey: 's3',
    desc: '三档调光，USB 供电，宿舍夜读利器，功能正常。',
  },
  {
    title: '折叠晾衣架 阳台落地',
    categoryId: '3',
    price: '30.00',
    itemCondition: 2,
    campus: '东校区',
    stock: 2,
    sellerKey: 's3',
    desc: '不锈钢材质，可折叠省空间，毕业搬宿舍用不上了。',
  },
  {
    title: '小型电热水壶 1.5L',
    categoryId: '3',
    price: '45.00',
    itemCondition: 2,
    campus: '西校区',
    stock: 1,
    sellerKey: 's3',
    desc: '烧水快，自动断电，用了一学期，无水垢。',
  },
  {
    title: '桌面收纳盒 多格文具整理',
    categoryId: '3',
    price: '18.00',
    itemCondition: 1,
    campus: '南校区',
    stock: 5,
    sellerKey: 's3',
    desc: '几乎没用，多格设计，桌面收纳很方便。',
  },
  {
    title: '迪卡侬 篮球 7 号 室外耐磨',
    categoryId: '4',
    price: '55.00',
    itemCondition: 2,
    campus: '北校区',
    stock: 1,
    sellerKey: 's3',
    desc: '手感好，防滑耐磨，打了几次球，气很足。',
  },
  {
    title: '瑜伽垫 加厚防滑 183cm',
    categoryId: '4',
    price: '38.00',
    itemCondition: 2,
    campus: '东校区',
    stock: 2,
    sellerKey: 's3',
    desc: 'NBR 材质加厚款，防滑无异味，带绑带，健身房/宿舍都能用。',
  },
  {
    title: '公路自行车 入门通勤款',
    categoryId: '4',
    price: '680.00',
    itemCondition: 3,
    campus: '西校区',
    stock: 1,
    sellerKey: 's3',
    desc: '骑了一年，链条刹车都正常，有使用痕迹，校内代步很划算。',
  },
  {
    title: '羽毛球拍 尤尼克斯 单支',
    categoryId: '4',
    price: '120.00',
    itemCondition: 2,
    campus: '南校区',
    stock: 1,
    sellerKey: 's3',
    desc: '碳素球拍，已重新缠手胶，手感轻，附拍套。',
  },
  {
    title: '优衣库 摇粒绒外套 男 L 码',
    categoryId: '5',
    price: '65.00',
    itemCondition: 2,
    campus: '东校区',
    stock: 1,
    sellerKey: 's3',
    desc: '深灰色，保暖百搭，洗过两次无起球，L 码。',
  },
  {
    title: '帆布双肩包 学院风 大容量',
    categoryId: '5',
    price: '48.00',
    itemCondition: 1,
    campus: '北校区',
    stock: 2,
    sellerKey: 's3',
    desc: '几乎全新，能装 15 寸笔记本，多隔层，颜值高。',
  },
  {
    title: 'Nike 运动短袖 T 恤 M 码',
    categoryId: '5',
    price: '55.00',
    itemCondition: 2,
    campus: '西校区',
    stock: 1,
    sellerKey: 's3',
    desc: '速干面料，黑色，M 码，跑步打球都合适。',
  },
  {
    title: '全新未拆 香水小样 3ml×5',
    categoryId: '5',
    price: '35.00',
    itemCondition: 0,
    campus: '南校区',
    stock: 3,
    sellerKey: 's3',
    desc: '专柜赠品，全新未拆，五个不同香型，送人自用都行。',
  },
  {
    title: '桌面绿植 多肉盆栽 带盆',
    categoryId: '6',
    price: '15.00',
    itemCondition: 1,
    campus: '东校区',
    stock: 6,
    sellerKey: 's3',
    desc: '好养活，办公桌小清新，一盆一价。',
  },
  {
    title: '毕业季 打包出闲置一批',
    categoryId: '6',
    price: '99.00',
    itemCondition: 3,
    campus: '北校区',
    stock: 1,
    sellerKey: 's3',
    desc: '一堆生活杂物打包价，具体清单私聊，仅限自提。',
  },
  {
    title: '演唱会荧光棒 全新 2 支',
    categoryId: '6',
    price: '20.00',
    itemCondition: 0,
    campus: '西校区',
    stock: 4,
    sellerKey: 's3',
    desc: '全新未使用，多档亮度，追星必备。',
  },
]

// 把种子数据补全成"完整商品对象"。这样种子里只写差异字段，公共字段集中在这里生成，改起来方便。
const ALL_PRODUCTS = SEED.map((item, index) => {
  const id = String(index + 1)
  // 封面/图集：用 picsum 占位图，seed=商品 id 保证同一商品每次拿到同一张图（详情页给 2~3 张）
  const images = [
    `https://picsum.photos/seed/ct${id}a/600/450`,
    `https://picsum.photos/seed/ct${id}b/600/450`,
    `https://picsum.photos/seed/ct${id}c/600/450`,
  ]
  return {
    id,
    title: item.title,
    description: item.desc,
    price: item.price,
    stock: item.stock,
    itemCondition: item.itemCondition,
    campus: item.campus,
    tradePlace: '校门口菜鸟驿站', // 第一版统一给个交易地点，详情页展示用
    status: 3, // 3=在售（首页只展示在售商品）
    categoryId: item.categoryId,
    categoryName: CATEGORY_NAME[item.categoryId],
    version: 1, // 乐观锁版本号：编辑提交时带上，后端比对，不一致说明数据被别处改过（返回 409）
    viewCount: 50 + ((index * 37) % 400), // 造一个稳定的浏览数（非随机，刷新不变）
    cover: images[0], // 列表卡片用第一张作封面
    images,
    seller: SELLERS[item.sellerKey],
    // 创建时间：越靠前的商品越新，用于列表默认按时间倒序
    createdAt: `2026-07-${String(24 - (index % 24)).padStart(2, '0')} 10:00:00`,
    // 详情页展示的最近评价（真实后端来自 review 表，这里造两条通用的）
    recentReviews: [
      {
        rating: 5,
        content: '东西和描述一致，卖家很好说话，推荐！',
        createdAt: '2026-07-10 15:20:00',
      },
      { rating: 4, content: '整体不错，就是约面交等了一会儿。', createdAt: '2026-07-08 19:05:00' },
    ],
  }
})

// "我的商品"种子：这些商品的卖家是当前登录用户小明（seller s0，id=1），
// 覆盖 6 种状态（0草稿/1待审核/2驳回/3在售/4已下架/5已售罄），方便"我的商品"页把各状态操作都测一遍。
// 追加进 ALL_PRODUCTS，这样：详情页/编辑回填能查到；其中"在售"的还会出现在首页（点进去按钮显示"去管理"）。
const MY_SEED = [
  {
    title: '我的 · 闲置机械键盘（草稿）',
    categoryId: '1',
    price: '150.00',
    itemCondition: 2,
    stock: 1,
    status: 0,
  },
  {
    title: '我的 · 二手显示器 24寸（待审核）',
    categoryId: '1',
    price: '400.00',
    itemCondition: 2,
    stock: 1,
    status: 1,
  },
  {
    title: '我的 · 高数教材（被驳回）',
    categoryId: '2',
    price: '20.00',
    itemCondition: 3,
    stock: 1,
    status: 2,
    rejectReason: '图片不清晰，请重新上传商品实拍图',
  },
  {
    title: '我的 · 九成新台灯（在售）',
    categoryId: '3',
    price: '35.00',
    itemCondition: 1,
    stock: 3,
    status: 3,
  },
  {
    title: '我的 · 篮球（已下架）',
    categoryId: '4',
    price: '45.00',
    itemCondition: 2,
    stock: 1,
    status: 4,
  },
  {
    title: '我的 · 限量球鞋（已售罄）',
    categoryId: '5',
    price: '520.00',
    itemCondition: 1,
    stock: 0,
    status: 5,
  },
]

MY_SEED.forEach((item, i) => {
  const id = `m${i + 1}`
  const images = [`https://picsum.photos/seed/${id}/600/450`]
  ALL_PRODUCTS.push({
    id,
    title: item.title,
    description: '这是我发布的闲置商品（mock 演示数据）。',
    price: item.price,
    stock: item.stock,
    itemCondition: item.itemCondition,
    campus: '东校区',
    tradePlace: '校门口菜鸟驿站',
    status: item.status,
    rejectReason: item.rejectReason, // 仅"审核驳回"状态有值，其余为 undefined
    categoryId: item.categoryId,
    categoryName: CATEGORY_NAME[item.categoryId],
    version: 1,
    viewCount: 10 + i,
    cover: images[0],
    images,
    seller: SELLERS.s0,
    createdAt: `2026-07-2${i} 09:00:00`,
    recentReviews: [],
  })
})

// 模拟网络延迟
function delay(data, ms = 300) {
  return new Promise((resolve) => setTimeout(() => resolve(data), ms))
}

// 模拟失败响应（详情查不到时用）：和 request.js 的失败处理保持一致——先 reject，页面 catch 处理
function fail(code, message) {
  return new Promise((_, reject) => setTimeout(() => reject({ code, message, data: null }), 300))
}

// GET /api/products —— 商品列表（支持关键词/分类/校区/价格区间筛选 + 分页）
// params: { keyword, categoryId, campus, minPrice, maxPrice, page, pageSize }
// 返回：{ list, total, page, pageSize }，list 元素只含卡片需要的字段（对齐 API 文档）
export function mockGetProducts(params = {}) {
  const { keyword, categoryId, campus, minPrice, maxPrice, page = 1, pageSize = 12 } = params

  // 1) 先按各筛选条件过滤（后端 SQL 的 WHERE 在这里用 JS 模拟）
  let filtered = ALL_PRODUCTS.filter((p) => {
    if (keyword && !p.title.includes(keyword)) return false
    if (categoryId && p.categoryId !== String(categoryId)) return false
    if (campus && p.campus !== campus) return false
    if (minPrice != null && minPrice !== '' && Number(p.price) < Number(minPrice)) return false
    if (maxPrice != null && maxPrice !== '' && Number(p.price) > Number(maxPrice)) return false
    return true
  })

  // 2) 排序：按创建时间倒序（最新的在前），和后端默认一致
  filtered = filtered.sort((a, b) => (a.createdAt < b.createdAt ? 1 : -1))

  // 3) 分页：total 是过滤后的总数（前端算总页数用），再切出当前页
  const total = filtered.length
  const start = (page - 1) * pageSize
  const pageItems = filtered.slice(start, start + pageSize)

  // 4) 只挑列表需要的字段返回（详情字段留给详情接口，减少传输）
  const list = pageItems.map((p) => ({
    id: p.id,
    title: p.title,
    price: p.price,
    cover: p.cover,
    campus: p.campus,
    itemCondition: p.itemCondition,
    stock: p.stock,
    createdAt: p.createdAt,
  }))

  return delay({ list, total, page, pageSize })
}

// GET /api/products/{id} —— 商品详情（返回完整字段）
export function mockGetProductDetail(id) {
  const found = ALL_PRODUCTS.find((p) => p.id === String(id))
  if (!found) return fail(404, '商品不存在或已下架')
  return delay(found)
}

// POST /api/products —— 发布商品（创建草稿）
// mock 里把新商品追加进内存数组（状态 0=草稿，首页只展示在售所以不会污染列表），返回新 id。
export function mockCreateProduct(data) {
  const id = String(ALL_PRODUCTS.length + 1)
  const images = data.images?.length ? data.images : [`https://picsum.photos/seed/ct${id}/600/450`]
  ALL_PRODUCTS.push({
    id,
    ...data,
    status: 0, // 新发布默认草稿
    version: 1,
    viewCount: 0,
    categoryName: CATEGORY_NAME[data.categoryId],
    cover: images[0],
    images,
    seller: SELLERS.s1, // mock 里统一挂到一个卖家，联调时后端按登录用户填
    createdAt: '2026-07-24 12:00:00',
    recentReviews: [],
  })
  return delay({ id })
}

// PUT /api/products/{id} —— 编辑商品
// mock 简化处理：找到就更新字段并返回成功；真实后端这里会校验 version 做乐观锁（不一致返回 409）。
export function mockUpdateProduct(id, data) {
  const found = ALL_PRODUCTS.find((p) => p.id === String(id))
  if (!found) return fail(404, '商品不存在')
  Object.assign(found, data, {
    categoryName: CATEGORY_NAME[data.categoryId] ?? found.categoryName,
    cover: data.images?.[0] ?? found.cover,
    version: (found.version ?? 1) + 1, // 每次成功编辑版本号 +1
  })
  return delay(null)
}

// 当前登录用户 id（mock 里固定是小明 id=1）。真实后端从 token 解析当前用户，前端不用传。
const MY_USER_ID = '1'

// 给其它 mock 模块（如订单）复用：按 id 拿到完整商品对象（下单要读价格/卖家/库存/快照）。
// 返回的是内存里的同一份引用，订单 mock 扣库存时能直接改到它。
export function findRawProduct(id) {
  return ALL_PRODUCTS.find((p) => p.id === String(id))
}

// GET /api/products/mine —— 我的商品列表（含全部状态，status 可选筛选）
export function mockGetMyProducts(status) {
  let mine = ALL_PRODUCTS.filter((p) => p.seller.id === MY_USER_ID)
  // status 传了才筛（'' 或 undefined 表示全部）
  if (status !== undefined && status !== null && status !== '') {
    mine = mine.filter((p) => p.status === Number(status))
  }
  // 按创建时间倒序
  mine = mine.slice().sort((a, b) => (a.createdAt < b.createdAt ? 1 : -1))
  return delay({ list: mine, total: mine.length })
}

// 小工具：找到我的某件商品，找不到就返回 404
function findMine(id) {
  return ALL_PRODUCTS.find((p) => p.id === String(id) && p.seller.id === MY_USER_ID)
}

// POST /api/products/{id}/submit-review —— 申请上架（草稿/驳回/已下架 且 stock>0 → 待审核）
export function mockSubmitReview(id) {
  const p = findMine(id)
  if (!p) return fail(404, '商品不存在')
  if (![0, 2, 4].includes(p.status)) return fail(409, '当前状态不能申请上架')
  if (p.stock <= 0) return fail(409, '库存为 0，无法申请上架')
  p.status = 1
  p.rejectReason = undefined // 重新申请后清掉上次的驳回原因
  return delay(null)
}

// POST /api/products/{id}/withdraw-review —— 撤回审核申请（待审核 → 草稿）
export function mockWithdrawReview(id) {
  const p = findMine(id)
  if (!p) return fail(404, '商品不存在')
  if (p.status !== 1) return fail(409, '只有待审核的商品能撤回')
  p.status = 0
  return delay(null)
}

// POST /api/products/{id}/off-shelf —— 下架（仅在售 → 已下架）
export function mockOffShelf(id) {
  const p = findMine(id)
  if (!p) return fail(404, '商品不存在')
  if (p.status !== 3) return fail(409, '只有在售商品能下架')
  p.status = 4
  return delay(null)
}

// POST /api/products/{id}/stock —— 在售时调库存 { delta }（减少后至少保留 1）
export function mockAdjustStock(id, delta) {
  const p = findMine(id)
  if (!p) return fail(404, '商品不存在')
  if (p.status !== 3) return fail(409, '只有在售商品能调整库存')
  const next = p.stock + Number(delta)
  if (next < 1) return fail(409, '库存减少后至少要保留 1 件')
  p.stock = next
  return delay({ stock: next })
}
