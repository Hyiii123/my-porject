<template>
  <header class="home-nav">
    <!-- 顶部动态扫描光线 (独立轨道裁剪溢出，彻底消除横向滚动条晃动) -->
    <div class="home-nav-scan-track">
      <div class="home-nav-scan"></div>
    </div>

    <div class="home-wrap home-nav-inner">
      <!-- 品牌标识 -->
      <router-link to="/" class="home-brand">
        <div class="home-brand-icon">智</div>
        <div class="home-brand-text">
          <span class="name">智问学伴</span>
          <span class="sub">AI-POWERED EDUCATION</span>
        </div>
      </router-link>

      <!-- 导航主菜单 -->
      <nav class="home-menu">
        <router-link to="/main/index" :class="{ active: route.path === '/main/index' || route.path === '/' }">
          首页
        </router-link>
        <router-link to="/classList/index" :class="{ active: route.path.startsWith('/classList') }">
          课程中心
        </router-link>
        <router-link to="/ask/index" :class="{ active: route.path.startsWith('/ask') }">
          问答社区
        </router-link>
        <router-link :to="{ name: 'interviewIndex' }" :class="{ active: route.path.startsWith('/interview') }">
          全真考场
        </router-link>
        <router-link :to="{ name: 'customerServiceIndex' }" :class="{ active: route.path.startsWith('/customerService') }">
          智能客服
        </router-link>
      </nav>

      <!-- 磨砂微型搜索框 -->
      <div class="home-nav-search">
        <el-input
          v-model="input"
          placeholder="搜索课程、技术栈..."
          size="small"
          class="nav-search-input"
          @keyup.enter="SearchHandle"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </div>

      <!-- 右侧操作区 -->
      <div class="home-nav-actions">
        <!-- 购物车 -->
        <div class="nav-icon-btn" @click="$router.push('/pay/carts')" title="购物车">
          <el-badge :value="cartCount" :hidden="cartCount === 0" class="badge">
            <el-icon :size="18"><ShoppingCart /></el-icon>
          </el-badge>
        </div>

        <!-- 我的学习 -->
        <div class="nav-icon-btn" @click="$router.push('/my-class/index')" title="我的学习">
          <el-icon :size="18"><Reading /></el-icon>
        </div>

        <!-- 用户头像 / 登录按钮 -->
        <div v-if="isLoggedIn" class="home-user-menu">
          <el-dropdown trigger="click" @command="handleCommand">
            <button class="home-user-avatar-btn">
              <div class="home-user-avatar">
                <img
                  v-if="userInfo.avatar && userInfo.avatar !== defaultAvatar"
                  :src="formatAvatarUrl(userInfo.avatar)"
                  alt="avatar"
                  class="avatar-img"
                  @error="handleAvatarError"
                />
                <span v-else>{{ (userInfo.nickname || userInfo.name || '智')[0] }}</span>
              </div>
            </button>
            <template #dropdown>
              <el-dropdown-menu class="home-dropdown-menu">
                <div class="home-user-dropdown-head">
                  <div class="home-user-avatar home-user-avatar-lg">
                    <img
                      v-if="userInfo.avatar && userInfo.avatar !== defaultAvatar"
                      :src="formatAvatarUrl(userInfo.avatar)"
                      alt="avatar"
                      class="avatar-img"
                      @error="handleAvatarError"
                    />
                    <span v-else>{{ (userInfo.nickname || userInfo.name || '智')[0] }}</span>
                  </div>
                  <div class="home-user-info">
                    <div class="home-user-name">{{ userInfo.nickname || userInfo.name || '学习者' }}</div>
                    <div class="home-user-role">学员认证</div>
                  </div>
                </div>
                <div class="home-user-dropdown-divider"></div>
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
                <div class="home-user-dropdown-divider"></div>
                <el-dropdown-item command="logout" class="logout-item">
                  <el-icon><SwitchButton /></el-icon>
                  <span>退出登录</span>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>

        <div v-else class="home-nav-auth">
          <router-link to="/login" class="home-btn-login">登录 / 注册</router-link>
        </div>
      </div>
    </div>
  </header>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from "vue";
