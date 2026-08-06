<script setup>
// 管理端·纠纷处理：列出纠纷（可按状态筛选），管理员对「待处理/待补材料」的纠纷做裁决。
// 四种处理动作对应订单不同走向：驳回(恢复原状态)/维持完成/取消交易(可退货回补库存)/待补材料。
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getDisputeDetail, getDisputes, handleDispute, triggerDisputeAiAssist, getDisputeAiAssist, adoptDisputeAiAssist } from '@/api/admin'
import { DISPUTE_STATUS, DISPUTE_REASON, statusLabel, statusType } from '@/constants'

const list = ref([])
const loading = ref(false)
const query = reactive({ status: '', pageSize: 10 })

// 游标历史只保存在当前页面内：可以连续上一页/下一页，但不支持跳转到未知的第 N 页。
// 每一项都是“请求这一页时使用的游标”，首项 null 表示从最新纠纷开始读取。
const cursorHistory = ref([null])
const cursorIndex = ref(0)
const hasNext = ref(false)
const nextCursor = ref(null)

// 状态筛选下拉：全部 + 各纠纷状态
const statusOptions = Object.entries(DISPUTE_STATUS).map(([value, { label }]) => ({
  value: Number(value),
  label,
}))

// 处理动作选项（value 对应后端 action 白名单）
const ACTION_OPTIONS = [
  { value: 'REJECT', label: '驳回纠纷（订单恢复原状态）' },
  { value: 'KEEP_COMPLETED', label: '维持完成（订单置为已完成）' },
  { value: 'CANCEL_TRADE', label: '取消交易（订单取消）' },
  { value: 'NEED_MORE', label: '要求补充材料' },
]

async function loadList() {
  loading.value = true
  try {
    const cursor = cursorHistory.value[cursorIndex.value]
    const params = { status: query.status, pageSize: query.pageSize }
    if (cursor) {
      params.cursorCreatedAt = cursor.createdAt
      params.cursorId = cursor.id
    }
    const res = await getDisputes(params)
    list.value = res.list
    hasNext.value = res.hasNext
    nextCursor.value = res.hasNext
      ? { createdAt: res.nextCursorCreatedAt, id: res.nextCursorId }
      : null
  } finally {
    loading.value = false
  }
}

function onFilterChange() {
  resetCursorPaging()
  loadList()
}

function resetCursorPaging() {
  cursorHistory.value = [null]
  cursorIndex.value = 0
  hasNext.value = false
  nextCursor.value = null
}

function previousPage() {
  if (cursorIndex.value === 0) return
  cursorIndex.value -= 1
  loadList()
}

function nextPage() {
  if (!hasNext.value || !nextCursor.value) return
  // 当前页发生过筛选或刷新后，历史中的“未来页”游标不再可信，先截断再追加。
  cursorHistory.value.splice(cursorIndex.value + 1)
  cursorHistory.value.push(nextCursor.value)
  cursorIndex.value += 1
  loadList()
}

// ====== 处理 dialog ======
const dialogVisible = ref(false)
const submitting = ref(false)
const current = ref(null) // 当前处理的纠纷行
const detail = ref(null)
const aiRun = ref(null)
const form = reactive({ action: 'REJECT', restock: true, note: '' })

function openHandle(row) {
  current.value = row
  detail.value = null
  aiRun.value = null
  getDisputeDetail(row.id).then((value) => { detail.value = value }).catch(() => {})
  form.action = 'REJECT'
  form.restock = true
  form.note = ''
  dialogVisible.value = true
}

