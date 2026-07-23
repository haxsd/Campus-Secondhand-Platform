# 校园二手交易平台 · API 接口文档（第一版契约）

> 前端按本契约 mock 开发，后端按本契约实现，联调时以本文件为准。改契约必须先改本文件。

## 0. 全局约定

- Base URL：开发期前端代理 `/api` → `http://localhost:8080`；生产由 Nginx 反代。
- 统一返回结构：

```json
{ "code": 0, "message": "ok", "data": { } }
```

| code | 含义 |
| --- | --- |
| 0 | 成功 |
| 400 | 参数错误（message 给出具体原因） |
| 401 | 未登录 / token 失效（前端统一跳登录页） |
| 403 | 无权限（非本人资源、非管理员） |
| 404 | 资源不存在 |
| 409 | 状态冲突（如"商品已被其他请求修改"、"库存不足"） |
| 429 | 操作太频繁（防抖） |
| 500 | 服务器内部错误（不暴露细节） |

- 认证：登录后返回 token，之后所有需登录接口带请求头 `Authorization: Bearer <token>`。
- ID/订单号：所有 BIGINT 主键在 JSON 里一律返回**字符串**，避免 JS 精度丢失。
- 金额：字符串或数字均保留两位小数；前端展示用字符串。
- 时间：`yyyy-MM-dd HH:mm:ss`。
- 分页请求：`page`（从 1 开始）、`pageSize`（默认 10，最大 50）；分页响应：

```json
{ "list": [], "total": 123, "page": 1, "pageSize": 10 }
```

- 枚举编码（与建表 SQL 一致，前端建 constants 文件映射中文）：
  - 商品状态：0草稿 1待审核 2审核驳回 3在售 4已下架 5已售罄
  - 订单状态：0待卖家确认 1已确认 2已完成 3已取消 4超时取消 5纠纷中
  - 成色：0全新 1几乎全新 2轻微使用 3明显使用
  - 用户角色：0普通 1管理员；用户状态：0正常 1封禁
  - 纠纷状态：0待处理 1待补充材料 2已驳回 3维持完成 4取消交易

---

## 1. 认证模块 `/api/auth`

| 方法 | 路径 | 登录 | 说明 |
| --- | --- | --- | --- |
| POST | /api/auth/register | 否 | 注册 |
| POST | /api/auth/login | 否 | 登录 |
| POST | /api/auth/logout | 是 | 退出（服务端失效当前 token） |
| GET  | /api/auth/me | 是 | 获取当前登录用户信息 |

### POST /api/auth/register
```json
// 请求
{ "studentNo": "20230001", "phone": "13812345678", "password": "Abc@1234", "nickname": "小明", "campus": "东校区" }
// 响应 data：null（注册成功后前端引导去登录）
```
校验：学号/手机号格式；密码 8~20 位含字母数字；学号或手机号已注册返回 400"学号或手机号已注册"。

### POST /api/auth/login
```json
// 请求（account 可以是学号或手机号）
{ "account": "20230001", "password": "Abc@1234" }
// 响应 data
{ "token": "eyJhbGciOi...", "user": { "id": "1", "nickname": "小明", "avatar": null, "campus": "东校区", "role": 0 } }
```
失败统一返回 400"账号或密码错误"；封禁账号返回 403。

### GET /api/auth/me
```json
// 响应 data
{ "id": "1", "studentNo": "20230001", "nickname": "小明", "avatar": null, "campus": "东校区", "role": 0 }
```

---

## 2. 类目与文件 `/api/categories` `/api/files`

| 方法 | 路径 | 登录 | 说明 |
| --- | --- | --- | --- |
| GET | /api/categories | 否 | 启用中的分类列表 `[{id,name}]` |
| POST | /api/files/upload | 是 | 图片上传（multipart，字段名 file），返回 `{ "url": "/uploads/xxx.jpg" }` |

上传限制：jpg/png/webp，单文件 ≤ 5MB，一次一个；后端校验 MIME 与文件头，随机文件名。

---

## 3. 商品模块 `/api/products`

### 公开接口（未登录可访问）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | /api/products | 商品列表（只返回在售且 stock>0） |
| GET | /api/products/{id} | 商品公开详情（走缓存；含卖家信用摘要） |

