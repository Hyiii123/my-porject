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
          <video
            v-if="currentSection.mediaUrl"
            ref="videoPlayerRef"
            class="video-element"
            controls
            :src="currentSection.mediaUrl"
            @timeupdate="handleVideoTimeUpdate"
            @ended="handleVideoEnded"
            @pause="handleVideoPause"
            @loadedmetadata="handleVideoLoadedMetadata"
          />
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
          <div class="course-desc">{{ course.shortDescription || course.description }}</div>
        </div>

        <!-- Tab 切换 -->
        <el-tabs v-model="activeTab" class="detail-tabs">
          <el-tab-pane label="课程介绍" name="intro">
            <div class="intro-content">
              <h3>课程简介</h3>
              <p>{{ course.description || course.shortDescription }}</p>
              <div v-if="course.skills" class="intro-section">
                <h3>核心技术栈</h3>
                <p>{{ course.skills }}</p>
              </div>
              <div v-if="course.prerequisites" class="intro-section">
                <h3>适合人群</h3>
                <p>{{ course.prerequisites }}</p>
              </div>
            </div>
          </el-tab-pane>

          <el-tab-pane label="随堂测验" name="quiz">
            <div class="quiz-content" v-loading="quizLoading">
              <!-- 无测验时的空状态 -->
              <div v-if="!quizExam && !quizQuestions.length" class="quiz-empty">
                <el-empty description="当前课程暂无配套随堂测试题" :image-size="100">
                  <template #description>
                    <p style="color: #64748b; font-size: 14px; margin-top: 8px;">当前课程暂无专属小节测验，您可在个人中心查阅综合能力模考</p>
                  </template>
                  <el-button type="primary" @click="$router.push('/personal/main/myExam')">查看我的考试</el-button>
                </el-empty>
              </div>

              <!-- 存在测验题目 -->
              <div v-else>
                <!-- 测验头部概要 -->
                <div class="quiz-header-card">
                  <div class="quiz-header-info">
                    <h3 class="quiz-title">{{ quizExam?.examName || '随堂能力测评' }}</h3>
                    <div class="quiz-badges">
                      <el-tag type="info" effect="plain">共 {{ quizQuestions.length }} 题</el-tag>
                      <el-tag type="warning" effect="plain">满分 {{ quizExam?.totalScore || 100 }} 分</el-tag>
                      <el-tag type="success" effect="plain">及格线 {{ quizExam?.passScore || 60 }} 分</el-tag>
                    </div>
                  </div>
                  <div class="quiz-header-actions" v-if="quizSubmitted">
                    <el-button size="small" @click="handleRetakeQuiz">重新测试</el-button>
                    <el-button type="primary" size="small" @click="$router.push({ name: 'myExamDetails', query: { id: quizResult?.id } })">查看完整批阅</el-button>
                  </div>
                </div>

                <!-- 成绩大屏 (提交后展示) -->
                <div v-if="quizSubmitted" class="quiz-result-banner" :class="quizResult?.status === 1 ? 'pass' : 'fail'">
                  <div class="result-score">
                    <span class="num">{{ quizResult?.score ?? 0 }}</span>
                    <span class="unit">分</span>
                  </div>
                  <div class="result-meta">
                    <div class="result-status">{{ quizResult?.status === 1 ? '🎉 恭喜通过随堂测评！' : '💪 还需继续加油哦！' }}</div>
                    <div class="result-detail">答对 {{ quizResult?.correctCount ?? 0 }} / {{ quizQuestions.length }} 题 ｜ 所属测评：{{ quizExam?.examName }}</div>
                  </div>
                </div>

                <!-- 题目列表 -->
                <div class="quiz-question-list">
                  <div v-for="(q, qIdx) in quizQuestions" :key="q.id || qIdx" class="quiz-card">
                    <div class="quiz-question-header">
                      <span class="quiz-index">{{ qIdx + 1 }}.</span>
                      <el-tag size="small" :type="getQuestionTypeTag(q.type)">{{ getQuestionTypeName(q.type) }}</el-tag>
                      <span class="quiz-score">({{ q.score || 10 }}分)</span>
                      <span class="quiz-stem" v-html="q.title || q.stem"></span>
                    </div>

                    <!-- 单选题 -->
                    <div v-if="q.type === 1" class="quiz-options">
                      <el-radio-group v-model="userAnswers[q.id]" :disabled="quizSubmitted">
                        <div v-for="(opt, optIdx) in q.options" :key="optIdx" class="quiz-opt-item">
                          <el-radio :label="getOptionLabel(optIdx)">
                            <span class="opt-label">{{ getOptionLabel(optIdx) }}.</span>
                            <span class="opt-text" v-html="opt"></span>
                          </el-radio>
                        </div>
                      </el-radio-group>
                    </div>

                    <!-- 多选题 / 不定向 -->
                    <div v-else-if="q.type === 2 || q.type === 3" class="quiz-options">
                      <el-checkbox-group v-model="userAnswers[q.id]" :disabled="quizSubmitted">
                        <div v-for="(opt, optIdx) in q.options" :key="optIdx" class="quiz-opt-item">
                          <el-checkbox :label="getOptionLabel(optIdx)">
                            <span class="opt-label">{{ getOptionLabel(optIdx) }}.</span>
                            <span class="opt-text" v-html="opt"></span>
                          </el-checkbox>
                        </div>
                      </el-checkbox-group>
                    </div>

                    <!-- 判断题 -->
                    <div v-else-if="q.type === 4" class="quiz-options">
                      <el-radio-group v-model="userAnswers[q.id]" :disabled="quizSubmitted">
                        <div class="quiz-opt-item">
                          <el-radio label="A">A. 正确</el-radio>
                        </div>
                        <div class="quiz-opt-item">
                          <el-radio label="B">B. 错误</el-radio>
                        </div>
                      </el-radio-group>
                    </div>

                    <!-- 问答 / 主观题 -->
                    <div v-else class="quiz-options">
                      <el-input v-model="userAnswers[q.id]" type="textarea" :rows="3" placeholder="请输入你的回答..." :disabled="quizSubmitted" />
                    </div>

                    <!-- 提交后的解析展示 -->
                    <div v-if="quizSubmitted" class="quiz-analysis-box">
                      <div class="ans-row">
                        <span class="ans-label">你的作答：</span>
                        <span :class="isAnswerCorrect(q) ? 'ans-right' : 'ans-wrong'">
                          {{ formatUserDisplayAnswer(q.type, userAnswers[q.id]) }}
                        </span>
                        <span class="ans-label marg-l">正确答案：</span>
                        <span class="ans-right">{{ formatUserDisplayAnswer(q.type, q.correctAnswer || q.answer) }}</span>
                      </div>
                      <div class="analysis-text" v-if="q.analysis">
                        <span class="anal-label">💡 考点解析：</span>
                        <span v-html="q.analysis"></span>
                      </div>
                    </div>
                  </div>
                </div>

                <!-- 提交答卷操作栏 (未提交状态) -->
                <div v-if="!quizSubmitted" class="quiz-submit-bar">
                  <div class="answered-progress">
                    已答 <strong>{{ answeredCount }}</strong> / {{ quizQuestions.length }} 题
                  </div>
                  <div class="bar-btns">
                    <el-button @click="handleResetAnswers">重置作答</el-button>
                    <el-button type="primary" :loading="quizSubmitting" @click="handleSubmitQuiz">提交测验</el-button>
                  </div>
                </div>
              </div>
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
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, ArrowRight, ArrowDown, VideoPlay, Document } from '@element-plus/icons-vue'
import { getClassDetails, getAskList, getReply, postQuestions } from '@/api/classDetails.js'
import { getCourseLearning, getLearningClassDetails, getMediasSignature, addPlayLog, getLearningLog } from '@/api/class.js'
import { getAllNotes, addNotes } from '@/api/notes.js'
import { getSubject, postSubject } from '@/api/subject.js'

