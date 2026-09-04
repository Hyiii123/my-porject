<!-- 登录页面 -->
<template>
  <div class="login-container">
    <!-- 背景装饰 -->
    <div class="bg-decoration">
      <div class="circle circle-1"></div>
      <div class="circle circle-2"></div>
      <div class="circle circle-3"></div>
    </div>

    <!-- 登录卡片 -->
    <div class="login-card">
      <div class="login-header">
        <div class="login-logo">
          <div class="login-logo-icon">智</div>
        </div>
        <h1 class="title">智问学伴</h1>
        <p class="subtitle">在线教育平台</p>
      </div>

      <div class="login-tabs">
        <span
          :class="['tab-item', { active: act === 'pass' }]"
          @click="changeLoginType('pass')"
        >
          账号登录
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

    <!-- 底部信息 -->
    <div class="footer">
      <p>Copyright © 2026 智问学伴在线教育平台</p>
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
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  position: relative;
  overflow: hidden;
}

.bg-decoration {
  position: absolute;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.circle {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
}

.circle-1 {
  width: 300px;
  height: 300px;
  top: -100px;
  left: -100px;
  animation: float 6s ease-in-out infinite;
}

.circle-2 {
  width: 200px;
  height: 200px;
  bottom: -50px;
  right: -50px;
  animation: float 8s ease-in-out infinite reverse;
}

.circle-3 {
  width: 150px;
  height: 150px;
  top: 50%;
  right: 10%;
  animation: float 10s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-20px); }
}

.login-card {
  width: 420px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 20px;
  padding: 40px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  backdrop-filter: blur(10px);
  z-index: 1;
}

.login-header {
  text-align: center;
  margin-bottom: 30px;
}

.login-logo {
  display: inline-flex;
  margin-bottom: 16px;
}

.login-logo-icon {
  width: 80px;
  height: 80px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 40px;
  font-weight: 700;
  box-shadow: 0 8px 24px rgba(102, 126, 234, 0.4);
}

.title {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
  margin: 0 0 8px 0;
}

.subtitle {
  font-size: 14px;
  color: #909399;
  margin: 0;
}

.login-tabs {
  display: flex;
  justify-content: center;
  gap: 40px;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #ebeef5;
}

.tab-item {
  font-size: 16px;
  color: #606266;
  cursor: pointer;
  padding-bottom: 8px;
  border-bottom: 2px solid transparent;
  transition: all 0.2s;
}

.tab-item:hover {
  color: #409eff;
}

.tab-item.active {
  color: #409eff;
  border-bottom-color: #409eff;
  font-weight: 600;
}

.footer {
  position: absolute;
  bottom: 20px;
  color: rgba(255, 255, 255, 0.8);
  font-size: 13px;
}

/* 响应式 */
@media (max-width: 480px) {
  .login-card {
    width: 90%;
    padding: 30px 20px;
    margin: 20px;
  }
}
</style>
