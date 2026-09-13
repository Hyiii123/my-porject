<!-- 发布成功页面 -->
<template>
  <div class="success container bg-wt fx-fd-col fx-ct">
    <IconSuccess></IconSuccess>
    <div class="title">发布成功</div>
    <div class="ft-14 marg-bt-20 ft-cl-des">
      请耐心等待，肯定会有很多热心的同学回答你的问题。
    </div>
    <div @click="handleNavigate" class="ask bt ft-14">{{ btnText }}</div>
  </div>
</template>

<script setup>
import { computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import IconSuccess from "@/assets/icon_success.svg";

const route = useRoute();
const router = useRouter();

const btnText = computed(() => route.query.btnText || '返回查看');

const handleNavigate = () => {
  if (route.query.to) {
    router.push(route.query.to);
  } else {
    router.go(-2);
  }
};
</script>
<style lang="scss">
.success {
  margin: 30px auto 40px auto;
  border-radius: 8px;
  padding: 60px 20px;
  .title {
    font-weight: 600;
    font-size: 24px;
    margin: 20px 20px 10px 20px;
  }
  .ask {
    min-width: 122px;
    padding: 0 16px;
    height: 36px;
    line-height: 36px;
    cursor: pointer;
  }
}
</style>
