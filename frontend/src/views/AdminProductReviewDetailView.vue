<script setup>
// 管理员审核详情页：先完整查看商品、图片和卖家信息，再执行通过或驳回操作。
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getPendingProductDetail, reviewProduct, getAiReview } from '@/api/admin'
import { ITEM_CONDITION, statusLabel } from '@/constants'

const route = useRoute()
const router = useRouter()
const detail = ref(null)
const loading = ref(false)
const acting = ref(false)
const aiReview = ref(null)

async function load() {
  loading.value = true
  try {
    detail.value = await getPendingProductDetail(route.params.id)
    aiReview.value = await getAiReview(route.params.id)
  } catch {
    router.replace('/admin/products')
  } finally {
    loading.value = false
  }
}

async function review(pass) {
  let reason
  if (!pass) {
    try {
      const result = await ElMessageBox.prompt('请输入驳回原因', '驳回申请', {
        inputType: 'textarea',
        inputValidator: (value) => (value?.trim() ? true : '驳回原因不能为空'),
      })
      reason = result.value.trim()
    } catch { return }
  } else {
    try { await ElMessageBox.confirm(`确认通过「${detail.value.title}」吗？`, '审核通过', { type: 'success' }) } catch { return }
  }
  acting.value = true
  try {
    await reviewProduct(detail.value.id, { pass, ...(reason ? { reason } : {}) })
    ElMessage.success(pass ? '已通过，商品已上架' : '已驳回')
    router.replace('/admin/products')
  } finally { acting.value = false }
}

onMounted(load)
</script>

<template>
  <div v-loading="loading" class="page review-detail">
    <el-button @click="router.push('/admin/products')">返回审核列表</el-button>
    <el-card v-if="detail" class="card" shadow="never">
      <template #header>审核商品：{{ detail.title }}</template>
      <el-carousel v-if="detail.images?.length" height="360px" class="images">
        <el-carousel-item v-for="(url, index) in detail.images" :key="url">
          <!-- 详情中只显示原图，不启用表格缩略图的预览层，避免当前图片弹层显示异常。 -->
          <el-image :src="url" fit="contain" class="image" />
          <span class="image-index">{{ index + 1 }} / {{ detail.images.length }}</span>
        </el-carousel-item>
      </el-carousel>
      <el-descriptions :column="2" border class="info">
        <el-descriptions-item label="价格">¥{{ detail.price }}</el-descriptions-item>
        <el-descriptions-item label="库存">{{ detail.stock }}</el-descriptions-item>
        <el-descriptions-item label="成色">{{ statusLabel(ITEM_CONDITION, detail.itemCondition) }}</el-descriptions-item>
        <el-descriptions-item label="分类">{{ detail.categoryName }}</el-descriptions-item>
        <el-descriptions-item label="校区">{{ detail.campus }}</el-descriptions-item>
        <el-descriptions-item label="交易地点">{{ detail.tradePlace }}</el-descriptions-item>
        <el-descriptions-item label="卖家">{{ detail.seller?.nickname }}</el-descriptions-item>
        <el-descriptions-item label="卖家信用">{{ detail.seller?.creditScore }} 分 / {{ detail.seller?.dealCount }} 次成交</el-descriptions-item>
        <el-descriptions-item label="商品描述" :span="2">{{ detail.description }}</el-descriptions-item>
      </el-descriptions>
      <div class="actions">
        <el-button type="success" :loading="acting" @click="review(true)">通过审核</el-button>
        <el-button type="danger" :loading="acting" @click="review(false)">驳回申请</el-button>
      </div>
      <el-card v-if="aiReview?.latestRun" class="ai-card" shadow="never">
        <template #header>AI初审结论（仅供管理员参考）</template>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="Decision">{{ aiReview.latestRun.decision }}</el-descriptions-item>
          <el-descriptions-item label="Risk">{{ aiReview.latestRun.riskLevel }}</el-descriptions-item>
          <el-descriptions-item label="Confidence">{{ aiReview.latestRun.confidence }}</el-descriptions-item>
          <el-descriptions-item label="Run ID">{{ aiReview.latestRun.runId }}</el-descriptions-item>
          <el-descriptions-item label="Reasons" :span="2">
            <ul><li v-for="reason in aiReview.latestRun.reasons" :key="reason">{{ reason }}</li></ul>
          </el-descriptions-item>
          <el-descriptions-item label="Suggestions" :span="2">
            <ul><li v-for="suggestion in aiReview.latestRun.suggestions" :key="suggestion">{{ suggestion }}</li></ul>
          </el-descriptions-item>
          <el-descriptions-item label="Rule Refs" :span="2">
            <div v-for="rule in aiReview.latestRun.ruleRefs" :key="`${rule.ruleId}-${rule.ruleVersion}`">
              {{ rule.title || `${rule.ruleId}@${rule.ruleVersion}` }}
              <span v-if="rule.evidence">｜证据：{{ rule.evidence }}</span>
            </div>
          </el-descriptions-item>
        </el-descriptions>
      </el-card>
    </el-card>
  </div>
</template>

<style scoped>
.review-detail { max-width: 900px; }
.card { margin-top: 16px; }
.images { background: var(--app-bg-soft); border-radius: 12px; overflow: hidden; }
.image { width: 100%; height: 100%; }
.image-index { position: absolute; right: 16px; bottom: 12px; color: #fff; background: rgb(0 0 0 / 45%); padding: 3px 8px; border-radius: 10px; }
.info { margin-top: 20px; }
.actions { margin-top: 20px; display: flex; justify-content: flex-end; gap: 12px; }
</style>
