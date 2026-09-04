<template>
  <div class="learning-container" v-loading="loading">
    <!-- 顶部导航 -->
    <div class="learning-header">
      <div class="header-left">
        <el-button @click="$router.go(-1)">
          <el-icon><ArrowLeft /></el-icon>
          返回
        </el-button>
        <span class="course-name">{{ course.title }}</span>
      </div>
      <div class="header-right">
        <span class="progress-text">学习进度：{{ course.progress }}%</span>
      </div>
    </div>

    <!-- 主要内容区域 -->
    <div class="learning-content">
      <!-- 左侧视频区域 -->
      <div class="video-section">
        <div class="video-player">
          <video v-if="currentSection.mediaUrl" class="video-element" controls :src="currentSection.mediaUrl" />
          <div v-if="!currentSection.mediaUrl" class="video-placeholder">
            <el-icon :size="64" color="#c0c4cc"><VideoPlay /></el-icon>
            <p>当前小节暂无可播放媒资</p>
            <p class="video-title">{{ currentSection.title || '请选择课程小节' }}</p>
          </div>
        </div>

        <!-- 课程信息 -->
        <div class="course-info">
          <h2>{{ course.title }}</h2>
          <div class="course-meta">
            <span>讲师：{{ course.teacherName }}</span>
            <span>课时：{{ course.lessons }}</span>
            <span>学习人数：{{ course.learners }}</span>
          </div>
          <div class="course-desc">{{ course.description }}</div>
        </div>

        <!-- Tab 切换 -->
        <el-tabs v-model="activeTab" class="detail-tabs">
          <el-tab-pane label="课程介绍" name="intro">
            <div class="intro-content">
              <h3>课程简介</h3>
              <p>{{ course.description }}</p>
              <h3 v-if="course.shortDescription">课程说明</h3>
              <p v-if="course.shortDescription">{{ course.shortDescription }}</p>
            </div>
          </el-tab-pane>

          <el-tab-pane label="问答" name="qa">
            <div class="qa-content">
              <div class="qa-input">
                <el-input v-model="newQuestion" placeholder="输入你的问题" />
                <el-button type="primary" @click="handleAsk">提问</el-button>
              </div>
              <div class="qa-list">
                <div v-for="(qa, index) in qaList" :key="qa.id || index" class="qa-item">
                  <div class="qa-question">
                    <span class="qa-badge">问</span>
                    <span>{{ qa.question }}</span>
                  </div>
                  <div class="qa-answer" v-if="qa.answer">
                    <span class="qa-badge answer">答</span>
                    <span>{{ qa.answer }}</span>
                  </div>
                </div>
              </div>
            </div>
          </el-tab-pane>

          <el-tab-pane label="笔记" name="notes">
            <div class="notes-content">
              <div class="note-input">
                <el-input v-model="newNote" type="textarea" :rows="3" placeholder="记录学习笔记" />
                <div class="note-actions">
                  <el-checkbox v-model="noteIsPublic">公开笔记</el-checkbox>
                  <el-button type="primary" @click="handleAddNote">保存笔记</el-button>
                </div>
              </div>
              <div class="note-list">
                <div v-for="(note, index) in notes" :key="note.id || index" class="note-item">
                  <div class="note-header">
                    <span class="note-section">{{ note.section }}</span>
                    <el-tag :type="note.isPublic ? 'success' : 'info'" size="small">
                      {{ note.isPublic ? '公开' : '私密' }}
                    </el-tag>
                  </div>
                  <div class="note-content">{{ note.content }}</div>
                  <div class="note-time">{{ note.time }}</div>
                </div>
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>

      <!-- 右侧目录 -->
      <div class="catalog-section">
        <div class="catalog-header">
          <h3>课程目录</h3>
        </div>
        <div class="catalog-list">
                <div v-for="(chapter, index) in chapters" :key="chapter.id || index" class="chapter">
            <div class="chapter-header" @click="chapter.open = !chapter.open">
              <el-icon><ArrowRight v-if="!chapter.open" /><ArrowDown v-else /></el-icon>
              <span>{{ chapter.title }}</span>
            </div>
            <div v-show="chapter.open" class="chapter-sections">
              <div
                v-for="(section, sIndex) in chapter.sections"
                :key="section.id || sIndex"
                class="section-item"
                :class="{ active: currentSection.id === section.id }"
                @click="handleSelectSection(section)"
              >
                <div class="section-info">
                  <el-icon v-if="section.type === 'video'"><VideoPlay /></el-icon>
                  <el-icon v-else><Document /></el-icon>
                  <span>{{ section.title }}</span>
                </div>
                <span class="section-duration">{{ section.duration }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, ArrowRight, ArrowDown, VideoPlay, Document } from '@element-plus/icons-vue'
import { getClassDetails, getAskList, getReply, postQuestions } from '@/api/classDetails.js'
import { getCourseLearning, getLearningClassDetails, getMediasSignature } from '@/api/class.js'
import { getAllNotes, addNotes } from '@/api/notes.js'

