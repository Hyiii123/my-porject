<template>
  <div class="login-container">
    <!-- 登录卡片 -->
    <div class="login-card">
      <div class="login-header">
        <div class="logo-box">
          <span class="logo-text">智</span>
        </div>
        <h1 class="title">智问学伴</h1>
        <p class="subtitle">在线教育平台 · 业务管理后台</p>
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
            placeholder="请输入用户名/手机号"
            :prefix-icon="User"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            :prefix-icon="Lock"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <div class="form-options">
          <el-checkbox v-model="loginForm.remember">记住密码</el-checkbox>
          <a class="forgot-link" @click="handleForgot">忘记密码？</a>
        </div>

        <el-form-item>
          <el-button
            type="primary"
            class="login-btn"
            :loading="loading"
            @click="handleLogin"
          >
            登 录
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 底部信息 -->
    <div class="footer">
      <p>Copyright © 2024 智问学伴在线教育平台 · 业务管理系统</p>
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
    { required: true, message: '请输入用户名', trigger: 'blur' }
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
      const userResponse = await getUserInfo()
      if (userResponse.code === 200 && userResponse.data) {
        await userStore.setUserInfo(userResponse.data)
      }
      ElMessage.success('登录成功！')
      const redirect = route.query.redirect || '/main/index'
      router.push(redirect)
    } catch (error) {
      ElMessage.error('登录失败，请重试')
    } finally {
      loading.value = false
    }
  })
}

const handleForgot = () => {
  ElMessage.info('请联系管理员重置密码')
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background-color: #0f172a;
  background-image: radial-gradient(#334155 1px, transparent 1px);
  background-size: 24px 24px;
  position: relative;
  overflow: hidden;
  padding: 20px;
  box-sizing: border-box;
}

.login-card {
  width: 420px;
  max-width: 100%;
  background: #ffffff;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  padding: 40px;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.25), 0 8px 10px -6px rgba(0, 0, 0, 0.2);
  z-index: 1;
  box-sizing: border-box;
}

.login-header {
  text-align: center;
  margin-bottom: 28px;
}

.logo-box {
  width: 44px;
  height: 44px;
  background: #2563eb;
  border-radius: 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
  box-shadow: 0 4px 10px rgba(37, 99, 235, 0.25);
}

.logo-text {
  color: #ffffff;
  font-size: 22px;
  font-weight: 700;
}

.title {
  font-size: 22px;
  font-weight: 700;
  color: #0f172a;
  margin: 0 0 6px 0;
  letter-spacing: -0.01em;
}

.subtitle {
  font-size: 13px;
  color: #64748b;
  margin: 0;
}

.login-form {
  margin-top: 20px;
}

.login-form :deep(.el-input__wrapper) {
  border-radius: 6px;
  box-shadow: 0 0 0 1px #cbd5e1 inset;
  padding: 4px 14px;
  transition: all 0.2s;
}

.login-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #94a3b8 inset;
}

.login-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1.5px #2563eb inset;
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  font-size: 13px;
}

.forgot-link {
  color: #2563eb;
  cursor: pointer;
  font-size: 13px;
  transition: color 0.2s;
}

.forgot-link:hover {
  color: #1d4ed8;
}

.login-btn {
  width: 100%;
  height: 44px;
  background-color: #2563eb;
  border-color: #2563eb;
  border-radius: 6px;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 2px;
  transition: all 0.2s;
}

.login-btn:hover {
  background-color: #1d4ed8;
  border-color: #1d4ed8;
}

.footer {
  margin-top: 32px;
  color: #64748b;
  font-size: 12px;
  text-align: center;
}

@media (max-width: 480px) {
  .login-card {
    padding: 28px 20px;
  }
}
</style>
