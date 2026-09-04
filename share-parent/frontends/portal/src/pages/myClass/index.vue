<template>
  <div class="my-class-container">
    <div class="container">
      <h2 class="page-title">我的课表</h2>

      <!-- 学习统计 -->
      <div class="learning-stats">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%)">
              <el-icon :size="28" color="#fff"><Reading /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ totalCourses }}</div>
              <div class="stat-label">在学课程</div>
            </div>
          </div>
        </el-card>
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)">
              <el-icon :size="28" color="#fff"><Check /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ completedCourses }}</div>
              <div class="stat-label">已完成</div>
            </div>
          </div>
        </el-card>
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%)">
              <el-icon :size="28" color="#fff"><Clock /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ totalHours }}</div>
              <div class="stat-label">学习时长(小时)</div>
            </div>
          </div>
        </el-card>
      </div>

      <!-- 课程筛选 -->
      <div class="filter-section">
        <el-tabs v-model="activeTab" @tab-change="handleTabChange">
          <el-tab-pane label="全部课程" name="all" />
          <el-tab-pane label="学习中" name="learning" />
          <el-tab-pane label="已完成" name="completed" />
        </el-tabs>
      </div>

      <!-- 课程列表 -->
      <div class="course-list">
        <el-card
          v-for="course in filteredCourses"
          :key="course.id"
          class="course-card"
          shadow="hover"
          @click="goToLearn(course)"
        >
          <div class="course-content">
            <div class="course-cover">
              <img :src="course.cover" :alt="course.courseName" />
              <div class="course-badge" v-if="course.progress === 100">已完成</div>
            </div>
            <div class="course-info">
              <h4 class="course-title">{{ course.courseName }}</h4>
              <div class="course-meta">
                <span><el-icon><User /></el-icon> {{ course.teacherName }}</span>
                <span><el-icon><Reading /></el-icon> {{ course.completedLessons }}/{{ course.totalLessons }} 课时</span>
              </div>
              <div class="progress-section">
                <div class="progress-bar">
                  <div class="progress-fill" :style="{ width: course.progress + '%' }"></div>
                </div>
                <span class="progress-text">{{ course.progress }}%</span>
              </div>
              <div class="course-time">
                上次学习：{{ course.lastLearnTime }}
              </div>
            </div>
            <div class="course-actions">
              <el-button
                v-if="course.progress < 100"
                type="primary"
                @click.stop="goToLearn(course)"
              >
                继续学习
              </el-button>
              <el-button
                v-else
                type="success"
                @click.stop="goToLearn(course)"
              >
                复习课程
              </el-button>
              <el-button
                v-if="course.progress === 100"
                :loading="restartingCourseId === course.courseId"
                @click.stop="handleRestart(course)"
              >
                重新学习
              </el-button>
              <el-button
                @click.stop="handleRemove(course)"
              >
                移除
              </el-button>
            </div>
          </div>
        </el-card>
      </div>

      <!-- 空状态 -->
      <div v-if="filteredCourses.length === 0" class="empty-state">
        <el-empty description="暂无课程">
          <el-button type="primary" @click="$router.push('/search/index')">去选课</el-button>
        </el-empty>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Reading, Check, Clock, User } from '@element-plus/icons-vue'
import { getMylessons, delMyClass, restartMyLesson } from '@/api/class.js'

const router = useRouter()

// Tab 切换
const activeTab = ref('all')

// 学习统计
const totalCourses = computed(() => courses.value.length)
const completedCourses = computed(() => courses.value.filter(course => Number(course.progress) >= 100).length)
const totalHours = computed(() => Math.round(courses.value.reduce((sum, course) => sum + Number(course.learnDurationSeconds || 0), 0) / 3600))

// 课程数据
const courses = ref([])
const restartingCourseId = ref(null)

const normalizeCourse = (course = {}) => ({
  ...course,
  id: course.id,
  courseId: course.courseId || course.id,
  courseName: course.courseName || course.title || '未命名课程',
  teacherName: course.teacherName || '讲师团队',
  cover: course.cover || course.coverUrl || '',
  progress: Math.min(100, Math.max(0, Number(course.progress ?? course.progressPercent ?? 0))),
  completedLessons: Number(course.completedLessons || 0),
  totalLessons: Number(course.totalLessons || course.lessonCount || 0),
  learnDurationSeconds: Number(course.learnDurationSeconds || 0),
  lastLearnTime: course.lastLearnTime || course.updateTime || '暂无记录'
})

