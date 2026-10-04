<!--  我的课程 - 卡片  -->
<template>
  <div class="orderCards">
    <img :src="getCover(data)" alt="" @error="handleImgError($event, data)" @click="() => $router.push({path: '/details/index', query:{id: data.courseId}})">
    <div class="info">
      <p class="tit">{{data.name}}</p>
      <p>{{data.price === 0 ?  '免费' : '¥' + (data.price / 100).toFixed(2)}}</p>
    </div>
  </div>
</template>
<script setup>
import defaultCover from '@/assets/images/courses/default-cover.svg?url'

const getCover = (item) => {
  if (item && item.courseId && item.courseId > 0) {
    return `/courses/course_${item.courseId}.png`
  }
  return item?.coverUrl || item?.courseCoverUrl || item?.cover || defaultCover
}

const handleImgError = (e, item) => {
  if (item) {
    item.coverUrl = defaultCover
    item.courseCoverUrl = defaultCover
    item.cover = defaultCover
  }
  if (e?.target && e.target.src !== defaultCover) {
    e.target.src = defaultCover
  }
}

// 接收父组件传来的标题
defineProps({
  data:{
    type: Object,
    default: () => ({})
  }
})  
</script>
<style lang="scss" scoped>
.orderCards{
  padding: 15px 0;
  display: flex;
  align-items: center;
  img{
    width: 120px;
    height: 68px;
    border-radius: 8px;
    object-fit: cover;
    margin-right: 20px;
    box-shadow: 0 1px 4px rgba(15, 23, 42, 0.08);
  }
  .info{
    line-height: 32px;
    font-size: 14px;
    .tit{
      font-size: 16px;
      font-weight: 500;
    }
  }
}
</style>
