<!-- 页面头部组件 -->
<template>
  <header class="header">
    <div class="container">
      <div class="header-content">
        <!-- 左侧Logo -->
        <div class="header-left">
          <router-link to="/" class="logo-link">
            <div class="logo-container">
              <div class="logo-icon">智</div>
              <div class="logo-text">
                <span class="logo-name">智问学伴</span>
                <span class="logo-slogan">ONLINE EDUCATION</span>
              </div>
            </div>
          </router-link>
        </div>

        <!-- 中间搜索框 -->
        <div class="header-center">
          <div class="search-box">
            <el-input
              v-model="input"
              placeholder="搜索感兴趣的课程、讲师..."
              size="large"
              @keyup.enter="SearchHandle"
            >
              <template #prefix>
                <el-icon class="search-icon"><Search /></el-icon>
              </template>
              <template #append>
                <el-button class="search-btn" type="primary" @click="SearchHandle">搜索</el-button>
              </template>
            </el-input>
          </div>
        </div>

        <!-- 右侧功能区 -->
        <div class="header-right">
          <!-- 购物车 -->
          <div class="nav-item" @click="$router.push('/pay/carts')">
            <el-badge :value="cartCount" :hidden="cartCount === 0" class="badge">
              <el-icon :size="20"><ShoppingCart /></el-icon>
            </el-badge>
            <span class="nav-text">购物车</span>
          </div>

          <!-- 我的学习 -->
          <div class="nav-item" @click="$router.push('/my-class/index')">
            <el-icon :size="20"><Reading /></el-icon>
            <span class="nav-text">我的学习</span>
          </div>

          <!-- 客服中心 -->
          <div class="nav-item customer-service-nav" @click="router.push({ name: 'customerServiceIndex' })">
            <el-icon :size="20"><Service /></el-icon>
            <span class="nav-text">智能客服</span>
          </div>

          <!-- AI 模拟面试 -->
          <div class="nav-item interview-nav" @click="router.push({ name: 'interviewIndex' })">
            <el-icon :size="20"><Trophy /></el-icon>
            <span class="nav-text">AI 模拟面试</span>
          </div>

          <!-- 分割线 -->
          <div class="divider"></div>

          <!-- 用户信息/登录 -->
          <div v-if="isLoggedIn" class="user-section">
            <el-dropdown trigger="click" @command="handleCommand">
              <div class="user-trigger">
                <el-avatar :size="34" :src="formatAvatarUrl(userInfo.avatar || userInfo.icon) || defaultAvatar" @error="() => true">
                  <img :src="defaultAvatar" alt="默认头像" />
                </el-avatar>
                <span class="username">{{ userInfo.nickname || userInfo.name || userInfo.nickName || '用户' }}</span>
                <el-icon class="arrow-icon"><ArrowDown /></el-icon>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="personal">
                    <el-icon><User /></el-icon>
                    <span>个人中心</span>
                  </el-dropdown-item>
                  <el-dropdown-item command="myClass">
                    <el-icon><Reading /></el-icon>
                    <span>我的课表</span>
                  </el-dropdown-item>
                  <el-dropdown-item command="notes">
                    <el-icon><Edit /></el-icon>
                    <span>我的笔记</span>
                  </el-dropdown-item>
                  <el-dropdown-item command="points">
                    <el-icon><Star /></el-icon>
                    <span>我的积分</span>
                  </el-dropdown-item>
                  <el-dropdown-item command="orders">
                    <el-icon><Collection /></el-icon>
                    <span>我的订单</span>
                  </el-dropdown-item>
                  <el-dropdown-item divided command="logout">
                    <el-icon><SwitchButton /></el-icon>
                    <span>退出登录</span>
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>

          <div v-else class="login-section">
            <el-button type="primary" class="header-btn" @click="$router.push('/login')">登录</el-button>
            <el-button type="primary" class="header-btn" @click="$router.push('/login?md=register')">注册</el-button>
          </div>
        </div>
      </div>
    </div>
  </header>
</template>

