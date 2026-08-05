// 应用入口文件：创建 Vue 应用实例，装配全局插件，最后挂载到 index.html 的 #app 上
import { createApp } from 'vue'
import { createPinia } from 'pinia'
// Element Plus UI 组件库：全量引入（学习项目不做按需加载优化）
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
// Element Plus 的中文语言包（否则分页、日期选择器等内置文案是英文）
import zhCn from 'element-plus/es/locale/lang/zh-cn'
// 全局主题（清新绿设计系统）：必须在 element-plus 样式之后引入才能覆盖其默认变量
import './styles/theme.css'

import App from './App.vue'
import router from './router'

const app = createApp(App)

app.use(createPinia()) // 状态管理：必须在任何组件使用 store 之前注册
app.use(router) // 路由
app.use(ElementPlus, { locale: zhCn })

app.mount('#app')
