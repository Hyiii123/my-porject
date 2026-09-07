<!-- 架构页面 -->
<template>
  <div class="layoutsWrapper">
    <LeftSide></LeftSide>
    <div class="contain fx-1">
      <Header></Header>
      <router-view class="mainWrapper" />
    </div>
  </div>
</template>
<script setup>
import LeftSide from "./components/LeftSide.vue";
import Header from "./components/Header.vue";
</script>
<style lang="scss" scoped>
.layoutsWrapper {
  min-height: 100vh;
  display: flex;
  background-color: var(--color-background);

  .contain {
    padding-left: 220px;
    width: 100%;
    min-width: 0;
    overflow-x: hidden;
  }
  .mainWrapper {
    padding-top: 64px;
    padding-left: 24px;
    padding-right: 24px;
    padding-bottom: 32px;
    box-sizing: border-box;
  }
}
</style>