<script setup>
// 商品详情页：展示单件商品的完整信息 + 卖家信用 + 最近评价，并提供"购买/管理/登录后购买"入口。
// 数据来源：src/api/product.js 的 getProductDetail（当前 mock 模式）。
// 路由：/products/:id —— 从 URL 里拿到商品 id 去请求详情。
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Picture, Star } from '@element-plus/icons-vue'
import { getProductDetail } from '@/api/product'
import { ITEM_CONDITION, statusLabel } from '@/constants'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const product = ref(null) // 商品详情对象
const loading = ref(false)
const notFound = ref(false) // 商品不存在/已下架时置 true，页面显示 el-result

// 拉详情：成功填 product；接口返回 404（reject code=404）则标记 notFound
async function loadDetail(id) {
  loading.value = true
  notFound.value = false
  try {
    product.value = await getProductDetail(id)
  } catch (e) {
    // 商品不存在或已下架：详情接口约定返回 404
    if (e?.code === 404) {
      notFound.value = true
    }
  } finally {
    loading.value = false
  }
}

// 是不是"我自己"发布的商品：登录了且当前用户 id === 卖家 id。
// 是自己的商品就显示"去管理"而不是"购买"（自己不能买自己的东西）。
const isOwner = computed(
  () =>
    userStore.isLoggedIn &&
    product.value &&
    String(product.value.seller.id) === String(userStore.user?.id),
)

// 好评率：后端给的是 0~1 的小数（0.96），展示成百分比（96%）
const goodRatePercent = computed(() =>
  product.value ? Math.round(product.value.seller.goodReviewRate * 100) : 0,
)

// 未登录点购买 → 跳登录页，登录成功后回到本详情页
function onLoginToBuy() {
  router.push({ path: '/login', query: { redirect: route.fullPath } })
}

// 立即购买 → 跳下单页，带上 productId
function onBuy() {
  router.push({ path: '/orders/create', query: { productId: product.value.id } })
}

// 自己的商品 → 去"我的商品"管理
function onManage() {
  router.push('/my/products')
}

// 监听路由里的 id 变化：从一个详情页点相关商品跳到另一个详情页时，要重新拉数据。
// immediate: true 让组件首次加载时也执行一次（省掉单独的 onMounted 调用）。
watch(
  () => route.params.id,
  (id) => {
    if (id) loadDetail(id)
  },
  { immediate: true },
)
</script>

<template>
  <div v-loading="loading" class="page detail">
    <!-- 情况一：商品不存在或已下架 -->
    <el-result
      v-if="notFound"
      icon="warning"
      title="商品不存在或已下架"
      sub-title="该商品可能已被卖家下架或删除"
    >
      <template #extra>
        <el-button type="primary" @click="router.push('/')">返回首页</el-button>
      </template>
    </el-result>

    <!-- 情况二：正常展示详情 -->
    <template v-else-if="product">
      <!-- 主信息卡：左图右文 -->
      <div class="main-card">
        <el-row :gutter="28">
          <!-- 左侧：图片轮播 -->
          <el-col :xs="24" :md="12">
            <el-carousel
              v-if="product.images?.length"
              height="380px"
              trigger="click"
              class="carousel"
            >
              <el-carousel-item v-for="(img, i) in product.images" :key="i">
                <el-image :src="img" fit="cover" class="carousel-img">
                  <template #error>
                    <div class="img-placeholder">
                      <el-icon><Picture /></el-icon>
                    </div>
                  </template>
                </el-image>
              </el-carousel-item>
            </el-carousel>
          </el-col>

          <!-- 右侧：核心信息 + 操作按钮 -->
          <el-col :xs="24" :md="12">
            <h2 class="title">{{ product.title }}</h2>

            <!-- 价格横幅：薄荷底色突出价格 -->
            <div class="price-banner">
              <span class="symbol">¥</span>
              <span class="num">{{ product.price }}</span>
            </div>

            <!-- 商品属性：用 el-descriptions 做整齐的键值对展示 -->
            <el-descriptions :column="2" border class="attrs">
              <el-descriptions-item label="成色">
                {{ statusLabel(ITEM_CONDITION, product.itemCondition) }}
              </el-descriptions-item>
              <el-descriptions-item label="校区">{{ product.campus }}</el-descriptions-item>
              <el-descriptions-item label="交易地点">{{ product.tradePlace }}</el-descriptions-item>
              <el-descriptions-item label="分类">{{ product.categoryName }}</el-descriptions-item>
              <el-descriptions-item label="库存">{{ product.stock }}</el-descriptions-item>
              <el-descriptions-item label="浏览量">{{ product.viewCount }}</el-descriptions-item>
            </el-descriptions>

            <!-- 操作按钮：按登录态 / 是否自己商品 / 是否有货 分别显示 -->
            <div class="actions">
              <el-button
                v-if="!userStore.isLoggedIn"
                type="primary"
                size="large"
                class="buy-btn"
                @click="onLoginToBuy"
              >
                登录后购买
              </el-button>
              <el-button
                v-else-if="isOwner"
                type="warning"
                size="large"
                class="buy-btn"
                @click="onManage"
              >
                去管理
              </el-button>
              <el-button
                v-else
                type="primary"
                size="large"
                class="buy-btn"
                :disabled="product.stock <= 0"
                @click="onBuy"
              >
                {{ product.stock > 0 ? '立即购买' : '已售罄' }}
              </el-button>
            </div>

            <!-- 卖家信用卡片 -->
            <div class="seller">
              <div class="seller-head">
                <el-avatar :size="42">{{ product.seller.nickname?.[0] || '卖' }}</el-avatar>
                <div class="seller-title">
                  <span class="seller-name">{{ product.seller.nickname }}</span>
                  <span class="seller-role">卖家信用档案</span>
                </div>
              </div>
              <div class="seller-stats">
                <div class="stat">
                  <span class="num">{{ product.seller.creditScore }}</span>
                  <span class="label">信用分</span>
                </div>
                <div class="stat">
                  <span class="num">{{ product.seller.dealCount }}</span>
                  <span class="label">成交数</span>
                </div>
                <div class="stat">
                  <span class="num">{{ product.seller.avgRating }}</span>
                  <span class="label">平均评分</span>
                </div>
                <div class="stat">
                  <span class="num">{{ goodRatePercent }}%</span>
                  <span class="label">好评率</span>
                </div>
              </div>
            </div>
          </el-col>
        </el-row>
      </div>

      <!-- 商品描述 -->
      <div class="section-card">
        <h3 class="section-head">商品描述</h3>
        <p class="desc">{{ product.description }}</p>
      </div>

      <!-- 最近评价 -->
      <div class="section-card">
        <h3 class="section-head">最近评价</h3>
        <div v-if="product.recentReviews?.length">
          <div v-for="(r, i) in product.recentReviews" :key="i" class="review">
            <div class="review-top">
              <el-rate :model-value="r.rating" disabled size="small" />
              <span class="review-time">{{ r.createdAt }}</span>
            </div>
            <p class="review-content">{{ r.content }}</p>
          </div>
        </div>
        <el-empty v-else :image-size="60" description="暂无评价">
          <template #image>
            <el-icon :size="48" color="#b9cfc6"><Star /></el-icon>
          </template>
        </el-empty>
      </div>
    </template>
  </div>
