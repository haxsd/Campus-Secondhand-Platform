# frontend · Vue 3 前端

在此目录用 Vite 初始化 Vue 3 工程：

```bash
npm create vite@latest . -- --template vue
npm i element-plus pinia vue-router axios
```

目录规划（见 docs/全栈实施方案.md 第 4 节）：

```text
src/
├── api/         按模块封装请求（每个函数带 mock 分支）
├── mock/        按 docs/API接口文档.md 1:1 编写的假数据
├── constants/   状态枚举中文映射（商品/订单/成色/纠纷）
├── stores/      Pinia（登录态等）
├── router/      路由 + 登录守卫
├── utils/       request.js（Axios 实例 + 401 拦截）
└── views/       11 个页面
```

开发模式：`.env.development` 设 `VITE_USE_MOCK=true` 先行开发全部页面，后端就绪后关闭联调。
