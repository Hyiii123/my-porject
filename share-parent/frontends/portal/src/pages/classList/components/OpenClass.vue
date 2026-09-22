<!-- 课程分类精选卡片网格 -->
<template>
  <div class="openClass">
    <MainTitle class="marg-bt-20" :title="title"></MainTitle>
    <div class="classCont">
      <ClassCards
        v-for="(item, index) in data"
        :key="item.id || index"
        :data="item"
        class="card-item"
      ></ClassCards>
    </div>
  </div>
</template>

<script setup>
import MainTitle from "@/components/MainTitle.vue";
import ClassCards from "@/components/ClassCards.vue";

const props = defineProps({
  title: {
    type: String,
    default: "模块标题",
  },
  data: {
    type: Array,
    default: () => [],
  },
});
</script>

<style lang="scss" scoped>
.openClass {
  padding-bottom: 40px;

  .classCont {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 22px;
  }
}

@media (max-width: 1200px) {
  .openClass .classCont {
    grid-template-columns: repeat(3, 1fr);
    gap: 18px;
  }
}

@media (max-width: 768px) {
  .openClass .classCont {
    grid-template-columns: repeat(2, 1fr);
    gap: 14px;
  }
}

@media (max-width: 480px) {
  .openClass .classCont {
    grid-template-columns: 1fr;
  }
}
</style>
