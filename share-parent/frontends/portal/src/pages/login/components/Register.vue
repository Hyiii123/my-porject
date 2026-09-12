<!-- 注册页面 - QQ邮箱注册 -->
<template>
  <div class="loginPass">
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
      <el-form-item prop="password" label="">
        <el-input type="password" show-password v-model="fromData.password" placeholder="请输入密码（6-20位）" clearable />
      </el-form-item>
      <el-form-item prop="code" label="">
        <div class="code-row">
          <el-input v-model="fromData.code" placeholder="请输入邮箱验证码" maxlength="6" clearable />
          <el-button
            class="code-button"
            :disabled="countdown > 0 || codeLoading"
            :loading="codeLoading"
            @click="verifycodeHandle"
          >
            {{ countdown > 0 ? `${countdown}s后重发` : '获取验证码' }}
          </el-button>
        </div>
      </el-form-item>
      <el-form-item class="marg-bt-15">
        <el-button type="primary" class="login-btn" :loading="loading" @click="submitForm(formRef)">注 册</el-button>
      </el-form-item>
    </el-form>
    <div class="font-bt text-center" @click="goLogin()">
        已有账号？去登录
    </div>
  </div>
</template>
<script setup>
import { reactive, ref, onUnmounted } from "vue";
import { useRouter } from 'vue-router';
import { userRegist, verifycode } from "@/api/user";
import { useUserStore } from '@/store';
import { ElMessage } from "element-plus";

const store = useUserStore();
const router = useRouter();
const loading = ref(false);
const codeLoading = ref(false);
const countdown = ref(0);
let timer = null;

const emit = defineEmits(['goHandle']);

// 注册表单数据初始化
const formRef = ref();
const fromData = reactive({
  email: "",
  password: "",
  code: ""
});

// QQ邮箱校验
const verifyEmail = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请输入QQ邮箱'));
    return;
  }
  const reg = /^[A-Za-z0-9._%+-]+@(qq|vip\.qq|foxmail)\.com$/i;
  if (!reg.test(value.trim())) {
    callback(new Error('请输入正确的QQ邮箱（例如：123456@qq.com）'));
    return;
  }
  callback();
};

// 校验规则
const rules = reactive({
  email: [
    { required: true, validator: verifyEmail, trigger: "blur" },
  ],
  password: [
    { required: true, message: "请输入密码", trigger: "blur" },
    { min: 6, max: 20, message: "密码长度必须在6到20个字符之间", trigger: "blur" }
  ],
  code: [
    { required: true, message: "请输入邮箱验证码", trigger: "blur" },
    { min: 6, max: 6, message: "验证码为6位数字", trigger: "blur" }
  ],
});

// 倒计时逻辑
const startCountdown = () => {
  countdown.value = 60;
  if (timer) clearInterval(timer);
  timer = setInterval(() => {
    countdown.value--;
    if (countdown.value <= 0) {
      clearInterval(timer);
      timer = null;
    }
  }, 1000);
};

onUnmounted(() => {
  if (timer) clearInterval(timer);
});

// 发送验证码
const verifycodeHandle = async () => {
  if (!fromData.email) {
    ElMessage.error('请输入QQ邮箱');
    return;
  }
  const reg = /^[A-Za-z0-9._%+-]+@(qq|vip\.qq|foxmail)\.com$/i;
  if (!reg.test(fromData.email.trim())) {
    ElMessage.error('请输入正确的QQ邮箱（例如：123456@qq.com）');
    return;
  }

  codeLoading.value = true;
  try {
    const res = await verifycode({ email: fromData.email.trim() });
    if (res.code === 200) {
      ElMessage.success(res.data?.message || '验证码已发送至您的QQ邮箱，请查收');
      startCountdown();
    } else {
      ElMessage.error(res.msg || '验证码发送失败，请稍后重试');
    }
  } catch (err) {
    ElMessage.error(err.message || '网络异常，验证码发送失败');
  } finally {
    codeLoading.value = false;
  }
};

// 提交注册
const submitForm = (formEl) => {
  if (!formEl) return;
  formEl.validate(async (valid) => {
    if (valid) {
      loading.value = true;
      try {
        const res = await userRegist({
          email: fromData.email.trim(),
          password: fromData.password,
          code: fromData.code.trim()
        });
        if (res.code === 200) {
          ElMessage.success('注册成功！请使用QQ邮箱或账号登录');
          setTimeout(() => {
            emit('goHandle', 'pass');
          }, 600);
        } else {
          ElMessage.error(res.msg || '注册失败');
        }
      } catch (err) {
        ElMessage.error(err.message || '注册异常，请稍后重试');
      } finally {
        loading.value = false;
      }
    }
  });
};

// 去登录
const goLogin = () => {
  emit('goHandle', 'pass');
};
</script>

<style lang="scss" scoped>
.loginPass {
  margin-top: 24px;

  .code-row {
    display: flex;
    gap: 12px;
    width: 100%;

    .code-button {
      flex-shrink: 0;
      width: 110px;
      height: 40px;
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
}
</style>
