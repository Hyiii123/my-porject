<template>
  <div class="course-detail">
    <!-- 课程头部 -->
    <div class="course-header">
      <div class="container">
        <div class="header-content">
          <div class="course-cover">
            <img :src="course.cover || course.coverUrl || course.courseCoverUrl || defaultCover" :alt="course.title" @error="handleImgError" />
          </div>
          <div class="course-info">
            <h1 class="course-title">{{ course.title }}</h1>
            <div class="course-meta">
              <span><el-icon><User /></el-icon> {{ course.teacherName }}</span>
              <span><el-icon><Reading /></el-icon> {{ course.learners }}人在学</span>
              <span><el-icon><Clock /></el-icon> {{ course.lessons }}课时</span>
              <span><el-icon><Pointer /></el-icon> {{ likeCount }}人点赞</span>
            </div>
            <div class="course-desc">{{ course.shortDescription || course.description }}</div>
            <div class="course-actions">
              <div class="price-section">
                <span v-if="course.price > 0" class="price">¥{{ (course.price / 100).toFixed(2) }}</span>
                <span v-else class="free">免费</span>
                <span v-if="course.originalPrice > course.price" class="original-price">¥{{ (course.originalPrice / 100).toFixed(2) }}</span>
              </div>
              <div class="action-buttons">
                <el-button v-if="!isBuyed && course.price > 0" type="primary" size="large" @click="handleBuy">立即购买</el-button>
                <el-button v-if="!isBuyed && course.price > 0" size="large" @click="handleAddCart">加入购物车</el-button>
                <el-button v-if="!isBuyed && course.price > 0" type="danger" size="large" :loading="seckilling" @click="handleSeckill">⚡ 限时秒杀抢购</el-button>
                <el-button v-if="!isBuyed && course.price === 0" type="primary" size="large" @click="handleEnroll">免费报名</el-button>
                <el-button v-if="isBuyed" type="primary" size="large" @click="handleLearn">马上学习</el-button>
                <el-button :type="isLiked ? 'primary' : 'default'" size="large" :plain="!isLiked" @click="handleToggleLike">
                  <el-icon><Pointer /></el-icon> {{ isLiked ? '已点赞' : '点赞' }} ({{ likeCount }})
                </el-button>
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
              <p>{{ course.description || course.shortDescription }}</p>
              <h3>适合人群</h3>
              <ul>
                <li v-if="course.prerequisites">{{ course.prerequisites }}</li>
                <li v-if="course.targetRole">目标岗位：{{ course.targetRole }}</li>
                <li>具备基础编程语法与计算机常识的学员</li>
                <li>希望系统进阶、掌握现代工程化技能的开发者</li>
              </ul>
              <h3>学习目标</h3>
              <ul>
                <li v-if="course.skills">系统掌握核心技术栈：{{ course.skills }}</li>
                <li>掌握核心架构概念和工程原理，具备独立完成项目开发的能力</li>
                <li>深入企业级高可用实战场景，全面提高解决复杂工程问题的能力</li>
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
              <img :src="item.cover || item.coverUrl || item.courseCoverUrl || defaultCover" :alt="item.title" @error="handleImgError" />
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
import { User, Reading, Clock, VideoPlay, Document, ArrowRight, ArrowDown, Pointer } from '@element-plus/icons-vue'
import { getClassDetails, getClassTeachers, getClassList, getAskList, getReply } from '@/api/classDetails.js'
import { getAllNotes } from '@/api/notes.js'
import { getCourseLearning, getRecommendClassList, signUp, likeCourse } from '@/api/class.js'
import { putCarts, seckillCourse } from '@/api/order.js'
import { getServiceFaqs } from '@/api/customerService.js'
import defaultCover from '@/assets/images/courses/default-cover.svg?url'

const route = useRoute()
const router = useRouter()

const handleImgError = (e) => {
  if (e?.target && e.target.src !== defaultCover) {
    e.target.src = defaultCover
  }
}

// 安全清洗 HTML 标签
const cleanHtml = (text) => {
  if (!text) return ''
  return String(text).replace(/<[^>]+>/g, '').trim()
}

// 课程数据
const course = ref({ id: null, title: '课程加载中', cover: defaultCover, price: 0, originalPrice: 0, teacherName: '讲师团队', learners: 0, lessons: 0, description: '', shortDescription: '', skills: '', targetRole: '', prerequisites: '' })

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

// 点赞与秒杀状态
const likeCount = ref(0)
const isLiked = ref(false)
const seckilling = ref(false)

// 课程点赞与取消点赞
const handleToggleLike = async () => {
  if (!course.value.id) return
  const targetStatus = !isLiked.value
  try {
    const res = await likeCourse({ bizId: course.value.id, liked: targetStatus })
    if (res?.code === 200) {
      isLiked.value = targetStatus
      likeCount.value = targetStatus ? likeCount.value + 1 : Math.max(0, likeCount.value - 1)
      ElMessage.success(targetStatus ? '点赞成功！已同步至热度榜' : '已取消点赞')
    }
  } catch (error) {
    ElMessage.error(error?.message || '点赞操作失败，请先登录')
  }
}