<script setup>
import { ref, onMounted, watch } from "vue";
import {
  Search, ShoppingCart, Reading, User, Edit, Star,
  Collection, SwitchButton, ArrowDown, Service, Trophy
} from "@element-plus/icons-vue";
import { useRouter, useRoute } from "vue-router";
import { ElMessage } from "element-plus";

import { getUserInfo } from "@/api/user.js";
import defaultAvatar from "@/assets/images/users/default-avatar.svg";

const router = useRouter();
const route = useRoute();

// 用户状态
const isLoggedIn = ref(false);
const userInfo = ref({
  nickname: '用户',
  avatar: defaultAvatar
});

const formatAvatarUrl = (url) => {
  if (!url) return '';
  if (url.startsWith('http://') || url.startsWith('https://') || url.startsWith('blob:') || url.startsWith('data:')) return url;
  const base = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '');
  return base ? `${base}${url}` : url;
};

// 购物车数量
const cartCount = ref(2);

// 搜索相关
const input = ref('');

// 检查登录状态
const checkLoginStatus = async () => {
  const token = sessionStorage.getItem('token');
  isLoggedIn.value = !!token;

  if (isLoggedIn.value) {
    const savedUserInfo = sessionStorage.getItem('userInfo');
    if (savedUserInfo) {
      try {
        userInfo.value = JSON.parse(savedUserInfo);
      } catch (e) {
        console.error('Failed to parse user info:', e);
      }
    }
    try {
      const res = await getUserInfo();
      if (res && res.code === 200 && res.data) {
        userInfo.value = res.data;
        sessionStorage.setItem('userInfo', JSON.stringify(res.data));
      }
    } catch (e) {
      // 降级使用本地缓存
    }
  }
};

// 监听路由变化
watch(() => route.path, () => {
  checkLoginStatus();
});

onMounted(() => {
  checkLoginStatus();
  window.addEventListener('user-profile-updated', checkLoginStatus);
});

// 搜索事件
const SearchHandle = () => {
  if (input.value === '') {
    ElMessage.warning('请输入搜索关键词');
    return;
  }
  router.push({ path: '/search/index', query: { keyword: input.value } });
};

// 下拉菜单命令
const handleCommand = (command) => {
  switch (command) {
    case 'personal':
      router.push('/personal/main/overview');
      break;
    case 'myClass':
      router.push('/my-class/index');
      break;
    case 'notes':
      router.push('/notes/index');
      break;
    case 'points':
      router.push('/points/index');
      break;
    case 'orders':
      router.push('/personal/main/myOrder');
      break;
    case 'logout':
      handleLogout();
      break;
  }
};

// 退出登录
const handleLogout = () => {
  sessionStorage.removeItem('token');
  sessionStorage.removeItem('userInfo');
  isLoggedIn.value = false;
  ElMessage.success('已退出登录');
  window.location.href = '/#/login';
  window.location.reload();
};
</script>

<style scoped lang="scss">
.header {
  background: #FFFFFF;
  border-bottom: 1px solid #E2E8F0;
  box-shadow: 0 1px 3px 0 rgba(15, 23, 42, 0.04);
  position: sticky;
  top: 0;
  z-index: 1000;
  height: 64px;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
  height: 100%;
}

.header-content {
  display: flex;
  align-items: center;
  height: 100%;
  gap: 28px;
}

/* 左侧Logo */
.header-left {
  flex-shrink: 0;
}

.logo-link {
  display: flex;
  align-items: center;
  text-decoration: none;
}

.logo-container {
  display: flex;
  align-items: center;
  gap: 10px;
}

.logo-icon {
  width: 36px;
  height: 36px;
  background: #2563EB;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #FFFFFF;
  font-size: 18px;
  font-weight: 700;
  letter-spacing: -0.5px;
  box-shadow: 0 2px 4px rgba(37, 99, 235, 0.2);
}

.logo-text {
  display: flex;
  flex-direction: column;
}

.logo-name {
  font-size: 17px;
  font-weight: 700;
  color: #0F172A;
  line-height: 1.2;
}

.logo-slogan {
  font-size: 9px;
  color: #64748B;
  letter-spacing: 0.8px;
  font-weight: 500;
}

/* 中间搜索框 */
.header-center {
  flex: 1;
  max-width: 480px;
}

