<script setup>
// 我的商品页：卖家管理自己发布的商品。
// - 顶部按状态筛选（全部 / 草稿 / 待审核 / 驳回 / 在售 / 已下架 / 已售罄）
// - 表格每行按"商品当前状态"显示不同的操作按钮（状态机：不同状态能做的事不一样）
// 操作对应后端卖家接口：申请上架 / 撤回申请 / 下架 / 调库存 / 编辑。每次操作成功后刷新列表。
import { onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Picture, QuestionFilled } from '@element-plus/icons-vue'
import { getMyProducts, submitReview, withdrawReview, offShelf, adjustStock, getAiReviewRun, getLatestAiReviewRun, getSellerAiReview, requestManualReview } from '@/api/product'
import { PRODUCT_STATUS, statusLabel, statusType } from '@/constants'

const router = useRouter()

const activeStatus = ref('') // 当前筛选的状态，'' = 全部
const list = ref([])
const loading = ref(false)
const pollingTimers = new Map()

// 每件"在售"商品在"调库存"弹窗里输入的增减量，按商品 id 存（默认 +1）
const deltaMap = reactive({})

// 状态筛选 tab 选项：全部 + 6 种状态
const statusTabs = [
  { value: '', label: '全部' },
  ...Object.entries(PRODUCT_STATUS).map(([value, { label }]) => ({ value, label })),
]

// 拉取我的商品列表
async function loadList() {
  loading.value = true
  try {
    const res = await getMyProducts(activeStatus.value)
    list.value = res
    // 给在售商品初始化调库存的默认增减量
    res.forEach((p) => {
      if (deltaMap[p.id] === undefined) deltaMap[p.id] = 1
    })
    await Promise.all(res.filter((p) => p.status === 2).map(async (p) => {
      try {
        const review = await getSellerAiReview(p.id)
        p.aiReview = review?.latestRun || null
        p.aiReviewOperatorType = review?.latestOperatorType
      } catch {
        p.aiReview = null
      }
    }))
  } finally {
    loading.value = false
  }
}

// 切换状态筛选
function onTabChange() {
  loadList()
}

// 去编辑页
function goEdit(row) {
  router.push(`/products/${row.id}/edit`)
}

// 统一处理"操作 + 成功提示 + 刷新"的小工具：传入执行函数和成功文案
async function doAction(action, successMsg) {
  try {
    await action()
    ElMessage.success(successMsg)
    loadList()
  } catch {
    // 失败（含 409 状态冲突）已由 request.js / mock 的 reject 统一弹提示，这里刷新一下拿最新状态
    loadList()
  }
}

function onSubmitReview(row) {
  doAction(async () => {
    const result = await submitReview(row.id)
    if (result?.runId) startPolling(row.id, result.runId)
  }, '已提交审核，请等待管理员审核')
}

function startPolling(productId, runId) {
  stopPolling(productId)
  let attempts = 0
  const timer = window.setInterval(async () => {
    attempts += 1
    try {
      const result = await getAiReviewRun(productId, runId)
      if (result && !['PENDING', 'RUNNING'].includes(result.status)) {
        stopPolling(productId)
        await loadList()
      } else if (attempts >= 120) {
        stopPolling(productId)
        ElMessage.warning('AI审核等待超时，请稍后刷新查看')
      }
    } catch {
      if (attempts >= 120) stopPolling(productId)
    }
  }, 500)
  pollingTimers.set(productId, timer)
}

function stopPolling(productId) {
  const timer = pollingTimers.get(productId)
  if (timer) {
    window.clearInterval(timer)
    pollingTimers.delete(productId)
  }
}

function onWithdraw(row) {
  doAction(() => withdrawReview(row.id), '已撤回申请，商品回到草稿')
}

function onRequestManualReview(row) {
  doAction(() => requestManualReview(row.id), '已提交人工复核申请')
}

function onOffShelf(row) {
  doAction(() => offShelf(row.id), '商品已下架')
}

function onAdjustStock(row) {
  const delta = Number(deltaMap[row.id])
  if (!delta) {
    ElMessage.warning('请输入非 0 的增减数量')
    return
  }
  doAction(() => adjustStock(row.id, delta), '库存已更新')
}

onMounted(async () => {
  await loadList()
  list.value.filter((row) => row.status === 6).forEach(async (row) => {
    try {
      const run = await getLatestAiReviewRun(row.id)
      if (run?.runId) startPolling(row.id, run.runId)
    } catch {
      ElMessage.warning('AI审核状态暂时无法获取，请稍后刷新')
    }
  })
})
onUnmounted(() => list.value.forEach((row) => stopPolling(row.id)))
</script>

