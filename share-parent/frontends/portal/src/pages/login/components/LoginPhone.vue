<!-- 登录页面 - QQ邮箱验证码登录 (IAIC 风格) -->
<template>
  <div class="loginPhone">
    <el-form
      ref="formRef"
      :model="fromData"
      :rules="rules"
      label-width="0px"
      class="iaic-login-form"
    >
      <el-form-item prop="email">
        <el-input
          v-model="fromData.email"
          placeholder="请输入QQ邮箱（如 123456@qq.com）"
          size="large"
          class="iaic-input"
          clearable
        />
      </el-form-item>
      <el-form-item prop="code">
        <div class="code-row">
          <el-input
            v-model="fromData.code"
            placeholder="请输入6位验证码"
            maxlength="6"
            size="large"
            class="iaic-input code-input"
            clearable
            @keyup.enter="submitForm(formRef)"
          />
          <button
            type="button"
            class="iaic-btn-code"
            :disabled="codeLoading || codeCountdown > 0"
            @click="sendCode"
          >
            {{ codeCountdown > 0 ? `${codeCountdown}s 后重发` : '获取验证码' }}
          </button>
        </div>
      </el-form-item>
      <el-form-item class="marg-b-10">
        <div class="fx-sb">
          <div>
            <el-checkbox v-model="fromData.rememberMe" label="7天免登录" size="default" />
          </div>
          <div class="forgot-pass" @click="emit('goHandle', 'pass')">账号密码登录</div>
        </div>
      </el-form-item>
      <el-form-item class="marg-bt-15">
        <button
          type="button"
          class="iaic-btn-submit"
          :disabled="loading"
          @click="submitForm(formRef)"
        >
          <span v-if="!loading">立即登录 ➔</span>
          <span v-else>正在验证登录...</span>
        </button>
      </el-form-item>
    </el-form>
    <div class="font-bt text-center" @click="goRegister">
      没有账号？<span class="reg-accent">去注册新用户</span>
    </div>
  </div>
</template>

<script setup>
import { onBeforeUnmount, reactive, ref } from "vue";
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from "element-plus";
import { emailLogin, getUserInfo, verifycode } from '@/api/user';
import { useUserStore } from '@/store';

const emit = defineEmits(['goHandle']);
const router = useRouter();
const route = useRoute();
const store = useUserStore();
const loading = ref(false);
const codeLoading = ref(false);
const codeCountdown = ref(0);
let countdownTimer;

// 登录数据初始化
const formRef = ref();
const fromData = reactive({
  email: "",
  code: "",
  rememberMe: false
});

// 校验规则
const rules = reactive({
  email: [
    { required: true, message: "请输入QQ邮箱", trigger: "blur" },
    {
      validator: (rule, value, callback) => {
        if (!value) {
          callback(new Error("请输入QQ邮箱"));
        } else if (!/^[a-zA-Z0-9_-]+@qq\.com$/i.test(value.trim()) && !/^[a-zA-Z0-9_-]+@foxmail\.com$/i.test(value.trim())) {
          callback(new Error("请输入正确的QQ邮箱（如 123456@qq.com）"));
        } else {
          callback();
        }
      },
      trigger: ["blur", "change"]
    }
  ],
  code: [
    { required: true, message: "请输入邮箱验证码", trigger: "blur" },
    { min: 6, max: 6, message: "请输入6位数字验证码", trigger: "blur" }
  ]
});

// 获取验证码
const sendCode = async () => {
  if (codeLoading.value || codeCountdown.value > 0) return;
  const email = (fromData.email || '').trim();
  if (!email) {
    ElMessage.warning('请先输入QQ邮箱');
    return;
  }
  if (!/^[a-zA-Z0-9_-]+@qq\.com$/i.test(email) && !/^[a-zA-Z0-9_-]+@foxmail\.com$/i.test(email)) {
    ElMessage.warning('请输入正确的QQ邮箱（如 123456@qq.com）');
    return;
  }

  codeLoading.value = true;
  try {
    const response = await verifycode({ email: email });
    if (response.code !== 200) {
      throw new Error(response.msg || response.message || '验证码发送失败');
    }
    ElMessage.success(response.data?.message || '验证码已发送至您的QQ邮箱，请查收');
    codeCountdown.value = 60;
    countdownTimer = window.setInterval(() => {
      codeCountdown.value -= 1;
      if (codeCountdown.value <= 0) {
        window.clearInterval(countdownTimer);
        countdownTimer = undefined;
      }
    }, 1000);
  } catch (error) {
    ElMessage.error(error.message || '验证码发送失败，请稍后重试');
  } finally {
    codeLoading.value = false;
  }
};

// 数据提交
const submitForm = async (formEl) => {
  if (!formEl || loading.value) return;
  const valid = await formEl.validate().catch(() => false);
  if (!valid) return;

  loading.value = true;
  try {
    const response = await emailLogin({
      email: fromData.email.trim(),
      code: fromData.code.trim()
    });
    if (response.code !== 200 || !response.data) {
      throw new Error(response.msg || response.message || '登录失败，请检查验证码');
    }
    const token = response.data?.access_token || response.data?.token || response.data;
    await store.setToken(token);
    sessionStorage.setItem('token', token);

    try {
      const userResponse = await getUserInfo();
      if (userResponse && userResponse.code === 200 && userResponse.data) {
        await store.setUserInfo(userResponse.data);
        sessionStorage.setItem('userInfo', JSON.stringify(userResponse.data));
        window.dispatchEvent(new CustomEvent('user-profile-updated', { detail: userResponse.data }));
      }
    } catch (e) {
      console.debug('UserInfo fetch fallback:', e);
    }

    ElMessage.success('登录成功！欢迎回来');
    const rawRedirect = route.query.redirect ? decodeURIComponent(route.query.redirect) : '/main/index';
    const target = (rawRedirect === '/login' || rawRedirect.startsWith('/login')) ? '/main/index' : rawRedirect;
    await router.push(target);
  } catch (error) {
    ElMessage.error(error.message || '登录失败，请检查验证码');
  } finally {
    loading.value = false;
  }
};

// 去注册
const goRegister = () => {
  emit('goHandle', 'register');
};

onBeforeUnmount(() => {
  if (countdownTimer) window.clearInterval(countdownTimer);
});
</script>

<style lang="scss" scoped>
.loginPhone {
  margin-top: 10px;
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

.code-row {
  display: flex;
  gap: 12px;
  width: 100%;
}

.code-input {
  flex: 1;
}

.iaic-btn-code {
  width: 120px;
  height: 40px;
  border-radius: 10px;
  border: 1px solid var(--azure);
  background: var(--sky-2);
  color: var(--azure);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s;

  &:hover:not(:disabled) {
    background: var(--azure);
    color: #fff;
  }

  &:disabled {
    cursor: not-allowed;
    border-color: var(--line);
    color: var(--slate-2);
    background: var(--sky);
  }
}

.fx-sb {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.forgot-pass {
  color: var(--slate);
  font-size: 13px;
  cursor: pointer;
  transition: color 0.2s;

  &:hover {
    color: var(--azure);
  }
}

.iaic-btn-submit {
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

.font-bt {
  margin-top: 16px;
  font-size: 13px;
  color: var(--slate);
  cursor: pointer;
  text-align: center;

  .reg-accent {
    color: var(--azure);
    font-weight: 600;
    &:hover {
      text-decoration: underline;
    }
  }
}
</style>
