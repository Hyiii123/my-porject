<!-- 登录页面 - 1:1 逆向 IAIC 科技蓝视觉规范 -->
<template>
  <div class="login-page">
    <!-- 径向发光体与浮动粒子背景 -->
    <div class="login-glow"></div>
    <div class="login-particles">
      <span></span><span></span><span></span><span></span><span></span>
      <span></span><span></span><span></span>
    </div>

    <!-- 登录主卡片 -->
    <div class="login-card">
      <div class="corner c1"></div>
      <div class="corner c2"></div>

      <div class="login-header">
        <div class="login-logo">
          <div class="login-logo-icon">智</div>
        </div>
        <h1 class="title">智问学伴 · <span class="accent">学习者登录</span></h1>
        <p class="subtitle">AI 产学研协同导学平台 · 持续赋能每一位求知者</p>
      </div>

      <!-- IAIC 胶囊切换器 -->
      <div class="login-tabs">
        <button
          :class="['tab-pill', { active: act === 'pass' }]"
          @click="changeLoginType('pass')"
        >
          账号密码登录
        </button>
        <button
          :class="['tab-pill', { active: act === 'phone' || act === 'email' }]"
          @click="changeLoginType('email')"
        >
          QQ邮箱验证码
        </button>
      </div>

      <!-- 用户名密码登录 -->
      <LoginPass v-if="act === 'pass'" @goHandle="goHandle"></LoginPass>

      <!-- 邮箱验证码登录 -->
      <LoginPhone v-if="act === 'phone' || act === 'email'" @goHandle="goHandle"></LoginPhone>

      <!-- 注册 -->
      <Register v-if="act === 'register'" @goHandle="goHandle"></Register>
    </div>

    <!-- 底部版权信息 -->
    <div class="login-footer">
      <p>© 2026 智问学伴 在线教育系统 · 产学研协同教育工程</p>
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
const goHandle = (val) => {
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
.login-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  padding: 32px 16px;
  background: radial-gradient(60% 80% at 50% 0%, rgba(33, 198, 232, 0.14), transparent 60%),
              radial-gradient(50% 70% at 5% 100%, rgba(43, 134, 240, 0.12), transparent 60%),
              linear-gradient(135deg, #eaf4ff, #d6eaff 55%, #c8e0ff);

  &:before {
    content: "";
    position: absolute;
    top: 0; right: 0; bottom: 0; left: 0;
    background-image: linear-gradient(rgba(42, 143, 255, 0.07) 1px, transparent 1px),
                      linear-gradient(90deg, rgba(42, 143, 255, 0.07) 1px, transparent 1px);
    background-size: 44px 44px;
    mask-image: radial-gradient(ellipse at center, #000 35%, transparent 85%);
    -webkit-mask-image: radial-gradient(ellipse at center, #000 35%, transparent 85%);
    pointer-events: none;
  }
}

.login-glow {
  position: absolute;
  width: 800px;
  height: 500px;
  background: radial-gradient(ellipse, rgba(56, 182, 255, 0.35), transparent 65%);
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  pointer-events: none;
  animation: pulseGlow 5s ease-in-out infinite;
}

.login-particles {
  position: absolute;
  top: 0; right: 0; bottom: 0; left: 0;
  pointer-events: none;
  overflow: hidden;

  span {
    position: absolute;
    bottom: -10px;
    background: #38b6ff;
    border-radius: 50%;
    box-shadow: 0 0 10px #38b6ff, 0 0 18px rgba(56, 182, 255, 0.6);
    animation: floatUp linear infinite;
    opacity: 0.7;

    &:nth-child(1) { left: 10%; width: 5px; height: 5px; animation-duration: 11s; animation-delay: 0s; }
    &:nth-child(2) { left: 22%; width: 6px; height: 6px; animation-duration: 14s; animation-delay: 2s; }
    &:nth-child(3) { left: 35%; width: 4px; height: 4px; animation-duration: 12s; animation-delay: 4s; }
    &:nth-child(4) { left: 50%; width: 6px; height: 6px; animation-duration: 16s; animation-delay: 1s; }
    &:nth-child(5) { left: 65%; width: 4px; height: 4px; animation-duration: 10s; animation-delay: 3s; }
    &:nth-child(6) { left: 78%; width: 5px; height: 5px; animation-duration: 13s; animation-delay: 5s; }
    &:nth-child(7) { left: 90%; width: 6px; height: 6px; animation-duration: 15s; animation-delay: 2s; }
    &:nth-child(8) { left: 45%; width: 4px; height: 4px; animation-duration: 9s; animation-delay: 6s; }
  }
}

.login-card {
  position: relative;
  width: 440px;
  max-width: 100%;
  background: #FFFFFF;
  border: 1px solid var(--line);
  border-radius: 20px;
  padding: 40px 36px 32px;
  box-shadow: var(--shadow-lg);
  z-index: 2;

  .corner {
    position: absolute;
    width: 14px;
    height: 14px;
    border: 2px solid var(--cyan);
    opacity: 0.75;
    pointer-events: none;

    &.c1 { top: 12px; left: 12px; border-right: none; border-bottom: none; }
    &.c2 { bottom: 12px; right: 12px; border-left: none; border-top: none; }
  }
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
  width: 52px;
  height: 52px;
  background: var(--grad);
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #FFFFFF;
  font-family: var(--display);
  font-size: 26px;
  font-weight: 900;
  box-shadow: 0 4px 14px rgba(33, 198, 232, 0.4);
}

.title {
  font-size: 22px;
  font-weight: 800;
  color: var(--ink);
  margin: 0 0 6px 0;
  letter-spacing: -0.2px;

  .accent {
    background: var(--grad);
    -webkit-background-clip: text;
    background-clip: text;
    color: transparent;
  }
}

.subtitle {
  font-size: 13px;
  color: var(--slate);
  margin: 0;
}

.login-tabs {
  display: flex;
  background: var(--sky-2);
  border-radius: 30px;
  padding: 4px;
  margin-bottom: 24px;
  border: 1px solid var(--line);
}

.tab-pill {
  flex: 1;
  background: transparent;
  border: none;
  padding: 8px 12px;
  font-size: 13.5px;
  font-weight: 600;
  color: var(--slate);
  border-radius: 24px;
  cursor: pointer;
  transition: all 0.25s ease;

  &:hover {
    color: var(--azure);
  }

  &.active {
    background: var(--azure);
    color: #FFFFFF;
    box-shadow: 0 4px 12px rgba(30, 137, 241, 0.35);
  }
}

.login-footer {
  margin-top: 28px;
  color: var(--slate-2);
  font-size: 12px;
  text-align: center;
  position: relative;
  z-index: 2;
  font-family: var(--mono);
}

@media (max-width: 480px) {
  .login-card {
    padding: 28px 20px;
  }
}
</style>
