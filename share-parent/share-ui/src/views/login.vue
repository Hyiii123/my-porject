<template>
  <div class="login">
    <div class="login-card">
      <div class="login-header">
        <div class="logo-box">
          <span class="logo-text">智</span>
        </div>
        <h3 class="title">在线教育管理系统</h3>
        <p class="subtitle">基础管理中心 · 微服务集群控制台</p>
      </div>

      <el-form ref="loginRef" :model="loginForm" :rules="loginRules" class="login-form">
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            type="text"
            size="large"
            auto-complete="off"
            placeholder="账号"
          >
            <template #prefix><svg-icon icon-class="user" class="el-input__icon input-icon" /></template>
          </el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            size="large"
            auto-complete="off"
            placeholder="密码"
            @keyup.enter="handleLogin"
          >
            <template #prefix><svg-icon icon-class="password" class="el-input__icon input-icon" /></template>
          </el-input>
        </el-form-item>
        <el-form-item prop="code" v-if="captchaEnabled">
          <div class="code-wrapper">
            <el-input
              v-model="loginForm.code"
              size="large"
              auto-complete="off"
              placeholder="验证码"
              class="code-input"
              @keyup.enter="handleLogin"
            >
              <template #prefix><svg-icon icon-class="validCode" class="el-input__icon input-icon" /></template>
            </el-input>
            <div class="login-code">
              <img :src="codeUrl" @click="getCode" class="login-code-img" title="点击切换验证码"/>
            </div>
          </div>
        </el-form-item>
        <div class="form-options">
          <el-checkbox v-model="loginForm.rememberMe">记住密码</el-checkbox>
          <router-link v-if="register" class="link-type" :to="'/register'">立即注册</router-link>
        </div>
        <el-form-item style="width:100%; margin-bottom: 0;">
          <el-button
            :loading="loading"
            size="large"
            type="primary"
            class="login-btn"
            @click.prevent="handleLogin"
          >
            <span v-if="!loading">登 录</span>
            <span v-else>登 录 中...</span>
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <!--  底部  -->
    <div class="el-login-footer">
      <span>Copyright © 2024 智问学伴在线教育平台 · 基础管理端</span>
    </div>
  </div>
</template>

<script setup>
import { getCodeImg } from "@/api/login";
import Cookies from "js-cookie";
import { encrypt, decrypt } from "@/utils/jsencrypt";
import useUserStore from '@/store/modules/user'

const userStore = useUserStore()
const route = useRoute();
const router = useRouter();
const { proxy } = getCurrentInstance();

const loginForm = ref({
  username: "admin",
  password: "admin123",
  rememberMe: false,
  code: "",
  uuid: ""
});

const loginRules = {
  username: [{ required: true, trigger: "blur", message: "请输入您的账号" }],
  password: [{ required: true, trigger: "blur", message: "请输入您的密码" }],
  code: [{ required: true, trigger: "change", message: "请输入验证码" }]
};

const codeUrl = ref("");
const loading = ref(false);
// 验证码开关
const captchaEnabled = ref(true);
// 注册开关
const register = ref(false);
const redirect = ref(undefined);

watch(route, (newRoute) => {
    redirect.value = newRoute.query && newRoute.query.redirect;
}, { immediate: true });

function handleLogin() {
  proxy.$refs.loginRef.validate(valid => {
    if (valid) {
      loading.value = true;
      // 勾选了需要记住密码设置在 cookie 中设置记住用户名和密码
      if (loginForm.value.rememberMe) {
        Cookies.set("username", loginForm.value.username, { expires: 30 });
        Cookies.set("password", encrypt(loginForm.value.password), { expires: 30 });
        Cookies.set("rememberMe", loginForm.value.rememberMe, { expires: 30 });
      } else {
        // 否则移除
        Cookies.remove("username");
        Cookies.remove("password");
        Cookies.remove("rememberMe");
      }
      // 调用action的登录方法
      userStore.login(loginForm.value).then(() => {
        const query = route.query;
        const otherQueryParams = Object.keys(query).reduce((acc, cur) => {
          if (cur !== "redirect") {
            acc[cur] = query[cur];
          }
          return acc;
        }, {});
        router.push({ path: redirect.value || "/", query: otherQueryParams });
      }).catch(() => {
        loading.value = false;
        // 重新获取验证码
        if (captchaEnabled.value) {
          getCode();
        }
      });
    }
  });
}

function getCode() {
  getCodeImg().then(res => {
    captchaEnabled.value = res.captchaEnabled === undefined ? true : res.captchaEnabled;
    if (captchaEnabled.value) {
      codeUrl.value = "data:image/gif;base64," + res.img;
      loginForm.value.uuid = res.uuid;
    }
  });
}

function getCookie() {
  const username = Cookies.get("username");
  const password = Cookies.get("password");
  const rememberMe = Cookies.get("rememberMe");
  loginForm.value = {
    username: username === undefined ? loginForm.value.username : username,
    password: password === undefined ? loginForm.value.password : decrypt(password),
    rememberMe: rememberMe === undefined ? false : Boolean(rememberMe)
  };
}

getCode();
getCookie();
</script>

<style lang='scss' scoped>
.login {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background-color: #0f172a;
  background-image: radial-gradient(#334155 1px, transparent 1px);
  background-size: 24px 24px;
  position: relative;
  padding: 20px;
  box-sizing: border-box;
}

.login-card {
  width: 420px;
  max-width: 100%;
  background: #ffffff;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  padding: 36px 32px 30px;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.25), 0 8px 10px -6px rgba(0, 0, 0, 0.2);
  z-index: 1;
  box-sizing: border-box;
}

.login-header {
  text-align: center;
  margin-bottom: 24px;
}

.logo-box {
  width: 44px;
  height: 44px;
  background: #2563eb;
  border-radius: 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 14px;
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
  .el-input {
    height: 42px;
    :deep(.el-input__wrapper) {
      border-radius: 6px;
      box-shadow: 0 0 0 1px #cbd5e1 inset;
      transition: all 0.2s;
      &:hover {
        box-shadow: 0 0 0 1px #94a3b8 inset;
      }
      &.is-focus {
        box-shadow: 0 0 0 1.5px #2563eb inset;
      }
    }
  }
}

.code-wrapper {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
}

.code-input {
  flex: 1;
}

.login-code {
  width: 115px;
  height: 42px;
  border-radius: 6px;
  overflow: hidden;
  border: 1px solid #e2e8f0;
  img {
    width: 100%;
    height: 100%;
    cursor: pointer;
    vertical-align: middle;
  }
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  font-size: 13px;
  .link-type {
    color: #2563eb;
    text-decoration: none;
    &:hover {
      color: #1d4ed8;
    }
  }
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
  &:hover {
    background-color: #1d4ed8;
    border-color: #1d4ed8;
  }
}

.el-login-footer {
  margin-top: 32px;
  color: #64748b;
  font-size: 12px;
  text-align: center;
}
</style>
