<!--课程题目-->
<template>
  <div class="contentBox">
    <div class="courseList">
      <el-collapse accordion v-model="activeNames">
        <el-collapse-item v-for="(item, index) in itemData" :key="index">
          <template v-slot:title>
            <div class="titText">
              <span class="icon" v-if="item.sections.length > 0"></span>
              <div class="textL">
                <span
                  ><span v-if="index + 1 > 9">{{ index + 1 }}</span
                  ><span v-else>{{ "0" + (index + 1) }}</span></span
                >
                <span>{{ item.name }}</span>
              </div>
              <div class="textR">
                <span class="textForbidden">添加阶段考试</span>
              </div>
            </div>
          </template>
          <div class="itemCon" v-if="item.sections.length > 0">
            <div class="headTitle">
              <span>序号</span>
              <span>小节名称</span>
              <span>题目</span>
              <span>题目数目</span>
              <span>题目分数</span>
              <span>操作</span>
            </div>
            <div class="item">
              <ul>
                <li v-for="(val, i) in item.sections" :key="i">
                  <div class="leftLine"></div>
                  <div class="con">
                    <!-- 序号 -->
                    <div>
                      <div v-if="val.type !== 3">
                        <span v-if="i + 1 > 9">{{ i + 1 }}</span
                        ><span v-else>{{ "0" + (i + 1) }}</span>
                      </div>
                    </div>
                    <div>
                      <span>{{ val.name }}</span>
                    </div>
                    <div>
                      <span
                        @click="handleWatch(val)"
                        :class="
                          val.subjectNum > 0 ? 'textDefault' : 'textForbidden'
                        "
                      >
                        查看题目</span
                      >
                    </div>
                    <div>
                      {{ val.subjectNum }}
                    </div>
                    <div>
                      {{ val.totalScore }}
                    </div>
                    <div>
                      <span class="textForbidden" v-if="val.type === 3"
                        >删除阶段考试</span
                      >
                    </div>
                  </div>
                </li>
              </ul>
            </div>
            <div class="cover"></div>
          </div>
        </el-collapse-item>
      </el-collapse>
    </div>
    <!-- 查看题目弹层 -->
    <detailwatchTopic
      :dialogVisible="dialogVisible"
      :jectIds="jectIds"
      :score="score"
      @detailwatchTopicInfo="detailwatchTopicInfo"
      @handleClose="handleWatchClose"
    ></detailwatchTopic>
    <!-- end -->
    <!-- 删除弹层 -->
    <!-- end -->
  </div>
</template>
<script setup>
import { ref, watch } from "vue";
import { useRoute } from "vue-router";
import { getSubjects } from "@/api/curriculum";
// 导入组件
// 查看题目弹层
import detailwatchTopic from "./detailwatchTopic.vue";
// ------定义变量------
// 获取父组件值、方法
const props = defineProps({
  courseTopicData: {
    type: Array,
    default: () => [],
  },
});
const dialogVisible = ref(false); //弹层隐藏显示
const itemData = ref([]); //目录数据
const activeNames = ref(["1"]);
const chapterId = ref(null); //当前触发的章id
const courseId = useRoute().params.id;
// 详情接口通过目录编号查询题目，因此这里保存当前目录编号而不是响应式数组。
const jectIds = ref(null);
const score = ref(0); //当前小节的分数

watch(
  () => props.courseTopicData,
  async (value) => {
    const catalogs = Array.isArray(value) ? value : [];
    if (catalogs.length === 0) {
      itemData.value = [];
      return;
    }
    try {
      const response = await getSubjects({ id: courseId });
      const groups = response?.code === 200 && Array.isArray(response.data)
        ? response.data
        : [];
      const relations = new Map(
        groups.map((group) => [
          String(group.cataId ?? group.catalogId),
          {
            subjectIds: Array.isArray(group.subjectIds) ? group.subjectIds : [],
            totalScore: Array.isArray(group.subjects)
              ? group.subjects.reduce((total, subject) => total + (Number(subject.score) || 0), 0)
              : 0,
          },
        ])
      );
      itemData.value = catalogs.map((chapter) => ({
        ...chapter,
        sections: (chapter.sections || []).map((section) => {
          const relation = relations.get(String(section.id));
          const subjectIds = relation?.subjectIds || section.subjectIds || [];
          return {
            ...section,
            subjectIds,
            subjectNum: subjectIds.length,
            totalScore: relation?.totalScore || Number(section.totalScore) || 0,
          };
        }),
      }));
    } catch (error) {
      itemData.value = catalogs;
    }
  },
  { immediate: true, deep: true }
);

// 获取设置题目的ids
const detailwatchTopicInfo = (data) => {
  itemData.value.forEach((val) => {
    (val.sections || []).forEach((ele) => {
      if (ele.id === chapterId.value) {
        ele.subjectIds = Array.isArray(data?.value) ? data.value : [];
        ele.subjectNum = ele.subjectIds.length;
        ele.totalScore = data?.totalScore || 0;
        // subjectData.push(JSON.parse(JSON.stringify(ele)))
        // arr.push(ele)
      }
    });
  });
  // subjectData.value = arr;
  // console.log(itemData.value);
};
// 打开查看题目弹层
const handleWatch = (obj) => {
  const subjectCount = Number(obj.subjectNum || obj.subjectIds?.length || 0);
  if (subjectCount <= 0) return;
  chapterId.value = obj.id;
  score.value = Number(obj.totalScore) || 0; //分数
  jectIds.value = obj.id;
  dialogVisible.value = true;
};
// 关闭设置题目弹层
const handleWatchClose = () => {
  // jectIds.value = []
  dialogVisible.value = false;
};

</script>
<style lang="scss" scoped>
.courseList .titText .textL span {
  font-size: 16px;
  color: #332929;
}
.headTitle {
  color: #332929;
}
.textR {
  font-size: 14px;
  font-family: PingFangSC-Regular;
  font-weight: 400;
}
</style>
