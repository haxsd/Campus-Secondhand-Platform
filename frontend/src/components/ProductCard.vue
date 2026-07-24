<script setup>
// 商品卡片：首页商品列表、浏览记录页共用的一个小方块。
// 只负责"展示一件商品 + 点击跳详情"，数据由父组件通过 product 传进来（对齐列表接口返回的字段）。
import { useRouter } from 'vue-router'
import { Picture } from '@element-plus/icons-vue'
import { ITEM_CONDITION, statusLabel } from '@/constants'

// defineProps：声明这个组件需要外部传入的数据。这里要一件商品对象。
const props = defineProps({
  product: {
    type: Object,
    required: true,
  },
})

const router = useRouter()

// 点击卡片 → 跳到该商品的详情页 /products/:id
function goDetail() {
  router.push(`/products/${props.product.id}`)
}
</script>

<template>
  <el-card class="product-card" shadow="hover" :body-style="{ padding: '0' }" @click="goDetail">
    <!-- 封面图：el-image 支持懒加载；加载失败时显示一个图片占位图标（error 插槽） -->
    <el-image :src="product.cover" class="cover" fit="cover" lazy>
      <template #error>
        <div class="cover-placeholder">
          <el-icon><Picture /></el-icon>
        </div>
      </template>
    </el-image>

    <div class="info">
      <!-- 标题：最多显示两行，超出用省略号（下方 CSS 控制） -->
      <p class="title">{{ product.title }}</p>
      <div class="bottom">
        <!-- 价格：红色加粗，前面加 ¥ 符号 -->
        <span class="price">¥{{ product.price }}</span>
        <!-- 成色：把数字编码翻译成中文（0全新/1几乎全新/...） -->
        <span class="condition">{{ statusLabel(ITEM_CONDITION, product.itemCondition) }}</span>
      </div>
      <!-- 校区小字 -->
      <div class="campus">{{ product.campus }}</div>
    </div>
  </el-card>
</template>

<style scoped>
.product-card {
  cursor: pointer;
  border-radius: 8px;
  overflow: hidden;
}

.cover,
.cover-placeholder {
  width: 100%;
  height: 180px;
  display: block;
}

.cover-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  color: #c0c4cc;
  font-size: 32px;
}

.info {
  padding: 10px 12px 12px;
}

.title {
  margin: 0 0 8px;
  font-size: 14px;
  line-height: 1.4;
  color: #303133;
  /* 标题最多两行，超出省略号 */
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 39px;
}

.bottom {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
}

.price {
  color: #f56c6c;
  font-weight: bold;
  font-size: 18px;
}

.condition {
  font-size: 12px;
  color: #909399;
}

.campus {
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
}
</style>
