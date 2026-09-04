<!-- 我的考试 -->
<template>
  <div class="myExamDetails">
     <BreadCrumb></BreadCrumb>
     <div class="examHeadle">
      <div class="tit">考试批阅</div>
      <div class="table">
        <div class="fx-sb">
          <div class="td fx-1">
            <div class="marg-bt-10 ft-wt-600 ft-cl-1">所属课程</div>
            <div>{{record.courseName || route.query.courseName || '未关联课程'}}</div>
          </div>
          <div  class="td fx-1">
            <div class="marg-bt-10 ft-wt-600 ft-cl-1">测试名称</div>
            <div>{{record.sectionName || route.query.sectionName || '阶段测试'}}</div>
          </div>
          <div  class="td fx-1">
            <div class="marg-bt-10 ft-wt-600 ft-cl-1">学员名称</div>
            <div>{{stroe.getUserInfo?.nickname || stroe.getUserInfo?.name || '学习者'}}</div>
          </div>
        </div>
        <div class="fx-sb">
          <div class="td fx-1">
            <div class="marg-bt-10 ft-wt-600 ft-cl-1">所用时长</div>
            <div>{{record.duration ? timeFormat(record.duration) : '00 : 00 : 00'}} </div>
          </div>
          <div  class="td fx-1">
            <div class="marg-bt-10 ft-wt-600 ft-cl-1">提交时间</div>
            <div>{{record.commitTime || record.submittedAt || '--'}}</div>
          </div>
          <div  class="td fx-1">
            <div class="marg-bt-10 ft-wt-600 ft-cl-1">总 分 数</div>
            <div>{{record.score || 0}} / {{total}}</div>
          </div>
        </div>
      </div>
     </div>
     <div class="answerCardTitle" v-if="myExamDetails.length">答题卡</div>
     <div class="answerCards">
      <span v-for="(item, index) in myExamDetails" :class="{right:item.correct,wrong:!item.correct && item.answer != ''}">{{index + 1}}</span>
     </div>
     <div class="examCont" >
        <div class="item" v-for="(item, index) in myExamDetails">
        <div class="examTitle">
          <div>
            <img v-if="item.correct" src="@/assets/icon_right.png" alt="">
            <img v-else src="@/assets/icon_wrong.png" alt="">
          </div>
        <div class="quest fx">
            {{index+1}}. <span v-html="item.question.title || item.question.stem"></span>
          </div>
        </div>
        <div class="answer">
          <li v-for="(it, optionIndex) in normalizedOptions(item.question.options)" :key="optionIndex"><span v-html="it"></span></li>
        </div>
        <div class="analysis">
          <div class="fx marg-bt-20">
            <div class="col ft-wt-600">你的答案：{{answerChange(item.question.type, item.answer)}}</div>
            <div class="col rt ft-wt-600">正确答案：{{answerChange(item.question.type, item.question.answer)}}</div>
            <div class="col">难易程度：{{defficultyChange(item.question.difficulty)}}</div>
            <div>得分：{{item.score}}</div>
          </div>
          <div class="fx" v-if="item.question.analysis">答案解析：<span v-html="item.question.analysis"></span></div>
        </div>
        </div>
     </div>
  </div>
</template>
<script setup>

/** 数据导入 **/
import { onMounted, ref } from "vue";
import { ElMessage } from "element-plus";
import { getExamRecordDetails } from "@/api/class.js";
import { upperAlpha, timeFormat } from "@/utils/tool.js"
import { useRoute } from "vue-router";
import { useUserStore } from '@/store'


// 组件导入
import BreadCrumb from './components/BreadCrumb.vue'

const stroe = useUserStore()
const route = useRoute();

// mounted生命周期
onMounted(async () => {
  // 查询我的考试记录
  getExamDetailsData()
});

/** 方法定义 **/

// 查询我的考试记录
const total = ref(0)
// 查询我的详情
const myExamDetails = ref([])
const record = ref({})
const getExamDetailsData = async () => {
  await getExamRecordDetails(route.query.id)
    .then((res) => {
      if (res.code == 200 && res.data != null){
        record.value = res.data
        myExamDetails.value = res.data.details || res.data.list || []
        total.value = Number(res.data.totalScore || 0) || myExamDetails.value.reduce((sum, item) => sum + Number(item.question?.score || 0), 0)
      }
    })
    .catch(() => {
      ElMessage({
        message: "最近学习数据请求出错！",
        type: 'error'
      });
    });
}
// 问题类型，1：单选题，2：多选题，3：不定向选择题，4：判断题，5：主观题
const answerChange = (type, val) => {
  let data = ''
  switch (parseInt(type)){
    case 1 : {
      data = isNaN(Number(val)) ? val : upperAlpha(Number(val))
      break
    }
    case 2:
    case 3: {
      const arr = typeof val == 'string' ? val.split(',') : val
      data = (Array.isArray(arr) ? arr : [arr]).map(n => isNaN(Number(n)) ? n : upperAlpha(Number(n))).join(',')
      break
    }
    case 4 : {
      data = val  ? '正确' : '错误'
      break
    }
    case 5 : {
      data = val
      break
    }
  }
  return data
}
const normalizedOptions = (value) => {
  if (Array.isArray(value)) return value
  if (!value) return []
  try { return JSON.parse(value) }
  catch (error) { return String(value).split(',') }
}
const defficultyChange = (item) => {
 return item == 1 ? '简单' : item == 2 ? '中等' : '困难'
}
</script>
<style lang="scss" src="./index.scss"> </style>
