<template>
  <div class="register">
    <div class="register-card">
      <div class="register-header">
        <div class="logo-box">
          <span class="logo-text">智</span>
        </div>
        <h3 class="title">在线教育管理系统</h3>
        <p class="subtitle">创建管理人员账户</p>
      </div>

      <el-form ref="registerRef" :model="registerForm" :rules="registerRules" class="register-form">
        <el-form-item prop="username">
          <el-input
            v-model="registerForm.username"
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
            v-model="registerForm.password"
            type="password"
            size="large"
            auto-complete="off"
            placeholder="密码"
            @keyup.enter="handleRegister"
          >
            <template #prefix><svg-icon icon-class="password" class="el-input__icon input-icon" /></template>
          </el-input>
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input
            v-model="registerForm.confirmPassword"
            type="password"
            size="large"
            auto-complete="off"
            placeholder="确认密码"
            @keyup.enter="handleRegister"
          >
            <template #prefix><svg-icon icon-class="password" class="el-input__icon input-icon" /></template>
          </el-input>
        </el-form-item>
        <el-form-item prop="code" v-if="captchaEnabled">
          <div class="code-wrapper">
            <el-input
              size="large"
              v-model="registerForm.code"
              auto-complete="off"
              placeholder="验证码"
              class="code-input"
              @keyup.enter="handleRegister"
            >
              <template #prefix><svg-icon icon-class="validCode" class="el-input__icon input-icon" /></template>
            </el-input>
            <div class="register-code">
              <img :src="codeUrl" @click="getCode" class="register-code-img" title="点击切换验证码"/>
            </div>
          </div>
        </el-form-item>
        <div class="form-options">
          <span></span>
          <router-link class="link-type" :to="'/login'">使用已有账户登录</router-link>
        </div>
        <el-form-item style="width:100%; margin-bottom: 0;">
          <el-button
            :loading="loading"
            size="large"
            type="primary"
            class="register-btn"
            @click.prevent="handleRegister"
          >
            <span v-if="!loading">注 册</span>
            <span v-else>注 册 中...</span>
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <!--  底部  -->
    <div class="el-register-footer">
      <span>Copyright © 2024 智问学伴在线教育平台 · 基础管理端</span>
    </div>
  </div>
</template>

<script setup>
import { ElMessageBox } from "element-plus";
import { getCodeImg, register } from "@/api/login";

const router = useRouter();
const { proxy } = getCurrentInstance();

const registerForm = ref({
  username: "",
  password: "",
  confirmPassword: "",
  code: "",
  uuid: ""
});

const equalToPassword = (rule, value, callback) => {
  if (registerForm.value.password !== value) {
    callback(new Error("两次输入的密码不一致"));
  } else {
    callback();
  }
};

const registerRules = {
  username: [
    { required: true, trigger: "blur", message: "请输入您的账号" },
    { min: 2, max: 20, message: "用户账号长度必须介于 2 和 20 之间", trigger: "blur" }
  ],
  password: [
    { required: true, trigger: "blur", message: "请输入您的密码" },
    { min: 5, max: 20, message: "用户密码长度必须介于 5 和 20 之间", trigger: "blur" }
  ],
  confirmPassword: [
    { required: true, trigger: "blur", message: "请再次输入您的密码" },
    { required: true, validator: equalToPassword, trigger: "blur" }
  ],
  code: [{ required: true, trigger: "change", message: "请输入验证码" }]
};

const codeUrl = ref("");
const loading = ref(false);
const captchaEnabled = ref(true);

function handleRegister() {
  proxy.$refs.registerRef.validate(valid => {
    if (valid) {
      loading.value = true;
      register(registerForm.value).then(res => {
        const username = registerForm.value.username;
        ElMessageBox.alert("<font color='#16a34a'>恭喜你，您的账号 " + username + " 注册成功！</font>", "系统提示", {
          dangerouslyUseHTMLString: true,
          type: "success",
        }).then(() => {
          router.push("/login");
        }).catch(() => {});
      }).catch(() => {
        loading.value = false;
        if (captchaEnabled) {
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
      registerForm.value.uuid = res.uuid;
    }
  });
}

getCode();
</script>

<style lang='scss' scoped>
.register {
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

.register-card {
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

.register-header {
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

.register-form {
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

.register-code {
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

.register-btn {
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

.el-register-footer {
  margin-top: 32px;
  color: #64748b;
  font-size: 12px;
  text-align: center;
}
</style>
