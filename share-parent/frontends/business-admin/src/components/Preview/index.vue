<!-- 媒资预览。当前文件服务使用本地卷，不依赖腾讯云 TCPlayer。 -->
<template>
  <el-dialog
    :model-value="dialogFormVisible"
    :title="title || '视频预览'"
    width="800px"
    @close="handleClose"
  >
    <div class="video-box" v-loading="loading">
      <video
        v-if="videoUrl"
        ref="videoRef"
        class="video-element"
        controls
        playsinline
        preload="metadata"
        :src="videoUrl"
        @error="handleVideoError"
      />
      <el-empty v-else-if="!loading" description="该媒资暂无可播放地址" />
    </div>
    <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>
  </el-dialog>
</template>
<script setup>
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getMediasSignature } from '@/api/media'

const props = defineProps({
  dialogFormVisible: { type: Boolean, default: false },
  mediaId: { type: [String, Number], default: '' },
  title: { type: String, default: '视频预览' },
})

const emit = defineEmits(['handleClose'])
const videoRef = ref(null)
const videoUrl = ref('')
const loading = ref(false)
const errorMessage = ref('')

const defaultBaseURL = import.meta.env.MODE === 'production' ? '' : 'http://localhost:8080'
const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL || defaultBaseURL).replace(/\/$/, '')

const resolveMediaUrl = (value) => {
  if (!value) return ''
  if (/^(https?:|blob:|data:)/i.test(value)) return value
  return `${apiBaseUrl}/${String(value).replace(/^\//, '')}`
}

const loadMedia = async (id) => {
  if (id === '' || id === null || id === undefined) return
  loading.value = true
  videoUrl.value = ''
  errorMessage.value = ''
  try {
    const response = await getMediasSignature({ mediaId: id })
    if (response?.code !== 200) throw new Error(response?.msg || '媒资地址获取失败')
    const data = response.data || {}
    const rawUrl = data.fileUrl || data.url || data.playUrl || ''
    if (!rawUrl) throw new Error('该媒资暂无可播放地址')
    videoUrl.value = resolveMediaUrl(rawUrl)
  } catch (error) {
    errorMessage.value = error?.message || '媒资地址获取失败'
    ElMessage.error(errorMessage.value)
  } finally {
    loading.value = false
  }
}

// 兼容旧课程编辑页和媒资列表页通过 ref 调用的方式。
const getId = (id) => loadMedia(id || props.mediaId)

const handleVideoError = () => {
  errorMessage.value = '视频文件暂时无法播放，请检查文件是否仍在文件服务卷中'
}

const handleClose = () => {
  if (videoRef.value) videoRef.value.pause()
  videoUrl.value = ''
  errorMessage.value = ''
  emit('handleClose')
}

watch(
  () => [props.dialogFormVisible, props.mediaId],
  ([visible, id]) => {
    if (visible && id !== '' && id !== null && id !== undefined && !loading.value && !videoUrl.value) {
      loadMedia(id)
    }
  }
)

defineExpose({ getId })
</script>

<style scoped>
.video-box {
  min-height: 220px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  border-radius: 8px;
  overflow: hidden;
}

.video-element {
  display: block;
  width: 100%;
  max-height: 460px;
  background: #000;
}

.error-message {
  margin: 12px 0 0;
  color: #f56c6c;
  text-align: center;
  font-size: 13px;
}
</style>