const route = useRoute()
const loading = ref(false)
const course = ref({
  id: null,
  title: '课程加载中',
  teacherName: '讲师团队',
  lessons: 0,
  learners: 0,
  progress: 0,
  description: '',
  shortDescription: ''
})
const currentSection = ref({})
const chapters = reactive([])
const activeTab = ref('intro')
const newQuestion = ref('')
const qaList = reactive([])
const newNote = ref('')
const noteIsPublic = ref(true)
const notes = reactive([])

const courseId = () => Number(route.query.courseId || route.query.id || 0)

const listFrom = (data) => {
  if (Array.isArray(data)) return data
  return data?.list || data?.rows || []
}

const replaceReactive = (target, rows) => {
  target.splice(0, target.length, ...(rows || []))
}

const formatDuration = (seconds) => {
  const value = Number(seconds || 0)
  const minutes = Math.floor(value / 60)
  const rest = value % 60
  return `${String(minutes).padStart(2, '0')}:${String(rest).padStart(2, '0')}`
}

const normalizeSection = (section = {}) => ({
  ...section,
  title: section.title || section.catalogTitle || section.name || '未命名课时',
  type: Number(section.catalogType ?? section.type) === 2 ? 'video' : 'document',
  duration: section.duration || formatDuration(section.durationSeconds ?? section.mediaDuration),
  mediaUrl: section.mediaUrl || section.fileUrl || section.url || ''
})

const normalizeCatalogs = (rows) => (rows || []).map((chapter, index) => ({
  ...chapter,
  id: chapter.id || `chapter-${index}`,
  title: chapter.title || chapter.catalogTitle || chapter.name || `第${index + 1}章`,
  open: index === 0,
  sections: (chapter.sections || []).map(normalizeSection)
}))

const normalizeNote = (note = {}) => ({
  ...note,
  section: note.sectionName || note.title || '学习笔记',
  content: note.content || '',
  isPublic: note.isPublic ?? Number(note.visibility ?? 1) === 1,
  time: note.createTime || ''
})

const loadQuestions = async (id) => {
  const response = await getAskList({ courseId: id, pageNo: 1, pageSize: 20 })
  const questions = listFrom(response?.data)
  const rows = await Promise.all(questions.map(async (question) => {
    const replyResponse = await getReply({ questionId: question.id, pageNo: 1, pageSize: 1 }).catch(() => null)
    const replies = listFrom(replyResponse?.data)
    return {
      id: question.id,
      question: question.title || question.content || '未命名问题',
      answer: replies[0]?.content || ''
    }
  }))
  replaceReactive(qaList, rows)
}

const loadNotes = async (id) => {
  const response = await getAllNotes({ courseId: id, pageNo: 1, pageSize: 20 })
  replaceReactive(notes, listFrom(response?.data).map(normalizeNote))
}

const loadCourse = async () => {
  const id = courseId()
  if (!id) {
    ElMessage.error('课程编号无效')
    return
  }
  loading.value = true
  try {
    const [courseResponse, catalogResponse, learningResponse, questionResponse, noteResponse] = await Promise.allSettled([
      getClassDetails(id),
      getLearningClassDetails(id),
      getCourseLearning(id),
      getAskList({ courseId: id, pageNo: 1, pageSize: 20 }),
      getAllNotes({ courseId: id, pageNo: 1, pageSize: 20 })
    ])

    if (courseResponse.status === 'fulfilled' && courseResponse.value?.code === 200) {
      const value = courseResponse.value.data || {}
      course.value = {
        ...course.value,
        ...value,
        id,
        title: value.title || value.courseName || value.name || '未命名课程',
        teacherName: value.teacherName || '讲师团队',
        lessons: Number(value.lessons ?? value.lessonCount ?? 0),
        learners: Number(value.learners ?? value.learnerCount ?? 0),
        description: value.description || value.shortDescription || '',
        shortDescription: value.shortDescription || ''
      }
    }

    if (catalogResponse.status === 'fulfilled' && catalogResponse.value?.code === 200) {
      replaceReactive(chapters, normalizeCatalogs(listFrom(catalogResponse.value.data)))
    }

    if (learningResponse.status === 'fulfilled' && learningResponse.value?.code === 200 && learningResponse.value.data) {
      const learning = learningResponse.value.data
      course.value.progress = Math.min(100, Math.max(0, Number(learning.progress ?? learning.progressPercent ?? 0)))
      const matched = chapters.flatMap(chapter => chapter.sections || []).find(section => Number(section.id) === Number(learning.catalogId))
      if (matched) currentSection.value = matched
    }

    if (!currentSection.value.id) {
      currentSection.value = chapters.flatMap(chapter => chapter.sections || [])[0] || {}
    }

    if (questionResponse.status === 'fulfilled' && questionResponse.value?.code === 200) {
      const questions = listFrom(questionResponse.value.data)
      const rows = await Promise.all(questions.map(async (question) => {
        const replyResponse = await getReply({ questionId: question.id, pageNo: 1, pageSize: 1 }).catch(() => null)
        const replies = listFrom(replyResponse?.data)
        return { id: question.id, question: question.title || question.content || '未命名问题', answer: replies[0]?.content || '' }
      }))
      replaceReactive(qaList, rows)
    }

    if (noteResponse.status === 'fulfilled' && noteResponse.value?.code === 200) {
      replaceReactive(notes, listFrom(noteResponse.value.data).map(normalizeNote))
    }
  } catch (error) {
    ElMessage.error(error?.message || '学习数据加载失败')
  } finally {
    loading.value = false
  }
}

