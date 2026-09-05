<template>
  <div class="media-management">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2>媒资管理</h2>
      <el-button type="primary" :icon="Upload" @click="handleUpload">上传视频</el-button>
    </div>

    <!-- 统计卡片 -->
    <div class="stat-cards">
      <el-card shadow="hover" class="stat-card">
        <div class="stat-content">
          <div class="stat-icon" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%)">
            <span class="stat-number">{{ total }}</span>
          </div>
          <div class="stat-label">视频总数</div>
        </div>
      </el-card>
      <el-card shadow="hover" class="stat-card">
        <div class="stat-content">
          <div class="stat-icon" style="background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)">
            <span class="stat-number">{{ totalSize }}</span>
          </div>
          <div class="stat-label">总容量</div>
        </div>
      </el-card>
      <el-card shadow="hover" class="stat-card">
        <div class="stat-content">
          <div class="stat-icon" style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%)">
            <span class="stat-number">{{ usedCount }}</span>
          </div>
          <div class="stat-label">已使用</div>
        </div>
      </el-card>
    </div>

    <!-- 搜索区域 -->
    <el-card class="search-card" shadow="hover">
      <el-form :model="searchForm" inline>
        <el-form-item label="视频类型">
          <el-select v-model="searchForm.type" placeholder="全部类型" clearable style="width: 150px">
            <el-option label="全部" value="" />
            <el-option label="课程视频" value="course" />
            <el-option label="宣传视频" value="promo" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="searchForm.keyword" placeholder="视频名称" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 视频列表 -->
    <el-card class="table-card" shadow="hover">
      <el-table :data="mediaList" stripe style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column label="视频信息" min-width="300">
          <template #default="{ row }">
            <div class="media-info">
              <div class="media-cover">
                <el-icon :size="32" color="#409eff"><VideoPlay /></el-icon>
              </div>
              <div class="media-detail">
                <div class="media-name">{{ row.name }}</div>
                <div class="media-meta">
                  <span>{{ row.format }}</span>
                  <span>{{ row.size }}</span>
                  <span>{{ row.duration }}</span>
                </div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="getTypeTag(row.type)">{{ getTypeText(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="resolution" label="分辨率" width="120" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusTag(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="uploadTime" label="上传时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handlePreview(row)">预览</el-button>
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 上传弹窗 -->
    <el-dialog
      v-model="uploadDialogVisible"
      title="上传视频"
      width="600px"
    >
      <div class="upload-area">
        <el-upload
          class="upload-dragger"
          drag
          action="#"
          :auto-upload="false"
          :on-change="handleFileChange"
          accept="video/*"
        >
          <el-icon class="el-icon--upload"><Upload /></el-icon>
          <div class="el-upload__text">
            将文件拖到此处，或<em>点击上传</em>
          </div>
          <template #tip>
            <div class="el-upload__tip">
              Demo 环境支持 mp4、avi、mov 等视频格式，单个文件不超过 20MB
            </div>
          </template>
        </el-upload>
      </div>
      <template #footer>
        <el-button @click="uploadDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleConfirmUpload">开始上传</el-button>
      </template>
    </el-dialog>

    <!-- 编辑弹窗 -->
    <el-dialog
      v-model="editDialogVisible"
      title="编辑视频信息"
      width="500px"
    >
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="视频名称">
          <el-input v-model="editForm.name" placeholder="请输入视频名称" />
        </el-form-item>
        <el-form-item label="视频类型">
          <el-select v-model="editForm.type" placeholder="请选择类型" style="width: 100%">
            <el-option label="课程视频" value="course" />
            <el-option label="宣传视频" value="promo" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="editForm.description" type="textarea" :rows="3" placeholder="请输入描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 预览弹窗 -->
    <el-dialog
      v-model="previewDialogVisible"
      title="视频预览"
      width="800px"
      @closed="handlePreviewClosed"
    >
      <div class="preview-area">
        <video
          v-if="previewUrl && !previewError"
          class="preview-video"
          controls
          playsinline
          preload="metadata"
          :src="previewUrl"
          @error="handlePreviewError"
        />
        <div v-else class="video-placeholder">
          <el-icon :size="64" color="#c0c4cc"><VideoPlay /></el-icon>
          <p>{{ previewUrl ? '视频文件暂时无法播放' : '该媒资暂无可播放地址' }}</p>
          <p class="video-name">{{ currentMedia?.name }}</p>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Upload, VideoPlay } from '@element-plus/icons-vue'
import {
  deleteMedia,
  getMedia,
  getMediaStatistics,
  mediaUpload,
  updateMedia,
  uploadFile,
} from '@/api/media'

// 搜索
const searchForm = reactive({
  type: '',
  keyword: ''
})

// 分页
const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})

// 视频列表
const mediaList = ref([])
const loading = ref(false)

// 统计数据
const mediaStatistics = reactive({
  total: 0,
  totalSizeText: '0 B',
  usedCount: 0,
})
const total = computed(() => mediaStatistics.total)
const totalSize = computed(() => mediaStatistics.totalSizeText)
const usedCount = computed(() => mediaStatistics.usedCount)

// 弹窗
const uploadDialogVisible = ref(false)
const editDialogVisible = ref(false)
const previewDialogVisible = ref(false)
const currentMedia = ref(null)
const previewError = ref(false)

const defaultBaseURL = import.meta.env.MODE === 'production' ? '' : 'http://localhost:8080'
const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL || defaultBaseURL).replace(/\/$/, '')
const resolveMediaUrl = (value) => {
  if (!value) return ''
  if (/^(https?:|blob:|data:)/i.test(value)) return value
  return `${apiBaseUrl}/${String(value).replace(/^\//, '')}`
}
const previewUrl = computed(() => resolveMediaUrl(
  currentMedia.value?.fileUrl || currentMedia.value?.url || currentMedia.value?.playUrl
))