<template>
  <div class="page my-products">
    <h2 class="page-title">我的商品</h2>

    <el-card shadow="never">

      <!-- 状态筛选 -->
      <el-tabs v-model="activeStatus" @tab-change="onTabChange">
        <el-tab-pane v-for="t in statusTabs" :key="t.value" :label="t.label" :name="t.value" />
      </el-tabs>

      <el-table v-loading="loading" :data="list" style="width: 100%">
        <!-- 封面 -->
        <el-table-column label="封面" width="90">
          <template #default="{ row }">
            <el-image :src="row.cover" fit="cover" class="cover">
              <template #error>
                <div class="cover-ph">
                  <el-icon><Picture /></el-icon>
                </div>
              </template>
            </el-image>
          </template>
        </el-table-column>

        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />

        <el-table-column label="价格" width="110">
          <template #default="{ row }">
            <span class="price">¥{{ row.price }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="stock" label="库存" width="80" />

        <!-- 状态：驳回时鼠标悬停显示驳回原因 -->
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusType(PRODUCT_STATUS, row.status)">
              {{ statusLabel(PRODUCT_STATUS, row.status) }}
            </el-tag>
            <el-tooltip v-if="row.status === 2 && row.rejectReason" :content="row.rejectReason">
              <el-icon class="reason-icon"><QuestionFilled /></el-icon>
            </el-tooltip>
            <div v-if="row.status === 2 && row.aiReview" class="ai-reject-summary">
              <div v-for="reason in row.aiReview.reasons" :key="reason">{{ reason }}</div>
              <div v-for="rule in row.aiReview.ruleRefs" :key="`${rule.ruleId}-${rule.ruleVersion}`">
                {{ rule.title || rule.ruleId }}<span v-if="rule.evidence">：{{ rule.evidence }}</span>
              </div>
            </div>
          </template>
        </el-table-column>

        <!-- 操作：按状态显示不同按钮 -->
        <el-table-column label="操作" min-width="220">
          <template #default="{ row }">
            <!-- 草稿(0)/驳回(2)/已下架(4)：可编辑 + 申请上架 -->
            <template v-if="[0, 2, 4].includes(row.status)">
              <el-button size="small" @click="goEdit(row)">编辑</el-button>
              <el-button v-if="row.status === 2 && row.aiReview?.decision === 'REJECT' && row.aiReviewOperatorType === 1"
                size="small" type="warning" @click="onRequestManualReview(row)">申请人工复核</el-button>
              <el-popconfirm title="确认提交审核？" @confirm="onSubmitReview(row)">
                <template #reference>
                  <el-button size="small" type="primary">申请上架</el-button>
                </template>
              </el-popconfirm>
            </template>

            <!-- 待审核(1)：撤回申请 -->
            <template v-else-if="row.status === 1">
              <el-popconfirm title="确认撤回审核申请？" @confirm="onWithdraw(row)">
                <template #reference>
                  <el-button size="small">撤回申请</el-button>
                </template>
              </el-popconfirm>
            </template>

            <!-- 在售(3) / 已售罄(5)：下架 + 调库存（售罄商品补货后后端会自动恢复为在售） -->
            <template v-else-if="row.status === 6">
              <el-button size="small" type="primary" loading disabled>AI审核中</el-button>
            </template>

            <template v-else-if="[3, 5].includes(row.status)">
              <el-popconfirm title="确认下架该商品？" @confirm="onOffShelf(row)">
                <template #reference>
                  <el-button size="small" type="warning">下架</el-button>
                </template>
              </el-popconfirm>
              <el-popover :width="240" trigger="click">
                <template #reference>
                  <el-button size="small">调库存</el-button>
                </template>
                <div class="stock-pop">
                  <span>增减：</span>
                  <el-input-number v-model="deltaMap[row.id]" :step="1" size="small" />
                  <el-button size="small" type="primary" @click="onAdjustStock(row)"
                    >确定</el-button
                  >
                </div>
                <p class="stock-tip">正数增加、负数减少；减少后至少保留 1 件</p>
              </el-popover>
            </template>

            <!-- 其余状态（如待审核已在上面处理）没有可执行操作，这里兜底 -->
            <template v-else>
              <span class="no-action">当前状态无可执行的操作</span>
            </template>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="还没有商品，去发布一个吧" />
        </template>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.my-products :deep(.el-card__body) {
  padding-top: 10px;
}

.cover,
.cover-ph {
  width: 60px;
  height: 60px;
  border-radius: 8px;
}

.ai-reject-summary {
  margin-top: 6px;
  color: var(--el-color-danger);
  font-size: 12px;
  line-height: 1.5;
}

.cover-ph {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  color: #c0c4cc;
}

.price {
  color: var(--app-price);
  font-weight: 700;
}

.reason-icon {
  margin-left: 4px;
  color: var(--el-color-danger);
  vertical-align: middle;
  cursor: help;
}

.stock-pop {
  display: flex;
  align-items: center;
  gap: 8px;
}

.stock-tip {
  margin: 8px 0 0;
  font-size: 12px;
  color: var(--app-text-3);
}

.no-action {
  font-size: 12px;
  color: var(--app-text-3);
}
</style>
