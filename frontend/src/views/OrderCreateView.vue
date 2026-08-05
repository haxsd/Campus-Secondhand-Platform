<script setup>
// 下单页：从商品详情"立即购买"跳来（/orders/create?productId=xx）。
// 展示商品摘要 + 填写数量/交易时间/地点/备注，提交创建订单，成功后跳订单详情。
// 关键点 requestId：进入页面时生成一次 UUID 存起来，提交失败重试沿用同一个，
//   这样即使用户多点了几次，后端也只会创建一个订单（幂等防重）。
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getProductDetail } from '@/api/product'
import { createOrder } from '@/api/order'

const route = useRoute()
const router = useRouter()

const productId = route.query.productId
const product = ref(null)
const loading = ref(false)
const submitting = ref(false)
const formRef = ref()

// requestId 只在进入页面时生成一次（用 ref 存住），后面失败重试不重新生成
const requestId = ref(crypto.randomUUID())

const form = reactive({
  quantity: 1,
  tradeTime: '',
  tradePlace: '',
  remark: '',
})

// 实时计算总金额 = 单价 × 数量
const totalAmount = computed(() => {
  if (!product.value) return '0.00'
  return (Number(product.value.price) * form.quantity).toFixed(2)
})

const rules = {
  quantity: [
    { required: true, type: 'number', min: 1, message: '数量至少为 1', trigger: 'blur' },
    {
      validator: (rule, value, cb) =>
        product.value && value > product.value.stock ? cb(new Error('数量不能超过库存')) : cb(),
      trigger: 'change',
    },
  ],
  tradeTime: [{ required: true, message: '请选择交易时间', trigger: 'change' }],
  tradePlace: [{ required: true, message: '请填写交易地点', trigger: 'blur' }],
}

// 加载商品摘要（下单要展示标题/价格/库存，交易地点默认用商品的）
async function loadProduct() {
  if (!productId) {
    ElMessage.error('缺少商品参数')
    router.push('/')
    return
  }
  loading.value = true
  try {
    product.value = await getProductDetail(productId)
    form.tradePlace = product.value.tradePlace // 默认填商品的交易地点，用户可改
  } catch {
    // 商品不存在/已下架：报错提示已由请求层统一弹出，这里只负责跳走
    router.push('/')
  } finally {
    loading.value = false
  }
}

async function onSubmit() {
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      const res = await createOrder({
        productId,
        quantity: form.quantity,
        tradeTime: form.tradeTime,
        tradePlace: form.tradePlace,
        remark: form.remark,
        requestId: requestId.value, // 幂等键：整页只生成一次
      })
      ElMessage.success('下单成功，等待卖家确认')
      router.push(`/orders/${res.id}`)
    } catch (e) {
      // 错误提示（429/409/400 等）已由请求层统一弹出；
      // 409 说明库存/商品变化，额外刷新一下拿到最新库存与状态
      if (e?.code === 409) loadProduct()
    } finally {
      submitting.value = false
    }
  })
}

onMounted(loadProduct)
</script>

<template>
  <div v-loading="loading" class="page order-create">
    <h2 class="page-title">确认下单</h2>

    <div v-if="product" class="panel">
      <!-- 商品摘要：薄荷底的小卡片 -->
      <div class="summary">
        <el-image :src="product.cover || product.images?.[0]" fit="cover" class="cover" />
        <div class="summary-info">
          <p class="title">{{ product.title }}</p>
          <p class="price"><span class="symbol">¥</span>{{ product.price }}</p>
          <p class="stock">库存 {{ product.stock }} 件</p>
        </div>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px" class="order-form">
        <el-form-item label="购买数量" prop="quantity">
          <el-input-number v-model="form.quantity" :min="1" :max="product.stock" :step="1" />
        </el-form-item>

        <el-form-item label="交易时间" prop="tradeTime">
          <el-date-picker
            v-model="form.tradeTime"
            type="datetime"
            placeholder="选择约定交易时间"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>

        <el-form-item label="交易地点" prop="tradePlace">
          <el-input v-model="form.tradePlace" placeholder="约定的当面交易地点" class="w280" />
        </el-form-item>

        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="form.remark"
            type="textarea"
            :rows="2"
            placeholder="给卖家的留言（选填）"
          />
        </el-form-item>

        <el-form-item label="应付金额">
          <span class="total"><span class="symbol">¥</span>{{ totalAmount }}</span>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" size="large" class="submit-btn" :loading="submitting" @click="onSubmit">
            提交订单
          </el-button>
          <el-button size="large" @click="router.back()">返回</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<style scoped>
.order-create {
  max-width: 720px;
}

.panel {
  background: #fff;
  border: 1px solid var(--app-border);
  border-radius: 16px;
  box-shadow: var(--app-shadow-sm);
  padding: 22px 24px;
}

/* 商品摘要 */
.summary {
  display: flex;
  gap: 16px;
  padding: 14px;
  border-radius: 12px;
  background: var(--app-bg-soft);
  border: 1px solid #ddf2e8;
  margin-bottom: 22px;
}

.cover {
  width: 96px;
  height: 96px;
  border-radius: 10px;
  flex-shrink: 0;
}

.summary-info .title {
  margin: 2px 0 8px;
  font-size: 16px;
  font-weight: 600;
  color: var(--app-text-1);
}

.summary-info .price {
  margin: 0 0 4px;
  color: var(--app-price);
  font-size: 18px;
  font-weight: 700;
}

.summary-info .price .symbol {
  font-size: 13px;
}

.summary-info .stock {
  margin: 0;
  color: var(--app-text-3);
  font-size: 13px;
}

/* 应付金额 */
.total {
  color: var(--app-price);
  font-size: 24px;
  font-weight: 800;
  line-height: 1;
}

.total .symbol {
  font-size: 15px;
  font-weight: 600;
  margin-right: 1px;
}

.submit-btn {
  min-width: 140px;
  font-weight: 600;
}

.w280 {
  width: 280px;
}
</style>
