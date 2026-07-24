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
    ElMessage.error('商品不存在或已下架')
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
      // 按后端约定的错误码分别提示
      if (e?.code === 429) {
        ElMessage.warning('操作太频繁，请稍后再试')
      } else if (e?.code === 409) {
        ElMessage.warning('库存不足或商品已变化，已为你刷新商品信息')
        loadProduct() // 刷新拿最新库存/状态
      }
      // 400（买自己商品）等其它错误由 request.js / mock 统一提示
    } finally {
      submitting.value = false
    }
  })
}

onMounted(loadProduct)
</script>

<template>
  <div v-loading="loading" class="order-create">
    <el-card v-if="product" shadow="never">
      <template #header>确认下单</template>

      <!-- 商品摘要 -->
      <div class="summary">
        <el-image :src="product.cover || product.images?.[0]" fit="cover" class="cover" />
        <div class="summary-info">
          <p class="title">{{ product.title }}</p>
          <p class="price">单价 ¥{{ product.price }}</p>
          <p class="stock">库存 {{ product.stock }} 件</p>
        </div>
      </div>

      <el-divider />

      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
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
          <span class="total">¥{{ totalAmount }}</span>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="onSubmit">提交订单</el-button>
          <el-button @click="router.back()">返回</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.order-create {
  padding: 16px 0;
  max-width: 680px;
  margin: 0 auto;
}

.summary {
  display: flex;
  gap: 16px;
}

.cover {
  width: 96px;
  height: 96px;
  border-radius: 6px;
  flex-shrink: 0;
}

.summary-info .title {
  margin: 0 0 8px;
  font-size: 16px;
  color: #303133;
}

.summary-info .price,
.summary-info .stock {
  margin: 4px 0;
  color: #909399;
  font-size: 13px;
}

.total {
  color: #f56c6c;
  font-size: 22px;
  font-weight: bold;
}

.w280 {
  width: 280px;
}
</style>