// 编辑表单
const editForm = reactive({
  id: null,
  name: '',
  type: '',
  description: ''
})

const selectedFile = ref(null)

// 获取视频列表
const getMediaList = async () => {
  loading.value = true
  try {
    const [listResponse, statisticsResponse] = await Promise.all([
      getMedia({
        pageNo: pagination.page,
        pageSize: pagination.pageSize,
        type: searchForm.type || undefined,
        keyword: searchForm.keyword || undefined,
      }),
      getMediaStatistics(),
    ])
    if (listResponse?.code === 200) {
      const page = listResponse.data || {}
      mediaList.value = page.list || []
      pagination.total = page.total || 0
    }
    if (statisticsResponse?.code === 200) {
      Object.assign(mediaStatistics, statisticsResponse.data || {})
    }
  } catch (error) {
    ElMessage.error(error?.message || '媒资列表加载失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.page = 1
  getMediaList()
}

// 重置
const handleReset = () => {
  searchForm.type = ''
  searchForm.keyword = ''
  handleSearch()
}

// 分页
const handleSizeChange = () => {
  pagination.page = 1
  getMediaList()
}

const handleCurrentChange = () => {
  getMediaList()
}

// 上传
const handleUpload = () => {
  selectedFile.value = null
  uploadDialogVisible.value = true
}

// 文件选择
const handleFileChange = (file) => {
  selectedFile.value = file?.raw || null
}

// 确认上传
const handleConfirmUpload = async () => {
  if (!selectedFile.value) {
    ElMessage.warning('请选择要上传的视频文件')
    return
  }
  loading.value = true
  try {
    const uploadResponse = await uploadFile(selectedFile.value)
    if (uploadResponse?.code !== 200 || !uploadResponse.data) {
      throw new Error(uploadResponse?.msg || '文件上传失败')
    }
    const fileData = uploadResponse.data
    const mediaResponse = await mediaUpload({
      name: fileData.name || selectedFile.value.name,
      filename: fileData.name || selectedFile.value.name,
      url: fileData.url,
      sizeBytes: selectedFile.value.size,
      type: 'course',
      status: 'unused',
    })
    if (mediaResponse?.code !== 200) {
      throw new Error(mediaResponse?.msg || '媒资保存失败')
    }
    ElMessage.success('视频上传成功')
    uploadDialogVisible.value = false
    await getMediaList()
  } catch (error) {
    ElMessage.error(error?.message || '视频上传失败')
  } finally {
    loading.value = false
  }
}

// 预览
const handlePreview = (row) => {
  currentMedia.value = row
  previewError.value = false
  previewDialogVisible.value = true
}

const handlePreviewError = () => {
  previewError.value = true
}

const handlePreviewClosed = () => {
  currentMedia.value = null
  previewError.value = false
}

// 编辑
const handleEdit = (row) => {
  Object.assign(editForm, row)
  editDialogVisible.value = true
}

// 保存编辑
const handleSaveEdit = async () => {
  if (!editForm.name?.trim()) {
    ElMessage.warning('视频名称不能为空')
    return
  }
  try {
    const response = await updateMedia(editForm.id, {
      name: editForm.name.trim(),
      type: editForm.type,
      description: editForm.description,
    })
    if (response?.code !== 200) {
      throw new Error(response?.msg || '保存失败')
    }
    ElMessage.success('保存成功')
    editDialogVisible.value = false
    await getMediaList()
  } catch (error) {
    ElMessage.error(error?.message || '保存失败')
  }
}

// 删除
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该视频吗？', '提示', { type: 'warning' })
    const response = await deleteMedia(row.id)
    if (response?.code !== 200) {
      throw new Error(response?.msg || '删除失败')
    }
    ElMessage.success('删除成功')
    await getMediaList()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error?.message || '删除失败')
    }
  }
}

