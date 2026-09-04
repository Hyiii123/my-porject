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
                <span class="logo-slogan">ZHIWEN XUEBAN</span>
              </div>
            </div>
          </router-link>
        </div>

        <!-- 中间搜索框 -->
        <div class="header-center">
          <div class="search-box">
            <el-input
              v-model="input"
              placeholder="搜索课程、教师..."
              size="large"
              @keyup.enter="SearchHandle"
            >
              <template #prefix>
                <el-icon class="search-icon"><Search /></el-icon>
              </template>
              <template #append>
                <el-button type="primary" @click="SearchHandle">搜索</el-button>
              </template>
            </el-input>
          </div>
        </div>

        <!-- 右侧功能区 -->
        <div class="header-right">
          <!-- 购物车 -->
          <div class="nav-item" @click="$router.push('/pay/carts')">
            <el-badge :value="cartCount" :hidden="cartCount === 0" class="badge">
              <el-icon :size="22"><ShoppingCart /></el-icon>
            </el-badge>
            <span class="nav-text">购物车</span>
          </div>

          <!-- 我的学习 -->
          <div class="nav-item" @click="$router.push('/my-class/index')">
            <el-icon :size="22"><Reading /></el-icon>
            <span class="nav-text">我的学习</span>
          </div>

          <!-- 客服中心 -->
          <div class="nav-item customer-service-nav" @click="router.push({ name: 'customerServiceIndex' })">
            <el-icon :size="22"><Service /></el-icon>
            <span class="nav-text">客服中心</span>
          </div>

          <!-- 分割线 -->
          <div class="divider"></div>

          <!-- 用户信息/登录 -->
          <div v-if="isLoggedIn" class="user-section">
            <el-dropdown trigger="click" @command="handleCommand">
              <div class="user-trigger">
                <el-avatar :size="36" :src="userInfo.avatar || '/src/assets/images/users/default-avatar.svg'" />
                <span class="username">{{ userInfo.nickname }}</span>
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
            <el-button type="primary" @click="$router.push('/login')">登录</el-button>
            <el-button @click="$router.push('/login')">注册</el-button>
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
  Collection, SwitchButton, ArrowDown, Service
} from "@element-plus/icons-vue";
import { useRouter, useRoute } from "vue-router";
import { ElMessage } from "element-plus";

const router = useRouter();
const route = useRoute();

// 用户状态
const isLoggedIn = ref(false);
const userInfo = ref({
  nickname: '用户',
  avatar: '/src/assets/images/users/default-avatar.svg'
});

// 购物车数量
const cartCount = ref(2);

// 搜索相关
const input = ref('');

// 检查登录状态
const checkLoginStatus = () => {
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
  }
};

// 监听路由变化
watch(() => route.path, () => {
  checkLoginStatus();
});

onMounted(() => {
  checkLoginStatus();
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
      router.push('/personal/main');
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
      router.push('/personal/main');
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
  router.push('/login');
};
</script>

<style scoped>
.header {
  background: #fff;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  position: sticky;
  top: 0;
  z-index: 1000;
  height: 70px;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  height: 100%;
}

.header-content {
  display: flex;
  align-items: center;
  height: 100%;
  gap: 32px;
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
  gap: 12px;
}

.logo-icon {
  width: 42px;
  height: 42px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 22px;
  font-weight: 700;
}

.logo-text {
  display: flex;
  flex-direction: column;
}

.logo-name {
  font-size: 18px;
  font-weight: 700;
  color: #303133;
  line-height: 1.2;
}

.logo-slogan {
  font-size: 10px;
  color: #909399;
  letter-spacing: 1px;
}

/* 中间搜索框 */
.header-center {
  flex: 1;
  max-width: 520px;
}

.search-box {
  width: 100%;
}

.search-box :deep(.el-input__wrapper) {
  border-radius: 8px 0 0 8px;
  box-shadow: 0 0 0 1px #dcdfe6 inset;
}

.search-box :deep(.el-input-group__append) {
  border-radius: 0 8px 8px 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-color: transparent;
}

.search-box :deep(.el-input-group__append .el-button) {
  color: #fff;
  border: none;
}

.search-icon {
  color: #909399;
}

/* 右侧功能区 */
.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
  flex-shrink: 0;
}

.nav-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  padding: 8px 12px;
  border-radius: 8px;
  transition: all 0.2s;
  color: #606266;
}

.nav-item:hover {
  background: #f5f7fa;
  color: #409eff;
}

.nav-text {
  font-size: 12px;
  white-space: nowrap;
}

.badge :deep(.el-badge__content) {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.divider {
  width: 1px;
  height: 24px;
  background: #e4e7ed;
  margin: 0 4px;
}

/* 用户信息 */
.user-section {
  display: flex;
  align-items: center;
}

.user-trigger {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 6px 12px;
  border-radius: 8px;
  transition: all 0.2s;
}

.user-trigger:hover {
  background: #f5f7fa;
}

.username {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.arrow-icon {
  font-size: 12px;
  color: #909399;
  transition: transform 0.2s;
}

/* 登录按钮 */
.login-section {
  display: flex;
  gap: 8px;
}

.login-section .el-button--primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-color: transparent;
}

/* 下拉菜单样式 */
:deep(.el-dropdown-menu__item) {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
}

:deep(.el-dropdown-menu__item .el-icon) {
  font-size: 16px;
}

/* 响应式 */
@media (max-width: 768px) {
  .header-content {
    gap: 16px;
  }

  .header-center {
    max-width: 300px;
  }

  .nav-item {
    padding: 6px 8px;
  }

  .nav-text {
    display: none;
  }

  .username {
    display: none;
  }
}
</style>
