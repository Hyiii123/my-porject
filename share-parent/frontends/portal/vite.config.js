// 项目配置页面
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import svgLoader from 'vite-svg-loader';
import vueJsx from '@vitejs/plugin-vue-jsx';
import path from 'path';
import fs from 'fs';

const CWD = process.cwd();

// 数据库中的演示数据使用 /src/assets/... 形式保存资源地址。
// Vite 只会打包被 import 的资源，因此生产构建时额外复制静态资源目录，
// 保证这些动态地址在 Nginx/静态服务器上仍然可访问。
const copySourceAssets = () => ({
  name: 'copy-source-assets',
  closeBundle() {
    const source = path.resolve(CWD, 'src/assets');
    const target = path.resolve(CWD, 'dist/src/assets');
    if (fs.existsSync(source)) {
      fs.cpSync(source, target, { recursive: true });
    }
  },
});

//配置参考 https://vitejs.dev/config/
export default defineConfig((mode) => {
  // const { VITE_BASE_URL } = loadEnv(mode, CWD);
  return {
    base: '/',
    resolve: {
      alias: {
        '@': path.resolve(__dirname, './src'),
      },
    },
    plugins: [
      vue(),
      vueJsx(),
      svgLoader(),
      copySourceAssets()
    ],
    server: {
      port: 18081,
      host: '0.0.0.0',
      proxy: {
        '/img-tx': {
          target: process.env.VITE_IMG_TARGET || 'http://www.zhiwen.com/',
          changeOrigin: true,
        },
        '/cs': {
          target: process.env.VITE_PROXY_TARGET || 'http://localhost:8080',
          changeOrigin: true,
        },
        '/ts': {
          target: process.env.VITE_PROXY_TARGET || 'http://localhost:8080',
          changeOrigin: true,
        },
        '/ls': {
          target: process.env.VITE_PROXY_TARGET || 'http://localhost:8080',
          changeOrigin: true,
        },
        '/es': {
          target: process.env.VITE_PROXY_TARGET || 'http://localhost:8080',
          changeOrigin: true,
        },
        '/as': {
          target: process.env.VITE_PROXY_TARGET || 'http://localhost:8080',
          changeOrigin: true,
        },
        '/fs': {
          target: process.env.VITE_PROXY_TARGET || 'http://localhost:8080',
          changeOrigin: true,
        },
        '/ws': {
          target: process.env.VITE_PROXY_TARGET || 'http://localhost:8080',
          changeOrigin: true,
          ws: true,
        },
        '/pixel': {
          target: process.env.VITE_PROXY_TARGET || 'http://localhost:8080',
          changeOrigin: true,
        },
        '/api': {
          target: process.env.VITE_PROXY_TARGET || 'http://localhost:8080',
          changeOrigin: true,
        },
      }
    },
  }
})