const handleSelectSection = async (section) => {
  currentSection.value = section
  if (section.mediaId && !section.mediaUrl) {
    try {
      const response = await getMediasSignature({ id: section.mediaId })
      const data = response?.data || {}
      section.mediaUrl = data.fileUrl || data.url || data.playUrl || ''
    } catch (error) {
      ElMessage.warning('媒资地址获取失败，请稍后重试')
    }
  }
  ElMessage.success(`已切换：${section.title}`)
}

const handleAsk = async () => {
  const content = newQuestion.value.trim()
  if (!content) {
    ElMessage.warning('请输入问题')
    return
  }
  try {
    const response = await postQuestions({ courseId: course.value.id, catalogId: currentSection.value.id, title: content, content })
    if (response?.code !== 200) throw new Error(response?.msg || '问题提交失败')
    newQuestion.value = ''
    await loadQuestions(course.value.id)
    ElMessage.success('问题已提交')
  } catch (error) {
    ElMessage.error(error?.message || '问题提交失败，请先登录')
  }
}

const handleAddNote = async () => {
  const content = newNote.value.trim()
  if (!content) {
    ElMessage.warning('请输入笔记内容')
    return
  }
  try {
    const response = await addNotes({
      courseId: course.value.id,
      catalogId: currentSection.value.id,
      title: currentSection.value.title || '学习笔记',
      content,
      visibility: noteIsPublic.value ? 1 : 0
    })
    if (response?.code !== 200) throw new Error(response?.msg || '笔记保存失败')
    newNote.value = ''
    await loadNotes(course.value.id)
    ElMessage.success('笔记已保存')
  } catch (error) {
    ElMessage.error(error?.message || '笔记保存失败，请先登录')
  }
}

onMounted(loadCourse)
</script>

<style scoped>
.learning-container {
  background: #f5f7fa;
  min-height: 100vh;
}

/* 顶部导航 */
.learning-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.course-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.progress-text {
  font-size: 14px;
  color: #909399;
}

/* 主要内容 */
.learning-content {
  display: flex;
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
  gap: 20px;
}

/* 视频区域 */
.video-section {
  flex: 1;
}

.video-player {
  background: #000;
  border-radius: 12px;
  overflow: hidden;
  margin-bottom: 20px;
}

.video-placeholder {
  height: 450px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #c0c4cc;
}

.video-element {
  display: block;
  width: 100%;
  height: 450px;
  background: #000;
}

.video-placeholder p {
  margin: 12px 0 0;
  font-size: 16px;
}

.video-title {
  font-size: 14px !important;
  color: #909399 !important;
}

/* 课程信息 */
.course-info {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 20px;
}

.course-info h2 {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 12px;
}

.course-meta {
  display: flex;
  gap: 24px;
  font-size: 14px;
  color: #909399;
  margin-bottom: 12px;
}

.course-desc {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
}

/* Tab 内容 */
.detail-tabs {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
}

/* 课程介绍 */
.intro-content h3 {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 20px 0 10px;
}

.intro-content h3:first-child {
  margin-top: 0;
}

.intro-content p {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
}

.intro-content ul {
  padding-left: 20px;
}

.intro-content li {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
}

/* 问答 */
.qa-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.qa-input {
  display: flex;
  gap: 12px;
}

.qa-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.qa-item {
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
}

.qa-question,
.qa-answer {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}

.qa-question:last-child,
.qa-answer:last-child {
  margin-bottom: 0;
}

.qa-badge {
  width: 24px;
  height: 24px;
  border-radius: 4px;
  background: #409eff;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  flex-shrink: 0;
}

.qa-badge.answer {
  background: #67c23a;
}

/* 笔记 */
.notes-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.note-input {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.note-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.note-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.note-item {
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
}

.note-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.note-section {
  font-size: 13px;
  color: #409eff;
}

.note-content {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
  margin-bottom: 8px;
}

.note-time {
  font-size: 12px;
  color: #909399;
}

/* 目录区域 */
.catalog-section {
  width: 320px;
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  height: fit-content;
  position: sticky;
  top: 80px;
}

.catalog-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 16px;
}

.catalog-list {
  max-height: 600px;
  overflow-y: auto;
}

.chapter {
  margin-bottom: 8px;
}

.chapter-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  background: #f8f9fa;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.chapter-sections {
  padding: 4px 0;
}

.section-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px 10px 32px;
  cursor: pointer;
  transition: background 0.2s;
  border-radius: 4px;
}

.section-item:hover {
  background: #ecf5ff;
}

.section-item.active {
  background: #ecf5ff;
  color: #409eff;
}

.section-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.section-duration {
  font-size: 12px;
  color: #909399;
}

/* 响应式 */
@media (max-width: 1200px) {
  .learning-content {
    flex-direction: column;
  }

  .catalog-section {
    width: 100%;
    position: static;
  }
}
</style>