async function submitHandle() {
  submitting.value = true
  try {
    // 只有"取消交易"才需要 restock 字段，其它动作不传
    const payload = { action: form.action, note: form.note, evidenceVersion: detail.value?.evidenceVersion ?? current.value.evidenceVersion }
    if (form.action === 'CANCEL_TRADE') payload.restock = form.restock
    await handleDispute(current.value.id, payload)
    ElMessage.success('处理成功')
    dialogVisible.value = false
    // 裁决会改变纠纷状态和排序结果，回到首页重新读取，避免使用可能过期的游标。
    resetCursorPaging()
    await loadList()
  } catch (e) {
    if (e?.code === 409) {
      dialogVisible.value = false
      resetCursorPaging()
      await loadList()
    }
  } finally {
    submitting.value = false
  }
}


async function startAiAssist() { aiRun.value = await triggerDisputeAiAssist(current.value.id); if (aiRun.value?.runId) { setTimeout(async () => { aiRun.value = await getDisputeAiAssist(current.value.id) }, 1500) } }
async function adoptAi() { const action = JSON.parse(aiRun.value.resultJson).suggestedAction; await adoptDisputeAiAssist(current.value.id, aiRun.value.runId, action); form.action = action; ElMessage.success('AI 建议已预填，请确认后提交') }

onMounted(loadList)
</script>