import {
  Search, ShoppingCart, Reading, User, Edit, Star,
  Collection, SwitchButton
} from "@element-plus/icons-vue";
import { useRouter, useRoute } from "vue-router";
import { ElMessage } from "element-plus";

import { getUserInfo } from "@/api/user.js";
import { getCarts } from "@/api/order.js";
import defaultAvatar from "@/assets/images/users/default-avatar.svg?url";

const router = useRouter();
const route = useRoute();

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

const handleAvatarError = () => {
  userInfo.value.avatar = defaultAvatar;
};

const cartCount = ref(0);

const updateCartCount = async () => {
  const token = sessionStorage.getItem('token');
  if (!token) {
    cartCount.value = 0;
    return;
  }
  try {
    const res = await getCarts();
    if (res && res.code === 200) {
      const rows = Array.isArray(res.data) ? res.data : (res.data?.list || []);
      cartCount.value = rows.length;
    } else {
      cartCount.value = 0;
    }
  } catch (e) {
    cartCount.value = 0;
  }
};

const input = ref('');

const checkLoginStatus = async () => {
  const token = sessionStorage.getItem('token');
  isLoggedIn.value = !!token;

  if (isLoggedIn.value) {
    updateCartCount();
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
        userInfo.value = { ...userInfo.value, ...res.data };
        sessionStorage.setItem('userInfo', JSON.stringify(userInfo.value));
      }
    } catch (e) {
      console.error('Failed to fetch user info:', e);
    }
  } else {
    cartCount.value = 0;
  }
};

const handleCommand = (command) => {
  switch (command) {
    case 'personal':
      router.push('/personal/main');
      break;
    case 'myClass':
      router.push('/my-class/index');
      break;
    case 'notes':
      router.push('/personal/notes');
      break;
    case 'points':
      router.push('/personal/myIntegral');
      break;
    case 'orders':
      router.push('/personal/myOrder');
      break;
    case 'logout':
      sessionStorage.clear();
      isLoggedIn.value = false;
      cartCount.value = 0;
      ElMessage.success('已安全退出登录');
      router.push('/');
      break;
  }
};

const SearchHandle = () => {
  if (input.value.trim() === '') {
    router.push({ path: '/classList/index' });
  } else {
    router.push({
      path: '/classList/index',
      query: { keyword: input.value.trim() }
    });
  }
};

watch(() => route.path, () => {
  checkLoginStatus();
});

const handleCartUpdated = (e) => {
  if (e?.detail != null && typeof e.detail === 'number') {
    cartCount.value = e.detail;
  } else {
    updateCartCount();
  }
};

onMounted(() => {
  checkLoginStatus();
  updateCartCount();
  window.addEventListener('user-profile-updated', checkLoginStatus);
  window.addEventListener('cart-updated', handleCartUpdated);
});

onUnmounted(() => {
  window.removeEventListener('user-profile-updated', checkLoginStatus);
  window.removeEventListener('cart-updated', handleCartUpdated);
});
</script>

<style lang="scss" scoped>
.home-nav {
  position: sticky;
  top: 0;
  z-index: 100;
  background: var(--azure);
  box-shadow: 0 6px 24px -14px rgba(27, 98, 214, 0.6);
  width: 100%;
}

.home-nav-scan-track {
  position: absolute;
  left: 0;
  bottom: 0;
  width: 100%;
  height: 2px;
  overflow: hidden;
  pointer-events: none;
}

.home-nav-scan {
  position: absolute;
  left: 0;
  top: 0;
  height: 100%;
  width: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.9), transparent);
  opacity: 0.55;
  animation: scan 5s linear infinite;
  pointer-events: none;
}

.home-wrap {
  max-width: 1280px;
  margin: 0 auto;
  padding: 0 28px;
}

