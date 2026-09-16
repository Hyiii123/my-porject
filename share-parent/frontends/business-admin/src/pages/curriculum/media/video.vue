<template>
  <div class="video-preview-page">
    <el-card>
      <template #header>
        <span>视频预览 (ID: {{ route.params.id }})</span>
      </template>
      <div class="video-placeholder">
        <p>暂无视频预览内容</p>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { useRoute } from 'vue-router';

const route = useRoute();
</script>

<style scoped>
.video-preview-page {
  padding: 20px;
}
.video-placeholder {
  text-align: center;
  padding: 40px;
  color: #909399;
}
</style>
