// 权限配置页面
// import { MessagePlugin } from 'tdesign-vue-next';
import NProgress from 'nprogress'; // progress bar
import 'nprogress/nprogress.css'; // progress bar style

import { getPermissionStore, getToken } from '@/store';
import router from '@/router';

const permissionStore = getPermissionStore();
const userStore = getToken();

// 页面加载进度
NProgress.configure({ showSpinner: false });

const { whiteListRouters } = permissionStore;

const PUBLIC_PATHS = ['/', '/login', '/main', '/search', '/details', '/customer-service', '/points', '/result', '/askDetails'];
const isPublicRoute = (path) => PUBLIC_PATHS.some((p) => path === p || path.startsWith(p === '/' ? '///' : p + '/'));

// 登录状态效验
router.beforeEach(async (to, from, next) => {
  NProgress.start();
  const { token } = userStore;

  if (token) {
    if (to.path === '/login') {
      userStore.logout();
      permissionStore.restore();
      next();
      return;
    }
    next();
  } else {
    // 未登录用户仅允许访问公开白名单路由，避免非白名单受限页面产生级联 401 风暴
    if (isPublicRoute(to.path)) {
      next();
    } else {
      next(`/login?redirect=${encodeURIComponent(to.fullPath)}`);
      NProgress.done();
    }
  }
});

router.afterEach(() => {
  NProgress.done();
});