const cleanHtml = (text) => {
  if (!text) return ''
  return String(text).replace(/<[^>]+>/g, '').trim()
}

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
  shortDescription: '',
  skills: '',
  targetRole: '',
  prerequisites: ''
})
const currentSection = ref({})
const chapters = reactive([])
const activeTab = ref('intro')
const newQuestion = ref('')
const qaList = reactive([])
const newNote = ref('')
const noteIsPublic = ref(true)
const notes = reactive([])

const quizLoading = ref(false)
const quizSubmitting = ref(false)
const quizExam = ref(null)
const quizQuestions = ref([])
const userAnswers = reactive({})
const quizSubmitted = ref(false)
const quizResult = ref(null)

const answeredCount = computed(() => {
  return quizQuestions.value.filter(q => {
    const ans = userAnswers[q.id]
    if (Array.isArray(ans)) return ans.length > 0
    return ans !== undefined && ans !== null && String(ans).trim() !== ''
  }).length
})

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
      const rawDesc = value.description || value.shortDescription || ''
      const rawShortDesc = value.shortDescription || value.description || ''
      course.value = {
        ...course.value,
        ...value,
        id,
        title: value.title || value.courseName || value.name || '未命名课程',
        teacherName: value.teacherName || '讲师团队',
        lessons: Number(value.lessons ?? value.lessonCount ?? 0),
        learners: Number(value.learners ?? value.learnerCount ?? 0),
        description: cleanHtml(rawDesc),
        shortDescription: cleanHtml(rawShortDesc),
        skills: cleanHtml(value.skills || ''),
        targetRole: cleanHtml(value.targetRole || ''),
        prerequisites: cleanHtml(value.prerequisites || '')
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

    // 异步拉取随堂测验考题
    loadQuiz(id)
  } catch (error) {
    ElMessage.error(error?.message || '学习数据加载失败')
  } finally {
    loading.value = false
  }
}

