<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getDisputes, getDisputeDetail, handleDispute } from '@/api/admin'
import { DISPUTE_STATUS, statusLabel, statusType } from '@/constants'

const list = ref([]); const loading = ref(false); const detailLoading = ref(false)
const query = reactive({ status: '', pageSize: 10 }); const detailVisible = ref(false); const detail = ref(null)
const current = ref(null); const dialogVisible = ref(false); const submitting = ref(false)
const form = reactive({ action: 'REJECT', restock: true, note: '' })
const cursorHistory = ref([null]); const cursorIndex = ref(0); const hasNext = ref(false); const nextCursor = ref(null)
const statusOptions = Object.entries(DISPUTE_STATUS).map(([value, item]) => ({ value: Number(value), label: item.label }))
const actionOptions = [
  { value: 'REJECT', label: 'Reject and restore order' },
  { value: 'KEEP_COMPLETED', label: 'Keep completed' },
  { value: 'CANCEL_TRADE', label: 'Cancel trade' },
  { value: 'NEED_MORE', label: 'Request more evidence' },
]
async function loadList() {
  loading.value = true
  try {
    const cursor = cursorHistory.value[cursorIndex.value]; const params = { status: query.status, pageSize: query.pageSize }
    if (cursor) { params.cursorCreatedAt = cursor.createdAt; params.cursorId = cursor.id }
    const res = await getDisputes(params); list.value = res.list; hasNext.value = res.hasNext
    nextCursor.value = res.hasNext ? { createdAt: res.nextCursorCreatedAt, id: res.nextCursorId } : null
  } finally { loading.value = false }
}
function resetPaging() { cursorHistory.value = [null]; cursorIndex.value = 0; hasNext.value = false; nextCursor.value = null }
function nextPage() { if (!hasNext.value) return; cursorHistory.value.splice(cursorIndex.value + 1); cursorHistory.value.push(nextCursor.value); cursorIndex.value++; loadList() }
function previousPage() { if (cursorIndex.value) { cursorIndex.value--; loadList() } }
async function openDetail(row) { detailLoading.value = true; detailVisible.value = true; try { detail.value = await getDisputeDetail(row.id) } finally { detailLoading.value = false } }
function openHandle(row) { current.value = row; form.action = 'REJECT'; form.restock = true; form.note = ''; dialogVisible.value = true }
async function submitHandle() {
  submitting.value = true
  try { const payload = { action: form.action, note: form.note, evidenceVersion: current.value.evidenceVersion }; if (form.action === 'CANCEL_TRADE') payload.restock = form.restock; await handleDispute(current.value.id, payload); ElMessage.success('Handled'); dialogVisible.value = false; resetPaging(); await loadList() }
  catch (e) { if (e?.code === 409) { dialogVisible.value = false; resetPaging(); await loadList() } }
  finally { submitting.value = false }
}
onMounted(loadList)
</script>

<template>
  <div class="page admin-dispute">
    <div class="head"><h2 class="page-title">Dispute handling</h2><el-select v-model="query.status" clearable placeholder="All statuses" @change="resetPaging(); loadList()"><el-option v-for="o in statusOptions" :key="o.value" :label="o.label" :value="o.value" /></el-select></div>
    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column prop="orderNo" label="Order" width="170" /><el-table-column prop="productTitle" label="Product" min-width="150" />
      <el-table-column label="Status" width="120"><template #default="{ row }"><el-tag :type="statusType(DISPUTE_STATUS, row.status)">{{ statusLabel(DISPUTE_STATUS, row.status) }}</el-tag></template></el-table-column>
      <el-table-column prop="evidenceVersion" label="Evidence version" width="130" /><el-table-column prop="createdAt" label="Created" width="170" />
      <el-table-column label="Actions" width="180"><template #default="{ row }"><el-button link type="primary" @click="openDetail(row)">Detail</el-button><el-button v-if="row.status === 0 || row.status === 1" link type="primary" @click="openHandle(row)">Handle</el-button></template></el-table-column>
    </el-table>
    <div class="pager"><el-button :disabled="cursorIndex === 0" @click="previousPage">Previous</el-button><span>Page {{ cursorIndex + 1 }}</span><el-button :disabled="!hasNext" @click="nextPage">Next</el-button></div>
    <el-dialog v-model="detailVisible" title="Dispute detail" width="760px"><div v-loading="detailLoading" v-if="detail"><el-descriptions border :column="2"><el-descriptions-item label="Status">{{ statusLabel(DISPUTE_STATUS, detail.status) }}</el-descriptions-item><el-descriptions-item label="Evidence version">{{ detail.evidenceVersion }}</el-descriptions-item><el-descriptions-item label="Statement" :span="2">{{ detail.statement }}</el-descriptions-item><el-descriptions-item label="Snapshot title">{{ detail.snapshot?.title }}</el-descriptions-item><el-descriptions-item label="Snapshot price">{{ detail.snapshot?.price }}</el-descriptions-item><el-descriptions-item label="Applicant credit">{{ detail.applicantCredit?.creditScore ?? '-' }}</el-descriptions-item><el-descriptions-item label="Respondent credit">{{ detail.respondentCredit?.creditScore ?? '-' }}</el-descriptions-item></el-descriptions><h4>Evidence append log</h4><el-timeline><el-timeline-item v-for="item in detail.evidenceLogs" :key="item.id" :timestamp="item.createdAt">v{{ item.evidenceVersion }}: {{ item.statement || '-' }} ({{ item.evidence?.length || 0 }} images)</el-timeline-item></el-timeline><h4>Order status timeline</h4><el-timeline><el-timeline-item v-for="(item, index) in detail.orderLogs" :key="index" :timestamp="item.createdAt">{{ item.fromStatus }} -> {{ item.toStatus }}: {{ item.reason || '-' }}</el-timeline-item></el-timeline><div v-if="detail.review">Review: {{ detail.review.rating }} / {{ detail.review.content }}</div></div></el-dialog>
    <el-dialog v-model="dialogVisible" title="Handle dispute" width="480px"><el-form label-width="150px"><el-form-item label="Action"><el-select v-model="form.action"><el-option v-for="item in actionOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item><el-form-item v-if="form.action === 'CANCEL_TRADE'" label="Restore stock"><el-switch v-model="form.restock" /></el-form-item><el-form-item label="Note"><el-input v-model="form.note" type="textarea" maxlength="1000" /></el-form-item></el-form><template #footer><el-button @click="dialogVisible = false">Cancel</el-button><el-button type="primary" :loading="submitting" @click="submitHandle">Submit</el-button></template></el-dialog>
  </div>
</template>