#### GET /api/products 查询参数
`keyword` `categoryId` `campus` `minPrice` `maxPrice` `page` `pageSize`
```json
// 响应 data.list 元素
{ "id": "10", "title": "九成新 iPad", "price": "1800.00", "cover": "/uploads/a.jpg",
  "campus": "东校区", "itemCondition": 1, "stock": 1, "createdAt": "2026-07-01 10:00:00" }
```

#### GET /api/products/{id}
```json
// 响应 data
{ "id": "10", "title": "...", "description": "...", "price": "1800.00", "stock": 1,
  "itemCondition": 1, "campus": "东校区", "tradePlace": "三食堂门口", "status": 3,
  "categoryId": "1", "categoryName": "数码电子", "viewCount": 88,
  "images": ["/uploads/a.jpg", "/uploads/b.jpg"],
  "seller": { "id": "2", "nickname": "卖家", "avatar": null,
              "creditScore": 98, "dealCount": 12, "avgRating": 4.8, "goodReviewRate": 0.95 },
  "recentReviews": [ { "rating": 5, "content": "很好", "createdAt": "..." } ] }
```
商品不存在或不可公开展示：404（后端写空值缓存）。登录用户访问时后端记录浏览历史。

### 卖家接口（需登录，只操作本人商品）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | /api/products/mine | 我的商品列表（含全部状态），参数 status 可选 |
| POST | /api/products | 发布商品（创建草稿） |
| PUT | /api/products/{id} | 编辑（仅草稿/驳回/已下架，需带 version） |
| POST | /api/products/{id}/submit-review | 申请上架（进入待审核；前提：状态为草稿/驳回/已下架 且 stock>0） |
| POST | /api/products/{id}/withdraw-review | 撤回审核申请（回到草稿） |
| POST | /api/products/{id}/off-shelf | 下架（仅在售） |
| POST | /api/products/{id}/stock | 在售时调库存 `{ "delta": 1 }`（减少后至少保留 1） |

#### POST /api/products（发布）
```json
{ "title": "...", "description": "...", "price": "1800.00", "stock": 1, "itemCondition": 1,
  "categoryId": "1", "campus": "东校区", "tradePlace": "三食堂门口",
  "images": ["/uploads/a.jpg", "/uploads/b.jpg"] }
// 响应 data: { "id": "10" }
```
PUT 编辑请求体同上 + `"version": 3`；version 不匹配返回 409"商品已被其他请求修改，请刷新后重试"。

### 管理员接口（role=1）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | /api/admin/products/pending | 待审核商品列表 |
| POST | /api/admin/products/{id}/review | 审核 `{ "pass": true }` 或 `{ "pass": false, "reason": "..." }` |

---

## 4. 浏览记录 `/api/browse-history`

| 方法 | 路径 | 登录 | 说明 |
| --- | --- | --- | --- |
| GET | /api/browse-history | 是 | 我的浏览记录（按最后浏览时间倒序，分页） |

写入由商品详情接口在服务端完成，前端不单独调用写接口。

---

## 5. 订单模块 `/api/orders`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | /api/orders | 创建订单（下单） |
| GET | /api/orders | 我的订单列表，参数 `role=buyer|seller`、`status` 可选、分页 |
| GET | /api/orders/{id} | 订单详情（仅买卖双方/管理员可见，含快照与状态日志） |
| POST | /api/orders/{id}/confirm | 卖家确认（0→1） |
| POST | /api/orders/{id}/cancel | 买/卖家取消（0或1→3，回补库存）`{ "reason": "..." }` |
| POST | /api/orders/{id}/complete | 买家确认完成（1→2） |

### POST /api/orders
```json
// 请求（requestId 由前端在进入下单页时生成一次 UUID，重试沿用同一个）
{ "productId": "10", "quantity": 1, "tradeTime": "2026-07-25 18:00:00",
  "tradePlace": "三食堂门口", "remark": "", "requestId": "550e8400-..." }
// 响应 data
{ "id": "100", "orderNo": "20260723xxxx", "status": 0, "confirmDeadline": "2026-07-24 06:00:00" }
```
- `confirmDeadline` = 下单时间 + 24 小时（后端常量，可配置）。
- 重复 requestId：返回第一次创建的订单（code=0）。
- 库存不足/商品状态变化：409"库存不足或商品已变化"。
- 连续点击：429。买家=卖家本人：400。

