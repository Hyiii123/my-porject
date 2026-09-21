<!-- 登录页面 - 用户名密码登录 (IAIC 风格) -->
<template>
  <div class="loginPass">
    <el-form
      ref="formRef"
      :model="fromData"
      :rules="rules"
      label-width="0px"
      class="iaic-login-form"
    >
      <el-form-item prop="username">
        <el-input
          v-model="fromData.username"
          placeholder="请输入用户名或注册邮箱"
          size="large"
          class="iaic-input"
          clearable
        />
      </el-form-item>
      <el-form-item prop="password">
        <el-input
          type="password"
          :show-password="true"
          v-model="fromData.password"
          placeholder="请输入密码"
          size="large"
          class="iaic-input"
          @keyup.enter="submitForm(formRef)"
        />
      </el-form-item>
      <el-form-item class="marg-b-10">
        <div class="fx-sb">
          <div>
            <el-checkbox v-model="fromData.rememberMe" label="7天免登录" size="default" />
          </div>
          <div class="forgot-link">找回密码</div>
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
import { reactive, ref } from "vue";
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from "element-plus";
import { userLogins, getUserInfo } from '@/api/user';
import { useUserStore } from '@/store';

const emit = defineEmits(['goHandle']);
const router = useRouter();
const route = useRoute();
const loading = ref(false);
const store = useUserStore();

const formRef = ref();

// 登录参数
const fromData = reactive({
  username: "admin",
  password: "admin123",
  rememberMe: true
});

// 验证规则
const rules = reactive({
  username: [
    { required: true, message: "请输入正确的用户名或邮箱", trigger: "blur" },
  ],
  password: [
    { required: true, message: "请输入正确的密码", trigger: "blur"},
  ],
});

// 登录提交
const submitForm = async (formEl) => {
  if (!formEl || loading.value) return;

  await formEl.validate(async (valid) => {
    if (valid) {
      loading.value = true;

      try {
        const response = await userLogins({
          username: fromData.username.trim(),
          password: fromData.password,
          rememberMe: fromData.rememberMe,
        });
        if (response.code !== 200 || !response.data) {
          throw new Error(response.msg || response.message || '用户名或密码错误');
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
        ElMessage.error(error.message || '登录失败，请检查账号和密码');
      } finally {
        loading.value = false;
      }
    } else {
      ElMessage.error('请填写完整的登录信息');
    }
  });
};

// 去注册
const goRegister = () => {
  emit('goHandle', 'register');
};
</script>

<style lang="scss" scoped>
.loginPass {
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

.fx-sb {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.forgot-link {
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