const videoPlayerRef = ref(null)
let lastReportTime = 0
let lastReportProgress = 0

// 上报小节学习记录
const reportProgress = async (isFinished = false) => {
  const video = videoPlayerRef.value
  const cId = course.value.id
  const catId = currentSection.value.id
  if (!video || !cId || !catId) return

  const currentTime = Math.floor(video.currentTime || 0)
  const duration = Math.floor(video.duration || 0)
  const percent = isFinished ? 100 : (duration > 0 ? Math.min(100, Math.round((currentTime / duration) * 100)) : 0)

  try {
    const res = await addPlayLog({
      courseId: cId,
      catalogId: catId,
      progressSeconds: currentTime,
      learnDurationSeconds: currentTime,
      progressPercent: percent,
      totalLessons: course.value.lessons || 1,
      status: percent >= 90 ? 2 : 1
    })
    lastReportTime = Date.now()
    lastReportProgress = currentTime
    if (res?.data?.progressPercent != null) {
      course.value.progress = Math.min(100, Math.max(0, Number(res.data.progressPercent)))
    }
  } catch (err) {
    console.debug('上报学习记录稍后重试:', err)
  }
}

// 视频播放进度节流更新（每15秒或变动较大时上报）
const handleVideoTimeUpdate = () => {
  const now = Date.now()
  const video = videoPlayerRef.value
  if (!video) return
  const currentTime = Math.floor(video.currentTime || 0)
  if (now - lastReportTime > 15000 || Math.abs(currentTime - lastReportProgress) >= 15) {
    reportProgress(false)
  }
}

const handleVideoPause = () => {
  reportProgress(false)
}

const handleVideoEnded = () => {
  reportProgress(true)
  const allSections = chapters.flatMap(c => c.sections || [])
  const currentIndex = allSections.findIndex(s => s.id === currentSection.value.id)
  if (currentIndex !== -1 && currentIndex < allSections.length - 1) {
    const nextSec = allSections[currentIndex + 1]
    ElMessage.success({
      message: `恭喜完成【${currentSection.value.title}】！即将自动播放下一节：${nextSec.title}`,
      duration: 3000
    })
    setTimeout(() => {
      handleSelectSection(nextSec)
    }, 2500)
  } else {
    ElMessage.success({
      message: `恭喜学完本课程全部小节！`,
      duration: 4000
    })
  }
}

