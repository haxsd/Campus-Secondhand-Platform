<script setup>
// 首页 · 商品列表：本项目的门面页面。
// 组成：顶部筛选区（关键词/分类/校区/价格区间）+ 商品网格（ProductCard）+ 底部分页。
// 数据来源：src/api/product.js 的 getProducts（当前 mock 模式，联调时自动切真实后端）。
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getProducts } from '@/api/product'
import { getCategories } from '@/api/category'
import { CAMPUS_LIST } from '@/constants'
import ProductCard from '@/components/ProductCard.vue'

const route = useRoute()

// 筛选条件（reactive 让对象里的字段变化能被 Vue 追踪）
const filters = reactive({
  keyword: '',
  categoryId: '', // '' 表示不限分类
  campus: '',
  minPrice: undefined,
  maxPrice: undefined,
})

const categories = ref([]) // 分类下拉选项，来自 GET /categories
const list = ref([]) // 当前页商品
const total = ref(0) // 符合筛选条件的商品总数（分页组件算总页数用）
const page = ref(1)
const pageSize = ref(12)
const loading = ref(false)

// 拉取商品列表：把筛选条件 + 分页参数一起传给接口
async function loadProducts() {
  loading.value = true
  try {
    const data = await getProducts({
      keyword: filters.keyword || undefined,
      categoryId: filters.categoryId || undefined,
      campus: filters.campus || undefined,
      minPrice: filters.minPrice,
      maxPrice: filters.maxPrice,
      page: page.value,
      pageSize: pageSize.value,
    })
    list.value = data.list
    total.value = data.total
  } finally {
    // 无论成功失败都要关掉 loading，否则页面会一直转圈
    loading.value = false
  }
}

// 点"搜索"或切换分类/校区：筛选条件变了要回到第 1 页再查
function onSearch() {
  page.value = 1
  loadProducts()
}

// 重置：清空所有筛选条件并回到第 1 页
function onReset() {
  filters.keyword = ''
  filters.categoryId = ''
  filters.campus = ''
  filters.minPrice = undefined
  filters.maxPrice = undefined
  page.value = 1
  loadProducts()
}

// 切换页码
function onPageChange(p) {
  page.value = p
  loadProducts()
}

// 监听 URL 上的 keyword 参数（/?keyword=xxx，例如外部链接/分享链接带进来），
// 变化时同步到本页筛选框并重新查询。
watch(
  () => route.query.keyword,
  (kw) => {
    filters.keyword = kw || ''
    page.value = 1
    loadProducts()
  },
)

onMounted(async () => {
  // 先带上 URL 里可能已有的关键词（如外部链接 /?keyword=xxx 进来）
  filters.keyword = route.query.keyword || ''
  // 分类下拉和商品列表并行加载，页面出得更快
  const [cats] = await Promise.all([getCategories(), loadProducts()])
  categories.value = cats
})
</script>

<template>
  <div class="home">
    <!-- 筛选区 -->
    <el-card class="filter-bar" shadow="never">
      <div class="filter-row">
        <el-input
          v-model="filters.keyword"
          class="f-keyword"
          placeholder="搜索商品标题"
          clearable
          @keyup.enter="onSearch"
        />
        <el-select v-model="filters.categoryId" class="f-item" placeholder="全部分类" clearable>
          <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
        <el-select v-model="filters.campus" class="f-item" placeholder="全部校区" clearable>
          <el-option v-for="c in CAMPUS_LIST" :key="c" :label="c" :value="c" />
        </el-select>
        <el-input-number
          v-model="filters.minPrice"
          class="f-price"
          :min="0"
          :controls="false"
          placeholder="最低价"
        />
        <span class="price-sep">-</span>
        <el-input-number
          v-model="filters.maxPrice"
          class="f-price"
          :min="0"
          :controls="false"
          placeholder="最高价"
        />
        <el-button type="primary" @click="onSearch">搜索</el-button>
        <el-button @click="onReset">重置</el-button>
      </div>
    </el-card>

    <!-- 商品网格：v-loading 在加载时盖一层转圈遮罩 -->
    <div v-loading="loading" class="grid-wrap">
      <el-row v-if="list.length" :gutter="16">
        <el-col v-for="item in list" :key="item.id" :xs="12" :sm="8" :md="6">
          <ProductCard :product="item" class="grid-item" />
        </el-col>
      </el-row>
      <!-- 没有符合条件的商品时显示空状态（加载中不显示，避免闪一下空） -->
      <el-empty v-else-if="!loading" description="没有找到符合条件的商品" />
    </div>

    <!-- 分页：只有超过一页时才显示 -->
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
  </div>
</template>

<style scoped>
.home {
  padding: 16px 0;
}

.filter-bar {
  margin-bottom: 16px;
}

.filter-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.f-keyword {
  width: 240px;
}

.f-item {
  width: 140px;
}

.f-price {
  width: 110px;
}

.price-sep {
  color: #909399;
}

.grid-wrap {
  min-height: 200px;
}

.grid-item {
  margin-bottom: 16px;
}

.pager {
  display: flex;
  justify-content: center;
  margin-top: 8px;
}
</style>
