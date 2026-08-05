<script setup>
// 商品卡片：首页商品列表、浏览记录页共用的一个小方块。
// 只负责"展示一件商品 + 点击跳详情"，数据由父组件通过 product 传进来（对齐列表接口返回的字段）。
// 视觉：封面按 4:3 比例自适应，悬停时卡片上浮、图片轻微放大；成色以小徽章叠在图上。
import { useRouter } from 'vue-router'
import { Picture, Location } from '@element-plus/icons-vue'
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
  <div class="product-card" @click="goDetail">
    <!-- 封面图：el-image 支持懒加载；加载失败时显示一个图片占位图标（error 插槽） -->
    <div class="cover-wrap">
      <el-image :src="product.cover" class="cover" fit="cover" lazy>
        <template #error>
          <div class="cover-placeholder">
            <el-icon><Picture /></el-icon>
          </div>
        </template>
      </el-image>
      <!-- 成色徽章：把数字编码翻译成中文（0全新/1几乎全新/...） -->
      <span class="condition-chip">{{ statusLabel(ITEM_CONDITION, product.itemCondition) }}</span>
    </div>

    <div class="info">
      <!-- 标题：最多显示两行，超出用省略号（下方 CSS 控制） -->
      <p class="title">{{ product.title }}</p>
      <div class="bottom">
        <!-- 价格：暖橙色加粗，¥ 符号缩小 -->
        <span class="price"><span class="symbol">¥</span>{{ product.price }}</span>
        <!-- 校区小字 -->
        <span class="campus">
          <el-icon><Location /></el-icon>
          {{ product.campus }}
        </span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.product-card {
  cursor: pointer;
  background: #fff;
  border: 1px solid var(--app-border);
  border-radius: 14px;
  overflow: hidden;
  box-shadow: var(--app-shadow-sm);
  transition:
    transform 0.25s ease,
    box-shadow 0.25s ease,
    border-color 0.25s ease;
}

.product-card:hover {
  transform: translateY(-4px);
  border-color: var(--el-color-primary-light-7);
  box-shadow: var(--app-shadow-md);
}

/* 封面区：固定 4:3 比例，悬停时图片轻微放大 */
.cover-wrap {
  position: relative;
  aspect-ratio: 4 / 3;
  overflow: hidden;
  background: var(--app-bg-soft);
}

.cover,
.cover-placeholder {
  width: 100%;
  height: 100%;
  display: block;
}

.cover {
  transition: transform 0.45s ease;
}

.product-card:hover .cover {
  transform: scale(1.06);
}

.cover-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #b9cfc6;
  font-size: 32px;
}

/* 成色徽章：白色毛玻璃小胶囊 */
.condition-chip {
  position: absolute;
  top: 10px;
  left: 10px;
  padding: 3px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 500;
  color: var(--el-color-primary-dark-2);
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(4px);
  box-shadow: 0 1px 4px rgba(23, 46, 38, 0.12);
}

.info {
  padding: 12px 14px 14px;
}

.title {
  margin: 0 0 10px;
  font-size: 14px;
  font-weight: 500;
  line-height: 1.45;
  color: var(--app-text-1);
  /* 标题最多两行，超出省略号 */
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 40px;
}

.bottom {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
}

.price {
  color: var(--app-price);
  font-weight: 700;
  font-size: 19px;
  line-height: 1;
}

.price .symbol {
  font-size: 13px;
  margin-right: 1px;
}

.campus {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-size: 12px;
  color: var(--app-text-3);
  white-space: nowrap;
}
</style>
