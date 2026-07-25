// 文件上传模块 mock：模拟后端 POST /api/files/upload。
// 真实后端会把图片存到服务器并返回可访问的 URL（如 /api/uploads/xxx.jpg）。
// mock 阶段没有服务器，这里用浏览器的 URL.createObjectURL(file) 生成一个"本地临时地址"，
// 这样你在页面上能立刻看到自己刚选的那张图（预览效果和真实上传一致）。
// 注意：这个临时地址只在当前页面有效，刷新就失效——这是 mock 的正常现象，联调接真实后端后就是持久 URL 了。

function delay(data, ms = 400) {
  return new Promise((resolve) => setTimeout(() => resolve(data), ms))
}

// 返回结构对齐 API 文档：{ url: "..." }
export function mockUploadFile(file) {
  const url = URL.createObjectURL(file)
  return delay({ url })
}
