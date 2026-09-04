<!-- 登录页面 - 用户名密码登录 -->
<template>
  <div class="loginPass">
    <el-form
      ref="formRef"
      :model="fromData"
      :rules="rules"
      label-width="0px"
      class="demo-dynamic"
    >
      <el-form-item prop="username" label="">
        <el-input v-model="fromData.username" placeholder="请输入用户名或手机号" />
      </el-form-item>
      <el-form-item prop="password" label="">
        <el-input type="password" :show-password="true" v-model="fromData.password" placeholder="请输入密码" />
      </el-form-item>
      <el-form-item class="marg-b-10">
        <div class="fx-sb">
            <div>
                <el-checkbox v-model="fromData.rememberMe" label="7天免登录" size="large" />
            </div>
            <div class="forgot-link">找回密码</div>
        </div>
      </el-form-item>
      <el-form-item class="marg-bt-15">
        <el-button type="primary" class="login-btn" @click="submitForm(formRef)" :loading="loading">登 录</el-button>
      </el-form-item>
    </el-form>
    <div class="font-bt text-center" @click="goRegister">
        去注册
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from "vue";
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from "element-plus";
import { userLogins, getUserInfo } from '@/api/user'
import { useUserStore } from '@/store'

const emit = defineEmits(['goHandle'])
const router = useRouter()
const route = useRoute()
const loading = ref(false)
const store = useUserStore()

const formRef = ref();

// 登录参数
const fromData = reactive({
  username: "admin",
  // 与 share-parent 演示数据中的 BCrypt 密码保持一致，避免打开登录页后直接填入失效密码。
  password: "admin123",
  rememberMe: true
});

// 验证规则
const rules = reactive({
  username: [
    { required: true, message: "请输入正确的用户名或手机号", trigger: "blur" },
  ],
  password: [
    { required: true, message: "请输入正确的密码", trigger: "blur"},
  ],
});

// 登录
const submitForm = async (formEl) => {
  if (!formEl) return;

  await formEl.validate(async (valid) => {
    if (valid) {
      loading.value = true;

      try {
        const response = await userLogins({
          username: fromData.username.trim(),
          password: fromData.password,
          rememberMe: fromData.rememberMe,
        })
        if (response.code !== 200 || !response.data) {
          throw new Error(response.msg || response.message || '用户名或密码错误')
        }
        const token = response.data?.access_token || response.data?.token || response.data
        await store.setToken(token)
        const userResponse = await getUserInfo()
        if (userResponse.code === 200 && userResponse.data) {
          await store.setUserInfo(userResponse.data)
        }
        ElMessage.success('登录成功！')
        router.push(route.query.redirect || '/main/index')
      } catch (error) {
        ElMessage.error(error.message || '登录失败，请检查账号和密码')
      } finally {
        loading.value = false
      }
    } else {
      ElMessage.error('请填写完整的登录信息');
    }
  });
};

// 去注册
const goRegister = () => {
  emit('goHandle', 'register')
}
</script>

<style lang="scss" scoped>
.loginPass {
  margin-top: 40px;
}

.login-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
}

.forgot-link {
  color: #909399;
  cursor: pointer;

  &:hover {
    color: #409eff;
  }
}

</style>
