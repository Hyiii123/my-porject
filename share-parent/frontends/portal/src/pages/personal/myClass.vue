<!-- 个人中心 - 我的课程 -->
<template>
  <div class="myClassWrapper" v-loading="loading">
    <div v-if="hasData">
      <!-- 最近学习 -->
      <div class="personalCards" v-if="learningData != null && typeof(learningData) != 'string'">
        <CardsTitle class="marg-bt-20" title="最近学习"/>
        <ClassCards :data="learningData" type="1"/>
      </div>
      <!-- 学习计划 -->
      <div class="personalCards" v-if="planData && planData.length > 0 && typeof(planData) != 'string'">
        <CardsTitle title="学习计划">
          <div class="ft-wt-400"><span
              class="marg-rt-20">本周计划：<em>{{ weekFinishedAmount || 0 }}</em> / {{ weekPlanAmount || 0 }}</span> <span>积分奖励：<em>{{ totalPoints || 0 }}</em></span>
          </div>
        </CardsTitle>
        <PlanTable :data="planData"></PlanTable>
      </div>
      <!-- 全部课程 -->
      <div id="allClass" v-if="myClassData != null && myClassData.length > 0">
        <div class="personalCards">
          <CardsTitle class="marg-bt-20" title="全部课程"/>
          <div class=""><span></span></div>
          <div class="item marg-bt-20" v-for="item in myClassData" :key="item.id || item.courseId">
            <ClassCards :data="item" @planHandle="planHandle" type="2"/>
          </div>
        </div>
        <div v-if="count > 10" class="fx-ct ft-18 ft-wt-600">查看全部</div>
      </div>
    </div>

    <!-- 空状态 -->
    <div class="empty-state" v-else-if="!loading">
      <el-empty description="您暂未加入任何课程，快去挑选心仪的课程开启学习吧！">
        <el-button type="primary" @click="$router.push('/search/index')">去选课</el-button>
      </el-empty>
    </div>
    <el-dialog
        v-model="dialogVisible"
        :title="title"
        width="30%"
    >
      <div class="dialogCont">
        <div class="fx marg-bt-20"><span>每周学习节数:</span>
          <el-input @input="planDayHandle" v-model="number" min="1" type="number"></el-input>
        </div>
        <div class="fx"><span>预计学完时间:</span>
          <div class="lastTime">{{ lastTime }}</div>
        </div>
      </div>
      <template #footer>
          <span class="dialogFooter">
            <div @click="dialogVisible = false"><span class="bt bt-grey">取消</span></div>
            <div @click="createPlan"><span class="bt">确定</span></div>
          </span>
      </template>
    </el-dialog>
  </div>
</template>
<script setup>

/** 数据导入 **/
import {computed, onMounted, ref, reactive} from "vue";
import {ElMessage} from "element-plus";
import {getMyLearning, getMylessons, getMyPlan, creatPlans, delMyClass} from "@/api/class.js";
import {useRoute} from "vue-router";
import {dataCacheStore} from "@/store"
import moment from 'moment'
// 组件导入
import CardsTitle from './components/CardsTitle.vue'
import ClassCards from './components/ClassCards.vue'
import PlanTable from './components/PlanTable.vue'

const route = useRoute()
const store = dataCacheStore()
const loading = ref(false)

const hasData = computed(() => {
  return (learningData.value != null && typeof learningData.value !== 'string') ||
         (planData.value && planData.value.length > 0) ||
         (myClassData.value && myClassData.value.length > 0)
})

// mounted生命周期
onMounted(async () => {
  loading.value = true
  try {
    await Promise.allSettled([
      getLearningData(),
      getMylessonsData(),
      getPlanData()
    ])
  } finally {
    loading.value = false
  }
})

/** 方法定义 **/

// 获取最近学习计划数据
const learningData = ref(null)
const getLearningData = async () => {
  try {
    const res = await getMyLearning()
    if (res && res.code == 200 && res.data != null) {
      learningData.value = res.data
      store.setMyLearnClassInfo(res.data)
    } else {
      learningData.value = null
    }
  } catch (err) {
    console.warn('获取最近学习数据:', err)
    learningData.value = null
  }
}

// 获取我的学习计划
const planData = ref([]) // 列表数据
const planTotal = ref(0) // 总条数
const weekFinishedAmount = ref(0) // 本周完成的计划数量
const totalPoints = ref(0) // 本周积分值
const weekPlanAmount = ref(0) // 本周总的计划数量
const planParams = {
  page: 1,
  pageSize: 10,
}

// 获取计划数据
const getPlanData = async () => {
  try {
    const res = await getMyPlan(planParams)
    if (res && res.code == 200 && res.data != null) {
      planData.value = res.data.list || []
      planTotal.value = res.data.total || 0
      weekFinishedAmount.value = res.data.weekFinished || 0
      totalPoints.value = res.data.weekPoints || 0
      weekPlanAmount.value = res.data.weekTotalPlan || 0
    } else {
      planData.value = []
    }
  } catch (err) {
    console.warn('获取学习计划数据:', err)
    planData.value = []
  }
}
const days = ref(0)
const number = ref(1)
const lastTime = computed(() => {
  // 学完的时间按周 每周学n节 m = n/总节数 不足一周按一周算 从今天开始往后延 m*7天 
  const num = Math.ceil(days.value / number.value) * 7
  return number.value ? moment().add(num, 'days').format("YYYY-MM-DD") : ''
})
// 处理计划天数不能小于1
const planDayHandle = val => {

  val != '' && val < 1 ? number.value = 1 :
      val != '' && val > 50 ? number.value = 50 : null

}

const dialogVisible = ref(false)
const title = ref('创建计划')
const currentData = ref();
// 打开创建、修改弹窗
const planHandle = (val) => {
  const {data, type} = val
  dialogVisible.value = true
  currentData.value = data
  if (type == 'edit') {
    number.value = data.weekFreq
    days.value = data.sections
    title.value = '修改计划'
  } else if (type == 'add') {
    days.value = data.sections
    title.value = '创建计划'
  } else if (type == 'del') {
    delMyClassData(data.course.id)
  }
}
// 创建、修改计划
const createPlan = async () => {
  const params = {
    freq: number.value,
    courseId: currentData.value ? currentData.value.courseId : ''
  }
  await creatPlans(params)
      .then((res) => {
        if (res.code == 200) {
          getPlanData()
          ElMessage({
            message: `${title.value}成功`,
            type: 'success'
          });
          dialogVisible.value = false
        }
      })
      .catch(() => {
        ElMessage({
          message: "最近学习数据请求出错！",
          type: 'error'
        });
      });
}
// 删除课程表 - 我的课程下的课程删除
const delMyClassData = async (id) => {
  try {
    const res = await delMyClass(id)
    if (res && res.code == 200) {
      ElMessage.success('课程已成功移除')
      await getMylessonsData()
    }
  } catch (err) {
    console.error('删除课程失败:', err)
    ElMessage.error('删除课程失败，请稍后重试')
  }
}

// 我的课程
const myClassData = ref(null)
const count = ref(0)
const params = {
  page: 1,
  pageSize: 10,
}

// 查询我的课
const getMylessonsData = async () => {
  try {
    const res = await getMylessons(params)
    if (res && res.code == 200 && res.data != null) {
      myClassData.value = res.data.list || []
      count.value = Number(res.data.total || 0)
    } else {
      myClassData.value = []
    }
  } catch (err) {
    console.warn('查询课程列表:', err)
    myClassData.value = []
  }
}

</script>
<style lang="scss" src="./index.scss"></style>
