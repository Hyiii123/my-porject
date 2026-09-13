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
          accept="image/jpeg,image/png,image/gif,image/bmp"
          :show-file-list="false"
          :on-success="handleAvatarSuccess"
          :on-error="handleAvatarUploadError"
          :before-upload="beforeAvatarUpload"
          :headers="uploadHeaders"
          >
          <img
            v-if="imageUrl"
            :src="formatAvatarUrl(imageUrl)"
            @error="handleAvatarError"
            class="avatar"
            alt="用户头像"
          >
          <img
            v-else
            :src="defaultAvatar"
            class="avatar avatar-default"
            alt="默认头像"
          >
          <div class="uploadBut" :class="{ disabled: uploading }">
            <span>{{ uploading ? '上传中...' : '上传头像' }}</span>
          </div>
        </el-upload>
      </div>
    </div>
    <div v-else class="set pd-tp-30">
      <div class="line fx-sb">
        <div><span>登录账号</span> 当前登录名：{{ userInfo.username || userInfo.userName || '未设置' }}</div>
      </div>
      <div class="line fx-sb">
        <div><span>登录密码</span> 账户登录密码：已设置安全保护</div>
        <span class="font-bt" @click="passwordDialogVisible = true">修改密码</span>
      </div>
      <div class="line fx-sb">
        <div><span>绑定手机</span> 已绑定手机：{{ formatPhone(userInfo.phone || userInfo.phonenumber) }}</div>
        <span class="font-bt" @click="openPhoneModal">修改</span>
      </div>
      <div class="line fx-sb">
        <div><span>绑定邮箱</span> 已绑定邮箱：{{ userInfo.email || '未绑定邮箱' }}</div>
        <span class="font-bt" @click="openEmailModal">修改</span>
      </div>
    </div>

    <!-- 修改密码弹窗 -->
    <el-dialog v-model="passwordDialogVisible" title="修改登录密码" width="420px" destroy-on-close>
      <el-form label-width="90px" :model="passwordForm">
        <el-form-item label="原密码" required>
          <el-input v-model="passwordForm.oldPassword" type="password" show-password placeholder="请输入当前原密码" />
        </el-form-item>
        <el-form-item label="新密码" required>
          <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="请输入不少于6位的新密码" />
        </el-form-item>
        <el-form-item label="确认新密码" required>
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="passwordSubmitting" @click="handleUpdatePassword">确定修改</el-button>
      </template>
    </el-dialog>

    <!-- 绑定/更换手机弹窗 -->
    <el-dialog v-model="phoneDialogVisible" title="绑定/更换手机号码" width="400px" destroy-on-close>
      <el-form label-width="80px" :model="phoneForm">
        <el-form-item label="手机号码" required>
          <el-input v-model="phoneForm.phone" placeholder="请输入11位手机号码" maxlength="11" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="phoneDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="phoneSubmitting" @click="handleUpdatePhone">确定保存</el-button>
      </template>
    </el-dialog>

    <!-- 绑定/更换邮箱弹窗 -->
    <el-dialog v-model="emailDialogVisible" title="绑定/更换邮箱" width="400px" destroy-on-close>
      <el-form label-width="80px" :model="emailForm">
        <el-form-item label="电子邮箱" required>
          <el-input v-model="emailForm.email" placeholder="请输入有效邮箱地址" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="emailDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="emailSubmitting" @click="handleUpdateEmail">确定保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
/** 数据导入 **/
import { ref, reactive, computed, onMounted } from "vue";
import { ElMessage } from "element-plus";
import { updateUserInfo, updateStudentPassword, getUserInfo } from "@/api/user.js";
import { useUserStore } from "@/store";
import defaultAvatar from "@/assets/icon_touxiang.png";

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
const uploading = ref(false);

const handleAvatarError = () => {
  // 当网络图片路径失效或404时，自动回滚至默认头像占位
  imageUrl.value = '';
};

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
  const fileName = file.name ? file.name.toLowerCase() : '';
  const validExts = ['.jpg', '.jpeg', '.png', '.gif', '.bmp'];
  const hasValidExt = validExts.some(ext => fileName.endsWith(ext));

  if (!isImage || !hasValidExt) {
    ElMessage.error('上传头像图片只能是 JPG、JPEG、PNG、GIF、BMP 格式!');
    return false;
  }
  const isLt10M = file.size / 1024 / 1024 < 10;
  if (!isLt10M) {
    ElMessage.error('上传头像图片大小不能超过 10MB!');
    return false;
  }
  uploading.value = true;
  return true;
};

