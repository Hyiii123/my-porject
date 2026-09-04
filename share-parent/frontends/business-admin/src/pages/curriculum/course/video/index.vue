<!--课程视频-->
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
            </div>
          </template>
          <div class="itemCon" v-if="item.sections.length > 0">
            <div class="headTitle">
              <span>序号</span>
              <span style="margin-left: 14px">小节名称</span>
              <span class="textLeft">视频名称</span>
              <span>视频时长</span>
              <span>免费试看</span>
            </div>
            <div class="item">
              <ul>
                <li v-for="(val, i) in item.sections" :key="i">
                  <div class="leftLine"></div>
                  <div class="con">
                    <!-- 序号 -->
                    <div>
                      <span v-if="i + 1 > 9">{{ i + 1 }}</span
                      ><span v-else>{{ "0" + (i + 1) }}</span>
                    </div>
                    <div style="margin-left: 14px; color: #332929">
                      {{ val.name }}
                    </div>
                    <div class="videoName">
                      <div v-if="val.mediaName !== ''"  class="textLeft">
                        <span @click="handleSeeVideo(val.mediaId)"
                          >{{ ellipsis(val.mediaName,8) }} .mp4</span
                        >
                        <i
                          class="deleteIcon"
                          @click="handleDelete(val)"
                          style="margin: 0 0 4px 4px"
                        ></i>
                      </div>
                      <div v-else class="textLeft">
                        <span
                          class="textDefault"
                          @click="handleOpen(val.id)"
                          style="margin-left: 0px"
                          >选择视频</span
                        ><span class="textDefault">
                          <el-upload
                            class="upload-demo"
                            action="#"
                            :multiple="true"
                            :http-request="
                              (param) => httpRequest(param, val.id)
                            "
                            :accept="accept"
                            :limit="100"
                            :show-file-list="false"
                            :on-remove="handleRemove"
                            :on-change="() => handleChange(val.id)"
                            :on-progress="handleProgress"
                            :file-list="fileList"
                          >
                            <el-button size="small" type="primary"
                              >本地上传</el-button
                            >
                          </el-upload></span
                        >
                      </div>
                    </div>
                    <div>
                      {{
                        val.mediaDuration > 0
                          ? formatSeconds(val.mediaDuration)
                          : ""
                      }}
                    </div>
                    <div class="textWarning">
                      <el-switch
                        v-model="val.trailer"
                        active-color="#00BE76"
                        active-text="试看3分钟"
                        :disabled="free"
                        @change="handleTrailer($event, val)"
                      >
                      </el-switch>
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
    <!-- 添加媒资视频弹层 -->
    <AddVideo
      :dialogVisible="dialogVisible"
      :itemData="itemData"
      @setVideoInfo="setVideoInfo"
      @handleClose="handleClose"
    ></AddVideo>
    <!-- end -->
    <!-- 预览弹层 -->
    <Preview
      ref="preview"
      :title="title"
      :mediaId="mediaId"
      :dialogFormVisible="dialogFormVisible"
      @handleClose="handlePreviewClose"
    ></Preview>
    <!-- end -->
  </div>
