<template>
  <div class="course-detail">
    <!-- 课程头部 -->
    <div class="course-header" :style="{ background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)' }">
      <div class="container">
        <div class="header-content">
          <div class="course-cover">
            <img :src="course.cover" :alt="course.title" />
          </div>
          <div class="course-info">
            <h1 class="course-title">{{ course.title }}</h1>
            <div class="course-meta">
              <span><el-icon><User /></el-icon> {{ course.teacherName }}</span>
              <span><el-icon><Reading /></el-icon> {{ course.learners }}人在学</span>
              <span><el-icon><Clock /></el-icon> {{ course.lessons }}课时</span>
            </div>
            <div class="course-desc">{{ course.description }}</div>
            <div class="course-actions">
              <div class="price-section">
                <span v-if="course.price > 0" class="price">¥{{ (course.price / 100).toFixed(2) }}</span>
                <span v-else class="free">免费</span>
                <span v-if="course.originalPrice > course.price" class="original-price">¥{{ (course.originalPrice / 100).toFixed(2) }}</span>
              </div>
              <div class="action-buttons">
                <el-button v-if="!isBuyed && course.price > 0" type="primary" size="large" @click="handleBuy">立即购买</el-button>
                <el-button v-if="!isBuyed && course.price > 0" size="large" @click="handleAddCart">加入购物车</el-button>
                <el-button v-if="!isBuyed && course.price === 0" type="primary" size="large" @click="handleEnroll">免费报名</el-button>
                <el-button v-if="isBuyed" type="primary" size="large" @click="handleLearn">马上学习</el-button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 课程内容 -->
    <div class="container course-content">
      <div class="main-content">
        <!-- Tab 切换 -->
        <el-tabs v-model="activeTab" class="detail-tabs">
          <el-tab-pane label="课程介绍" name="intro">
            <div class="intro-content">
              <h3>课程简介</h3>
              <p>{{ course.description }}</p>
              <h3>适合人群</h3>
              <ul>
                <li>有一定编程基础的开发者</li>
                <li>想要提升技能的工程师</li>
                <li>对技术有热情的学习者</li>
              </ul>
              <h3>学习目标</h3>
              <ul>
                <li>掌握核心概念和原理</li>
                <li>能够独立完成项目开发</li>
                <li>提升解决实际问题的能力</li>
              </ul>
            </div>
          </el-tab-pane>

          <el-tab-pane label="课程目录" name="catalog">
            <div class="catalog-content">
              <div v-for="(chapter, index) in chapters" :key="index" class="chapter">
                <div class="chapter-header" @click="chapter.open = !chapter.open">
                  <el-icon><ArrowRight v-if="!chapter.open" /><ArrowDown v-else /></el-icon>
                  <span>{{ chapter.title }}</span>
                  <span class="chapter-meta">{{ chapter.sections.length }}节</span>
                </div>
                <div v-show="chapter.open" class="chapter-sections">
                  <div v-for="(section, sIndex) in chapter.sections" :key="sIndex" class="section-item">
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
          </el-tab-pane>

          <el-tab-pane label="问答" name="qa">
            <div class="qa-content">
              <div v-for="(qa, index) in qaList" :key="index" class="qa-item">
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
          </el-tab-pane>

          <el-tab-pane label="笔记" name="notes">
            <div class="notes-content">
              <div v-for="(note, index) in notes" :key="index" class="note-item">
                <div class="note-header">
                  <span class="note-section">{{ note.section }}</span>
                  <span class="note-time">{{ note.time }}</span>
                </div>
                <div class="note-content">{{ note.content }}</div>
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>

      <!-- 侧边栏 -->
      <div class="sidebar">
        <!-- 教师信息 -->
        <el-card class="teacher-card" shadow="hover">
          <template #header>
            <span>授课教师</span>
          </template>
          <div class="teacher-info">
            <div class="teacher-avatar">
              <img :src="teacher.avatar" :alt="teacher.name" />
            </div>
            <div class="teacher-detail">
              <h4>{{ teacher.name }}</h4>
              <p>{{ teacher.title }}</p>
            </div>
          </div>
          <p class="teacher-desc">{{ teacher.description }}</p>
        </el-card>

        <!-- 常见问题 -->
        <el-card class="faq-card" shadow="hover">
          <template #header>
            <span>常见问题</span>
          </template>
          <div class="faq-list">
            <div v-for="(faq, index) in faqs" :key="index" class="faq-item">
              <div class="faq-question">Q: {{ faq.question }}</div>
              <div class="faq-answer">A: {{ faq.answer }}</div>
            </div>
          </div>
        </el-card>

        <!-- 猜你喜欢 -->
        <el-card class="like-card" shadow="hover">
          <template #header>
            <span>猜你喜欢</span>
          </template>
          <div class="like-list">
            <div v-for="(item, index) in likeCourses" :key="index" class="like-item" @click="$router.push(`/details/index?id=${item.id}`)">
              <img :src="item.cover" :alt="item.title" />
              <div class="like-info">
                <div class="like-title">{{ item.title }}</div>
                <div class="like-price">¥{{ (item.price / 100).toFixed(0) }}</div>
              </div>
            </div>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Reading, Clock, VideoPlay, Document, ArrowRight, ArrowDown } from '@element-plus/icons-vue'