.search-box {
  width: 100%;
}

.search-box :deep(.el-input) {
  --el-input-height: 40px;
}

.search-box :deep(.el-input__wrapper) {
  border-radius: 8px 0 0 8px !important;
  border: 1px solid #E2E8F0 !important;
  border-right: none !important;
  box-shadow: none !important;
  padding: 0 14px;
  background-color: #FFFFFF;
  box-sizing: border-box;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;

  &:hover {
    border-color: #CBD5E1 !important;
  }

  &.is-focus {
    border-color: #2563EB !important;
    box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.12) !important;
    z-index: 1;
  }
}

.search-box :deep(.el-input-group__append) {
  background-color: #2563EB !important;
  border: 1px solid #2563EB !important;
  border-left: none !important;
  border-radius: 0 8px 8px 0 !important;
  padding: 0 !important;
  box-shadow: none !important;
  overflow: hidden;
  box-sizing: border-box;
}

.search-box :deep(.el-input-group__append .el-button) {
  margin: 0 !important;
  height: 100% !important;
  min-height: 38px;
  border: none !important;
  border-radius: 0 !important;
  background-color: #2563EB !important;
  color: #FFFFFF !important;
  padding: 0 20px !important;
  font-weight: 500;
  font-size: 14px;
  letter-spacing: 1px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  box-shadow: none !important;
  transition: background-color 0.2s ease;

  &:hover {
    background-color: #1D4ED8 !important;
  }

  &:active {
    background-color: #1E40AF !important;
  }
}

.search-icon {
  color: #94A3B8;
  font-size: 16px;
}

/* 右侧功能区 */
.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-shrink: 0;
  margin-left: auto;
}

.nav-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  cursor: pointer;
  padding: 6px 10px;
  border-radius: 6px;
  transition: all 0.2s ease;
  color: #475569;

  &:hover {
    background: #F1F5F9;
    color: #2563EB;
  }
}

.nav-text {
  font-size: 11px;
  white-space: nowrap;
  font-weight: 500;
}

.badge :deep(.el-badge__content) {
  background: #DC2626;
  border: none;
  font-size: 11px;
}

.divider {
  width: 1px;
  height: 20px;
  background: #E2E8F0;
  margin: 0 2px;
}

/* 用户信息 */
.user-section {
  display: flex;
  align-items: center;
}

.user-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 6px;
  transition: all 0.2s;
  &:hover {
    background: #F1F5F9;
  }
}

.username {
  font-size: 13px;
  font-weight: 500;
  color: #0F172A;
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.arrow-icon {
  font-size: 12px;
  color: #94A3B8;
  transition: transform 0.2s;
}

/* 登录和注册按钮统一规范 */
.login-section {
  display: flex;
  align-items: center;
  gap: 10px;

  :deep(.header-btn) {
    height: 32px;
    padding: 0 16px;
    font-size: 13px;
    font-weight: 500;
    border-radius: 6px;
    background-color: #2563EB !important;
    border-color: #2563EB !important;
    color: #FFFFFF !important;
    box-shadow: 0 1px 2px rgba(37, 99, 235, 0.12);
    transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);

    &:hover, &:focus {
      background-color: #1D4ED8 !important;
      border-color: #1D4ED8 !important;
      color: #FFFFFF !important;
      box-shadow: 0 2px 4px rgba(37, 99, 235, 0.22);
    }

    &:active {
      background-color: #1E40AF !important;
      border-color: #1E40AF !important;
      color: #FFFFFF !important;
    }
  }
}

/* 下拉菜单 */
:deep(.el-dropdown-menu__item) {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  font-size: 13px;
}

/* 响应式适配 */
@media (max-width: 768px) {
  .header-content {
    gap: 12px;
  }
  .logo-slogan {
    display: none;
  }
  .header-center {
    max-width: 180px;
  }
  .search-box :deep(.el-input-group__append) {
    display: none;
  }
  .search-box :deep(.el-input__wrapper) {
    border-radius: 8px !important;
    border-right: 1px solid #E2E8F0 !important;
  }
  .nav-text, .username {
    display: none;
  }
  .nav-item {
    padding: 4px 6px;
  }
}
</style>
