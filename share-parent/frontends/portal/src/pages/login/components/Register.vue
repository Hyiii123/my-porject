<!-- 注册页面 -->
<template>
  <div class="loginPass">
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
        <el-input v-model="fromData.password" placeholder="请输入密码" />
      </el-form-item>
      <el-form-item prop="code" label="">
        <div class="code-row">
          <el-input v-model="fromData.code" placeholder="请确认短信验证码" />
          <el-button class="code-button" @click="verifycodeHandle">发送验证码</el-button>
        </div>
      </el-form-item>
      <el-form-item class="marg-bt-15">
        <el-button type="primary" class="login-btn" :loading="loading" @click="submitForm(formRef)">注 册</el-button>
      </el-form-item>
    </el-form>
    <div class="font-bt text-center" @click="goLogin()">
        去登录
    </div>
  </div>
</template>
<script setup>
import { reactive, ref } from "vue";
import { useRouter } from 'vue-router'
import { userRegist, verifycode } from "@/api/user"
import { useUserStore } from '@/store'
import { ElMessage } from "element-plus";

const store = useUserStore();
const router = useRouter()
const loading = ref(false);

const emit = defineEmits(['goHandle'])
// 登录数据初始化
const formRef = ref();
const fromData = reactive({
  cellPhone: "",
  password: "",
  code: "123456"
});
// 手机号效验
const verifyPone = (rull, value, callback) => {
  const reg = /^(13[0-9]|14[01456879]|15[0-35-9]|16[2567]|17[0-8]|18[0-9]|19[0-35-9])\d{8}$/
  if (!value) {
    callback(new Error('请输入手机号'));
    return
  }
  if(!reg.test(value)){
    callback(new Error('请输入正确的手机号'));
    return
  }
  callback()
}
// 效验
const rules = reactive({
  cellPhone: [
    { validator: verifyPone, trigger: "blur" },
  ],
  password: [
    { required: true, message: "请输入密码", trigger: "blur"},
  ],
  code: [
    { required: true, message: "请输入短信验证码", trigger: "blur"},
  ],
});
// 发送验证码
// const isSend = ref(true)
const verifycodeHandle = async() => {
  const reg = /^(13[0-9]|14[01456879]|15[0-35-9]|16[2567]|17[0-8]|18[0-9]|19[0-35-9])\d{8}$/
  if (fromData.cellPhone == "" || !reg.test(fromData.cellPhone)){
    ElMessage({
              message: '请输入正确的手机号',
              type: 'error'
          });
    return 
  }
  // 发送验证码
  await verifycode({cellPhone:fromData.cellPhone})
			.then(async res => {
				if (res.code == 200) {
					ElMessage({
              message: '验证码发送成功',
              type: 'success'
          });
				} else {
          ElMessage({
              message: res.msg,
              type: 'error'
          });
					console.log('登录失败')
				}
			})
			.catch(err => {});
}
// 注册
const submitForm = (formEl) => {
  if (!formEl) return;
  formEl.validate( async (valid) => {
    if (valid) {
      // 提交注册 
      await userRegist(fromData)
			.then(async res => {
				if (res.code == 200) {
					ElMessage({
              message: '注册成功！请登录',
              type: 'success'
          });
          setTimeout(() => {
            emit('goHandle', 'pass')
          }, 500)
				} else {
          ElMessage({
              message: res.msg,
              type: 'error'
          });
				}
			})
			.catch(err => {});
    }
  });
};
// 去登陆
const goLogin = () => {
  emit('goHandle', 'pass')
}
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
