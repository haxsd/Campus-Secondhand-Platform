import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue(), vueDevTools()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    proxy: {
      // 浏览器仍请求同源的 /api，由 Vite 开发服务器转发到 Spring Boot。
      // 不重写路径，因为后端本身的 context-path 就是 /api。
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      // 兼容初始化数据中可能已经保存的旧图片地址 /uploads/xxx。
      // 后端现在统一在 /api/uploads/** 提供静态图片，因此代理时补上 /api 前缀。
      '/uploads': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => `/api${path}`,
      },
    },
  },
})