// 类型标签
const getTypeTag = (type) => {
  const map = { course: '', promo: 'success', other: 'info' }
  return map[type] || 'info'
}

// 类型文本
const getTypeText = (type) => {
  const map = { course: '课程视频', promo: '宣传视频', other: '其他' }
  return map[type] || '未知'
}

// 状态标签
const getStatusTag = (status) => {
  const map = { used: 'success', unused: 'warning', processing: 'info' }
  return map[status] || 'info'
}

// 状态文本
const getStatusText = (status) => {
  const map = { used: '已使用', unused: '未使用', processing: '处理中' }
  return map[status] || '未知'
}

onMounted(() => {
  getMediaList()
})
</script>

<style scoped>
.media-management {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  font-size: 20px;
  color: #303133;
}

/* 统计卡片 */
.stat-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  border-radius: 12px;
  border: none;
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-number {
  font-size: 20px;
  font-weight: 700;
  color: #fff;
}

.stat-label {
  font-size: 14px;
  color: #606266;
}

/* 搜索卡片 */
.search-card {
  margin-bottom: 20px;
}

.search-card :deep(.el-card__body) {
  padding-bottom: 0;
}

/* 表格卡片 */
.table-card {
  border-radius: 12px;
  border: none;
}

.table-card :deep(.el-card__body) {
  padding: 0;
}

/* 视频信息 */
.media-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.media-cover {
  width: 64px;
  height: 36px;
  background: #f5f7fa;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.media-detail {
  flex: 1;
}

.media-name {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.media-meta {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #909399;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  padding: 20px;
}

/* 上传区域 */
.upload-area {
  padding: 20px;
}

.upload-dragger {
  width: 100%;
}

/* 预览区域 */
.preview-area {
  padding: 20px;
}

.video-placeholder {
  height: 400px;
  background: #000;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #c0c4cc;
}

.preview-video {
  display: block;
  width: 100%;
  max-height: 400px;
  background: #000;
  border-radius: 8px;
}

.video-placeholder p {
  margin: 12px 0 0;
  font-size: 16px;
}

.video-name {
  font-size: 14px;
  color: #909399;
}
</style>
