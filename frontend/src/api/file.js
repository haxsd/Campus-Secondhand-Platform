// 文件上传接口层：页面/组件调 uploadFile(file)，不关心走 mock 还是真实后端。
import { post } from '@/utils/request'
import { mockUploadFile } from '@/mock/file'

const useMock = import.meta.env.VITE_USE_MOCK === 'true'

// POST /api/files/upload —— 图片上传（multipart，字段名 file），真实后端返回 { url: '/api/uploads/...' }
export function uploadFile(file) {
  if (useMock) return mockUploadFile(file)
  // 真实上传：用 FormData 以 multipart 形式发送，字段名必须是后端约定的 'file'
  const formData = new FormData()
  formData.append('file', file)
  return post('/files/upload', formData)
}
