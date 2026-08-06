<script setup>
// 订单详情页：展示订单快照 / 金额 / 双方 / 状态时间线，并按 can* 标记显示状态机操作。
// 操作：卖家确认(0→1)、买/卖家取消(0或1→3，回补库存)、买家完成(1→2)、
//       买家评价（已完成）、双方发起纠纷（已确认/已完成 → 纠纷中5）。
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getOrderDetail, confirmOrder, cancelOrder, completeOrder } from '@/api/order'
import { createReview } from '@/api/review'
import { createDispute } from '@/api/dispute'
import { ORDER_STATUS, ITEM_CONDITION, DISPUTE_REASON, statusLabel, statusType } from '@/constants'
import OrderStatusTag from '@/components/OrderStatusTag.vue'
import ImageUploader from '@/components/ImageUploader.vue'
import DisputeParticipantPanel from '@/components/DisputeParticipantPanel.vue'

const route = useRoute()
const orderId = route.params.id

const order = ref(null)
const loading = ref(false)
const notFound = ref(false)
const acting = ref(false) // 操作按钮的加载态，防止连点

// 状态日志里的操作方文案
const OPERATOR_LABEL = { 0: '买家', 1: '卖家', 2: '系统', 3: '管理员' }

async function loadDetail() {
  loading.value = true
  try {
    order.value = await getOrderDetail(orderId)
    notFound.value = false
  } catch {
    // 订单不存在的提示已由请求层弹出，这里标记成空态
    notFound.value = true
  } finally {
    loading.value = false
  }
}

// 统一封装"二次确认 → 调操作接口 → 成功提示 → 刷新"。
// confirmText 是弹窗提示语；action 是真正调用的接口函数。
async function runAction(confirmText, action, successMsg) {
  try {
    await ElMessageBox.confirm(confirmText, '提示', { type: 'warning' })
  } catch {
    return // 用户点了"取消"，什么都不做
  }
  acting.value = true
  try {
    await action()
    ElMessage.success(successMsg)
    await loadDetail()
  } catch (e) {
    // 报错提示已由请求层统一弹出；409 说明状态已变，刷新拿最新
    if (e?.code === 409) await loadDetail()
  } finally {
    acting.value = false
  }
}

function onConfirm() {
  runAction(
    '确认这笔订单吗？确认后买家可线下交易并完成订单。',
    () => confirmOrder(orderId),
    '已确认订单',
  )
}

function onComplete() {
  runAction('确认已完成交易吗？完成后订单不可撤销。', () => completeOrder(orderId), '订单已完成')
}

// 取消需要填原因（选填），用 prompt 单独处理
async function onCancel() {
  let res
  try {
    res = await ElMessageBox.prompt('请输入取消原因（选填）', '取消订单', {
      confirmButtonText: '确定取消',
      cancelButtonText: '再想想',
      inputType: 'textarea',
      inputPlaceholder: '例如：临时不需要了 / 与卖家协商一致',
    })
  } catch {
    return // 用户点了"再想想"
  }
  const reason = res.value || ''
  acting.value = true
  try {
    await cancelOrder(orderId, reason)
    ElMessage.success('订单已取消')
    await loadDetail()
  } catch (e) {
    if (e?.code === 409) await loadDetail()
  } finally {
    acting.value = false
  }
}

// ====== 评价 dialog ======
const reviewVisible = ref(false)
const reviewSubmitting = ref(false)
// tags 后端存逗号分隔字符串，页面用多选框（数组），提交时 join
const REVIEW_TAGS = ['守时', '好沟通', '描述相符', '物美价廉', '包装仔细']
const reviewForm = reactive({ rating: 5, content: '', tags: [] })

function openReview() {
  reviewForm.rating = 5
  reviewForm.content = ''
  reviewForm.tags = []
  reviewVisible.value = true
}

async function submitReview() {
  if (!reviewForm.rating) {
    ElMessage.warning('请先打分')
    return
  }
  reviewSubmitting.value = true
  try {
    await createReview({
      orderId,
      rating: reviewForm.rating,
      content: reviewForm.content,
      tags: reviewForm.tags.join(','), // 数组 → "守时,好沟通"
    })
    ElMessage.success('评价成功，感谢反馈')
    reviewVisible.value = false
    await loadDetail()
  } catch {
    // 错误提示已由请求层统一弹出
  } finally {
    reviewSubmitting.value = false
  }
}

