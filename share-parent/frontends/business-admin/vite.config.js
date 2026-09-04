// 项目配置页面
import { defineConfig, loadEnv } from "vite";
import vue from "@vitejs/plugin-vue";
import svgLoader from "vite-svg-loader";
import vueJsx from "@vitejs/plugin-vue-jsx";
import path from "path";
import fs from "fs";

const CWD = process.cwd();

// 数据库中的演示数据使用 /src/assets/... 形式保存资源地址。
// 动态字符串不会被 Vite 的 import 分析自动收集，生产构建时复制资源目录，
// 确保课程封面、教师头像等地址可以直接访问。
const copySourceAssets = () => ({
  name: "copy-source-assets",
  closeBundle() {
    const source = path.resolve(CWD, "src/assets");
    const target = path.resolve(CWD, "dist/src/assets");
    if (fs.existsSync(source)) {
      fs.cpSync(source, target, { recursive: true });
    }
  },
});

//配置参考 https://vitejs.dev/config/
export default defineConfig((mode) => {
  const { VITE_BASE_URL } = loadEnv(mode, CWD);
  return {
    base: "/",
    resolve: {
      alias: {
        "@": path.resolve(__dirname, "./src"),
      },
    },
    plugins: [vue(), vueJsx(), svgLoader(), copySourceAssets()],
    // server:{
    //   port: 8081
    // }
    server: {
      port: 18081,
      host: "0.0.0.0",
      // proxy: {
      //     '/img-tx': {
      //     target: 'http://wisehub-1312394356.cos.ap-shanghai.myqcloud.com',
      //     // rewrite: (path) => {
      //     //   return path.replace(/^\/img-tx/, '')
      //     // }
      //   },
      // },
      proxy: {
        "/img-tx": {
          // target: "https://tjxt-dev.itheima.net",
          target:  'http://www.tianji.com/',
          changeOrigin: true,
          // rewrite: (path) => {
          //   return path.replace(/^\/img-tx/, '')
          // }
        },
      },
    },
  };
});
