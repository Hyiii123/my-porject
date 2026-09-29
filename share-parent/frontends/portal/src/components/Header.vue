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
  z-index: 1000;
  background: rgba(7, 9, 14, 0.85);
  backdrop-filter: blur(24px) saturate(180%);
  -webkit-backdrop-filter: blur(24px) saturate(180%);
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: 0 16px 36px -10px rgba(0, 0, 0, 0.7), inset 0 -1px 0 rgba(255, 255, 255, 0.04);
  width: 100%;
  transition: background 0.3s ease, border-color 0.3s ease;
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
  background: linear-gradient(90deg, transparent 0%, rgba(56, 189, 248, 0.8) 50%, transparent 100%);
  opacity: 0.65;
  animation: scan 4.5s linear infinite;
  pointer-events: none;
}

.home-wrap {
  max-width: 1320px;
  margin: 0 auto;
  padding: 0 28px;
}

.home-nav-inner {
  display: flex;
  align-items: center;
  gap: 32px;
  height: 70px;
}

.home-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
  text-decoration: none;
  transition: transform 0.25s cubic-bezier(0.16, 1, 0.3, 1);

  &:hover {
    transform: scale(1.02);
  }
}

.home-brand-icon {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: linear-gradient(135deg, rgba(56, 189, 248, 0.2) 0%, rgba(99, 102, 241, 0.2) 100%);
  border: 1px solid rgba(56, 189, 248, 0.45);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.3), 0 0 18px -2px rgba(56, 189, 248, 0.35);
  font-family: var(--display);
  font-weight: 900;
  font-size: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #38BDF8;
}

.home-brand-text {
  display: flex;
  flex-direction: column;
  .name {
    font-family: var(--display);
    font-weight: 800;
    font-size: 19px;
    letter-spacing: -0.02em;
    background: linear-gradient(135deg, #FFFFFF 0%, #E2E8F0 60%, #94A3B8 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    line-height: 1.15;
    white-space: nowrap;
  }
  .sub {
    font-family: var(--mono);
    font-size: 9.5px;
    color: #38BDF8;
    letter-spacing: 0.12em;
    margin-top: 3px;
    font-weight: 600;
  }
}

.home-menu {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;

  a {
    font-size: 14px;
    font-weight: 500;
    color: #94A3B8;
    text-decoration: none;
    position: relative;
    padding: 7px 16px;
    border-radius: 999px;
    transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
    letter-spacing: -0.01em;

    &:hover {
      color: #FFFFFF;
      background: rgba(255, 255, 255, 0.06);
    }

    &.active {
      color: #38BDF8;
      background: rgba(56, 189, 248, 0.12);
      border: 1px solid rgba(56, 189, 248, 0.25);
      font-weight: 600;
      box-shadow: 0 0 16px -4px rgba(56, 189, 248, 0.25);
    }
  }
}

.home-nav-search {
  width: 220px;
  .nav-search-input {
    :deep(.el-input__wrapper) {
      background: rgba(15, 23, 42, 0.65) !important;
      border-radius: 999px !important;
      border: 1px solid rgba(255, 255, 255, 0.1) !important;
      box-shadow: inset 0 1px 2px rgba(0, 0, 0, 0.4) !important;
      padding: 0 12px !important;
      transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1) !important;

      &:hover {
        background: rgba(15, 23, 42, 0.85) !important;
        border-color: rgba(56, 189, 248, 0.4) !important;
      }
      &.is-focus {
        border-color: #38BDF8 !important;
        box-shadow: 0 0 0 2px rgba(56, 189, 248, 0.25), inset 0 1px 2px rgba(0, 0, 0, 0.3) !important;
      }
    }
    :deep(.el-input__inner) {
      color: #F8FAFC !important;
      font-size: 13px !important;
      &::placeholder {
        color: #64748B !important;
      }
    }
    :deep(.el-icon) {
      color: #94A3B8 !important;
    }
  }
}

.home-nav-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.nav-icon-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border-radius: 10px;
  color: #CBD5E1;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.08);
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);

  &:hover {
    background: rgba(255, 255, 255, 0.12);
    border-color: rgba(56, 189, 248, 0.35);
    color: #38BDF8;
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
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
  width: 38px;
  height: 38px;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(56, 189, 248, 0.3) 0%, rgba(99, 102, 241, 0.3) 100%);
  border: 2px solid rgba(56, 189, 248, 0.5);
  box-shadow: 0 0 12px rgba(56, 189, 248, 0.35);
  color: #fff;
  font-weight: 700;
  font-size: 15px;
  overflow: hidden;
  transition: all 0.25s ease;

  &:hover {
    border-color: #38BDF8;
    box-shadow: 0 0 18px rgba(56, 189, 248, 0.6);
    transform: scale(1.05);
  }

  .avatar-img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.home-nav-auth {
  display: flex;
  align-items: center;
  gap: 10px;

  .home-btn-login {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    padding: 7px 18px;
    border-radius: 999px;
    background: linear-gradient(135deg, #0284C7 0%, #2563EB 100%);
    border: 1px solid rgba(56, 189, 248, 0.4);
    color: #fff;
    font-size: 13.5px;
    font-weight: 600;
    text-decoration: none;
    box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.3), 0 4px 16px -2px rgba(37, 99, 235, 0.5);
    transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);

    &:hover {
      background: linear-gradient(135deg, #0369A1 0%, #1D4ED8 100%);
      box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.45), 0 8px 24px -2px rgba(56, 189, 248, 0.5);
      transform: translateY(-2px);
    }
  }
}

.home-dropdown-menu {
  min-width: 220px;
}
.home-user-dropdown-head {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
}
.home-user-avatar-lg {
  width: 44px;
  height: 44px;
  font-size: 18px;
}
.home-user-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  .home-user-name {
    font-weight: 600;
    font-size: 14.5px;
    color: #FFFFFF;
  }
  .home-user-role {
    font-size: 11px;
    color: #38BDF8;
    background: rgba(56, 189, 248, 0.15);
    padding: 2px 8px;
    border-radius: 4px;
    width: fit-content;
    font-family: var(--mono);
  }
}
.home-user-dropdown-divider {
  height: 1px;
  background: rgba(255, 255, 255, 0.08);
  margin: 6px 0;
}
.logout-item {
  color: #FB7185 !important;
  &:hover {
    background: rgba(244, 63, 94, 0.15) !important;
    color: #F43F5E !important;
  }
}
</style>