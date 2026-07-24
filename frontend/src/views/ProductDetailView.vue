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
  <div v-loading="loading" class="detail">
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
      <el-row :gutter="24">
        <!-- 左侧：图片轮播 -->
        <el-col :xs="24" :md="12">
          <el-carousel v-if="product.images?.length" height="360px" trigger="click">
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
          <div class="price">¥{{ product.price }}</div>

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
              @click="onLoginToBuy"
            >
              登录后购买
            </el-button>
            <el-button v-else-if="isOwner" type="warning" size="large" @click="onManage">
              去管理
            </el-button>
            <el-button
              v-else
              type="primary"
              size="large"
              :disabled="product.stock <= 0"
              @click="onBuy"
            >
              {{ product.stock > 0 ? '立即购买' : '已售罄' }}
            </el-button>
          </div>

          <!-- 卖家信用卡片 -->
          <el-card class="seller" shadow="never">
            <div class="seller-head">
              <el-avatar :size="40">{{ product.seller.nickname?.[0] || '卖' }}</el-avatar>
              <span class="seller-name">{{ product.seller.nickname }}</span>
            </div>
            <div class="seller-stats">
              <div class="stat">
                <span class="num">{{ product.seller.creditScore }}</span
                >信用分
              </div>
              <div class="stat">
                <span class="num">{{ product.seller.dealCount }}</span
                >成交数
              </div>
              <div class="stat">
                <span class="num">{{ product.seller.avgRating }}</span
                >平均评分
              </div>
              <div class="stat">
                <span class="num">{{ goodRatePercent }}%</span>好评率
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 商品描述 -->
      <el-card class="section" shadow="never">
        <template #header>商品描述</template>
        <p class="desc">{{ product.description }}</p>
      </el-card>

      <!-- 最近评价 -->
      <el-card class="section" shadow="never">
        <template #header>最近评价</template>
        <div v-if="product.recentReviews?.length">
          <div v-for="(r, i) in product.recentReviews" :key="i" class="review">
            <el-rate :model-value="r.rating" disabled size="small" />
            <p class="review-content">{{ r.content }}</p>
            <span class="review-time">{{ r.createdAt }}</span>
          </div>
        </div>
        <el-empty v-else :image-size="60" description="暂无评价">
          <template #image
            ><el-icon :size="48"><Star /></el-icon
          ></template>
        </el-empty>
      </el-card>
    </template>
  </div>
</template>

<style scoped>
.detail {
  padding: 16px 0;
  min-height: 300px;
}

.carousel-img,
.img-placeholder {
  width: 100%;
  height: 360px;
}

.img-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  color: #c0c4cc;
  font-size: 48px;
}

.title {
  margin: 0 0 12px;
  font-size: 20px;
  color: #303133;
}

.price {
  color: #f56c6c;
  font-weight: bold;
  font-size: 30px;
  margin-bottom: 16px;
}

.attrs {
  margin-bottom: 20px;
}

.actions {
  margin-bottom: 20px;
}

.seller-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.seller-name {
  font-size: 15px;
  font-weight: 500;
  color: #303133;
}

.seller-stats {
  display: flex;
  gap: 24px;
}

.stat {
  display: flex;
  flex-direction: column;
  font-size: 12px;
  color: #909399;
  gap: 4px;
}

.stat .num {
  font-size: 18px;
  font-weight: bold;
  color: #303133;
}

.section {
  margin-top: 16px;
}

.desc {
  margin: 0;
  line-height: 1.7;
  color: #606266;
  white-space: pre-wrap;
}

.review {
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.review:last-child {
  border-bottom: none;
}

.review-content {
  margin: 6px 0 4px;
  color: #303133;
}

.review-time {
  font-size: 12px;
  color: #909399;
}
</style>
