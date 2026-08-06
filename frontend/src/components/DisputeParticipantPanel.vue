<script setup>
// 纠纷参与方材料面板：只在管理员要求补充材料时开放追加。
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { appendDisputeEvidence, getDispute } from '@/api/dispute'
import ImageUploader from '@/components/ImageUploader.vue'

const props = defineProps({
  dispute: { type: Object, required: true },
})
const emit = defineEmits(['updated'])
const statement = ref('')
const evidence = ref([])
const submitting = ref(false)
const canAppend = computed(() => props.dispute.status === 1)

async function submit() {
  if (!canAppend.value || (!statement.value.trim() && evidence.value.length === 0)) {
    ElMessage.warning('请填写补充说明或上传证据')
    return
  }
  submitting.value = true
  try {
    await appendDisputeEvidence(props.dispute.id, { statement: statement.value, evidence: evidence.value })
    const latest = await getDispute(props.dispute.id)
    statement.value = ''
    evidence.value = []
    emit('updated', latest)
    ElMessage.success('补充材料已提交')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <section class="dispute-participant-panel">
    <div>当前状态：{{ dispute.status }}</div>
    <div>证据版本：{{ dispute.evidenceVersion }}</div>
    <p>{{ dispute.statement }}</p>
    <el-image v-for="url in dispute.evidence || []" :key="url" :src="url" fit="cover" />
    <template v-if="canAppend">
      <el-input v-model="statement" type="textarea" maxlength="2000" show-word-limit placeholder="补充说明" />
      <ImageUploader v-model="evidence" :max="5 - (dispute.evidence || []).length" />
      <el-button type="primary" :loading="submitting" @click="submit">提交补充材料</el-button>
    </template>
  </section>
</template>
