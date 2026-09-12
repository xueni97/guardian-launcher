import { createApp } from 'vue'
import { createRouter, createWebHashHistory } from 'vue-router'
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

const app = createApp(App)
app.use(router)
app.use(Vant)
app.mount('#app')
