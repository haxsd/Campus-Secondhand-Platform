<script setup>
// 首页 · 商品列表：本项目的门面页面。
// 组成：品牌横幅（渐变 Hero）+ 悬浮筛选卡（关键词/分类/校区/价格区间）+ 商品网格（ProductCard）+ 底部分页。
// 数据来源：src/api/product.js 的 getProducts（当前 mock 模式，联调时自动切真实后端）。
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Search, CircleCheck } from '@element-plus/icons-vue'
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
// 首页每页展示 8 条。初始化数据较少时也能直观看到第二页，方便验证分页接口。
const pageSize = ref(8)
const loading = ref(false)

// 平台特性小标签：只做展示，向新同学传达平台的交易方式
const HERO_TAGS = ['免费发布', '人工审核', '线下面交', '信用评价']

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
    // 分页总数是普通统计值，后端直接返回 JSON 数字。
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
    <!-- 品牌横幅：铺满全宽的渐变 Hero -->
    <section class="hero">
      <div class="hero-inner">
        <h1 class="hero-title">校园二手 · 好物新生</h1>
        <p class="hero-sub">让闲置在校园里流转起来 —— 同学之间的靠谱交易，放心又实在</p>
        <div class="hero-tags">
          <span v-for="t in HERO_TAGS" :key="t" class="hero-tag">
            <el-icon><CircleCheck /></el-icon>
            {{ t }}
          </span>
        </div>
      </div>
    </section>

    <div class="page home-body">
      <!-- 筛选卡：悬浮叠在横幅下沿 -->
      <div class="filter-bar">
        <div class="filter-row">
          <el-input
            v-model="filters.keyword"
            class="f-keyword"
            placeholder="搜索你想要的宝贝…"
            :prefix-icon="Search"
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
          <span class="price-sep">—</span>
          <el-input-number
            v-model="filters.maxPrice"
            class="f-price"
            :min="0"
            :controls="false"
            placeholder="最高价"
          />
          <el-button type="primary" :icon="Search" @click="onSearch">搜索</el-button>
          <el-button text bg @click="onReset">重置</el-button>
        </div>
      </div>

      <!-- 列表区标题 -->
      <h2 class="section-title">全部好物</h2>

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

      <!--
        始终在有查询结果时显示分页与总数：即使当前只有一页，用户也能知道总商品数，
        当数据增长到多页时无需再改变页面结构。
      -->
      <div v-if="total > 0" class="pager">
        <!-- 明确展示当前页和总数量，数据超过一页时可直观看到分页状态。 -->
        <span class="pager-summary">
          第 {{ page }} / {{ Math.ceil(total / pageSize) }} 页，共 {{ total }} 件商品
        </span>
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
  </div>
</template>

<style scoped>
/* ---------- 渐变横幅 ---------- */
.hero {
  position: relative;
  overflow: hidden;
  background: linear-gradient(118deg, #0ca678 0%, #10b981 48%, #0d9488 100%);
  color: #fff;
}

/* 两个柔光圆：给纯色渐变加一点空气感 */
.hero::before,
.hero::after {
  content: '';
  position: absolute;
  border-radius: 50%;
  pointer-events: none;
}

.hero::before {
  width: 380px;
  height: 380px;
  right: 8%;
  top: -180px;
  background: rgba(255, 255, 255, 0.14);
  filter: blur(6px);
}

.hero::after {
  width: 260px;
  height: 260px;
  left: -80px;
  bottom: -150px;
  background: rgba(255, 255, 255, 0.1);
  filter: blur(4px);
}

.hero-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 46px 20px 78px;
  position: relative;
  z-index: 1;
}

.hero-title {
  margin: 0 0 10px;
  font-size: 32px;
  font-weight: 800;
  letter-spacing: 1px;
  text-shadow: 0 2px 12px rgba(6, 78, 59, 0.25);
}

.hero-sub {
  margin: 0 0 18px;
  font-size: 14.5px;
  color: rgba(255, 255, 255, 0.88);
}

.hero-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.hero-tag {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 5px 13px;
  border-radius: 999px;
  font-size: 13px;
  color: #fff;
  background: rgba(255, 255, 255, 0.16);
  border: 1px solid rgba(255, 255, 255, 0.28);
  backdrop-filter: blur(4px);
}

/* ---------- 内容区 ---------- */
.home-body {
  padding-top: 0;
}

/* 筛选卡：白色悬浮卡片，上移叠在横幅下沿 */
.filter-bar {
  margin-top: -44px;
  position: relative;
  z-index: 2;
  background: #fff;
  border: 1px solid var(--app-border);
  border-radius: 14px;
  box-shadow: var(--app-shadow-md);
  padding: 18px 20px;
}

.filter-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.f-keyword {
  width: 260px;
}

.f-item {
  width: 140px;
}

.f-price {
  width: 110px;
}

.price-sep {
  color: var(--app-text-3);
}

.section-title {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 26px 0 16px;
  font-size: 18px;
  font-weight: 700;
  color: var(--app-text-1);
}

.section-title::before {
  content: '';
  width: 5px;
  height: 16px;
  border-radius: 999px;
  background: var(--app-gradient);
}

.grid-wrap {
  min-height: 200px;
}

.grid-item {
  margin-bottom: 16px;
}

.pager {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 12px;
}

.pager-summary {
  color: var(--app-text-3);
  font-size: 13px;
}

/* 窄屏适配：搜索框占满整行，横幅字号收敛 */
@media (max-width: 640px) {
  .hero-title {
    font-size: 24px;
  }

  .hero-inner {
    padding: 34px 20px 66px;
  }

  .f-keyword {
    width: 100%;
  }
}
</style>
