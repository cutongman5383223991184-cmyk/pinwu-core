import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'
// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    uni(),
  ],
  server: {
    port: 5200,      // 这里指定端口为 5200 (或者你喜欢的任何数字)
    host: '0.0.0.0', // 允许局域网访问 (方便手机连同一个wifi查看)
    open: true,      // 运行后自动打开浏览器
  }
})
