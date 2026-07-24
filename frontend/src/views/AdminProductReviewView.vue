<script setup>
// 管理端·商品审核：列出全站待审核(status=1)商品，管理员逐条「通过 / 驳回」。
// 通过 → 商品变在售(3)；驳回 → 变审核驳回(2)并附原因（卖家在"我的商品"能看到原因）。
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getPendingProducts, reviewProduct } from '@/api/admin'
import { ITEM_CONDITION, statusLabel } from '@/constants'

const list = ref([])
const loading = ref(false)
const acting = ref(false) // 操作中，防连点

async function loadList() {
  loading.value = true
  try {
    const res = await getPendingProducts()
    list.value = res.list
  } finally {
    loading.value = false
  }
}

// 通过审核
async function onPass(row) {
  try {
    await ElMessageBox.confirm(`确认通过「${row.title}」的上架申请吗？`, '审核通过', {
      type: 'success',
    })
  } catch {
    return
  }
  acting.value = true
  try {
    await reviewProduct(row.id, { pass: true })
    ElMessage.success('已通过，商品已上架')
    await loadList()
  } catch (e) {
    if (e?.code === 409) await loadList() // 状态已变（可能被撤回），刷新
  } finally {
    acting.value = false
  }
}

// 驳回审核：必须填原因
async function onReject(row) {
  let res
  try {
    res = await ElMessageBox.prompt('请输入驳回原因', '驳回申请', {
      confirmButtonText: '确定驳回',
      cancelButtonText: '取消',
      inputType: 'textarea',
      inputValidator: (v) => (v && v.trim() ? true : '驳回原因不能为空'),
    })
  } catch {
    return
  }
  acting.value = true
  try {
    await reviewProduct(row.id, { pass: false, reason: res.value.trim() })
    ElMessage.success('已驳回')
    await loadList()
  } catch (e) {
    if (e?.code === 409) await loadList()
  } finally {
    acting.value = false
  }
}

onMounted(loadList)
</script>

<template>
  <div class="admin-review">
    <h2 class="title">商品审核</h2>

    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column label="封面" width="90">
        <template #default="{ row }">
          <el-image
            :src="row.cover"
            fit="cover"
            class="cover"
            :preview-src-list="row.images || []"
          />
        </template>
      </el-table-column>
      <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
      <el-table-column label="价格" width="100">
        <template #default="{ row }">
          <span class="price">¥{{ row.price }}</span>
        </template>
      </el-table-column>
      <el-table-column label="库存" prop="stock" width="70" />
      <el-table-column label="成色" width="110">
        <template #default="{ row }">
          {{ statusLabel(ITEM_CONDITION, row.itemCondition) }}
        </template>
      </el-table-column>
      <el-table-column label="分类" prop="categoryName" width="100" />
      <el-table-column label="卖家" width="100">
        <template #default="{ row }">{{ row.seller?.nickname }}</template>
      </el-table-column>
      <el-table-column prop="createdAt" label="提交时间" width="170" />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button type="success" size="small" :loading="acting" @click="onPass(row)">
            通过
          </el-button>
          <el-button type="danger" size="small" :loading="acting" @click="onReject(row)">
            驳回
          </el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无待审核商品" />
      </template>
    </el-table>
  </div>
</template>

<style scoped>
.admin-review {
  padding: 16px 0;
}

.title {
  margin: 0 0 16px;
  font-size: 20px;
}

.cover {
  width: 56px;
  height: 56px;
  border-radius: 4px;
}

.price {
  color: #f56c6c;
  font-weight: bold;
}
</style>