### GET /api/orders/{id}
```json
{ "id": "100", "orderNo": "...", "status": 1, "quantity": 1, "unitPrice": "1800.00",
  "totalAmount": "1800.00", "tradeTime": "...", "tradePlace": "...", "remark": "",
  "confirmDeadline": "...", "finishedAt": null, "createdAt": "...",
  "buyer": { "id": "1", "nickname": "小明" }, "seller": { "id": "2", "nickname": "卖家" },
  "snapshot": { "title": "...", "description": "...", "price": "1800.00",
                "itemCondition": 1, "campus": "...", "tradePlace": "...", "images": ["..."] },
  "logs": [ { "fromStatus": 0, "toStatus": 1, "operatorType": 1, "reason": null, "createdAt": "..." } ],
  "canConfirm": false, "canCancel": true, "canComplete": true, "canReview": false, "canDispute": true }
```
`can*` 字段由后端按当前用户身份和订单状态计算，前端据此显示按钮（后端仍做最终校验）。

状态机操作通用行为：条件更新影响行数为 0 时返回 409 并附最新状态，前端刷新展示。

---

## 6. 评价模块 `/api/reviews`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | /api/reviews | 提交评价（订单已完成且当前用户是买家） |
| GET | /api/reviews/seller/{sellerId} | 某卖家收到的可见评价（分页） |

### POST /api/reviews
```json
{ "orderId": "100", "rating": 5, "content": "描述相符", "tags": "守时,好沟通" }
```
重复评价：返回 409"该订单已评价"。评价成功后信用摘要异步重算（第一版可同步重算，接口契约不变）。

---

## 7. 纠纷模块 `/api/disputes`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | /api/disputes | 发起纠纷（订单已确认，或已完成且在售后窗口内；窗口默认 7 天，后端常量可配置） |
| GET | /api/disputes/{id} | 纠纷详情（当事双方/管理员） |
| GET | /api/admin/disputes | 管理员：纠纷列表，参数 status、分页 |
| POST | /api/admin/disputes/{id}/handle | 管理员处理 |

### POST /api/disputes
```json
{ "orderId": "100", "reasonType": 0, "statement": "实物与描述不符...",
  "evidence": ["/uploads/e1.jpg"] }
```
同一订单已有纠纷：409。证据图片复用 /api/files/upload；第一版存公共 uploads 目录（简化），接 OSS 后再改私有权限 + 短时签名地址，面试口径按此说明。

### POST /api/admin/disputes/{id}/handle
```json
// action 白名单：REJECT(驳回,恢复原状态) | KEEP_COMPLETED(维持完成) |
//               CANCEL_TRADE(取消交易, restock: true|false) | NEED_MORE(待补材料)
{ "action": "CANCEL_TRADE", "restock": true, "note": "双方同意取消，商品已退回" }
```

---

## 8. 前端 Mock 建议

- 用 Vite 插件 `vite-plugin-mock` 或直接在 `src/api/` 里做一层"mock 开关"：`VITE_USE_MOCK=true` 时各 api 函数返回本文档中的示例 JSON（加 300ms 延迟模拟网络）。
- Mock 数据按本文档字段名和类型 1:1 编写，后端完成后只需关掉开关联调。
- 先把 `constants/`（状态枚举中文映射）、`utils/request.js`（Axios 实例 + 401 拦截 + code!==0 统一弹错）写好，所有页面共用。

## 9. 后端实现提示（与文档口径对齐）

- 下单：Redis `SET NX EX 5` 防抖 → 查 `uk_buyer_request` 幂等 → 条件更新扣库存（`where status=3 and stock>=?`）→ 同事务写订单+快照+日志 → afterCommit 删商品缓存。
- 超时关单：定时任务每分钟扫 `status=0 and confirm_deadline<=now()`（走 `idx_timeout_scan`），调用与手动取消同一个 `timeoutCancel(orderId)`。
- 所有状态流转：`update ... where id=? and status=旧状态`，影响行数 0 → 409。
- 详情缓存：`product:detail:{id}` 5 分钟+随机；空值 `product:null:{id}` 10 秒；写路径 afterCommit 同时删两个 key。
