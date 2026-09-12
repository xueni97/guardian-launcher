import { createApp } from 'vue'
import { createRouter, createWebHashHistory } from 'vue-router'
import { App as CapacitorApp } from '@capacitor/app'
import Vant from 'vant'
import 'vant/lib/index.css'
import App from './App.vue'
import HomePage from './views/HomePage.vue'
import ContactsPage from './views/ContactsPage.vue'
import SettingsPage from './views/SettingsPage.vue'
import './styles/global.css'

// 路由用 hash 模式，兼容 WebView
const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: '/', name: 'home', component: HomePage },
    { path: '/contacts', name: 'contacts', component: ContactsPage },
    { path: '/settings', name: 'settings', component: SettingsPage }
  ]
})

// Android 返回键/侧滑手势拦截：
// 子页面返回首页；首页本身是桌面启动器，不响应返回（防误退）
CapacitorApp.addListener('backButton', () => {
  if (router.currentRoute.value.path !== '/') {
    router.push('/')
  }
})

const app = createApp(App)
app.use(router)
app.use(Vant)
app.mount('#app')