function handleAvatarSuccess(res, file) {
  uploading.value = false;
  if (res.code === 200) {
    const uploadedUrl = res.data?.url || res.data?.path || '';
    user.icon = uploadedUrl;
    // 优先通过本地对象URL实时渲染，保证用户上传瞬间立刻看到新头像
    if (file && file.raw) {
      imageUrl.value = URL.createObjectURL(file.raw);
    } else {
      imageUrl.value = uploadedUrl;
    }
    ElMessage.success('头像上传成功，请点击“更新信息”保存');
  } else {
    ElMessage.error(res.msg || res.message || '图片上传出错，请联系管理员');
  }
}

function handleAvatarUploadError(err) {
  uploading.value = false;
  let msg = '头像上传失败，请检查网络或图片格式/大小';
  try {
    if (typeof err === 'string') msg = err;
    else if (err?.message) msg = err.message;
    if (err?.status === 413) msg = '上传图片体积超出限制，请选择 10MB 以内的图片';
  } catch(e) {}
  ElMessage.error(msg);
}

// 安全设置弹窗与状态
const passwordDialogVisible = ref(false);
const passwordSubmitting = ref(false);
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
});

const phoneDialogVisible = ref(false);
const phoneSubmitting = ref(false);
const phoneForm = reactive({ phone: '' });

const emailDialogVisible = ref(false);
const emailSubmitting = ref(false);
const emailForm = reactive({ email: '' });

const openPhoneModal = () => {
  phoneForm.phone = userInfo.value.phone || userInfo.value.phonenumber || '';
  phoneDialogVisible.value = true;
};

const openEmailModal = () => {
  emailForm.email = userInfo.value.email || '';
  emailDialogVisible.value = true;
};

const handleUpdatePassword = async () => {
  if (!passwordForm.oldPassword) {
    ElMessage.warning('请输入当前原密码');
    return;
  }
  if (!passwordForm.newPassword || passwordForm.newPassword.length < 6) {
    ElMessage.warning('新密码长度不能少于6位');
    return;
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致');
    return;
  }
  passwordSubmitting.value = true;
  try {
    const res = await updateStudentPassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    });
    if (res?.code === 200) {
      ElMessage.success('登录密码修改成功，请牢记新密码！');
      passwordDialogVisible.value = false;
      passwordForm.oldPassword = '';
      passwordForm.newPassword = '';
      passwordForm.confirmPassword = '';
    } else {
      ElMessage.error(res?.msg || '修改密码失败');
    }
  } catch (err) {
    ElMessage.error(err?.msg || err?.message || '修改密码失败，请核对原密码');
  } finally {
    passwordSubmitting.value = false;
  }
};

const handleUpdatePhone = async () => {
  const clean = (phoneForm.phone || '').trim();
  if (!/^1[3-9]\d{9}$/.test(clean)) {
    ElMessage.warning('请输入正确的11位手机号码');
    return;
  }
  phoneSubmitting.value = true;
  try {
    const res = await updateUserInfo({ phonenumber: clean, phone: clean });
    if (res?.code === 200) {
      ElMessage.success('手机号码绑定成功！');
      phoneDialogVisible.value = false;
      userInfo.value.phone = clean;
      userInfo.value.phonenumber = clean;
      const cached = store.getUserInfo || {};
      cached.phone = clean;
      cached.phonenumber = clean;
      await store.setUserInfo(cached);
    } else {
      ElMessage.error(res?.msg || '绑定手机号码失败');
    }
  } catch (err) {
    ElMessage.error(err?.msg || err?.message || '绑定失败，请重试');
  } finally {
    phoneSubmitting.value = false;
  }
};

const handleUpdateEmail = async () => {
  const clean = (emailForm.email || '').trim();
  if (!/^[\w.-]+@[\w.-]+\.[a-zA-Z]{2,}$/.test(clean)) {
    ElMessage.warning('请输入有效的邮箱地址');
    return;
  }
  emailSubmitting.value = true;
  try {
    const res = await updateUserInfo({ email: clean });
    if (res?.code === 200) {
      ElMessage.success('邮箱绑定成功！');
      emailDialogVisible.value = false;
      userInfo.value.email = clean;
      const cached = store.getUserInfo || {};
      cached.email = clean;
      await store.setUserInfo(cached);
    } else {
      ElMessage.error(res?.msg || '绑定邮箱失败');
    }
  } catch (err) {
    ElMessage.error(err?.msg || err?.message || '绑定失败，请重试');
  } finally {
    emailSubmitting.value = false;
  }
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
        // 通知顶栏 Header 即时刷新用户信息
        window.dispatchEvent(new Event('user-profile-updated'));
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
