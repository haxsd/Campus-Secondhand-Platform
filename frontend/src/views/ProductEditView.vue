<script setup>
// 发布 / 编辑商品页（同一个组件，按 URL 里有没有 :id 区分模式）：
//   - /publish            → 发布模式（新建草稿）
//   - /products/:id/edit  → 编辑模式（进入时拉详情回填，提交带 version 做乐观锁）
// 提交成功后统一跳到"我的商品"，并提示"已保存为草稿，请申请上架"。
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createProduct, updateProduct, getMyProducts } from '@/api/product'
import { getCategories } from '@/api/category'
import { ITEM_CONDITION, CAMPUS_LIST } from '@/constants'
import ImageUploader from '@/components/ImageUploader.vue'

const route = useRoute()
const router = useRouter()

// 编辑模式判断：路由参数里有 id 就是编辑，否则是发布
const productId = computed(() => route.params.id || null)
const isEdit = computed(() => !!productId.value)

const formRef = ref() // el-form 实例，用于触发校验
const categories = ref([]) // 分类下拉选项
const pageLoading = ref(false) // 编辑模式拉详情时的加载态
const submitting = ref(false) // 提交按钮的加载态

// 表单数据。price 用数字方便 el-input-number 编辑，提交时再转成两位小数字符串（对齐后端）。
const form = reactive({
  title: '',
  description: '',
  price: undefined,
  stock: 1,
  itemCondition: undefined,
  categoryId: '',
  campus: '',
  tradePlace: '',
  images: [],
  version: undefined, // 仅编辑模式使用
})

// 成色下拉选项：把常量对象 { 0:{label:'全新'}, ... } 转成 [{ value, label }]
const conditionOptions = computed(() =>
  Object.entries(ITEM_CONDITION).map(([value, { label }]) => ({
    value: Number(value),
    label,
  })),
)

// 表单校验规则
const rules = {
  title: [
    { required: true, message: '请输入商品标题', trigger: 'blur' },
    { max: 50, message: '标题不超过 50 个字', trigger: 'blur' },
  ],
  price: [
    { required: true, message: '请输入价格', trigger: 'blur' },
    { type: 'number', min: 0.01, message: '价格必须大于 0', trigger: 'blur' },
  ],
  stock: [{ required: true, type: 'number', min: 1, message: '库存至少为 1', trigger: 'blur' }],
  itemCondition: [{ required: true, message: '请选择成色', trigger: 'change' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  campus: [{ required: true, message: '请选择校区', trigger: 'change' }],
  tradePlace: [{ required: true, message: '请填写交易地点', trigger: 'blur' }],
  // 图片自定义校验：至少上传 1 张
  images: [
    {
      validator: (rule, value, callback) =>
        value && value.length ? callback() : callback(new Error('请至少上传 1 张图片')),
      trigger: 'change',
    },
  ],
}

// 编辑模式：从“我的商品”中查找当前商品后回填表单。
// 不能使用公开详情接口：草稿、待审核和驳回商品按后端规则不允许匿名公开访问。
async function loadForEdit() {
  pageLoading.value = true
  try {
    const response = await getMyProducts()
    const p = response.find((item) => String(item.id) === String(productId.value))
    if (!p) throw new Error('商品不存在或无权编辑')
    form.title = p.title
    form.description = p.description
    form.price = Number(p.price)
    form.stock = p.stock
    form.itemCondition = p.itemCondition
    form.categoryId = p.categoryId
    form.campus = p.campus
    form.tradePlace = p.tradePlace
    form.images = p.images ? [...p.images] : []
    form.version = p.version ?? 1
  } catch {
    // 加载失败的提示已由请求层统一弹出，这里只回到"我的商品"
    router.push('/my/products')
  } finally {
    pageLoading.value = false
  }
}

// 提交：先校验，再按模式调发布或编辑接口
async function onSubmit() {
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      // 组装请求体：price 转成两位小数字符串（如 1800 -> "1800.00"）
      const payload = {
        title: form.title,
        description: form.description,
        price: Number(form.price).toFixed(2),
        stock: form.stock,
        itemCondition: form.itemCondition,
        categoryId: form.categoryId,
        campus: form.campus,
        tradePlace: form.tradePlace,
        images: form.images,
      }
      if (isEdit.value) {
        // 编辑要带 version：后端比对，不一致返回 409
        await updateProduct(productId.value, { ...payload, version: form.version })
      } else {
        await createProduct(payload)
      }
      ElMessage.success('已保存为草稿，请到"我的商品"申请上架')
      router.push('/my/products')
    } catch {
      // 错误提示（含 409 乐观锁冲突）已由请求层统一弹出，这里无需重复处理
    } finally {
      submitting.value = false
    }
  })
}

onMounted(async () => {
  categories.value = await getCategories()
  if (isEdit.value) loadForEdit()
})
</script>

<template>
  <div v-loading="pageLoading" class="page edit-page">
    <h2 class="page-title">{{ isEdit ? '编辑商品' : '发布闲置' }}</h2>

    <el-card shadow="never">

      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px" class="edit-form">
        <el-form-item label="标题" prop="title">
          <el-input
            v-model="form.title"
            placeholder="一句话描述你的宝贝"
            maxlength="50"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="4"
            placeholder="补充成色、入手渠道、可小刀等信息"
          />
        </el-form-item>

        <el-form-item label="价格" prop="price">
          <el-input-number v-model="form.price" :min="0" :precision="2" :step="1" />
          <span class="unit">元</span>
        </el-form-item>

        <el-form-item label="库存" prop="stock">
          <el-input-number v-model="form.stock" :min="1" :step="1" />
        </el-form-item>

        <el-form-item label="成色" prop="itemCondition">
          <el-select v-model="form.itemCondition" placeholder="请选择成色" class="w220">
            <el-option
              v-for="opt in conditionOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="分类" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="请选择分类" class="w220">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="校区" prop="campus">
          <el-select v-model="form.campus" placeholder="请选择校区" class="w220">
            <el-option v-for="c in CAMPUS_LIST" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>

        <el-form-item label="交易地点" prop="tradePlace">
          <el-input v-model="form.tradePlace" placeholder="如：三食堂门口菜鸟驿站" class="w220" />
        </el-form-item>

        <el-form-item label="商品图片" prop="images">
          <ImageUploader v-model="form.images" :max="5" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="onSubmit">
            {{ isEdit ? '保存修改' : '发布' }}
          </el-button>
          <el-button @click="router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.edit-page {
  max-width: 800px;
}

.edit-form {
  margin-top: 8px;
}

.unit {
  margin-left: 8px;
  color: var(--app-text-3);
}

.w220 {
  width: 220px;
}
</style>
