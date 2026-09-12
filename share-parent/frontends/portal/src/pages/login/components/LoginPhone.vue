<!-- 登录页面 - QQ邮箱验证码登录 -->
<template>
  <div class="loginPhone">
    <el-form
      ref="formRef"
      :model="fromData"
      :rules="rules"
      label-width="0px"
      class="demo-dynamic"
    >
      <el-form-item prop="email" label="">
        <el-input v-model="fromData.email" placeholder="请输入QQ邮箱（如 123456@qq.com）" clearable />
      </el-form-item>
      <el-form-item prop="code" label="">
        <div class="code-row">
          <el-input v-model="fromData.code" placeholder="请输入6位邮箱验证码" maxlength="6" clearable />
          <el-button class="code-button" :disabled="codeLoading || codeCountdown > 0" :loading="codeLoading" @click="sendCode">
            {{ codeCountdown > 0 ? `${codeCountdown}s 后重发` : '获取验证码' }}
          </el-button>
        </div>
      </el-form-item>
      <el-form-item class="marg-b-10">
        <div class="fx-sb">
            <div>
                <el-checkbox v-model="fromData.rememberMe" label="7天免登录" size="large" />
            </div>
            <div class="forgot-pass" @click="emit('goHandle', 'pass')">密码登录</div>
        </div>
      </el-form-item>
      <el-form-item class="marg-bt-15">
        <el-button type="primary" class="login-btn" :loading="loading" @click="submitForm(formRef)">登 录</el-button>
      </el-form-item>
    </el-form>
    <div class="font-bt text-center" @click="goRegister">
        没有账号？去注册
    </div>
  </div>
</template>
<script setup>
import { onBeforeUnmount, reactive, ref } from "vue";
import { useRouter } from 'vue-router';
import { ElMessage } from "element-plus";
import { emailLogin, getUserInfo, verifycode } from '@/api/user';
import { useUserStore } from '@/store';

const emit = defineEmits(['goHandle']);
const router = useRouter();
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
      throw new Error(response.msg || response.message || '登录失败');
    }
    const token = response.data?.access_token || response.data?.token || response.data;
    await store.setToken(token);
    sessionStorage.setItem('token', token);

    const userResponse = await getUserInfo();
    if (userResponse.code === 200 && userResponse.data) {
      await store.setUserInfo(userResponse.data);
      sessionStorage.setItem('userInfo', JSON.stringify(userResponse.data));
    }
    ElMessage.success('登录成功！');
    window.location.href = '/#/main/index';
    window.location.reload();
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
  margin-top: 24px;
}
.code-row {
  display: flex;
  gap: 12px;
  width: 100%;
}
.code-row :deep(.el-input) {
  flex: 1;
}
.code-button {
  width: 118px;
  height: 40px;
  flex-shrink: 0;
  font-size: 13px;
  border-radius: 6px;
  border-color: #CBD5E1;
  color: #334155;
  background-color: #F8FAFC;
  &:hover {
    color: #2563EB;
    border-color: #2563EB;
    background-color: #EFF6FF;
  }
}
.login-btn {
  width: 100%;
  height: 40px;
  font-size: 15px;
  font-weight: 500;
  border-radius: 6px;
  background-color: #2563EB !important;
  border-color: #2563EB !important;
  color: #FFFFFF !important;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 1px 2px rgba(37, 99, 235, 0.15);
  &:hover, &:focus {
    background-color: #1D4ED8 !important;
    border-color: #1D4ED8 !important;
    color: #FFFFFF !important;
    box-shadow: 0 2px 6px rgba(37, 99, 235, 0.25);
  }
  &:active {
    background-color: #1E40AF !important;
    border-color: #1E40AF !important;
    color: #FFFFFF !important;
  }
}
</style>
