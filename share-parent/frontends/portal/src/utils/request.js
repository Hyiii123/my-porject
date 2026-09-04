import axios from 'axios';
import { ElMessage, ElMessageBox } from 'element-plus';
import router from '../router';

const CODE = {
  LOGIN_TIMEOUT: 1000,
  REQUEST_SUCCESS: 200,
  REQUEST_FOBID: 1001,
};

// 开发环境直接访问 Spring Cloud Gateway；部署时通过 VITE_API_BASE_URL 覆盖。
const instance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  timeout: 5000,
  withCredentials: false,
});

// 请求拦截器
instance.interceptors.request.use((config) => {
  const TOKEN = sessionStorage.getItem('token');
  config.headers = {
    "Content-Type": "application/json",
    "authorization": TOKEN
  }
  return config
});

// 响应拦截器
instance.interceptors.response.use(
  async (response) => {
    // 1.获取业务状态码
    let code = response.data.code;
    // 2.业务状态码为200，直接返回
    if (code === CODE.REQUEST_SUCCESS) {
      return response.data;
    }

    // 3.业务状态码为401，代表未登录
    if (code === 401) {
      ElMessageBox.confirm(
        '您的账号登录超时或在其他机器登录，请重新登录或更换账号登录！',
        '登录超时',
        {
          confirmButtonText: '重新登录',
          cancelButtonText: '继续浏览',
          type: 'warning',
        }
      ).then(() => {
        router.push('/login')
      }).catch(() => {
        // 继续浏览
      })
    }

    return response.data;
  },
  async (err) => {
    return Promise.reject(err);
  },
);

export default instance;
