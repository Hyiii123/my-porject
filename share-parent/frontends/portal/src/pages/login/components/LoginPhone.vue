<!-- 登录页面 - 手机号 -->
<template>
  <div class="loginPhone">
    <el-form
      ref="formRef"
      :model="fromData"
      :rules="rules"
      label-width="0px"
      class="demo-dynamic"
    >
      <el-form-item prop="cellPhone" label="">
        <el-input v-model="fromData.cellPhone" placeholder="请输入手机号" />
      </el-form-item>
      <el-form-item prop="password" label="">
        <div class="code-row">
          <el-input v-model="fromData.password" placeholder="请输入验证码（本地演示为 123456）" />
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
            <div>找回密码</div>
        </div>
      </el-form-item>
      <el-form-item class="marg-bt-15">
        <el-button type="primary" class="login-btn" :loading="loading" @click="submitForm(formRef)">登 录</el-button>
      </el-form-item>
    </el-form>
    <div class="font-bt text-center"  @click="goRegister">
        去注册
    </div>
  </div>
</template>
<script setup>
import { onBeforeUnmount, reactive, ref } from "vue";
import { useRouter } from 'vue-router'
import { ElMessage } from "element-plus";
import { phoneLogins, getUserInfo, verifycode } from '@/api/user'
import { useUserStore } from '@/store'

const emit = defineEmits(['goHandle'])
const router = useRouter()
const store = useUserStore()
const loading = ref(false)
const codeLoading = ref(false)
const codeCountdown = ref(0)
let countdownTimer
// 登录数据初始化
const formRef = ref();
const fromData = reactive({
  // 使用初始化 SQL 中存在的演示学员手机号，配合本地演示验证码 123456 可直接验证短信登录链路。
  cellPhone: "13800138001",
  password: "123456",
  type: 2
});
// 效验规则
const rules = reactive({
  cellPhone: [
    { required: true, message: "请输入正确的手机号", trigger: "blur" },
  ],
  password: [
    { required: true, message: "请输入正确的用验证码", trigger: "blur"},
  ],
});
const sendCode = async () => {
  if (codeLoading.value || codeCountdown.value > 0) return
  const phone = fromData.cellPhone.trim()
  if (!/^1\d{10}$/.test(phone)) {
    ElMessage.warning('请输入正确的手机号')
    return
  }
  codeLoading.value = true
  try {
    const response = await verifycode({ cellPhone: phone })
    if (response.code !== 200) throw new Error(response.msg || response.message || '验证码发送失败')
    const demoCode = response.data?.code || '123456'
    ElMessage.success(`验证码已发送，本地演示验证码：${demoCode}`)
    codeCountdown.value = 60
    countdownTimer = window.setInterval(() => {
      codeCountdown.value -= 1
      if (codeCountdown.value <= 0) {
        window.clearInterval(countdownTimer)
        countdownTimer = undefined
      }
    }, 1000)
  } catch (error) {
    ElMessage.error(error.message || '验证码发送失败')
  } finally {
    codeLoading.value = false
  }
}
// 数据提交
const submitForm = async (formEl) => {
  if (!formEl || loading.value) return
  const valid = await formEl.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const response = await phoneLogins({
      cellPhone: fromData.cellPhone.trim(),
      password: fromData.password,
      rememberMe: fromData.rememberMe,
    })
    if (response.code !== 200 || !response.data) {
      throw new Error(response.msg || response.message || '手机号登录失败')
    }
    const token = response.data?.access_token || response.data?.token || response.data
    await store.setToken(token)
    const userResponse = await getUserInfo()
    if (userResponse.code === 200 && userResponse.data) {
      await store.setUserInfo(userResponse.data)
    }
    ElMessage.success('登录成功！')
    window.location.href = '/#/main/index'
    window.location.reload()
  } catch (error) {
    ElMessage.error(error.message || '手机号登录失败')
  } finally {
    loading.value = false
  }
};

// 去注册
const goRegister = () => {
  emit('goHandle', 'register')
}
onBeforeUnmount(() => {
  if (countdownTimer) window.clearInterval(countdownTimer)
})
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