.home-nav-inner {
  display: flex;
  align-items: center;
  gap: 28px;
  height: 68px;
}

.home-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
  text-decoration: none;
}

.home-brand-icon {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  background: #fff;
  color: var(--azure);
  font-family: var(--display);
  font-weight: 900;
  font-size: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
}

.home-brand-text {
  display: flex;
  flex-direction: column;
  .name {
    font-family: var(--display);
    font-weight: 800;
    font-size: 18px;
    letter-spacing: 0.05em;
    color: #fff;
    line-height: 1.1;
    white-space: nowrap;
  }
  .sub {
    font-family: var(--mono);
    font-size: 10px;
    color: rgba(255, 255, 255, 0.75);
    letter-spacing: 0.08em;
    margin-top: 3px;
  }
}

.home-menu {
  display: flex;
  align-items: center;
  gap: 24px;
  flex: 1;

  a {
    font-size: 14.5px;
    font-weight: 500;
    color: rgba(255, 255, 255, 0.92);
    text-decoration: none;
    position: relative;
    padding: 6px 0;
    transition: color 0.2s;

    &:after {
      content: "";
      position: absolute;
      left: 0;
      bottom: -2px;
      height: 2px;
      width: 0;
      background: #fff;
      transition: width 0.25s ease;
      border-radius: 2px;
    }

    &:hover, &.active {
      color: #fff;
      &:after {
        width: 100%;
      }
    }

    &.active {
      font-weight: 700;
    }
  }
}

.home-nav-search {
  width: 200px;
  .nav-search-input {
    :deep(.el-input__wrapper) {
      background: rgba(255, 255, 255, 0.2);
      border-radius: 20px;
      border: 1px solid rgba(255, 255, 255, 0.35);
      box-shadow: none;
      color: #fff;
      padding-left: 10px;

      &:hover, &.is-focus {
        background: rgba(255, 255, 255, 0.3);
        border-color: #fff;
      }
    }
    :deep(.el-input__inner) {
      color: #fff;
      font-size: 12.5px;
      &::placeholder {
        color: rgba(255, 255, 255, 0.7);
      }
    }
    :deep(.el-icon) {
      color: rgba(255, 255, 255, 0.85);
    }
  }
}

.home-nav-actions {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-shrink: 0;
}

.nav-icon-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  color: #fff;
  background: rgba(255, 255, 255, 0.15);
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: rgba(255, 255, 255, 0.28);
    transform: translateY(-1px);
  }
}

.home-user-menu {
  position: relative;
}

.home-user-avatar-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: none;
  padding: 0;
  cursor: pointer;
}

.home-user-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  background: var(--grad);
  border: 2px solid rgba(255, 255, 255, 0.7);
  overflow: hidden;

  .avatar-img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.home-user-avatar-lg {
  width: 42px;
  height: 42px;
  font-size: 16px;
  flex-shrink: 0;
}

.home-user-dropdown-head {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
}

.home-user-info {
  min-width: 0;
  .home-user-name {
    font-size: 14px;
    font-weight: 700;
    color: var(--ink);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  .home-user-role {
    font-size: 11px;
    color: var(--slate);
    margin-top: 2px;
  }
}

.home-user-dropdown-divider {
  height: 1px;
  background: var(--line);
  margin: 4px 0;
}

.home-btn-login {
  font-size: 13.5px;
  font-weight: 600;
  color: #fff;
  border: 1px solid rgba(255, 255, 255, 0.6);
  background: rgba(255, 255, 255, 0.18);
  padding: 8px 18px;
  border-radius: 20px;
  text-decoration: none;
  transition: all 0.2s;
  white-space: nowrap;

  &:hover {
    background: #fff;
    color: var(--azure);
    border-color: #fff;
  }
}

@media (max-width: 1024px) {
  .home-menu {
    gap: 14px;
  }
  .home-nav-search {
    width: 150px;
  }
}
</style>