// 课程限时秒杀抢购
const handleSeckill = async () => {
  if (!course.value.id) return
  seckilling.value = true
  try {
    const res = await seckillCourse(course.value.id)
    if (res?.code === 200) {
      ElMessage.success('⚡ 恭喜您，秒杀抢购成功！已为您自动开通课程权限')
      isBuyed.value = true
    } else {
      ElMessage.error(res?.msg || '秒杀名额已抢光或抢购失败')
    }
  } catch (error) {
    ElMessage.error(error?.message || '抢购失败，请先登录')
  } finally {
    seckilling.value = false
  }
}

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
    window.dispatchEvent(new CustomEvent('cart-updated'))
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
      const rawDesc = value.description || value.shortDescription || ''
      const rawShortDesc = value.shortDescription || value.description || ''
      course.value = {
        ...value,
        id: value.id || courseId,
        title: value.title || value.courseName || value.name || '未命名课程',
        cover: value.cover || value.coverUrl || defaultCover,
        price: Number(value.price || 0),
        originalPrice: Number(value.originalPrice ?? value.price ?? 0),
        learners: Number(value.learners ?? value.learnerCount ?? 0),
        lessons: Number(value.lessons ?? value.lessonCount ?? 0),
        description: cleanHtml(rawDesc),
        shortDescription: cleanHtml(rawShortDesc),
        skills: cleanHtml(value.skills || ''),
        targetRole: cleanHtml(value.targetRole || ''),
        prerequisites: cleanHtml(value.prerequisites || '')
      }
      likeCount.value = Number(value.likeCount ?? value.likes ?? 0)
      isLiked.value = Boolean(value.isLiked)
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
      replaceReactive(likeCourses, rows.filter(item => Number(item.id) !== courseId).slice(0, 4).map(item => ({ ...item, title: item.title || item.courseName, cover: item.cover || item.coverUrl || defaultCover, price: Number(item.price || 0) })))
    }
    isBuyed.value = learningResponse.status === 'fulfilled' && learningResponse.value?.code === 200 && learningResponse.value.data != null
  } catch (error) {
    ElMessage.error(error?.message || '课程信息加载失败')
  }
}

onMounted(loadCourse)
</script>

<style scoped lang="scss">
.course-detail {
  background: #07090E;
  min-height: 100vh;
  color: #F8FAFC;
  font-family: var(--cn);
}

.container {
  max-width: 1280px;
  margin: 0 auto;
  padding: 0 24px;
}

/* 课程头部 */
.course-header {
  padding: 56px 0;
  background: 
    radial-gradient(80% 60% at 50% 0%, rgba(56, 189, 248, 0.15), transparent 70%),
    radial-gradient(60% 50% at 90% 20%, rgba(99, 102, 241, 0.12), transparent 65%),
    #0B0F19;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.header-content {
  display: flex;
  gap: 36px;
  align-items: center;
}

.course-cover {
  width: 400px;
  height: 248px;
  border-radius: 16px;
  overflow: hidden;
  flex-shrink: 0;
  border: 1px solid rgba(255, 255, 255, 0.12);
  box-shadow: 0 16px 40px -8px rgba(0, 0, 0, 0.7);

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.course-info {
  flex: 1;
}

.course-title {
  font-family: var(--display);
  font-size: 30px;
  font-weight: 800;
  color: #FFFFFF;
  margin: 0 0 14px;
  line-height: 1.25;
  letter-spacing: -0.02em;
}

.course-meta {
  display: flex;
  gap: 20px;
  margin-bottom: 16px;
  color: #94A3B8;
  font-size: 13.5px;
  font-family: var(--mono);

  span {
    display: flex;
    align-items: center;
    gap: 6px;
  }
}

.course-desc {
  font-size: 14.5px;
  line-height: 1.65;
  color: #CBD5E1;
  margin-bottom: 24px;
}

.course-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: rgba(16, 23, 38, 0.75);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 14px;
  padding: 18px 24px;
  backdrop-filter: blur(16px);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.1);
}

.price-section {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.price {
  font-family: var(--mono);
  font-size: 32px;
  font-weight: 800;
  color: #34D399;
}

.free {
  font-family: var(--mono);
  font-size: 30px;
  font-weight: 800;
  color: #38BDF8;
}

.original-price {
  font-family: var(--mono);
  font-size: 16px;
  color: #64748B;
  text-decoration: line-through;
}

.action-buttons {
  display: flex;
  gap: 12px;
}

/* 课程内容 */
.course-content {
  display: flex;
  gap: 28px;
  margin-top: 32px;
  padding-bottom: 60px;
}

.main-content {
  flex: 1;
  background: rgba(16, 23, 38, 0.72);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 20px;
  padding: 30px;
  backdrop-filter: blur(20px);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.1), 0 16px 40px -8px rgba(0, 0, 0, 0.6);
  color: #F8FAFC;
}

