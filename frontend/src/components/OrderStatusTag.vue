<script setup>
// 订单状态标签：把订单 status 数字统一渲染成带颜色的中文 el-tag。
// 订单列表、订单详情等多处共用，避免各写一份颜色/文案。
import { ORDER_STATUS, statusLabel, statusType } from '@/constants'

defineProps({
  status: {
    type: Number,
    required: true,
  },
})
</script>

<template>
  <el-tag :type="statusType(ORDER_STATUS, status)">
    {{ statusLabel(ORDER_STATUS, status) }}
  </el-tag>
</template>