</template>
<script setup>
import { ref, reactive, onMounted, nextTick, watch } from "vue"
import { useRouter, useRoute } from "vue-router"
import { ElMessage, ElLoading } from "element-plus"
import { formatSeconds,ellipsis } from '@/utils/index'
// 接口
import {
  getCoursesCatalogue,
  baseVideoSave,
  getCoursesDetail,
} from "@/api/curriculum"
import { mediaUpload, uploadFile } from "@/api/media"
// 导入组件
// 删除弹层
import Delete from "@/components/Delete/index.vue"
// 添加视频弹层
import AddVideo from "./addVideo.vue"
// 预览弹层
import Preview from "@/components/Preview/index.vue"
// ------定义变量------
// 获取父组件值、方法
const props = defineProps({
  // // 课程id
  // courseId: {
  //   type: Number,
  //   default: 0,
  // },
})
const router = useRouter() //获取全局
const route = useRoute() //获取局部
const emit = defineEmits() //子组件获取父组件事件传值
let itemData = ref([]) //目录数据
let activeNames = ref(["1"])
let dialogVisible = ref(false) //选择视频列表弹层
let dialogFormVisible = ref(false) //弹层隐藏显示
let sectionId = ref("") //小节id
let uploaderG = ref(null)//定义全局用于取消上传
let videoName = ref("") //上传的视频名称
const loadingInstance = ref(null)
let free = ref(false) //是否免费
let courseId = route.params.id //课程id
let mediaId = ref("")//视频id
let preview = ref(null)
const title = "视频预览"
const accept = "video/mp4,video/quicktime"
const fileList = ref([])
const startLoading = () => {
  loadingInstance.value = ElLoading.service({
    lock: true,
    text: "视频上传中…",
    background: "rgba(51, 51, 51, 0.4)",
  })
}
// ------生命周期------
onMounted(() => {
  getCatalogue()//获取目录数据
  getDetailData()//
})
// ------定义方法------
// 获取目录数据
const getCatalogue = async () => {
  let params = {
    id: courseId,
    see: false,
    withPractice: 0 //是否带着练习题，1：带着练习题，0：不带练习题，默认1
  }
  await getCoursesCatalogue(params)
    .then((res) => {
      if (res.code === 200) {
        itemData.value = Array.isArray(res.data) ? res.data : []
      }
    })
    .catch((err) => { })
}
// 获取详情
let getDetailData = async () => {
  await getCoursesDetail(courseId)
    .then((res) => {
      if (res.code === 200) {
        free.value = res.data.free
      }
    })
    .catch((err) => { })
}
// 提交
const handleSubmit = async (str) => {
  let arr = []
  let data = {}
  itemData.value.map((obj) => {
    obj.sections.map((val) => {
      data = {
        cataId: val.id,
        mediaId: val.mediaId,
        trailer: val.trailer,
        videoName: val.mediaName,
        mediaDuration: val.mediaDuration,
      }
      arr.push(data)
    })
  })
  let params = {
    datas: arr,
    id: courseId,
  }
  await baseVideoSave(params)
    .then((res) => {
      if (res.code === 200) {
        ElMessage({

          message: "恭喜你，操作成功！",
          type: "success",
          showClose:false,
        })
        emit("getActive", 3)
        if (str === "getback") {
          router.push({
            path: "/curriculum/index",
          })
        }
      } else {
        ElMessage({

          message: res.data.msg,
          type: "error",
          showClose:false,
        })
      }
    })
    .catch((err) => { })
}
// 当前文件服务使用本地卷上传，不依赖腾讯云媒资签名。
const httpRequest = async (file, id) => {
  sectionId.value = id
  // 限制视频格式
  if (["video/mp4", "video/quicktime"].indexOf(file.file.type) == -1) {
    ElMessage({

      message: "仅支持上传mp4格式的文件!",
      type: "error",
      showClose:false,
    })
    return false
  }

  startLoading()
  videoName.value = file.file.name //获取视频名称
  try {
    const uploadResponse = await uploadFile(file.file)
    if (uploadResponse?.code !== 200 || !uploadResponse.data) {
      throw new Error(uploadResponse?.msg || "文件上传失败")
    }
    const uploaded = uploadResponse.data
    const mediaResponse = await mediaUpload({
      mediaName: videoName.value,
      fileName: uploaded.name || videoName.value,
      fileUrl: uploaded.url || "",
      url: uploaded.url || "",
      sizeBytes: file.file.size,
      durationSeconds: 0,
      mediaType: "video",
      format: videoName.value.split(".").pop() || "MP4",
    })
    if (mediaResponse?.code !== 200 || !mediaResponse.data) {
      throw new Error(mediaResponse?.msg || "媒资记录保存失败")
    }
    const data = mediaResponse.data
    ElMessage({
      message: "上传成功",
      type: "success",
      showClose: false,
    })
    itemData.value.forEach((obj) => {
      (obj.sections || []).forEach((ele) => {
        if (sectionId.value === ele.id) {
          ele.mediaName = data.filename || data.fileName || videoName.value
          ele.mediaId = data.id
          ele.mediaDuration = data.mediaDuration || data.durationSeconds || 0
        }
      })
    })
  } catch (error) {
    ElMessage({
      message: error?.message || "视频上传失败",
      type: "error",
      showClose: false,
    })
  } finally {
    if (loadingInstance.value) {
      loadingInstance.value.close()
      loadingInstance.value = null
    }
  }
}
// 打开选择视频列表弹层
const handleOpen = (id) => {
  sectionId.value = id
  dialogVisible.value = true
}
// 上传视频前获取小节id
const handleChange = (id) => {
  sectionId.value = id
}
const handleRemove = () => {
  fileList.value = []
}
const handleProgress = () => {}
//获取视频信息
const setVideoInfo = (value) => {
  itemData.value.map((obj) => {
    obj.sections.map((val) => {
      if (sectionId.value === val.id) {
        val.mediaName = value.filename
        val.mediaDuration = value.duration
        val.mediaId = value.id
      }
    })
  })
}
// 是否观看
const handleTrailer = (e, value) => {
  itemData.value.map((obj) => {
    obj.sections.map((val) => {
      if (value.id === val.id) {
        val.trailer = e
      }
    })
  })
}
// 关闭视频 弹层
const handleClose = () => {
  dialogVisible.value = false
}
// 删除视频
const handleDelete = (value) => {
  itemData.value.map((obj) => {
    obj.sections.map((val) => {
      if (value.id === val.id) {
        val.mediaName = ""
        val.mediaDuration = ""
        val.mediaId = null
      }
    })
  })
}
// 视频观看
const handleSeeVideo = (id) => {
  mediaId.value = id
  preview.value.getId(id)
  dialogFormVisible.value = true
}
// 关闭弹层
const handlePreviewClose = () => {
  dialogFormVisible.value = false
};
// 向父组件暴露方法
defineExpose({
  handleSubmit,
});
</script>
<style lang="scss" scoped>
:deep(.videoBox .el-dialog){
  width: 1096px;
}
.courseList .el-collapse, .courseList .el-collapse-item__wrap{
  min-width: 1065px;
}
:deep(.videoBox .el-dialog){
  min-width: 1096px !important;
}
</style>