.detail-tabs :deep(.el-tabs__header) {
  margin-bottom: 24px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

/* 课程介绍 */
.intro-content {
  h3 {
    font-size: 18px;
    font-weight: 700;
    color: #FFFFFF;
    margin: 28px 0 14px;
    font-family: var(--display);
    letter-spacing: -0.01em;

    &:first-child {
      margin-top: 0;
    }
  }

  p {
    font-size: 15px;
    line-height: 1.8;
    color: #94A3B8;
  }

  ul {
    padding-left: 20px;
  }

  li {
    font-size: 14.5px;
    line-height: 1.8;
    color: #CBD5E1;
    margin-bottom: 6px;
  }
}

/* 课程目录 */
.catalog-content {
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 14px;
  overflow: hidden;
  background: rgba(12, 17, 29, 0.5);
}

.chapter {
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);

  &:last-child {
    border-bottom: none;
  }
}

.chapter-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px 22px;
  background: rgba(255, 255, 255, 0.03);
  cursor: pointer;
  font-weight: 600;
  font-size: 15px;
  color: #FFFFFF;
  transition: all 0.2s ease;

  &:hover {
    background: rgba(56, 189, 248, 0.08);
    color: #38BDF8;
  }
}

.chapter-meta {
  margin-left: auto;
  font-family: var(--mono);
  font-size: 12.5px;
  color: #64748B;
}

.chapter-sections {
  padding: 0;
}

.section-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 13px 22px 13px 48px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.04);
  color: #CBD5E1;
  font-size: 14px;
  transition: all 0.15s ease;

  &:last-child {
    border-bottom: none;
  }

  &:hover {
    background: rgba(255, 255, 255, 0.03);
    color: #38BDF8;
  }
}

.section-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.section-duration {
  font-family: var(--mono);
  font-size: 12px;
  color: #64748B;
}

/* 问答 */
.qa-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.qa-item {
  padding: 18px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 12px;
}

.qa-question,
.qa-answer {
  display: flex;
  gap: 10px;
  margin-bottom: 10px;
  font-size: 14px;
  line-height: 1.6;
}

.qa-question:last-child,
.qa-answer:last-child {
  margin-bottom: 0;
}

.qa-badge {
  width: 24px;
  height: 24px;
  border-radius: 6px;
  background: linear-gradient(135deg, #0284C7, #2563EB);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 700;
  flex-shrink: 0;
}

.qa-badge.answer {
  background: linear-gradient(135deg, #059669, #10B981);
}

/* 笔记 */
.notes-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.note-item {
  padding: 18px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 12px;
}

.note-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.note-section {
  font-size: 13px;
  color: #38BDF8;
  font-weight: 500;
}

.note-time {
  font-family: var(--mono);
  font-size: 11.5px;
  color: #64748B;
}

.note-content {
  font-size: 14px;
  color: #CBD5E1;
  line-height: 1.65;
}

/* 侧边栏 */
.sidebar {
  width: 320px;
  display: flex;
  flex-direction: column;
  gap: 24px;

  :deep(.el-card) {
    background: rgba(16, 23, 38, 0.72) !important;
    border: 1px solid rgba(255, 255, 255, 0.08) !important;
    border-radius: 18px !important;
    box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.1) !important;
  }
}

/* 教师卡片 */
.teacher-info {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 14px;
}

.teacher-avatar {
  width: 58px;
  height: 58px;
  border-radius: 50%;
  overflow: hidden;
  border: 2px solid rgba(56, 189, 248, 0.4);

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.teacher-detail {
  h4 {
    margin: 0 0 4px;
    font-size: 16px;
    font-weight: 700;
    color: #FFFFFF;
  }
  p {
    margin: 0;
    font-size: 13px;
    color: #94A3B8;
  }
}

.teacher-desc {
  font-size: 13px;
  color: #94A3B8;
  line-height: 1.65;
  margin: 0;
}

/* FAQ */
.faq-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.faq-question {
  font-size: 14px;
  font-weight: 600;
  color: #F8FAFC;
  margin-bottom: 4px;
}

.faq-answer {
  font-size: 13px;
  color: #94A3B8;
  line-height: 1.6;
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
  padding: 10px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid transparent;
  transition: all 0.2s;

  &:hover {
    background: rgba(255, 255, 255, 0.05);
    border-color: rgba(56, 189, 248, 0.3);
    transform: translateX(2px);
  }

  img {
    width: 88px;
    height: 52px;
    border-radius: 8px;
    object-fit: cover;
  }

  .like-info {
    flex: 1;
  }

  .like-title {
    font-size: 13.5px;
    font-weight: 500;
    color: #E2E8F0;
    margin-bottom: 6px;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  .like-price {
    font-family: var(--mono);
    font-size: 14px;
    font-weight: 700;
    color: #34D399;
  }
}
</style>