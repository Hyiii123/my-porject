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
      height: 24px;
      background: linear-gradient(180deg, #38BDF8 0%, #818CF8 100%);
      border-radius: 999px;
      box-shadow: 0 0 12px rgba(56, 189, 248, 0.5);
    }

    .title-text {
      font-size: 24px;
      font-weight: 800;
      color: #FFFFFF;
      margin: 0;
      letter-spacing: -0.025em;
      font-family: var(--display);
    }
  }

  .more-btn {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    font-size: 13.5px;
    font-weight: 600;
    color: #38BDF8;
    background: rgba(56, 189, 248, 0.08);
    border: 1px solid rgba(56, 189, 248, 0.2);
    cursor: pointer;
    padding: 6px 14px;
    border-radius: 999px;
    transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);

    &:hover {
      background: rgba(56, 189, 248, 0.16);
      border-color: #38BDF8;
      color: #7DD3FC;
      transform: translateY(-1px);
      box-shadow: 0 4px 16px -2px rgba(56, 189, 248, 0.3);

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