// 当视频元数据就绪后，尝试恢复上次学习进度
const handleVideoLoadedMetadata = async () => {
  const catId = currentSection.value.id
  if (!catId) return
  try {
    const res = await getLearningLog(catId)
    if (res?.code === 200 && res.data?.progressSeconds > 0) {
      const savedSeconds = Number(res.data.progressSeconds)
      const video = videoPlayerRef.value
      if (video && video.duration && savedSeconds < video.duration - 5) {
        video.currentTime = savedSeconds
        ElMessage.info({
          message: `已为您恢复到上次学习进度：${formatDuration(savedSeconds)}`,
          duration: 2500
        })
      }
    }
  } catch (err) {
    // 忽略加载历史进度失败
  }
}

const handleSelectSection = async (section) => {
  // 切换前先保存当前小节学习进度
  await reportProgress(false)
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

const loadQuiz = async (cId) => {
  quizLoading.value = true
  try {
    const res = await getSubject({ courseId: cId })
    if (res?.code === 200 && res.data) {
      quizExam.value = res.data
      const rawQuestions = res.data.questions || []
      quizQuestions.value = rawQuestions.map(q => {
        let opts = q.options
        if (typeof opts === 'string') {
          try { opts = JSON.parse(opts) } catch (e) { opts = [] }
        }
        return {
          ...q,
          options: opts || []
        }
      })
      quizQuestions.value.forEach(q => {
        if (q.type === 2 || q.type === 3) {
          userAnswers[q.id] = []
        } else {
          userAnswers[q.id] = ''
        }
      })
      quizSubmitted.value = false
      quizResult.value = null
    }
  } catch (err) {
    console.debug('获取课程随堂测试失败:', err)
  } finally {
    quizLoading.value = false
  }
}

const getOptionLabel = (idx) => String.fromCharCode(65 + idx)

const getQuestionTypeName = (type) => {
  switch (Number(type)) {
    case 1: return '单选题'
    case 2: return '多选题'
    case 3: return '不定项'
    case 4: return '判断题'
    case 5: return '主观题'
    default: return '选择题'
  }
}

const getQuestionTypeTag = (type) => {
  switch (Number(type)) {
    case 1: return 'primary'
    case 2: return 'warning'
    case 3: return 'danger'
    case 4: return 'success'
    default: return 'info'
  }
}

const formatUserDisplayAnswer = (type, val) => {
  if (val === undefined || val === null || val === '') return '未作答'
  if (Array.isArray(val)) return val.length ? val.join(', ') : '未作答'
  if (Number(type) === 4) {
    if (val === 'A' || val === '1' || val === 1 || val === true || val === 'true') return 'A. 正确'
    if (val === 'B' || val === '0' || val === 0 || val === false || val === 'false') return 'B. 错误'
  }
  return String(val)
}

const isAnswerCorrect = (q) => {
  if (quizResult.value?.answers) {
    const recordAns = quizResult.value.answers.find(a => Number(a.questionId) === Number(q.id))
    if (recordAns) return Number(recordAns.isCorrect) === 1
  }
  const ans = userAnswers[q.id]
  const target = q.correctAnswer || q.answer
  if (!ans || !target) return false
  if (Array.isArray(ans)) {
    const joined = [...ans].sort().join(',')
    const targetArr = String(target).split(',').map(s => s.trim()).sort().join(',')
    return joined === targetArr
  }
  return String(ans).trim().toUpperCase() === String(target).trim().toUpperCase()
}

const handleResetAnswers = () => {
  quizQuestions.value.forEach(q => {
    if (q.type === 2 || q.type === 3) {
      userAnswers[q.id] = []
    } else {
      userAnswers[q.id] = ''
    }
  })
  ElMessage.info('已重置所有答题选项')
}

const handleRetakeQuiz = () => {
  quizSubmitted.value = false
  quizResult.value = null
  handleResetAnswers()
}

const handleSubmitQuiz = async () => {
  if (!quizExam.value?.id) {
    ElMessage.warning('测验信息无效')
    return
  }
  quizSubmitting.value = true
  try {
    const formattedAnswers = quizQuestions.value.map(q => {
      let raw = userAnswers[q.id]
      let ansStr = ''
      if (Array.isArray(raw)) {
        ansStr = raw.sort().join(',')
      } else if (raw !== undefined && raw !== null) {
        ansStr = String(raw).trim()
      }
      return {
        questionId: q.id,
        answer: ansStr,
        userAnswer: ansStr
      }
    })

    const payload = {
      examId: quizExam.value.id,
      courseId: course.value.id,
      answers: formattedAnswers
    }

    const res = await postSubject(payload)
    if (res?.code === 200 && res.data) {
      quizResult.value = res.data
      quizSubmitted.value = true
      ElMessage.success(`测验提交成功！得分：${res.data.score || 0} 分`)
    } else {
      throw new Error(res?.msg || '提交失败')
    }
  } catch (err) {
    ElMessage.error(err?.message || '测验提交失败，请登录后重试')
  } finally {
    quizSubmitting.value = false
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

/* 随堂测验样式 */
.quiz-content {
  min-height: 240px;
}

.quiz-empty {
  padding: 40px 0;
  text-align: center;
}

.quiz-header-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: #f8fafc;
  border-radius: 8px;
  margin-bottom: 20px;
  border: 1px solid #e2e8f0;
}

.quiz-title {
  margin: 0 0 8px 0;
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
}

.quiz-badges {
  display: flex;
  gap: 8px;
}

.quiz-header-actions {
  display: flex;
  gap: 10px;
}

.quiz-result-banner {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 16px 24px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.quiz-result-banner.pass {
  background: linear-gradient(135deg, #ecfdf5 0%, #d1fae5 100%);
  border: 1px solid #a7f3d0;
  color: #065f46;
}

.quiz-result-banner.fail {
  background: linear-gradient(135deg, #fef2f2 0%, #fee2e2 100%);
  border: 1px solid #fecaca;
  color: #991b1b;
}

.result-score .num {
  font-size: 32px;
  font-weight: 700;
  line-height: 1;
}

.result-score .unit {
  font-size: 14px;
  margin-left: 2px;
}

.result-status {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 4px;
}

.result-detail {
  font-size: 13px;
  opacity: 0.85;
}

.quiz-question-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.quiz-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 18px 20px;
  transition: box-shadow 0.2s ease;
}

.quiz-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.quiz-question-header {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 14px;
  line-height: 1.6;
}

.quiz-index {
  font-weight: 700;
  color: #334155;
  font-size: 15px;
}

.quiz-score {
  font-size: 13px;
  color: #64748b;
  white-space: nowrap;
}

.quiz-stem {
  font-size: 15px;
  font-weight: 500;
  color: #1e293b;
  flex: 1;
}

.quiz-options {
  padding-left: 8px;
  margin-bottom: 12px;
}

.quiz-opt-item {
  margin-bottom: 10px;
}

.quiz-opt-item:last-child {
  margin-bottom: 0;
}

.opt-label {
  font-weight: 600;
  margin-right: 6px;
  color: #475569;
}

.opt-text {
  color: #334155;
}

.quiz-analysis-box {
  margin-top: 14px;
  padding: 12px 16px;
  background: #f8fafc;
  border-radius: 6px;
  border-left: 4px solid #3b82f6;
  font-size: 13px;
}

.ans-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 6px;
}

.ans-label {
  color: #64748b;
  font-weight: 500;
}

.ans-label.marg-l {
  margin-left: 16px;
}

.ans-right {
  color: #10b981;
  font-weight: 600;
}

.ans-wrong {
  color: #ef4444;
  font-weight: 600;
}

.analysis-text {
  color: #475569;
  line-height: 1.6;
  margin-top: 4px;
}

.anal-label {
  font-weight: 600;
  color: #3b82f6;
}

.quiz-submit-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  margin-top: 24px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
}

.answered-progress {
  font-size: 14px;
  color: #64748b;
}

.answered-progress strong {
  color: #3b82f6;
  font-size: 16px;
}

.bar-btns {
  display: flex;
  gap: 12px;
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
