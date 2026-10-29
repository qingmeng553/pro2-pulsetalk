import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// Vite 开发服务器配置
// 说明：
//   - 前端所有请求统一走 /api 前缀，由 Vite 代理转发到后端 8085 并去除前缀，规避跨域
//   - /files 反代到 MinIO：后端返回相对路径后，开发环境也能直接加载图片
//   - 请求自动携带 sa-token 头(见 src/api/request.js)
export default defineConfig({
  plugins: [vue()],
  server: {
    host: '0.0.0.0',
    port: 5173,
    proxy: {
      '/api': {
        target: process.env.VITE_API_TARGET || 'http://localhost:8085',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '')
      },
      '/files': {
        target: process.env.VITE_FILES_TARGET || 'http://localhost:9000',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/files/, '')
      }
    }
  },
  build: {
    outDir: 'dist',
    chunkSizeWarningLimit: 1500
  }
})
