<script setup>
// 我的订单列表页：
// - 顶部 el-tabs 切换角色：我买到的（role=buyer）/ 我卖出的（role=seller）
// - 角色下再按订单状态筛选
// - 表格每行显示商品快照标题+单价、数量、总额、状态标签、交易对方、创建时间，点击进订单详情
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getOrders } from '@/api/order'
import { ORDER_STATUS } from '@/constants'
import OrderStatusTag from '@/components/OrderStatusTag.vue'

const router = useRouter()

const role = ref('buyer') // buyer=我买到的 / seller=我卖出的
const status = ref('') // 订单状态筛选，'' = 全部
const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const loading = ref(false)

// 状态筛选下拉选项：全部 + 6 种订单状态
const statusOptions = [
  { value: '', label: '全部状态' },
  ...Object.entries(ORDER_STATUS).map(([value, { label }]) => ({ value, label })),
]

async function loadOrders() {
  loading.value = true
  try {
    const res = await getOrders({
      role: role.value,
      status: status.value,
      page: page.value,
      pageSize: pageSize.value,
    })
    list.value = res.list
    total.value = res.total
  } finally {
    loading.value = false
  }
}

// 切角色/切状态都回到第 1 页再查
function onRoleChange() {
  page.value = 1
  loadOrders()
}
function onStatusChange() {
  page.value = 1
  loadOrders()
}
function onPageChange(p) {
  page.value = p
  loadOrders()
}

function goDetail(row) {
  router.push(`/orders/${row.id}`)
}

onMounted(loadOrders)
</script>

<template>
  <div class="page my-orders">
    <h2 class="page-title">我的订单</h2>

    <el-card shadow="never">
      <!-- 角色切换 -->
      <el-tabs v-model="role" @tab-change="onRoleChange">
        <el-tab-pane label="我买到的" name="buyer" />
        <el-tab-pane label="我卖出的" name="seller" />
      </el-tabs>

      <!-- 状态筛选 -->
      <div class="filter">
        <el-select v-model="status" class="w160" @change="onStatusChange">
          <el-option v-for="o in statusOptions" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
      </div>

      <el-table
        v-loading="loading"
        :data="list"
        style="width: 100%"
        row-key="id"
        @row-click="goDetail"
      >
        <el-table-column label="商品" min-width="220">
          <template #default="{ row }">
            <div class="cell-product">
              <span class="p-title">{{ row.snapshot.title }}</span>
              <span class="p-price">单价 ¥{{ row.snapshot.price }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="quantity" label="数量" width="70" />

        <el-table-column label="总额" width="110">
          <template #default="{ row }">
            <span class="total">¥{{ row.totalAmount }}</span>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <OrderStatusTag :status="row.status" />
          </template>
        </el-table-column>

        <!-- 交易对方：买到的看卖家，卖出的看买家 -->
        <el-table-column label="对方" width="140">
          <template #default="{ row }">
            {{ role === 'buyer' ? row.seller.nickname : row.buyer.nickname }}
          </template>
        </el-table-column>

        <el-table-column prop="createdAt" label="下单时间" width="170" />

        <template #empty>
          <el-empty description="暂无订单" />
        </template>
      </el-table>

      <div v-if="total > pageSize" class="pager">
        <el-pagination
          background
          layout="prev, pager, next, total"
          :total="total"
          :page-size="pageSize"
          :current-page="page"
          @current-change="onPageChange"
        />
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.my-orders :deep(.el-card__body) {
  padding-top: 10px;
}

.filter {
  margin: 4px 0 14px;
}

.w160 {
  width: 160px;
}

.cell-product {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.p-title {
  color: var(--app-text-1);
  font-weight: 500;
}

.p-price {
  font-size: 12px;
  color: var(--app-text-3);
}

.total {
  color: var(--app-price);
  font-weight: 700;
}

.pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

:deep(.el-table__row) {
  cursor: pointer;
}
</style>
