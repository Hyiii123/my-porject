<!-- 个人设置 -->
<template>
  <div class="mySetWrapper content">
    <CardsTitle class="marg-bt-40" title="个人设置" />
    <TableSwitchBar :data="tabData" @changeTable="checkHandle"></TableSwitchBar>  
    <div v-if="act == 0" class="fx-sb pd-tp-30">
      <div>
        <div class="fx">
          <div class="item fx">
            <span class="lab">昵称：</span>
            <el-input v-model="user.name" placeholder="请输入用户昵称" maxlength="30" clearable style="width: 260px;"></el-input>
          </div>
        </div>
        <div class="item fx">
          <span class="lab">性别：</span>
          <el-radio-group class="radioGroup" v-model="user.gender">
            <el-radio :label="0" :value="0">男</el-radio>
            <el-radio :label="1" :value="1">女</el-radio>
          </el-radio-group>
        </div>
        <div class="item fx">
          <div class="bt" :class="{ disabled: submitting }" @click="updateUserInfoHandle">
            {{ submitting ? '保存中...' : '更新信息' }}
          </div>
        </div>
      </div>
      <div>
        <el-upload
          class="avatar-uploader"
          :action="actions"
          :show-file-list="false"
          :on-success="handleAvatarSuccess"
          :before-upload="beforeAvatarUpload"
          :headers="uploadHeaders"
          >
          <img v-if="imageUrl" :src="formatAvatarUrl(imageUrl)" class="avatar" alt="用户头像">
          <i v-else class="el-icon-plus avatar-uploader-icon"></i>
          <div class="uploadBut"><span>上传头像</span></div>
        </el-upload>
      </div>
    </div>
    <div v-else class="set pd-tp-30">
      <div class="line fx-sb">
        <div><span>登录账号</span> 当前登录名：{{ userInfo.username || userInfo.userName || '未设置' }}</div>
      </div>
      <div class="line fx-sb">
        <div><span>绑定手机</span> 已绑定手机：{{ formatPhone(userInfo.phone || userInfo.phonenumber) }}</div>
        <span class="font-bt" @click="changeHandle">修改</span>
      </div>
      <div class="line fx-sb">
        <div><span>绑定邮箱</span> 已绑定邮箱：{{ userInfo.email || '未绑定邮箱' }}</div>
        <span class="font-bt" @click="changeHandle">修改</span>
      </div>
    </div>
  </div>
</template>

<script setup>
/** 数据导入 **/
import { ref, reactive, computed, onMounted } from "vue";
import { ElMessage } from "element-plus";
import { updateUserInfo, getUserInfo } from "@/api/user.js";
import { useUserStore } from "@/store";

// 组件导入
import CardsTitle from "./components/CardsTitle.vue";
import TableSwitchBar from "@/components/TableSwitchBar.vue";

const store = useUserStore();
const rawUser = store.getUserInfo || JSON.parse(sessionStorage.getItem('userInfo') || '{}') || {};
const userInfo = ref(rawUser);

const defaultBaseURL = import.meta.env.MODE === 'production' ? '' : 'http://localhost:8080';
const actions = `${(import.meta.env.VITE_API_BASE_URL || defaultBaseURL).replace(/\/$/, '')}/ms/files`;

const uploadHeaders = computed(() => {
  const token = store.getToken || sessionStorage.getItem('token') || '';
  return token ? { Authorization: token.startsWith('Bearer ') ? token : `Bearer ${token}` } : {};
});

const tabData = [
  { id: 0, name: '基本信息' },
  { id: 1, name: '安全设置' }
];

// 切换基本信息和安全设置
const act = ref(0);
const checkHandle = (val) => {
  act.value = val;
};

// 更新信息的参数
const user = reactive({
  name: rawUser.nickname || rawUser.name || rawUser.nickName || '',
  icon: rawUser.avatar || rawUser.icon || '',
  gender: rawUser.gender ?? (rawUser.sex !== undefined ? parseInt(rawUser.sex) : 0)
});

// 图片上传状态
const imageUrl = ref(user.icon);
const submitting = ref(false);

const formatAvatarUrl = (url) => {
  if (!url) return '';
  if (url.startsWith('http://') || url.startsWith('https://') || url.startsWith('blob:') || url.startsWith('data:')) return url;
  const base = (import.meta.env.VITE_API_BASE_URL || defaultBaseURL).replace(/\/$/, '');
  return base ? `${base}${url}` : url;
};

const formatPhone = (phone) => {
  if (!phone) return '暂未绑定手机';
  const str = String(phone);
  if (str.length === 11) {
    return `${str.slice(0, 3)}****${str.slice(7)}`;
  }
  return str;
};

const beforeAvatarUpload = (file) => {
  const isImage = file.type.startsWith('image/');
  const isLt5M = file.size / 1024 / 1024 < 5;
  if (!isImage) {
    ElMessage.error('上传头像图片只能是 JPG/PNG/WEBP/GIF 格式!');
    return false;
  }
  if (!isLt5M) {
    ElMessage.error('上传头像图片大小不能超过 5MB!');
    return false;
  }
  return true;
};

function handleAvatarSuccess(res, file) {
  if (res.code === 200) {
    imageUrl.value = res.data?.url || (file.raw ? URL.createObjectURL(file.raw) : '');
    user.icon = res.data?.url || res.data?.path || imageUrl.value;
    ElMessage.success('头像上传成功，请点击“更新信息”保存');
  } else {
    ElMessage.error(res.msg || res.message || '图片上传出错，请联系管理员');
  }
}

// 更改密码 手机号提示
const changeHandle = () => {
  ElMessage.info('安全设置修改功能暂未开放，如需更换绑定请联系客服！');
};

// 提交更新信息
const updateUserInfoHandle = async () => {
  if (submitting.value) return;
  if (!user.name || !user.name.trim()) {
    ElMessage.warning('请输入用户昵称');
    return;
  }
  submitting.value = true;
  try {
    const payload = {
      name: user.name.trim(),
      icon: user.icon || '',
      gender: Number(user.gender || 0)
    };
    const res = await updateUserInfo(payload);
    if (res.code === 200) {
      ElMessage.success('个人信息更新成功！');
      // 重新获取当前登录用户的信息
      const data = await getUserInfo();
      if (data.code === 200 && data.data) {
        await store.setUserInfo(data.data);
        userInfo.value = data.data;
        user.name = data.data.nickname || data.data.name || data.data.nickName || user.name;
        user.icon = data.data.avatar || data.data.icon || user.icon;
        user.gender = data.data.gender ?? (data.data.sex !== undefined ? parseInt(data.data.sex) : user.gender);
        imageUrl.value = user.icon;
      }
    } else {
      ElMessage.error(res.msg || res.message || '更新个人信息失败');
    }
  } catch (err) {
    ElMessage.error(err?.msg || err?.message || '请求出错，请重试！');
  } finally {
    submitting.value = false;
  }
};

onMounted(async () => {
  try {
    const res = await getUserInfo();
    if (res.code === 200 && res.data) {
      await store.setUserInfo(res.data);
      userInfo.value = res.data;
      user.name = res.data.nickname || res.data.name || res.data.nickName || '';
      user.icon = res.data.avatar || res.data.icon || '';
      user.gender = res.data.gender ?? (res.data.sex !== undefined ? parseInt(res.data.sex) : 0);
      imageUrl.value = user.icon;
    }
  } catch (e) {
    // 保持使用 store 缓存
  }
});
</script>

<style lang="scss" src="./index.scss"></style>
