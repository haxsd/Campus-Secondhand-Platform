<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getDisputeByOrder, appendDisputeEvidence } from '@/api/dispute'
import ImageUploader from '@/components/ImageUploader.vue'
import { DISPUTE_STATUS, statusLabel } from '@/constants'

const props = defineProps({ orderId: { type: [String, Number], required: true } })
const dispute = ref(null); const loading = ref(false); const submitting = ref(false); const statement = ref(''); const evidence = ref([])
async function load() { loading.value = true; try { dispute.value = await getDisputeByOrder(props.orderId) } catch (e) { if (e?.code !== 404) dispute.value = null } finally { loading.value = false } }
async function submit() { if (!statement.value.trim() && !evidence.value.length) { ElMessage.warning('Please add a statement or image'); return }; submitting.value = true; try { await appendDisputeEvidence(dispute.value.id, { statement: statement.value, evidence: evidence.value }); ElMessage.success('Supplement submitted'); statement.value = ''; evidence.value = []; await load() } finally { submitting.value = false } }
onMounted(load)
</script>
<template>
  <el-card v-if="dispute" v-loading="loading" shadow="never" class="block"><template #header>My dispute</template><el-descriptions :column="2" border><el-descriptions-item label="Status">{{ statusLabel(DISPUTE_STATUS, dispute.status) }}</el-descriptions-item><el-descriptions-item label="Evidence version">{{ dispute.evidenceVersion }}</el-descriptions-item><el-descriptions-item label="Statement" :span="2">{{ dispute.statement }}</el-descriptions-item></el-descriptions><div v-if="dispute.status === 1" class="dispute-supplement"><el-input v-model="statement" type="textarea" maxlength="2000" placeholder="Add supplementary details" /><ImageUploader v-model="evidence" :limit="5" /><el-button type="primary" :loading="submitting" @click="submit">Submit supplementary materials</el-button></div></el-card>
</template>
