<script setup>
// 管理端·商品审核：列出全站待审核(status=1)商品，管理员逐条「通过 / 驳回」。
// 通过 → 商品变在售(3)；驳回 → 变审核驳回(2)并附原因（卖家在"我的商品"能看到原因）。
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getPendingProducts } from '@/api/admin'
import { ITEM_CONDITION, statusLabel } from '@/constants'

const list = ref([])
const loading = ref(false)
const router = useRouter()

async function loadList() {
  loading.value = true
  try {
    const res = await getPendingProducts()
    list.value = res.list
  } finally {
    loading.value = false
  }
}

function goDetail(row) {
  router.push(`/admin/products/${row.id}`)
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
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" size="small" @click="goDetail(row)">查看详情</el-button>
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
