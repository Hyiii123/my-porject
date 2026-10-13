<template>
  <div class="emptyPageBox">
    <div class="empty-vector-container">
      <svg width="140" height="110" viewBox="0 0 140 110" fill="none" xmlns="http://www.w3.org/2000/svg">
        <rect x="25" y="20" width="90" height="70" rx="10" fill="#F8FAFC" stroke="#E2E8F0" stroke-width="2"/>
        <line x1="40" y1="40" x2="100" y2="40" stroke="#CBD5E1" stroke-width="2.5" stroke-linecap="round"/>
        <line x1="40" y1="55" x2="85" y2="55" stroke="#E2E8F0" stroke-width="2.5" stroke-linecap="round"/>
        <line x1="40" y1="70" x2="70" y2="70" stroke="#E2E8F0" stroke-width="2.5" stroke-linecap="round"/>
        <circle cx="95" cy="72" r="14" fill="#FFFFFF" stroke="#3B82F6" stroke-width="2"/>
        <line x1="105" y1="82" x2="115" y2="92" stroke="#3B82F6" stroke-width="2.5" stroke-linecap="round"/>
        <circle cx="95" cy="72" r="4" fill="#93C5FD"/>
      </svg>
    </div>
    <div class="empty-title">{{ isSearch ? '未检索到相关数据' : '当前暂无数据记录' }}</div>
    <div class="empty-subtitle">{{ isSearch ? '请尝试调整筛选条件、更换搜索关键词后重试' : '系统正在持续同步业务数据，请稍后刷新查看' }}</div>
  </div>
</template>

<script setup>
defineProps({
  baseData: {
    type: Array,
    default: () => [],
  },
  isSearch: {
    type: Boolean,
    default: false,
  },
});
</script>

<style lang="scss">
.emptyPageBox {
  text-align: center;
  padding: 48px 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;

  .empty-vector-container {
    margin-bottom: 12px;
    opacity: 0.9;
    animation: float-gentle 3s ease-in-out infinite;
  }

  .empty-title {
    font-size: 14.5px;
    font-weight: 600;
    color: #334155;
    margin-bottom: 6px;
  }

  .empty-subtitle {
    font-size: 12.5px;
    color: #94A3B8;
    max-width: 320px;
    line-height: 1.6;
  }
}

@keyframes float-gentle {
  0% { transform: translateY(0px); }
  50% { transform: translateY(-4px); }
  100% { transform: translateY(0px); }
}
</style>