import { getClassDetails, getClassTeachers, getClassList, getAskList, getReply } from '@/api/classDetails.js'
import { getAllNotes } from '@/api/notes.js'
import { getCourseLearning, getRecommendClassList, signUp } from '@/api/class.js'
import { putCarts } from '@/api/order.js'
import { getServiceFaqs } from '@/api/customerService.js'

const route = useRoute()
const router = useRouter()

// 课程数据
const course = ref({ id: null, title: '课程加载中', cover: '', price: 0, originalPrice: 0, teacherName: '讲师团队', learners: 0, lessons: 0, description: '' })

// 教师信息
const teacher = ref({ name: '讲师团队', avatar: '', title: '', description: '' })

// 章节数据
const chapters = reactive([])

// 问答数据
const qaList = reactive([])

// 笔记数据
const notes = reactive([])

// 常见问题
const faqs = reactive([])

// 猜你喜欢
const likeCourses = reactive([])

// Tab 切换
const activeTab = ref('intro')

// 是否已购买
const isBuyed = ref(false)

// 购买
const handleBuy = () => {
  router.push({ path: '/pay/settlement', query: { courseId: course.value.id } })
}

// 加入购物车
const handleAddCart = async () => {
  try {
    const response = await putCarts({ courseId: course.value.id })
    if (response?.code !== 200) throw new Error(response?.msg || '加入购物车失败')
    ElMessage.success('已加入购物车')
  } catch (error) {
    ElMessage.error(error?.message || '加入购物车失败，请先登录')
  }
}

// 免费报名
const handleEnroll = async () => {
  try {
    const response = await signUp(course.value.id)
    if (response?.code !== 200) throw new Error(response?.msg || '报名失败')
    ElMessage.success('报名成功！')
    isBuyed.value = true
  } catch (error) {
    ElMessage.error(error?.message || '报名失败，请先登录')
  }
}

// 马上学习
const handleLearn = () => {
  router.push({ path: '/learning/index', query: { courseId: course.value.id } })
}

const formatDuration = (seconds) => {
  const value = Number(seconds || 0)
  const minutes = Math.floor(value / 60)
  const rest = value % 60
  return `${String(minutes).padStart(2, '0')}:${String(rest).padStart(2, '0')}`
}

const replaceReactive = (target, rows) => {
  target.splice(0, target.length, ...(rows || []))
}

