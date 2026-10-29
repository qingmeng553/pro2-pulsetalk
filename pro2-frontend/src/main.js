import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import 'element-plus/dist/index.css'
import '@fontsource/inter/latin-400.css'   // 正文字重（仅 latin 子集，中文回落系统字体）
import '@fontsource/inter/latin-500.css'   // 中等（按钮、标题）
import '@fontsource/inter/latin-600.css'   // 加粗（页面标题）
import './styles/tokens.css'         // 设计 Token：必须早于 main.css
import './styles/main.css'
import './styles/element-override.css' // 最后导入：覆盖 EP 默认值 + 1.0.x 旧变量

import App from './App.vue'
import router from './router'

const app = createApp(App)

// Element-Plus(中文)
app.use(ElementPlus, { locale: zhCn })

// 全局注册 Element Plus 图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(router)
app.mount('#app')
