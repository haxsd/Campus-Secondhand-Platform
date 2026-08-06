<script setup>
// 管理端·纠纷处理：列出纠纷（可按状态筛选），管理员对「待处理/待补材料」的纠纷做裁决。
// 四种处理动作对应订单不同走向：驳回(恢复原状态)/维持完成/取消交易(可退货回补库存)/待补材料。
import { onMounted, onUnmounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { adoptDisputeAiAssist, getDisputeAiAssist, getDisputeDetail, getDisputes, handleDispute, triggerDisputeAiAssist } from '@/api/admin'
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
const form = reactive({ action: 'REJECT', restock: true, note: '' })
const aiRun = ref(null)
let aiPollTimer = null

function openHandle(row) {
  current.value = row
  detail.value = null
  getDisputeDetail(row.id).then(async (value) => {
    detail.value = value
    await loadAiAssist()
  }).catch(() => {})
  form.action = 'REJECT'
  form.restock = true
  form.note = ''
  dialogVisible.value = true
}

async function loadAiAssist() {
  if (!current.value) return
  aiRun.value = await getDisputeAiAssist(current.value.id)
  if (aiRun.value && ['PENDING', 'RUNNING'].includes(aiRun.value.status)) {
    startAiPolling()
  }
}

function startAiPolling() {
  stopAiPolling()
  aiPollTimer = window.setInterval(async () => {
    await loadAiAssist()
    if (!aiRun.value || !['PENDING', 'RUNNING'].includes(aiRun.value.status)) {
      stopAiPolling()
    }
  }, 1500)
}

function stopAiPolling() {
  if (aiPollTimer) {
    window.clearInterval(aiPollTimer)
    aiPollTimer = null
  }
}

function parsedAiResult() {
  if (!aiRun.value?.resultJson) return null
  if (typeof aiRun.value.resultJson === 'object') return aiRun.value.resultJson
  try { return JSON.parse(aiRun.value.resultJson) } catch { return null }
}

async function triggerAiAssist() {
  if (!current.value || !detail.value || ![0, 1].includes(detail.value.status)) return
  if (aiRun.value?.status === 'SUCCEEDED' && aiRun.value.submittedEvidenceVersion === detail.value.evidenceVersion) return
  aiRun.value = await triggerDisputeAiAssist(current.value.id)
  if (['PENDING', 'RUNNING'].includes(aiRun.value.status)) startAiPolling()
}

async function adoptAiAssist() {
  const result = parsedAiResult()
  if (!current.value || !aiRun.value?.runId || !result) return
  await adoptDisputeAiAssist(current.value.id, aiRun.value.runId, { action: result.suggestedAction })
  form.action = result.suggestedAction
  form.restock = Boolean(result.suggestedRestock)
  form.note = result.adminSummary || ''
  ElMessage.success('已采纳建议并预填裁决表单，请确认后提交')
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

onMounted(loadList)
onUnmounted(stopAiPolling)
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

        <el-card shadow="never" class="ai-assist-card">
          <template #header>
            <div class="ai-assist-head">
              <span>AI 辅助分析</span>
              <el-button type="primary" size="small" :disabled="!detail || ![0, 1].includes(detail.status) || (aiRun?.status === 'PENDING' || aiRun?.status === 'RUNNING')" @click="triggerAiAssist">
                {{ aiRun?.status === 'SUCCEEDED' && aiRun?.submittedEvidenceVersion === detail?.evidenceVersion ? '查看已有建议' : '触发分析' }}
              </el-button>
            </div>
          </template>
          <p>AI 建议仅供参考，最终裁决由管理员做出。</p>
          <el-alert v-if="aiRun?.status === 'STALE'" type="warning" :closable="false" title="该建议基于旧版证据，已失效，请重新触发分析。" />
          <el-alert v-else-if="aiRun?.status === 'DISABLED'" type="info" :closable="false" title="纠纷 AI 辅助已关闭。" />
          <el-alert v-else-if="aiRun && ['PENDING', 'RUNNING'].includes(aiRun.status)" type="info" :closable="false" title="分析进行中，页面会自动轮询。" />
          <template v-if="parsedAiResult() && aiRun?.status === 'SUCCEEDED'">
            <el-descriptions :column="2" border>
              <el-descriptions-item label="建议动作">{{ parsedAiResult().suggestedAction }}</el-descriptions-item>
              <el-descriptions-item label="置信度">{{ parsedAiResult().confidence }}</el-descriptions-item>
              <el-descriptions-item label="责任倾向">{{ parsedAiResult().liability }}</el-descriptions-item>
              <el-descriptions-item label="管理员摘要">{{ parsedAiResult().adminSummary }}</el-descriptions-item>
            </el-descriptions>
            <p><strong>判断理由</strong></p>
            <ul><li v-for="reason in parsedAiResult().reasons || []" :key="reason">{{ reason }}</li></ul>
            <p><strong>核验事实（引用原文）</strong></p>
            <ul><li v-for="fact in parsedAiResult().verifiedFacts || []" :key="fact.field + fact.quote">{{ fact.field }}：{{ fact.quote }}</li></ul>
            <p><strong>缺失证据</strong></p>
            <ul><li v-for="item in parsedAiResult().missingEvidence || []" :key="item">{{ item }}</li></ul>
            <p><strong>命中规则</strong></p>
            <ul><li v-for="rule in parsedAiResult().ruleRefs || []" :key="rule.ruleId + rule.ruleVersion">{{ rule.ruleId }} / {{ rule.title }}：{{ rule.evidence }}</li></ul>
            <el-button type="success" @click="adoptAiAssist">采纳建议并预填裁决表单</el-button>
          </template>
        </el-card>

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
