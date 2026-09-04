<template>
  <div class="uploadBox">
    <el-upload
      class="avatar-uploader"
      :class="isCourse?'courseBox':''"
      v-model:file-list="fileList"
      :action="`${(import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080').replace(/\/$/, '')}/ms/files`"
      list-type="picture-card"
      :on-success="handleAvatarSuccess"
      :on-change="handleChangeSuccess"
      :before-upload="beforeAvatarUpload"
    >
      <img v-if="imageUrl" :src="imageUrl" class="avatar" />
      <div v-else class="avatar-uploader-icon"><span></span>上传<Plus /></div>
      <div v-if="imageUrl" class="el-upload-list__item-actions">
        <span class="el-delect" @click.stop="handleRemove"></span>
        <div class="avatar-uploader-icon"><span></span>上传<Plus /></div>
      </div>
    </el-upload>
    <div>图片大小不超过2M<br>建议图片尺寸472*264<br>仅能上传PNG JPG JPEG类型图片</div>
  </div>
</template>
<script setup>
import { ref, reactive, nextTick } from "vue";
import { ElMessage } from "element-plus";
import { Plus } from "@element-plus/icons-vue";
import { TOKEN_NAME } from "../../config/global";
// 获取父组件值、方法
const props = defineProps({
  // 搜索对象
  isCourse: {
    type: Boolean,
    default:false,
  },
});
// 定义变量
let imageUrl = ref(""); //上传的图片路径
const fileList = ref([]);
const emit = defineEmits(); //子组件获取父组件事件传值
const actions = `${(import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080').replace(/\/$/, '')}/ms/files`;
const uploadHeaders = { authorization: sessionStorage.getItem(TOKEN_NAME) };

// ------定义方法------
// 获取上传的图片路径
const handleAvatarSuccess = (response, uploadFile) => {
  const url = response?.data?.url || response?.data?.path;
  if (!url) {
    ElMessage.error("上传成功但未获取到文件地址");
    return;
  }
  imageUrl.value = url;
  emit("getCoverUrl", url);
};
// 获取上传的图片路径
const handleChangeSuccess = (uploadFile) => {
  if (uploadFile?.raw) imageUrl.value = URL.createObjectURL(uploadFile.raw);
  emit('getFlag',true)
};
// 上传前判断图片是否符合上传要求
const beforeAvatarUpload = (file) => {
  const types = ["image/jpeg", "image/jpg", "image/png"];
  const isImage = types.includes(file.type);
  const isLt2M = file.size / 1024 / 1024 < 2;
  // 判断图片格式
  if (!isImage) {
    ElMessage({

      message: "仅能是上传PNG、JPG、JPEG类型图片!",
      type: "error",
      showClose:false,
    });
    return false;
  }
  // 判断图片大小
  if (!isLt2M) {
    ElMessage({

      message: "图片大小不超过 2MB!",
      type: "error",
      showClose:false,
    });
    return false;
  }
  imageUrl.value = URL.createObjectURL(file);
  return true;
};
// 删除图片
const handleRemove = () => {
  imageUrl.value = "";
  emit('getFlag',false)
};
</script>

<style lang="scss" scoped>
</style>