const loadCourse = async () => {
  const courseId = Number(route.query.id)
  if (!courseId) {
    ElMessage.error('课程编号无效')
    return
  }
  try {
    const [courseResponse, teacherResponse, catalogResponse, questionResponse, noteResponse, faqResponse, recommendResponse, learningResponse] = await Promise.allSettled([
      getClassDetails(courseId),
      getClassTeachers(courseId),
      getClassList(courseId),
      getAskList({ courseId, pageNo: 1, pageSize: 20 }),
      getAllNotes({ courseId, pageNo: 1, pageSize: 20 }),
      getServiceFaqs({ pageNo: 1, pageSize: 6 }),
      getRecommendClassList('home'),
      getCourseLearning(courseId)
    ])

    if (courseResponse.status === 'fulfilled' && courseResponse.value?.code === 200) {
      const value = courseResponse.value.data || {}
      course.value = {
        ...value,
        id: value.id || courseId,
        title: value.title || value.courseName || value.name || '未命名课程',
        cover: value.cover || value.coverUrl || '',
        price: Number(value.price || 0),
        originalPrice: Number(value.originalPrice ?? value.price ?? 0),
        learners: Number(value.learners ?? value.learnerCount ?? 0),
        lessons: Number(value.lessons ?? value.lessonCount ?? 0),
        description: value.description || value.shortDescription || ''
      }
    }

    if (teacherResponse.status === 'fulfilled' && teacherResponse.value?.code === 200) {
      const teacherRows = Array.isArray(teacherResponse.value.data) ? teacherResponse.value.data : []
      const value = teacherRows[0]
      if (value) teacher.value = { ...value, name: value.name || value.teacherName || '讲师团队', avatar: value.avatar || value.avatarUrl || '', description: value.introduction || value.description || '' }
    }

    if (catalogResponse.status === 'fulfilled' && catalogResponse.value?.code === 200) {
      const rows = Array.isArray(catalogResponse.value.data) ? catalogResponse.value.data : []
      replaceReactive(chapters, rows.map((chapter, index) => ({
        ...chapter,
        title: chapter.title || chapter.catalogTitle || chapter.name || `第${index + 1}章`,
        open: index === 0,
        sections: (chapter.sections || []).map(section => ({
          ...section,
          title: section.title || section.catalogTitle || section.name || '未命名课时',
          type: Number(section.catalogType ?? section.type) === 2 ? 'video' : 'document',
          duration: formatDuration(section.durationSeconds ?? section.mediaDuration)
        }))
      })))
    }

    if (questionResponse.status === 'fulfilled' && questionResponse.value?.code === 200) {
      const data = questionResponse.value.data || {}
      const questions = Array.isArray(data) ? data : (data.list || data.rows || [])
      const withReplies = await Promise.all(questions.map(async question => {
        const replyResponse = await getReply({ questionId: question.id, pageNo: 1, pageSize: 1 }).catch(() => null)
        const replyData = replyResponse?.data || {}
        const replies = Array.isArray(replyData) ? replyData : (replyData.list || replyData.rows || [])
        return { question: question.title || question.content || '未命名问题', answer: replies[0]?.content || '' }
      }))
      replaceReactive(qaList, withReplies)
    }

    if (noteResponse.status === 'fulfilled' && noteResponse.value?.code === 200) {
      const data = noteResponse.value.data || {}
      const rows = Array.isArray(data) ? data : (data.list || data.rows || [])
      replaceReactive(notes, rows.map(note => ({ section: note.sectionName || note.title || '学习笔记', content: note.content || '', time: note.createTime || '' })))
    }

    if (faqResponse.status === 'fulfilled' && faqResponse.value?.code === 200) {
      const data = faqResponse.value.data || {}
      const rows = Array.isArray(data) ? data : (data.list || data.rows || [])
      replaceReactive(faqs, rows.map(faq => ({ question: faq.question || faq.title || faq.faqQuestion, answer: faq.answer || faq.content || faq.faqAnswer })))
    }

    if (recommendResponse.status === 'fulfilled' && recommendResponse.value?.code === 200) {
      const data = recommendResponse.value.data
      const rows = Array.isArray(data) ? data : (data?.list || [])
      replaceReactive(likeCourses, rows.filter(item => Number(item.id) !== courseId).slice(0, 4).map(item => ({ ...item, title: item.title || item.courseName, cover: item.cover || item.coverUrl || '', price: Number(item.price || 0) })))
    }
    isBuyed.value = learningResponse.status === 'fulfilled' && learningResponse.value?.code === 200 && learningResponse.value.data != null
  } catch (error) {
    ElMessage.error(error?.message || '课程信息加载失败')
  }
}

onMounted(loadCourse)
</script>

