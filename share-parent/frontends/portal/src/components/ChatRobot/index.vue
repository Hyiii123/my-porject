<template>
  <button class="chat-robot-entry" type="button" @click="openCustomerService">
    <el-icon :size="22"><ChatDotRound /></el-icon>
    <span>客服中心</span>
    <i class="entry-dot" />
  </button>
</template>

<script setup>
import { ChatDotRound } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'

const router = useRouter()

function openCustomerService() {
  router.push({ name: 'customerServiceIndex' })
}
</script>

<style scoped>
.chat-robot-entry {
  position: fixed;
  top: 18px;
  right: 28px;
  z-index: 1000;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  height: 38px;
  padding: 0 15px;
  color: #4f61d8;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  background: #f4f5ff;
  border: 1px solid #dfe3ff;
  border-radius: 20px;
  box-shadow: 0 6px 18px rgba(79, 97, 216, 0.12);
  transition: 0.2s ease;
}

.chat-robot-entry:hover {
  color: #fff;
  background: #5969d9;
  border-color: #5969d9;
  transform: translateY(-1px);
}

.entry-dot {
  width: 7px;
  height: 7px;
  background: #67c23a;
  border-radius: 50%;
  box-shadow: 0 0 0 3px rgba(103, 194, 58, 0.14);
}
</style>
