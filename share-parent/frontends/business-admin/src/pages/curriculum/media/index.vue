<template>
  <div class="media-management">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2>媒资管理</h2>
      <el-button type="primary" :icon="Upload" @click="handleUpload">上传视频</el-button>
    </div>

    <!-- 统计卡片 -->
    <div class="stat-cards">
      <el-card shadow="never" class="stat-card">
        <div class="stat-content">
          <div class="stat-icon" style="background: #2563eb">
            <span class="stat-number">{{ total }}</span>
          </div>
          <div class="stat-label">视频总数</div>
        </div>
      </el-card>
      <el-card shadow="never" class="stat-card">
        <div class="stat-content">
          <div class="stat-icon" style="background: #0d9488">
            <span class="stat-number">{{ totalSize }}</span>
          </div>
          <div class="stat-label">总容量</div>
        </div>
      </el-card>
      <el-card shadow="never" class="stat-card">
        <div class="stat-content">
          <div class="stat-icon" style="background: #16a34a">
            <span class="stat-number">{{ usedCount }}</span>
          </div>
          <div class="stat-label">已使用</div>
        </div>
      </el-card>
    </div>

    <!-- 搜索区域 -->
    <el-card class="search-card" shadow="hover">
      <el-form :model="searchForm" inline>
        <el-form-item label="所属课程">
          <el-select
            v-model="searchForm.courseId"
            placeholder="全部课程"
            clearable
            filterable
            style="width: 220px"
          >
            <el-option
              v-for="item in courseOptions"
              :key="item.id"
              :label="item.courseName || item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="视频类型">
          <el-select v-model="searchForm.type" placeholder="全部类型" clearable style="width: 140px">
            <el-option label="全部" value="" />
            <el-option label="课程视频" value="course" />
            <el-option label="宣传视频" value="promo" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="searchForm.keyword" placeholder="视频名称" clearable style="width: 180px" />
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
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="视频信息" min-width="260">
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
        <el-table-column prop="courseName" label="关联课程" min-width="180">
          <template #default="{ row }">
            <div v-if="row.courseName" class="course-cell">
              <span class="course-badge">{{ row.courseName }}</span>
            </div>
            <el-tag v-else type="info" size="small">未关联</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sectionName" label="对应小节" min-width="160">
          <template #default="{ row }">
            <div v-if="row.sectionName" class="section-cell">
              <el-tag type="success" size="small">{{ row.sectionName }}</el-tag>
            </div>
            <span v-else class="empty-text">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="getTypeTag(row.type)">{{ getTypeText(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="resolution" label="分辨率" width="110" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="getStatusTag(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="uploadTime" label="上传时间" width="170" />
        <el-table-column label="操作" width="180" fixed="right">
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

    <!-- 上传并关联弹窗 -->
    <el-dialog
      v-model="uploadDialogVisible"
      title="上传课程视频并绑定小节"
      width="640px"
      destroy-on-close
    >
      <el-form label-width="90px">
        <el-form-item label="所属课程" required>
          <el-select
            v-model="uploadForm.courseId"
            placeholder="请选择视频所属课程"
            filterable
            clearable
            style="width: 100%"
            @change="handleUploadCourseChange"
          >
            <el-option
              v-for="item in courseOptions"
              :key="item.id"
              :label="item.courseName || item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="对应小节" required>
          <el-select
            v-model="uploadForm.sectionId"
            placeholder="请选择对应课程小节（第几节）"
            :disabled="!uploadForm.courseId || uploadSectionLoading"
            :loading="uploadSectionLoading"
            filterable
            clearable
            style="width: 100%"
            @change="handleUploadSectionChange"
          >
            <el-option-group
              v-for="group in uploadSectionGroups"
              :key="group.id"
              :label="group.name"
            >
              <el-option
                v-for="sec in group.sections"
                :key="sec.id"
                :label="sec.name + (sec.mediaName ? ' [已绑定: ' + sec.mediaName + ']' : ' [未绑定]')"
                :value="sec.id"
              >
                <div class="section-option-item">
                  <span>{{ sec.name }}</span>
                  <el-tag v-if="sec.mediaName" size="small" type="warning">已绑定: {{ sec.mediaName }}</el-tag>
                  <el-tag v-else size="small" type="success">可绑定</el-tag>
                </div>
              </el-option>
            </el-option-group>
          </el-select>
          <div v-if="uploadForm.courseId && uploadSectionGroups.length === 0 && !uploadSectionLoading" class="form-tip text-warning">
            该课程暂未创建章节与小节，请先至课程管理添加目录大纲
          </div>
        </el-form-item>

        <el-form-item label="视频文件" required>
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
                  支持 mp4、avi、mov、mkv 等视频格式，建议单个文件不超过 50MB
                </div>
              </template>
            </el-upload>
          </div>
        </el-form-item>

        <el-form-item label="视频名称">
          <el-input v-model="uploadForm.name" placeholder="默认使用所选小节名或文件名" />
        </el-form-item>

        <el-form-item label="视频描述">
          <el-input v-model="uploadForm.description" type="textarea" :rows="2" placeholder="请输入描述（可选）" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="uploadDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="handleConfirmUpload">开始上传并关联</el-button>
      </template>
    </el-dialog>

    <!-- 编辑弹窗 -->
    <el-dialog
      v-model="editDialogVisible"
      title="编辑视频信息与课程关联"
      width="560px"
      destroy-on-close
    >
      <el-form :model="editForm" label-width="90px">
        <el-form-item label="视频名称" required>
          <el-input v-model="editForm.name" placeholder="请输入视频名称" />
        </el-form-item>
        <el-form-item label="视频类型">
          <el-select v-model="editForm.type" placeholder="请选择类型" style="width: 100%">
            <el-option label="课程视频" value="course" />
            <el-option label="宣传视频" value="promo" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属课程">
          <el-select
            v-model="editForm.courseId"
            placeholder="请选择课程（可重新选择或解绑）"
            filterable
            clearable
            style="width: 100%"
            @change="handleEditCourseChange"
          >
            <el-option
              v-for="item in courseOptions"
              :key="item.id"
              :label="item.courseName || item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="对应小节">
          <el-select
            v-model="editForm.sectionId"
            placeholder="请选择小节（第几节）"
            :disabled="!editForm.courseId || editSectionLoading"
            :loading="editSectionLoading"
            filterable
            clearable
            style="width: 100%"
          >
            <el-option-group
              v-for="group in editSectionGroups"
              :key="group.id"
              :label="group.name"
            >
              <el-option
                v-for="sec in group.sections"
                :key="sec.id"
                :label="sec.name + (sec.mediaId === editForm.id ? ' [当前已绑]' : (sec.mediaName ? ' [已绑: ' + sec.mediaName + ']' : ' [未绑定]'))"
                :value="sec.id"
              />
            </el-option-group>
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="editForm.description" type="textarea" :rows="3" placeholder="请输入描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingEdit" @click="handleSaveEdit">保存</el-button>
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
import {
  getSimpleCourses,
  getCoursesCatalogue,
  bindCourseMedia,
  unbindCourseMedia,
} from '@/api/curriculum'

// 课程选项列表
const courseOptions = ref([])

// 搜索
const searchForm = reactive({
  courseId: null,
  type: '',
  keyword: '',
})

// 分页
const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0,
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

// 弹窗状态
const uploadDialogVisible = ref(false)
const editDialogVisible = ref(false)
const previewDialogVisible = ref(false)
const currentMedia = ref(null)
const previewError = ref(false)

// 上传表单
const uploading = ref(false)
const selectedFile = ref(null)
const uploadForm = reactive({
  courseId: null,
  sectionId: null,
  name: '',
  description: '',
})
const uploadSectionGroups = ref([])
const uploadSectionLoading = ref(false)

// 编辑表单
const savingEdit = ref(false)
const editForm = reactive({
  id: null,
  name: '',
  type: '',
  description: '',
  courseId: null,
  sectionId: null,
})
const originalBinding = reactive({
  courseId: null,
  sectionId: null,
})
const editSectionGroups = ref([])
const editSectionLoading = ref(false)

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

// 获取课程列表
const loadCourseOptions = async () => {
  try {
    const res = await getSimpleCourses()
    if (res?.code === 200 && Array.isArray(res.data)) {
      courseOptions.value = res.data
    }
  } catch (error) {
    console.error('加载课程列表失败', error)
  }
}

// 获取课程目录结构（章节 + 小节）
const fetchCourseSectionGroups = async (courseId) => {
  if (!courseId) return []
  try {
    const res = await getCoursesCatalogue({ id: courseId, see: false, withPractice: 0 })
    if (res?.code === 200 && Array.isArray(res.data)) {
      return res.data.map(chapter => ({
        id: chapter.id,
        name: chapter.name || chapter.title || chapter.catalogTitle || '章节',
        sections: (chapter.sections || []).map(sec => ({
          id: sec.id,
          name: sec.name || sec.title || sec.catalogTitle || '小节',
          mediaId: sec.mediaId,
          mediaName: sec.mediaName || sec.videoName,
          mediaDuration: sec.mediaDuration || sec.durationSeconds || 0,
          sortNum: sec.index || sec.sortNum || 0,
        }))
      }))
    }
  } catch (error) {
    console.error('获取课程章节小节失败', error)
  }
  return []
}

// 获取视频列表
const getMediaList = async () => {
  loading.value = true
  try {
    const [listResponse, statisticsResponse] = await Promise.all([
      getMedia({
        pageNo: pagination.page,
        pageSize: pagination.pageSize,
        courseId: searchForm.courseId || undefined,
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
  searchForm.courseId = null
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

// 打开上传弹窗
const handleUpload = () => {
  selectedFile.value = null
  uploadForm.courseId = null
  uploadForm.sectionId = null
  uploadForm.name = ''
  uploadForm.description = ''
  uploadSectionGroups.value = []
  uploadDialogVisible.value = true
}

// 切换上传所属课程
const handleUploadCourseChange = async (courseId) => {
  uploadForm.sectionId = null
  uploadSectionGroups.value = []
  if (!courseId) return
  uploadSectionLoading.value = true
  try {
    uploadSectionGroups.value = await fetchCourseSectionGroups(courseId)
  } finally {
    uploadSectionLoading.value = false
  }
}

// 切换上传对应小节
const handleUploadSectionChange = (sectionId) => {
  if (!uploadForm.name) {
    for (const group of uploadSectionGroups.value) {
      const found = group.sections.find(s => s.id === sectionId)
      if (found) {
        uploadForm.name = found.name
        break
      }
    }
  }
}

// 文件选择
const handleFileChange = (file) => {
  selectedFile.value = file?.raw || null
  if (selectedFile.value && !uploadForm.name) {
    const rawName = selectedFile.value.name.replace(/\.[^/.]+$/, '')
    uploadForm.name = rawName
  }
}

// 确认上传并关联
const handleConfirmUpload = async () => {
  if (!uploadForm.courseId) {
    ElMessage.warning('请选择视频所属的课程')
    return
  }
  if (!uploadForm.sectionId) {
    ElMessage.warning('请选择视频对应的小节（第几节）')
    return
  }
  if (!selectedFile.value) {
    ElMessage.warning('请选择要上传的视频文件')
    return
  }

  uploading.value = true
  try {
    // 1. 上传文件到文件系统
    const uploadResponse = await uploadFile(selectedFile.value)
    if (uploadResponse?.code !== 200 || !uploadResponse.data) {
      throw new Error(uploadResponse?.msg || '文件上传失败')
    }
    const fileData = uploadResponse.data

    // 2. 匹配课程名称与小节名称
    const selectedCourse = courseOptions.value.find(c => c.id === uploadForm.courseId)
    let selectedSection = null
    for (const group of uploadSectionGroups.value) {
      const found = group.sections.find(s => s.id === uploadForm.sectionId)
      if (found) {
        selectedSection = found
        break
      }
    }

    const finalMediaName = uploadForm.name?.trim() || selectedSection?.name || fileData.name || selectedFile.value.name

    // 3. 保存媒资元数据到 tj_file.file_media
    const mediaResponse = await mediaUpload({
      name: finalMediaName,
      filename: fileData.name || selectedFile.value.name,
      url: fileData.url,
      sizeBytes: selectedFile.value.size,
      type: 'course',
      status: 'used',
      description: uploadForm.description?.trim() || '',
      courseId: uploadForm.courseId,
      courseName: selectedCourse ? (selectedCourse.courseName || selectedCourse.name) : '',
      sectionId: uploadForm.sectionId,
      sectionName: selectedSection ? selectedSection.name : '',
    })

    if (mediaResponse?.code !== 200 || !mediaResponse.data) {
      throw new Error(mediaResponse?.msg || '媒资信息保存失败')
    }
    const savedMedia = mediaResponse.data

    // 4. 同步绑定至课程大纲小节 tj_education.edu_course_catalog
    const bindResponse = await bindCourseMedia({
      courseId: uploadForm.courseId,
      sectionId: uploadForm.sectionId,
      mediaId: savedMedia.id,
      mediaName: savedMedia.name || finalMediaName,
      durationSeconds: savedMedia.durationSeconds || 0,
    })
    if (bindResponse?.code !== 200) {
      console.warn('同步绑定课程大纲提示:', bindResponse?.msg)
    }

    ElMessage.success('视频上传并关联课程小节成功！')
    uploadDialogVisible.value = false
    await getMediaList()
  } catch (error) {
    ElMessage.error(error?.message || '视频上传失败')
  } finally {
    uploading.value = false
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
const handleEdit = async (row) => {
  Object.assign(editForm, {
    id: row.id,
    name: row.name || row.mediaName || '',
    type: row.type || row.mediaType || 'course',
    description: row.description || '',
    courseId: row.courseId || null,
    sectionId: row.sectionId || null,
  })
  originalBinding.courseId = row.courseId || null
  originalBinding.sectionId = row.sectionId || null

  editSectionGroups.value = []
  if (row.courseId) {
    editSectionLoading.value = true
    try {
      editSectionGroups.value = await fetchCourseSectionGroups(row.courseId)
    } finally {
      editSectionLoading.value = false
    }
  }
  editDialogVisible.value = true
}

// 切换编辑课程
const handleEditCourseChange = async (courseId) => {
  editForm.sectionId = null
  editSectionGroups.value = []
  if (!courseId) return
  editSectionLoading.value = true
  try {
    editSectionGroups.value = await fetchCourseSectionGroups(courseId)
  } finally {
    editSectionLoading.value = false
  }
}

// 保存编辑
const handleSaveEdit = async () => {
  if (!editForm.name?.trim()) {
    ElMessage.warning('视频名称不能为空')
    return
  }

  savingEdit.value = true
  try {
    let selectedCourse = null
    let selectedSection = null
    if (editForm.courseId) {
      selectedCourse = courseOptions.value.find(c => c.id === editForm.courseId)
      if (editForm.sectionId) {
        for (const group of editSectionGroups.value) {
          const found = group.sections.find(s => s.id === editForm.sectionId)
          if (found) {
            selectedSection = found
            break
          }
        }
      }
    }

    // 1. 更新 tj_file.file_media
    const response = await updateMedia(editForm.id, {
      name: editForm.name.trim(),
      type: editForm.type,
      description: editForm.description,
      courseId: editForm.courseId || null,
      courseName: selectedCourse ? (selectedCourse.courseName || selectedCourse.name) : null,
      sectionId: editForm.sectionId || null,
      sectionName: selectedSection ? selectedSection.name : null,
      status: editForm.sectionId ? 'used' : 'unused',
    })
    if (response?.code !== 200) {
      throw new Error(response?.msg || '保存失败')
    }

    // 2. 联动更新 tj_education.edu_course_catalog
    const oldHasBinding = originalBinding.courseId && originalBinding.sectionId
    const newHasBinding = editForm.courseId && editForm.sectionId

    // 如果原先有绑定，且小节变更或已解绑，解绑原小节
    if (oldHasBinding && (!newHasBinding || String(originalBinding.sectionId) !== String(editForm.sectionId))) {
      try {
        await unbindCourseMedia({
          courseId: originalBinding.courseId,
          sectionId: originalBinding.sectionId,
        })
      } catch (err) {
        console.warn('原小节解绑异常:', err)
      }
    }

    // 如果新指定了绑定，且发生了变化，绑定到新小节
    if (newHasBinding && (!oldHasBinding || String(originalBinding.sectionId) !== String(editForm.sectionId))) {
      await bindCourseMedia({
        courseId: editForm.courseId,
        sectionId: editForm.sectionId,
        mediaId: editForm.id,
        mediaName: editForm.name.trim(),
      })
    }

    ElMessage.success('保存成功')
    editDialogVisible.value = false
    await getMediaList()
  } catch (error) {
    ElMessage.error(error?.message || '保存失败')
  } finally {
    savingEdit.value = false
  }
}

// 删除
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该视频吗？若已关联小节将自动解除绑定。', '提示', { type: 'warning' })
    // 若关联了小节，先解绑
    if (row.courseId && row.sectionId) {
      try {
        await unbindCourseMedia({
          courseId: row.courseId,
          sectionId: row.sectionId,
        })
      } catch (unbindErr) {
        console.warn('解绑关联小节异常:', unbindErr)
      }
    }
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
  loadCourseOptions()
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
  flex-shrink: 0;
}

.media-detail {
  flex: 1;
  min-width: 0;
}

.media-name {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.media-meta {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #909399;
}

.course-cell {
  display: flex;
  align-items: center;
}

.course-badge {
  color: #1d4ed8;
  font-weight: 500;
  font-size: 13px;
  background: #eff6ff;
  padding: 2px 8px;
  border-radius: 4px;
  display: inline-block;
  max-width: 100%;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.section-cell {
  display: flex;
  align-items: center;
}

.empty-text {
  color: #9ca3af;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  padding: 20px;
}

/* 上传区域 */
.upload-area {
  width: 100%;
}

.upload-dragger {
  width: 100%;
}

.section-option-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.form-tip {
  font-size: 12px;
  margin-top: 4px;
  line-height: 1.4;
}

.text-warning {
  color: #e6a23c;
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