// ====== 纠纷 dialog ======
const disputeVisible = ref(false)
const disputeSubmitting = ref(false)
// 纠纷原因下拉选项（来自常量 DISPUTE_REASON）
const disputeReasonOptions = Object.entries(DISPUTE_REASON).map(([value, { label }]) => ({
  value: Number(value),
  label,
}))
const disputeForm = reactive({ reasonType: 0, statement: '', evidence: [] })

function openDispute() {
  disputeForm.reasonType = 0
  disputeForm.statement = ''
  disputeForm.evidence = []
  disputeVisible.value = true
}

async function submitDispute() {
  if (!disputeForm.statement.trim()) {
    ElMessage.warning('请填写纠纷说明')
    return
  }
  disputeSubmitting.value = true
  try {
    await createDispute({
      orderId,
      reasonType: disputeForm.reasonType,
      statement: disputeForm.statement,
      evidence: disputeForm.evidence,
    })
    ElMessage.success('纠纷已提交，请等待处理')
    disputeVisible.value = false
    await loadDetail()
  } catch {
    // 错误提示已由请求层统一弹出
  } finally {
    disputeSubmitting.value = false
  }
}

onMounted(loadDetail)
</script>

<template>
  <div v-loading="loading" class="page">
    <div class="order-detail">
      <!-- 订单不存在 -->
      <el-result
        v-if="notFound"
        icon="warning"
        title="订单不存在"
        sub-title="订单可能已被删除，或你没有查看权限"
      >
        <template #extra>
          <el-button type="primary" @click="$router.push('/my/orders')">返回我的订单</el-button>
        </template>
      </el-result>

      <template v-else-if="order">
        <!-- 头部：订单号 + 状态 -->
        <el-card shadow="never" class="block">
          <div class="head">
            <div>
              <div class="order-no">订单号 {{ order.orderNo }}</div>
              <div class="created">下单时间：{{ order.createdAt }}</div>
            </div>
            <OrderStatusTag :status="order.status" />
          </div>
        </el-card>

        <!-- 商品快照 -->
        <el-card shadow="never" class="block">
          <template #header>商品信息（下单时快照）</template>
          <div class="snapshot">
            <el-image
              class="snap-img"
              :src="order.snapshot.images?.[0]"
              fit="cover"
              :preview-src-list="order.snapshot.images || []"
            />
            <div class="snap-info">
              <div class="snap-title">{{ order.snapshot.title }}</div>
              <div class="snap-price"><span class="symbol">¥</span>{{ order.snapshot.price }}</div>
              <div class="snap-meta">
                成色：{{ statusLabel(ITEM_CONDITION, order.snapshot.itemCondition) }} ·
                {{ order.snapshot.campus }}
              </div>
            </div>
          </div>
        </el-card>

        <!-- 交易信息 -->
        <el-card shadow="never" class="block">
          <template #header>交易信息</template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="单价">¥{{ order.unitPrice }}</el-descriptions-item>
            <el-descriptions-item label="数量">{{ order.quantity }}</el-descriptions-item>
            <el-descriptions-item label="应付总额">
              <span class="total">¥{{ order.totalAmount }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="订单状态">
              {{ statusLabel(ORDER_STATUS, order.status) }}
            </el-descriptions-item>
            <el-descriptions-item label="买家">{{ order.buyer.nickname }}</el-descriptions-item>
            <el-descriptions-item label="卖家">{{ order.seller.nickname }}</el-descriptions-item>
            <el-descriptions-item label="约定交易时间">{{ order.tradeTime }}</el-descriptions-item>
            <el-descriptions-item label="交易地点">{{ order.tradePlace }}</el-descriptions-item>
            <el-descriptions-item label="确认截止">{{ order.confirmDeadline }}</el-descriptions-item>
            <el-descriptions-item label="完成时间">{{
              order.finishedAt || '—'
            }}</el-descriptions-item>
            <el-descriptions-item label="买家备注" :span="2">
              {{ order.remark || '—' }}
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <DisputeParticipantPanel :order-id=orderId />

        <!-- 状态时间线 -->
        <el-card shadow="never" class="block">
          <template #header>状态记录</template>
          <el-timeline v-if="order.logs?.length">
            <el-timeline-item
              v-for="(log, i) in order.logs"
              :key="i"
              :timestamp="log.createdAt"
              :type="statusType(ORDER_STATUS, log.toStatus)"
            >
              {{ OPERATOR_LABEL[log.operatorType] }} 将订单变更为「{{
                statusLabel(ORDER_STATUS, log.toStatus)
              }}」
              <span v-if="log.reason" class="log-reason">（原因：{{ log.reason }}）</span>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无状态记录" :image-size="60" />
        </el-card>

        <!-- 操作区：按 can* 标记显示 -->
        <div class="actions">
          <el-button v-if="order.canConfirm" type="primary" :loading="acting" @click="onConfirm">
            确认订单
          </el-button>
          <el-button v-if="order.canComplete" type="success" :loading="acting" @click="onComplete">
            确认完成
          </el-button>
          <el-button v-if="order.canCancel" :loading="acting" @click="onCancel">取消订单</el-button>
          <el-button v-if="order.canReview" type="warning" @click="openReview">评价</el-button>
          <el-button v-if="order.canDispute" type="danger" plain @click="openDispute">
            发起纠纷
          </el-button>
        </div>
      </template>

      <!-- 评价 dialog -->
      <el-dialog v-model="reviewVisible" title="评价交易" width="460px">
        <el-form label-width="72px">
          <el-form-item label="评分">
            <el-rate v-model="reviewForm.rating" />
          </el-form-item>
          <el-form-item label="标签">
            <el-checkbox-group v-model="reviewForm.tags">
              <el-checkbox v-for="t in REVIEW_TAGS" :key="t" :value="t" :label="t" />
            </el-checkbox-group>
          </el-form-item>
          <el-form-item label="评价内容">
            <el-input
              v-model="reviewForm.content"
              type="textarea"
              :rows="3"
              maxlength="200"
              show-word-limit
              placeholder="说说这次交易体验（选填）"
            />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="reviewVisible = false">取消</el-button>
          <el-button type="primary" :loading="reviewSubmitting" @click="submitReview">
            提交评价
          </el-button>
        </template>
      </el-dialog>

      <!-- 纠纷 dialog -->
      <el-dialog v-model="disputeVisible" title="发起纠纷" width="500px">
        <el-form label-width="72px">
          <el-form-item label="纠纷类型">
            <el-select v-model="disputeForm.reasonType" class="full">
              <el-option
                v-for="o in disputeReasonOptions"
                :key="o.value"
                :label="o.label"
                :value="o.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="问题说明">
            <el-input
              v-model="disputeForm.statement"
              type="textarea"
              :rows="4"
              maxlength="500"
              show-word-limit
              placeholder="详细描述遇到的问题，便于管理员判断"
            />
          </el-form-item>
          <el-form-item label="证据图片">
            <ImageUploader v-model="disputeForm.evidence" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="disputeVisible = false">取消</el-button>
          <el-button type="danger" :loading="disputeSubmitting" @click="submitDispute">
            提交纠纷
          </el-button>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<style scoped>
.order-detail {
  max-width: 860px;
  margin: 0 auto;
}

.block {
  margin-bottom: 16px;
}

.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.order-no {
  font-weight: 700;
  font-size: 15px;
  color: var(--app-text-1);
  letter-spacing: 0.3px;
}

.created {
  font-size: 13px;
  color: var(--app-text-3);
  margin-top: 4px;
}

.snapshot {
  display: flex;
  gap: 16px;
}

.snap-img {
  width: 100px;
  height: 100px;
  border-radius: 10px;
  flex-shrink: 0;
  background: var(--app-bg-soft);
}

.snap-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--app-text-1);
}

.snap-price {
  color: var(--app-price);
  font-size: 19px;
  font-weight: 700;
  margin: 6px 0;
}

.snap-price .symbol {
  font-size: 13px;
}

.snap-meta {
  font-size: 13px;
  color: var(--app-text-3);
}

.total {
  color: var(--app-price);
  font-weight: 700;
}

.log-reason {
  color: var(--app-text-3);
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 4px 0 8px;
}

.full {
  width: 100%;
}
</style>
