<template>
  <div class="login-container">
    <!-- 径向呼吸发光体与浮动光斑粒子 -->
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
        <div class="logo-box">
          <span class="logo-text">智</span>
        </div>
        <h1 class="title">智问学伴 · <span class="accent">业务管理后台</span></h1>
        <p class="subtitle">高校产学研协同教育工程 · 统一权限管理系统</p>
      </div>

      <el-form
        ref="loginFormRef"
        :model="loginForm"
        :rules="loginRules"
        class="login-form"
        size="large"
      >
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="请输入管理员用户名/手机号"
            :prefix-icon="User"
            class="iaic-input"
            clearable
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            :prefix-icon="Lock"
            show-password
            class="iaic-input"
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <div class="form-options">
          <el-checkbox v-model="loginForm.remember">记住密码 (7天)</el-checkbox>
          <a class="forgot-link" @click="handleForgot">忘记密码？</a>
        </div>

        <el-form-item>
          <button
            type="button"
            class="iaic-login-btn"
            :disabled="loading"
            @click="handleLogin"
          >
            <span v-if="!loading">安全登录 ➔</span>
            <span v-else>正在验证管理员身份...</span>
          </button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 底部版权信息 -->
    <div class="footer">
      <p>© 2026 智问学伴在线教育系统 · 业务管理后台 (Enterprise Edition)</p>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { userLogins, getUserInfo } from '@/api/user'
import { useUserStore } from '@/store'

const router = useRouter()
const route = useRoute()
const loginFormRef = ref(null)
const loading = ref(false)
const userStore = useUserStore()

const loginForm = reactive({
  username: 'admin',
  password: 'admin123',
  remember: true
})

const loginRules = {
  username: [
    { required: true, message: '请输入管理员账号', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ]
}

const handleLogin = async () => {
  const formEl = loginFormRef.value
  if (!formEl) return

  await formEl.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    try {
      const response = await userLogins({
        username: loginForm.username.trim(),
        password: loginForm.password,
        rememberMe: loginForm.remember,
      })
      if (response.code !== 200 || !response.data) {
        throw new Error(response.msg || response.message || '用户名或密码错误')
      }
      const token = response.data?.access_token || response.data?.token || response.data
      await userStore.setToken(token)
      sessionStorage.setItem('token', token)

      const userResponse = await getUserInfo()
      if (userResponse.code === 200 && userResponse.data) {
        await userStore.setUserInfo(userResponse.data)
        sessionStorage.setItem('userInfo', JSON.stringify(userResponse.data))
      }
      ElMessage.success('登录成功！欢迎使用业务管理后台')
      const rawRedirect = route.query.redirect ? decodeURIComponent(route.query.redirect) : '/main/index'
      const target = (rawRedirect === '/login' || rawRedirect.startsWith('/login')) ? '/main/index' : rawRedirect
      router.push(target)
    } catch (error) {
      ElMessage.error(error.message || '登录失败，请检查账号密码')
    } finally {
      loading.value = false
    }
  })
}

const handleForgot = () => {
  ElMessage.info('请联系系统管理员或运维人员协助重置密码')
}
</script>

<style scoped lang="scss">
@keyframes scan {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(100%); }
}

@keyframes pulseGlow {
  0%, 100% { opacity: 0.35; transform: translate(-50%, -50%) scale(1); }
  50% { opacity: 0.55; transform: translate(-50%, -50%) scale(1.08); }
}

@keyframes floatUp {
  0% { transform: translateY(0) scale(0.8); opacity: 0; }
  15% { opacity: 0.8; }
  85% { opacity: 0.6; }
  100% { transform: translateY(-500px) scale(1.2); opacity: 0; }
}

@keyframes shine {
  0% { left: -75%; }
  100% { left: 125%; }
}

.login-container {
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
  font-family: var(--cn);

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

    &:nth-child(1) { left: 12%; width: 5px; height: 5px; animation-duration: 11s; animation-delay: 0s; }
    &:nth-child(2) { left: 25%; width: 6px; height: 6px; animation-duration: 14s; animation-delay: 2s; }
    &:nth-child(3) { left: 40%; width: 4px; height: 4px; animation-duration: 12s; animation-delay: 4s; }
    &:nth-child(4) { left: 55%; width: 6px; height: 6px; animation-duration: 16s; animation-delay: 1s; }
    &:nth-child(5) { left: 70%; width: 4px; height: 4px; animation-duration: 10s; animation-delay: 3s; }
    &:nth-child(6) { left: 85%; width: 5px; height: 5px; animation-duration: 13s; animation-delay: 5s; }
    &:nth-child(7) { left: 92%; width: 6px; height: 6px; animation-duration: 15s; animation-delay: 2s; }
    &:nth-child(8) { left: 48%; width: 4px; height: 4px; animation-duration: 9s; animation-delay: 6s; }
  }
}

.login-card {
  position: relative;
  width: 440px;
  max-width: 100%;
  background: #FFFFFF;
  border-radius: 20px;
  border: 1px solid var(--line);
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

.logo-box {
  width: 52px;
  height: 52px;
  background: var(--grad);
  border-radius: 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 14px;
  box-shadow: 0 4px 14px rgba(33, 198, 232, 0.4);
}

.logo-text {
  color: #ffffff;
  font-size: 26px;
  font-weight: 900;
  font-family: var(--display);
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

.login-form {
  margin-top: 20px;
}

.iaic-input {
  :deep(.el-input__wrapper) {
    border-radius: 10px;
    border: 1px solid var(--line);
    background: var(--sky);
    box-shadow: none;
    transition: all 0.2s ease;

    &:hover, &.is-focus {
      background: #fff;
      border-color: var(--azure);
      box-shadow: 0 0 0 1px var(--azure);
    }
  }
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 22px;
  font-size: 13px;
}

.forgot-link {
  color: var(--azure);
  cursor: pointer;
  font-size: 13px;
  transition: color 0.2s;

  &:hover {
    color: var(--blue-deep);
  }
}

.iaic-login-btn {
  width: 100%;
  height: 44px;
  border-radius: 22px;
  font-size: 15px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(90deg, #38b6ff, #2a8fff);
  border: none;
  cursor: pointer;
  box-shadow: 0 6px 18px rgba(42, 143, 255, 0.4);
  position: relative;
  overflow: hidden;
  transition: all 0.25s ease;

  &:after {
    content: "";
    position: absolute;
    top: 0; left: -75%;
    width: 50%; height: 100%;
    background: linear-gradient(120deg, transparent, rgba(255, 255, 255, 0.55), transparent);
    transform: skew(-20deg);
    animation: shine 3s infinite;
  }

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 10px 22px rgba(42, 143, 255, 0.55);
  }

  &:disabled {
    cursor: not-allowed;
    transform: none;
    background: linear-gradient(90deg, #8b98aa, #6f7d91);
    box-shadow: none;
    &:after { display: none; }
  }
}

.footer {
  margin-top: 32px;
  color: var(--slate-2);
  font-size: 12px;
  text-align: center;
  font-family: var(--mono);
  position: relative;
  z-index: 2;
}

@media (max-width: 480px) {
  .login-card {
    padding: 28px 20px;
  }
}
</style>
