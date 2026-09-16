<!--  我的课程 - 卡片  -->
<template>
  <div class="classCards fx-sb fx-ct">
    <div class="marg-rt-20">
      <img :src="data.courseCoverUrl || data.coverUrl || data.cover || defaultCover" alt="课程封面" @error="handleImgError($event, data)" @click="$router.push({path: '/details/index', query: {id: data.courseId || data.id}})">
    </div>
    <div class="info fx-1">
      <div class="tit">{{ data.courseName || data.name || data.title || '精品在线课程' }}</div>
      <div>
        <span>有效日期：</span>
        {{ data.expireTime ? (new Date(data.createTime || Date.now()).toLocaleDateString() + ' - ' + new Date(data.expireTime).toLocaleDateString()) : '永久有效' }}
      </div>
      <div>
        <span>已学习：</span>
        <em>{{ data.learnedSections ?? data.completedLessons ?? 0 }}</em> / {{ data.sections || data.totalLessons || 0 }} 节
      </div>
      <div v-if="type == '1'">
        <span>正在学习：</span>
        <template v-if="data.latestSectionName">
          第{{ data.latestSectionIndex || 1 }}节 {{ data.latestSectionName }}
        </template>
        <template v-else-if="data.sections || data.totalLessons">
          第1节 课程导学
        </template>
        <template v-else>
          尚未开始学习
        </template>
      </div>
    </div>
    <div class="btnCont">
      <div class="btn" v-if="type == '1'" @click="() => $router.push({path: '/learning/index', query: {id: data.courseId || data.id, courseId: data.courseId || data.id}})">
        <span class="bt bt-round">继续学习</span>
      </div>
      <div class="btn" v-if="type == '2' && data.status != 3" @click="() => $router.push({path: '/learning/index', query: {id: data.courseId || data.id, courseId: data.courseId || data.id}})">
        <span class="bt bt-round" v-if="data.status == 0">马上学习</span>
        <span class="bt bt-round" v-if="data.status == 1">继续学习</span>
        <span class="bt bt-round" v-if="data.status == 2">重新学习</span>
      </div>
      <div class="btn" v-if="type == '2' && data.status != 3 && data.planStatus == 0" @click="planActive(data, 'add')">
        <span class="bt-grey bt-round">创建计划</span>
      </div>
      <div class="btn" v-if="type == '2' && data.status != 3 && data.planStatus == 1" @click="planActive(data, 'edit')">
        <span class="bt-grey bt-round">修改计划</span>
      </div>
      <div class="btn" v-if="type == '2' && data.status == 3" @click="planActive(data, 'del')">
        <span class="bt-grey bt-round">删除课程</span>
      </div>
    </div> 
  </div>
</template>
<script setup>
import defaultCover from '@/assets/images/courses/default-cover.svg?url'

const handleImgError = (e, item) => {
  if (item) {
    item.courseCoverUrl = defaultCover
    item.coverUrl = defaultCover
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
  },
  type:{
    type: String,
    default: '1'
  }
})  
const emit = defineEmits(['planHandle'])

const planActive = (it, type) => {
  emit('planHandle', {data: it, type})
}

</script>
<style lang="scss" scoped>
.classCards{
  img{
    width: 236px;
    height: 132px;
    border-radius: 8px;
  }
  .info{
    line-height: 30px;
    font-size: 14px;
    em{
      font-style: normal;
      color: var(--color-main);
    }
    span{
      display: inline-block;
      color: var(--color-font3);
      min-width: 85px;
    }
    .tit{
      font-size: 20px;
      font-weight: 500;
      line-height: 40px;
    }
  }
  
  .btnCont{
    display: flex;
    flex-direction: column;
    flex-wrap: wrap;
     .btn{
        width: 114px;
        height: 40px;
        display: flex;
        align-items: center;
        margin: 10px 0;
      }
  }
  
}
</style>
