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
// 登录状态效验
router.beforeEach(async (to, from, next) => {
  NProgress.start();
  const { token } = userStore;
  
  if (token) {
    if (to.path === '/login') {
      userStore.logout();
      localStorage.removeItem('openeds')
      localStorage.removeItem('defaultIndex')
      permissionStore.restore();
      next();
      return;
    }
    // token 存在 进入下一页
    next();
    // const { roles } = userStore;

    // if (roles && roles.length > 0) {
    //   next();
    // } else {
    //   try {
    //     await userStore.getUserInfo();

    //     const { roles } = userStore;

    //     await permissionStore.initRoutes(roles);

    //     if (router.hasRoute(to.name)) {
    //       next();
    //     } else {
    //       next(`/`);
    //     }
    //   } catch (error) {
    //     // MessagePlugin.error(error);
    //     next(`/login?redirect=${to.path}`);
    //     NProgress.done();
    //   }
    // }
  } else {
    // 登录页允许匿名访问，其余页面必须先登录，否则页面虽然能打开，
    // 但所有受保护的业务接口都会被网关拒绝并显示为空数据。
    if (whiteListRouters.indexOf(to.path) !== -1) {
      next();
    } else {
      next(`/login?redirect=${to.fullPath}`);
    }
    NProgress.done();
  }
});

router.afterEach(() => {
  NProgress.done();
});
