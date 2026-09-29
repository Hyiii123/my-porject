<!-- 标题组件 - IAIC 风格 -->
<template>
  <div class="mainTitle-iaic fx-sb fx-al-ct">
    <div class="title-left">
      <div class="iaic-bar"></div>
      <h3 class="title-text">{{ title }}</h3>
    </div>
    <div v-if="more" @click="() => $router.push(moreUrl)" class="more-btn">
      <span>查看全部</span>
      <span class="arrow">➔</span>
    </div>
  </div>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    default: "课程标题",
  },
  more: {
    type: Boolean,
    default: true,
  },
  moreUrl:{
    type: String,
    default: '/search/index',
  },
});
</script>

<style lang="scss" scoped>
.mainTitle-iaic {
  width: 100%;
  margin-bottom: 24px;
  font-family: var(--cn);

  .title-left {
    display: flex;
    align-items: center;
    gap: 12px;

    .iaic-bar {
      width: 4px;
      height: 22px;
      background: var(--grad);
      border-radius: 2px;
    }

    .title-text {
      font-size: 22px;
      font-weight: 800;
      color: var(--ink);
      margin: 0;
      letter-spacing: -0.01em;
    }
  }

  .more-btn {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    font-size: 13.5px;
    font-weight: 600;
    color: var(--azure);
    cursor: pointer;
    padding: 6px 12px;
    border-radius: 8px;
    transition: all 0.2s;

    &:hover {
      background: var(--sky-2);
      .arrow {
        transform: translateX(3px);
      }
    }

    .arrow {
      transition: transform 0.2s;
    }
  }
}
</style>