const loadCourses = async () => {
  try {
    const response = await getMylessons()
    if (response?.code !== 200) throw new Error(response?.msg || '我的课表加载失败')
    const data = response.data || {}
    const rows = Array.isArray(data) ? data : (data.list || data.rows || [])
    courses.value = rows.map(normalizeCourse)
  } catch (error) {
    courses.value = []
    ElMessage.error(error?.message || '我的课表加载失败，请先登录')
  }
}

// 筛选后的课程
const filteredCourses = computed(() => {
  if (activeTab.value === 'learning') {
    return courses.value.filter(c => c.progress < 100)
  } else if (activeTab.value === 'completed') {
    return courses.value.filter(c => c.progress === 100)
  }
  return courses.value
})

// Tab 切换
const handleTabChange = () => {
  // 触发计算属性更新
}

// 去学习
const goToLearn = (course) => {
  router.push(`/learning/index?courseId=${course.courseId}`)
}

// 重置已完成课程的进度，但保留报名关系
const handleRestart = async (course) => {
  try {
    await ElMessageBox.confirm(
      `确定要重新开始《${course.courseName}》吗？当前学习进度和学习时长会清零。`,
      '重新学习',
      { type: 'warning', confirmButtonText: '重新开始', cancelButtonText: '取消' }
    )
    restartingCourseId.value = course.courseId
    const response = await restartMyLesson(course.courseId)
    if (response?.code !== 200) throw new Error(response?.msg || '课程进度重置失败')
    await loadCourses()
    ElMessage.success('课程已重新开始')
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error?.message || '课程进度重置失败')
    }
  } finally {
    restartingCourseId.value = null
  }
}

// 移除课程
const handleRemove = (course) => {
  ElMessageBox.confirm('确定要从课表中移除该课程吗？', '提示', {
    type: 'warning'
  }).then(async () => {
    const response = await delMyClass(course.courseId)
    if (response?.code !== 200) throw new Error(response?.msg || '移除失败')
    const index = courses.value.findIndex(c => c.id === course.id)
    if (index !== -1) courses.value.splice(index, 1)
    ElMessage.success('课程已移除')
  }).catch(error => {
    if (error) ElMessage.error(error?.message || '课程移除失败')
  })
}

onMounted(loadCourses)
</script>

<style scoped>
.my-class-container {
  background: #f5f7fa;
  min-height: 100vh;
  padding: 30px 0;
}

.container {
  max-width: 1000px;
  margin: 0 auto;
  padding: 0 20px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 24px;
}

/* 学习统计 */
.learning-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  margin-bottom: 24px;
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

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}

/* 筛选 */
.filter-section {
  margin-bottom: 20px;
}

/* 课程列表 */
.course-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.course-card {
  border-radius: 12px;
  border: none;
  cursor: pointer;
  transition: all 0.2s;
}

.course-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}

.course-content {
  display: flex;
  align-items: center;
  gap: 20px;
}

.course-cover {
  width: 160px;
  height: 90px;
  border-radius: 8px;
  overflow: hidden;
  position: relative;
  flex-shrink: 0;
}

.course-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.course-badge {
  position: absolute;
  top: 8px;
  left: 8px;
  padding: 2px 8px;
  background: #67c23a;
  color: #fff;
  font-size: 12px;
  border-radius: 4px;
}

.course-info {
  flex: 1;
}

.course-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 8px;
}

.course-meta {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: #909399;
  margin-bottom: 12px;
}

.course-meta .el-icon {
  margin-right: 4px;
}

.progress-section {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.progress-bar {
  flex: 1;
  height: 8px;
  background: #f0f0f0;
  border-radius: 4px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #409eff 0%, #67c23a 100%);
  border-radius: 4px;
  transition: width 0.3s ease;
}

.progress-text {
  font-size: 14px;
  font-weight: 600;
  color: #409eff;
  min-width: 40px;
}

.course-time {
  font-size: 12px;
  color: #c0c4cc;
}

.course-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex-shrink: 0;
}

/* 空状态 */
.empty-state {
  padding: 60px 0;
}

/* 响应式 */
@media (max-width: 768px) {
  .learning-stats {
    grid-template-columns: 1fr;
  }

  .course-content {
    flex-direction: column;
    align-items: flex-start;
  }

  .course-cover {
    width: 100%;
    height: 120px;
  }

  .course-actions {
    flex-direction: row;
    width: 100%;
  }

  .course-actions .el-button {
    flex: 1;
  }
}
</style>