</template>

<style scoped>
.detail {
  min-height: 300px;
}

/* 主信息卡 / 分区卡：统一的白色圆角面板 */
.main-card,
.section-card {
  background: #fff;
  border: 1px solid var(--app-border);
  border-radius: 16px;
  box-shadow: var(--app-shadow-sm);
  padding: 24px;
}

.section-card {
  margin-top: 16px;
}

.section-head {
  display: flex;
  align-items: center;
  gap: 9px;
  margin: 0 0 14px;
  font-size: 16px;
  font-weight: 600;
  color: var(--app-text-1);
}

.section-head::before {
  content: '';
  width: 4px;
  height: 15px;
  border-radius: 999px;
  background: var(--app-gradient);
}

/* 轮播：圆角 + 柔和底色 */
.carousel {
  border-radius: 12px;
  overflow: hidden;
  background: var(--app-bg-soft);
}

.carousel-img,
.img-placeholder {
  width: 100%;
  height: 380px;
}

.img-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--app-bg-soft);
  color: #b9cfc6;
  font-size: 48px;
}

.title {
  margin: 4px 0 14px;
  font-size: 22px;
  font-weight: 700;
  line-height: 1.4;
  color: var(--app-text-1);
}

/* 价格横幅 */
.price-banner {
  display: flex;
  align-items: baseline;
  gap: 2px;
  padding: 12px 16px;
  margin-bottom: 16px;
  border-radius: 12px;
  background: linear-gradient(120deg, #fff7f2, #fff1e8);
  border: 1px solid #ffe4d4;
  color: var(--app-price);
}

.price-banner .symbol {
  font-size: 16px;
  font-weight: 600;
}

.price-banner .num {
  font-size: 30px;
  font-weight: 800;
  line-height: 1;
}

.attrs {
  margin-bottom: 18px;
}

.actions {
  margin-bottom: 18px;
}

.buy-btn {
  min-width: 172px;
  border-radius: 12px;
  font-weight: 600;
}

/* 卖家信用卡片：薄荷底 */
.seller {
  background: var(--app-bg-soft);
  border: 1px solid #ddf2e8;
  border-radius: 14px;
  padding: 16px 18px;
}

.seller-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}

.seller-title {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.seller-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--app-text-1);
}

.seller-role {
  font-size: 12px;
  color: var(--app-text-3);
}

.seller-stats {
  display: flex;
}

.stat {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.stat + .stat {
  border-left: 1px solid #d9ede3;
}

.stat .num {
  font-size: 18px;
  font-weight: 700;
  color: var(--el-color-primary-dark-2);
}

.stat .label {
  font-size: 12px;
  color: var(--app-text-3);
}

.desc {
  margin: 0;
  line-height: 1.8;
  color: var(--app-text-2);
  white-space: pre-wrap;
}

.review {
  padding: 14px 0;
  border-bottom: 1px solid #f0f6f3;
}

.review:first-of-type {
  padding-top: 4px;
}

.review:last-child {
  border-bottom: none;
  padding-bottom: 4px;
}

.review-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.review-content {
  margin: 8px 0 0;
  color: var(--app-text-1);
  line-height: 1.6;
}

.review-time {
  font-size: 12px;
  color: var(--app-text-3);
}
</style>
