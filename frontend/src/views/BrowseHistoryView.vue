<script setup>
// 浏览记录页：展示"我最近看过的商品"，按最后浏览时间倒序、分页。
// 记录的写入是在打开商品详情时由（mock 的）详情接口顺手完成的，本页只负责读取展示。
import { onMounted, ref } from 'vue'
import { getBrowseHistory } from '@/api/history'
import ProductCard from '@/components/ProductCard.vue'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const page = ref(1)
const pageSize = 12

async function loadList() {
  loading.value = true
  try {
    const res = await getBrowseHistory({ page: page.value, pageSize })
    list.value = res.list
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function onPageChange(p) {
  page.value = p
  loadList()
}

onMounted(loadList)
</script>

<template>
  <div class="page history">
    <h2 class="page-title">浏览记录</h2>

    <div v-loading="loading" class="grid-wrap">
      <el-row v-if="list.length" :gutter="16">
        <el-col v-for="item in list" :key="item.productId" :xs="12" :sm="8" :md="6">
          <div class="grid-item">
            <ProductCard :product="item" />
            <!-- 最后浏览时间：浏览记录页比首页多出的一行小字 -->
            <div class="view-time">浏览于 {{ item.lastViewTime }}</div>
          </div>
        </el-col>
      </el-row>
      <el-empty v-else-if="!loading" description="还没有浏览记录，去首页逛逛吧" />
    </div>

    <div v-if="total > pageSize" class="pager">
      <el-pagination
        background
        layout="prev, pager, next"
        :total="total"
        :page-size="pageSize"
        :current-page="page"
        @current-change="onPageChange"
      />
    </div>
  </div>
</template>

<style scoped>
.history {
  min-height: 46vh;
}

.grid-item {
  margin-bottom: 16px;
}

.view-time {
  margin-top: 8px;
  padding-left: 2px;
  font-size: 12px;
  color: var(--app-text-3);
}

.pager {
  display: flex;
  justify-content: center;
  margin-top: 8px;
}
</style>
