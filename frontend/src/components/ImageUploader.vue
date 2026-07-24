<script setup>
// 图片上传组件：发布/编辑商品、纠纷证据等多处共用。
// 用法（父组件）：<ImageUploader v-model="form.images" :max="5" />
//   - v-model 绑定的是"图片 URL 字符串数组"（上传成功后后端返回的地址）
//   - 组件内部用 el-upload 做选择/预览/删除，选中文件后自动调 uploadFile 上传，成功再把 URL 同步给父组件
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { uploadFile } from '@/api/file'

const props = defineProps({
  // 已上传的图片 URL 列表（v-model）
  modelValue: {
    type: Array,
    default: () => [],
  },
  // 最多上传几张
  max: {
    type: Number,
    default: 5,
  },
})
const emit = defineEmits(['update:modelValue'])

// el-upload 展示用的文件列表（每项是 { name, url, status, uid }）
const fileList = ref([])

// 当前已成功上传的 URL 数组（用于和父组件的 modelValue 对比/同步）
function currentUrls() {
  return fileList.value.filter((f) => f.status === 'success').map((f) => f.url)
}

// 父组件传入的 modelValue 变化时，重建内部 fileList（主要用于"编辑模式"回填已有图片）。
// 加相等判断避免和自己的 emit 形成死循环：如果传进来的和当前显示的一样就不重建。
watch(
  () => props.modelValue,
  (val) => {
    const incoming = val || []
    if (JSON.stringify(incoming) === JSON.stringify(currentUrls())) return
    fileList.value = incoming.map((url, i) => ({
      name: `图片${i + 1}`,
      url,
      status: 'success',
      uid: Date.now() + i,
    }))
  },
  { immediate: true },
)

// 是否还能继续加（到达上限就隐藏"+"号）
const canAdd = computed(() => fileList.value.length < props.max)

// 上传前校验：只允许 jpg/png/webp，单张 ≤5MB（和 API 文档的后端限制一致，前端先拦一道）
function beforeUpload(file) {
  const okType = ['image/jpeg', 'image/png', 'image/webp'].includes(file.type)
  if (!okType) {
    ElMessage.error('只支持 jpg / png / webp 格式的图片')
    return false
  }
  if (file.size / 1024 / 1024 > 5) {
    ElMessage.error('单张图片不能超过 5MB')
    return false
  }
  return true
}

// 自定义上传：覆盖 el-upload 默认的上传行为，改成调我们自己的 uploadFile 接口。
// options.file 是用户选中的原始文件；成功后把后端返回的 url 存回对应条目，并通知父组件。
async function customUpload(options) {
  try {
    const res = await uploadFile(options.file)
    const entry = fileList.value.find((f) => f.uid === options.file.uid)
    if (entry) {
      entry.url = res.url
      entry.status = 'success'
    }
    options.onSuccess(res)
    emit('update:modelValue', currentUrls())
  } catch (e) {
    ElMessage.error('图片上传失败，请重试')
    options.onError(e)
  }
}

// 删除某张图后，同步最新 URL 列表给父组件
function handleRemove() {
  emit('update:modelValue', currentUrls())
}

// 超过数量上限时的提示
function handleExceed() {
  ElMessage.warning(`最多上传 ${props.max} 张图片`)
}
</script>

<template>
  <el-upload
    v-model:file-list="fileList"
    list-type="picture-card"
    :http-request="customUpload"
    :before-upload="beforeUpload"
    :limit="max"
    :on-remove="handleRemove"
    :on-exceed="handleExceed"
    accept="image/jpeg,image/png,image/webp"
  >
    <!-- 只有没到上限时才显示"+"添加按钮 -->
    <el-icon v-if="canAdd"><Plus /></el-icon>
  </el-upload>
</template>