<template>
  <div class="page admin-dispute">
    <div class="head">
      <h2 class="page-title">纠纷处理</h2>
      <el-select
        v-model="query.status"
        placeholder="全部状态"
        clearable
        class="filter"
        @change="onFilterChange"
      >
        <el-option v-for="o in statusOptions" :key="o.value" :label="o.label" :value="o.value" />
      </el-select>
    </div>

    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column prop="orderNo" label="订单号" width="180" show-overflow-tooltip />
      <el-table-column prop="productTitle" label="商品" min-width="160" show-overflow-tooltip />
      <el-table-column label="买家" prop="buyerName" width="90" />
      <el-table-column label="卖家" prop="sellerName" width="90" />
      <el-table-column label="纠纷类型" width="100">
        <template #default="{ row }">{{ statusLabel(DISPUTE_REASON, row.reasonType) }}</template>
      </el-table-column>
      <el-table-column prop="statement" label="问题说明" min-width="180" show-overflow-tooltip />
      <el-table-column label="纠纷状态" width="110">
        <template #default="{ row }">
          <el-tag :type="statusType(DISPUTE_STATUS, row.status)">
            {{ statusLabel(DISPUTE_STATUS, row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="发起时间" width="170" />
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <!-- 只有待处理(0)/待补材料(1)可处理 -->
          <el-button
            type="primary"
            size="small"
            :disabled="row.status !== 0 && row.status !== 1"
            @click="openHandle(row)"
          >
            处理
          </el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无纠纷" />
      </template>
    </el-table>


    <el-card v-if="detail" shadow="never" class="block">
      <template #header>纠纷详情（证据版本 {{ detail.evidenceVersion }}）</template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="下单时商品快照">{{ detail.snapshot?.title }}（非当前商品）</el-descriptions-item>
        <el-descriptions-item label="快照描述">{{ detail.snapshot?.description }}</el-descriptions-item>
        <el-descriptions-item label="下单价格">{{ detail.snapshot?.price }}</el-descriptions-item>
        <el-descriptions-item label="下单成色">{{ detail.snapshot?.itemCondition }}</el-descriptions-item>
        <el-descriptions-item label="买家信用">{{ detail.applicantCredit?.creditScore }} 分，差评 {{ detail.applicantCredit?.badReviewCount }} 条</el-descriptions-item>
        <el-descriptions-item label="卖家信用">{{ detail.respondentCredit?.creditScore }} 分，差评 {{ detail.respondentCredit?.badReviewCount }} 条</el-descriptions-item>
        <el-descriptions-item label="订单评价">{{ detail.review?.content || '暂无评价' }}</el-descriptions-item>
      </el-descriptions>
      <div class="detail-section">
        <strong>订单状态时间线</strong>
        <el-timeline>
          <el-timeline-item v-for="item in detail.orderLogs || []" :key="item.id" :timestamp="item.createdAt">
            {{ item.fromStatus }} → {{ item.toStatus }}：{{ item.reason }}
          </el-timeline-item>
        </el-timeline>
      </div>
      <div class="detail-section">
        <strong>证据追加流水</strong>
        <el-timeline>
          <el-timeline-item v-for="item in detail.evidenceLogs || []" :key="item.id" :timestamp="item.createdAt">
            版本 {{ item.evidenceVersion }}，{{ item.operatorRole === 0 ? '申请人' : '被申请人' }}：{{ item.statement || '仅追加图片' }}
          </el-timeline-item>
        </el-timeline>
      </div>
    </el-card>
    <div v-if="cursorIndex > 0 || hasNext" class="pager">
      <el-button :disabled="loading || cursorIndex === 0" @click="previousPage">上一页</el-button>
      <span class="page-indicator">第 {{ cursorIndex + 1 }} 页</span>
      <el-button :disabled="loading || !hasNext" @click="nextPage">下一页</el-button>
    </div>

    <!-- 处理 dialog -->
    <el-card v-if="current" shadow="never" class="ai-card">
      <template #header>AI 辅助分析（仅供参考，最终裁决由管理员做出）</template>
      <el-button :disabled="!detail || ![0, 1].includes(detail.status)" @click="startAiAssist">触发 AI 分析</el-button>
      <div v-if="aiRun">状态：{{ aiRun.status }}</div>
      <pre v-if="aiRun?.resultJson">{{ aiRun.resultJson }}</pre>
      <el-button v-if="aiRun?.status === 'SUCCEEDED'" @click="adoptAi">采纳建议并预填裁决</el-button>
    </el-card>

    <el-dialog v-model="dialogVisible" title="处理纠纷" width="560px">
      <template v-if="current">
        <el-descriptions :column="1" border class="detail">
          <el-descriptions-item label="订单号">{{ current.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="商品">{{ current.productTitle }}</el-descriptions-item>
          <el-descriptions-item label="纠纷类型">
            {{ statusLabel(DISPUTE_REASON, current.reasonType) }}
          </el-descriptions-item>
          <el-descriptions-item label="问题说明">{{ current.statement }}</el-descriptions-item>
          <el-descriptions-item v-if="current.evidence?.length" label="证据">
            <div class="evidence">
              <el-image
                v-for="(img, i) in current.evidence"
                :key="i"
                :src="img"
                fit="cover"
                class="ev-img"
                :preview-src-list="current.evidence"
                :initial-index="i"
              />
            </div>
          </el-descriptions-item>
        </el-descriptions>

        <el-form label-width="80px" class="handle-form">
          <el-form-item label="处理动作">
            <el-select v-model="form.action" class="full">
              <el-option
                v-for="o in ACTION_OPTIONS"
                :key="o.value"
                :label="o.label"
                :value="o.value"
              />
            </el-select>
          </el-form-item>
          <!-- 仅取消交易时显示"是否退货回补库存" -->
          <el-form-item v-if="form.action === 'CANCEL_TRADE'" label="退货">
            <el-checkbox v-model="form.restock">退回库存（把该商品数量补回在售）</el-checkbox>
          </el-form-item>
          <el-form-item label="处理备注">
            <el-input
              v-model="form.note"
              type="textarea"
              :rows="3"
              maxlength="200"
              show-word-limit
              placeholder="处理说明（会记入订单状态日志，选填）"
            />
          </el-form-item>
        </el-form>
      </template>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitHandle">提交处理</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.head .page-title {
  margin: 0;
}

.filter {
  width: 160px;
}

.pager {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 16px;
  justify-content: flex-end;
}

.page-indicator {
  color: var(--el-text-color-secondary);
  font-size: 14px;
}

.detail {
  margin-bottom: 16px;
}

.evidence {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.ev-img {
  width: 64px;
  height: 64px;
  border-radius: 8px;
}

.full {
  width: 100%;
}
</style>
