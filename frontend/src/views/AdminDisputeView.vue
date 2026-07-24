<script setup>
// 管理端·纠纷处理：列出纠纷（可按状态筛选），管理员对「待处理/待补材料」的纠纷做裁决。
// 四种处理动作对应订单不同走向：驳回(恢复原状态)/维持完成/取消交易(可退货回补库存)/待补材料。
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getDisputes, handleDispute } from '@/api/admin'
import { DISPUTE_STATUS, DISPUTE_REASON, statusLabel, statusType } from '@/constants'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ status: '', page: 1, pageSize: 10 })

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
    const res = await getDisputes({ ...query })
    list.value = res.list
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function onFilterChange() {
  query.page = 1
  loadList()
}

// ====== 处理 dialog ======
const dialogVisible = ref(false)
const submitting = ref(false)
const current = ref(null) // 当前处理的纠纷行
const form = reactive({ action: 'REJECT', restock: true, note: '' })

function openHandle(row) {
  current.value = row
  form.action = 'REJECT'
  form.restock = true
  form.note = ''
  dialogVisible.value = true
}

async function submitHandle() {
  submitting.value = true
  try {
    // 只有"取消交易"才需要 restock 字段，其它动作不传
    const payload = { action: form.action, note: form.note }
    if (form.action === 'CANCEL_TRADE') payload.restock = form.restock
    await handleDispute(current.value.id, payload)
    ElMessage.success('处理成功')
    dialogVisible.value = false
    await loadList()
  } catch (e) {
    if (e?.code === 409) {
      dialogVisible.value = false
      await loadList()
    }
  } finally {
    submitting.value = false
  }
}

onMounted(loadList)
</script>

<template>
  <div class="admin-dispute">
    <div class="head">
      <h2 class="title">纠纷处理</h2>
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

    <el-pagination
      v-if="total > query.pageSize"
      class="pager"
      layout="prev, pager, next"
      :total="total"
      :page-size="query.pageSize"
      :current-page="query.page"
      @current-change="
        (p) => {
          query.page = p
          loadList()
        }
      "
    />

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
.admin-dispute {
  padding: 16px 0;
}

.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.title {
  margin: 0;
  font-size: 20px;
}

.filter {
  width: 160px;
}

.pager {
  margin-top: 16px;
  justify-content: flex-end;
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
  border-radius: 4px;
}

.full {
  width: 100%;
}
</style>
