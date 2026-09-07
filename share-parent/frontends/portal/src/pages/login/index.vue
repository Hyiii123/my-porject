<!-- 登录页面 -->
<template>
  <div class="login-container">
    <!-- 登录主卡片 -->
    <div class="login-card">
      <div class="login-header">
        <div class="login-logo">
          <div class="login-logo-icon">智</div>
        </div>
        <h1 class="title">智问学伴</h1>
        <p class="subtitle">让学习更高效 · 在线教育平台</p>
      </div>

      <div class="login-tabs">
        <span
          :class="['tab-item', { active: act === 'pass' }]"
          @click="changeLoginType('pass')"
        >
          密码登录
        </span>
        <span
          :class="['tab-item', { active: act === 'phone' }]"
          @click="changeLoginType('phone')"
        >
          短信登录
        </span>
      </div>

      <!-- 用户名密码登录 -->
      <LoginPass v-if="act === 'pass'" @goHandle="goHandle"></LoginPass>

      <!-- 手机号登录 -->
      <LoginPhone v-if="act === 'phone'" @goHandle="goHandle"></LoginPhone>

      <!-- 注册 -->
      <Register v-if="act === 'register'" @goHandle="goHandle"></Register>
    </div>

    <!-- 底部版权信息 -->
    <div class="footer">
      <p>智问学伴 在线教育系统 · 持续赋能每一位求知者</p>
    </div>
  </div>
</template>

<script setup>
import { ref, watchEffect } from 'vue';
import { useRoute } from 'vue-router';
import LoginPass from './components/LoginPass.vue';
import LoginPhone from './components/LoginPhone.vue';
import Register from './components/Register.vue';

const route = useRoute();

// 选中的登录方式
const act = ref('pass');

// 切换登录方式
const changeLoginType = (type) => {
  act.value = type;
};

// 去注册/去登录
const goHandle = val => {
  act.value = val;
};

// 监听路由参数
watchEffect(() => {
  if (route.query.md) {
    goHandle(route.query.md);
  }
});
</script>

<style lang="scss" scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background-color: #F8FAFC;
  background-image: radial-gradient(#E2E8F0 1px, transparent 1px);
  background-size: 24px 24px;
  position: relative;
  padding: 24px 16px;
}

.login-card {
  width: 400px;
  max-width: 100%;
  background: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 8px;
  padding: 36px 32px;
  box-shadow: 0 4px 20px -4px rgba(15, 23, 42, 0.06), 0 2px 6px -1px rgba(15, 23, 42, 0.04);
  z-index: 1;
}

.login-header {
  text-align: center;
  margin-bottom: 24px;
}

.login-logo {
  display: inline-flex;
  margin-bottom: 12px;
}

.login-logo-icon {
  width: 48px;
  height: 48px;
  background: #2563EB;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #FFFFFF;
  font-size: 22px;
  font-weight: 700;
  box-shadow: 0 2px 6px rgba(37, 99, 235, 0.25);
}

.title {
  font-size: 22px;
  font-weight: 700;
  color: #0F172A;
  margin: 0 0 6px 0;
  letter-spacing: -0.3px;
}

.subtitle {
  font-size: 13px;
  color: #64748B;
  margin: 0;
}

.login-tabs {
  display: flex;
  justify-content: center;
  gap: 32px;
  margin-bottom: 20px;
  border-bottom: 1px solid #E2E8F0;
}

.tab-item {
  font-size: 15px;
  color: #64748B;
  cursor: pointer;
  padding-bottom: 10px;
  border-bottom: 2px solid transparent;
  font-weight: 500;
  transition: all 0.2s ease;

  &:hover {
    color: #2563EB;
  }

  &.active {
    color: #2563EB;
    border-bottom-color: #2563EB;
    font-weight: 600;
  }
}

.footer {
  margin-top: 32px;
  color: #94A3B8;
  font-size: 12px;
  text-align: center;
}

@media (max-width: 480px) {
  .login-card {
    width: 100%;
    padding: 24px 20px;
  }
}
</style>