<style scoped>
.course-detail {
  background: #f5f7fa;
  min-height: 100vh;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

/* 课程头部 */
.course-header {
  padding: 40px 0;
  color: #fff;
}

.header-content {
  display: flex;
  gap: 32px;
}

.course-cover {
  width: 380px;
  height: 234px;
  border-radius: 12px;
  overflow: hidden;
  flex-shrink: 0;
}

.course-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.course-info {
  flex: 1;
}

.course-title {
  font-size: 28px;
  font-weight: 700;
  margin: 0 0 16px;
}

.course-meta {
  display: flex;
  gap: 24px;
  margin-bottom: 16px;
  opacity: 0.9;
}

.course-meta span {
  display: flex;
  align-items: center;
  gap: 6px;
}

.course-desc {
  font-size: 15px;
  line-height: 1.6;
  opacity: 0.9;
  margin-bottom: 24px;
}

.course-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  padding: 20px;
}

.price-section {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.price {
  font-size: 32px;
  font-weight: 700;
}

.free {
  font-size: 32px;
  font-weight: 700;
  color: #67c23a;
}

.original-price {
  font-size: 16px;
  opacity: 0.7;
  text-decoration: line-through;
}

.action-buttons {
  display: flex;
  gap: 12px;
}

/* 课程内容 */
.course-content {
  display: flex;
  gap: 24px;
  margin-top: 24px;
  padding-bottom: 40px;
}

.main-content {
  flex: 1;
  background: #fff;
  border-radius: 12px;
  padding: 24px;
}

.detail-tabs :deep(.el-tabs__header) {
  margin-bottom: 24px;
}

/* 课程介绍 */
.intro-content h3 {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 24px 0 12px;
}

.intro-content h3:first-child {
  margin-top: 0;
}

.intro-content p {
  font-size: 15px;
  line-height: 1.8;
  color: #606266;
}

.intro-content ul {
  padding-left: 20px;
}

.intro-content li {
  font-size: 14px;
  line-height: 1.8;
  color: #606266;
}

/* 课程目录 */
.catalog-content {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  overflow: hidden;
}

.chapter {
  border-bottom: 1px solid #ebeef5;
}

.chapter:last-child {
  border-bottom: none;
}

.chapter-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px 20px;
  background: #f8f9fa;
  cursor: pointer;
  font-weight: 600;
  color: #303133;
}

.chapter-meta {
  margin-left: auto;
  font-size: 13px;
  color: #909399;
  font-weight: normal;
}

.chapter-sections {
  padding: 0;
}

.section-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px 12px 44px;
  border-bottom: 1px solid #f0f0f0;
}

.section-item:last-child {
  border-bottom: none;
}

.section-info {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #606266;
}

.section-duration {
  font-size: 13px;
  color: #909399;
}

/* 问答 */
.qa-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
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

.note-item {
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
}

.note-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.note-section {
  font-size: 13px;
  color: #409eff;
}

.note-time {
  font-size: 12px;
  color: #909399;
}

.note-content {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
}

/* 侧边栏 */
.sidebar {
  width: 300px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 教师卡片 */
.teacher-info {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.teacher-avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  overflow: hidden;
}

.teacher-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.teacher-detail h4 {
  margin: 0 0 4px;
  font-size: 16px;
  color: #303133;
}

.teacher-detail p {
  margin: 0;
  font-size: 13px;
  color: #909399;
}

.teacher-desc {
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
  margin: 0;
}

/* FAQ */
.faq-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.faq-question {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.faq-answer {
  font-size: 13px;
  color: #606266;
}

/* 猜你喜欢 */
.like-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.like-item {
  display: flex;
  gap: 12px;
  cursor: pointer;
  padding: 8px;
  border-radius: 8px;
  transition: background 0.2s;
}

.like-item:hover {
  background: #f8f9fa;
}

.like-item img {
  width: 80px;
  height: 45px;
  border-radius: 6px;
  object-fit: cover;
}

.like-info {
  flex: 1;
}

.like-title {
  font-size: 13px;
  color: #303133;
  margin-bottom: 4px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.like-price {
  font-size: 14px;
  font-weight: 600;
  color: #f56c6c;
}
</style>
