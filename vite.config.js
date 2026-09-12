import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 守护桌面 Vite 配置
// base 用相对路径，兼容 Capacitor Android 打包（file:// 协议加载）
export default defineConfig({
  base: './',
  plugins: [vue()],
  server: {
    host: '0.0.0.0',
    port: 5176
  }
})
