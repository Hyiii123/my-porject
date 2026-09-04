<template>
  <div class="qa-management">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2>问答管理</h2>
    </div>

    <!-- 统计卡片 -->
    <div class="stat-cards">
      <el-card shadow="hover" class="stat-card">
        <div class="stat-content">
          <div class="stat-icon" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%)">
            <span class="stat-number">{{ total }}</span>
          </div>
          <div class="stat-label">问题总数</div>
        </div>
      </el-card>
      <el-card shadow="hover" class="stat-card">
        <div class="stat-content">
          <div class="stat-icon" style="background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)">
            <span class="stat-number">{{ solvedCount }}</span>
          </div>
          <div class="stat-label">已解决</div>
        </div>
      </el-card>
      <el-card shadow="hover" class="stat-card">
        <div class="stat-content">
          <div class="stat-icon" style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%)">
            <span class="stat-number">{{ unsolvedCount }}</span>
          </div>
          <div class="stat-label">待解决</div>
        </div>
      </el-card>
    </div>

    <!-- 搜索区域 -->
    <el-card class="search-card" shadow="hover">
      <el-form :model="searchForm" inline>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部状态" clearable style="width: 120px">
            <el-option label="全部" value="" />
            <el-option label="已解决" value="solved" />
            <el-option label="待解决" value="unsolved" />
          </el-select>
        </el-form-item>
        <el-form-item label="课程">
          <el-select v-model="searchForm.courseId" placeholder="全部课程" clearable style="width: 150px">
            <el-option label="全部" value="" />
            <el-option v-for="course in courses" :key="course.id" :label="course.name" :value="course.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="searchForm.keyword" placeholder="问题内容" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 问答列表 -->
    <el-card class="table-card" shadow="hover">
      <el-table :data="qaList" stripe style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column label="问题内容" min-width="300">
          <template #default="{ row }">
            <div class="question-content">
              <div class="question-title">{{ row.title }}</div>
              <div class="question-meta">
                <span>提问者：{{ row.userName }}</span>
                <span>课程：{{ row.courseName }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="answers" label="回答数" width="80" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'solved' ? 'success' : 'warning'">
              {{ row.status === 'solved' ? '已解决' : '待解决' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="提问时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleView(row)">查看</el-button>
            <el-button type="success" link @click="handleReply(row)">回复</el-button>
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

    <!-- 查看弹窗 -->
    <el-dialog
      v-model="viewDialogVisible"
      title="问答详情"
      width="700px"
    >
      <div class="qa-detail" v-if="currentQA">
        <div class="question-section">
          <div class="question-header">
            <el-tag :type="currentQA.status === 'solved' ? 'success' : 'warning'">
              {{ currentQA.status === 'solved' ? '已解决' : '待解决' }}
            </el-tag>
            <span class="question-time">{{ currentQA.createTime }}</span>
          </div>
          <h3>{{ currentQA.title }}</h3>
          <div class="question-body">
            <p>{{ currentQA.content }}</p>
          </div>
          <div class="question-info">
            <span>提问者：{{ currentQA.userName }}</span>
            <span>课程：{{ currentQA.courseName }}</span>
          </div>
        </div>

        <div class="answers-section">
          <h4>回答 ({{ currentQA.answerList.length }})</h4>
          <div v-for="(answer, index) in currentQA.answerList" :key="index" class="answer-item">
            <div class="answer-header">
              <span class="answer-user">{{ answer.userName }}</span>
              <span class="answer-time">{{ answer.createTime }}</span>
              <el-tag v-if="answer.isAccepted" type="success" size="small">最佳答案</el-tag>
            </div>
            <div class="answer-content">
              <p>{{ answer.content }}</p>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- 回复弹窗 -->
    <el-dialog
      v-model="replyDialogVisible"
      title="回复问题"
      width="600px"
    >
      <el-form :model="replyForm" label-width="80px">
        <el-form-item label="问题">
          <span>{{ currentQA?.title }}</span>
        </el-form-item>
        <el-form-item label="回复内容">
          <el-input v-model="replyForm.content" type="textarea" :rows="4" placeholder="请输入回复内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="replyDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitReply">提交回复</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCoursesPage } from '@/api/curriculum'
import {
  getQuestionPage,
  getQuestionsDetails,
  getReplies,
  saveQuestionsReply,
  setQuestionsFolded
} from '@/api/question'

const courses = ref([])
const searchForm = reactive({ status: '', courseId: '', keyword: '' })
const pagination = reactive({ page: 1, pageSize: 10, total: 0 })
const qaList = ref([])
const loading = ref(false)
const total = ref(0)
const solvedCount = ref(0)
const unsolvedCount = ref(0)
const viewDialogVisible = ref(false)
const replyDialogVisible = ref(false)
const currentQA = ref(null)
const replyForm = reactive({ content: '' })

const responseData = response => response?.data ?? response
const ensureSuccess = response => {
  if (response?.code !== 200) throw new Error(response?.msg || '请求失败')
  return responseData(response)
}
const pageRows = response => {
  const data = responseData(response) || {}
  return {
    rows: Array.isArray(data.rows) ? data.rows : (Array.isArray(data.list) ? data.list : []),
    total: Number(data.total ?? 0)
  }
}
const courseName = id => courses.value.find(item => Number(item.id) === Number(id))?.name || ('课程' + (id || ''))
const normalizeQA = item => {
  const answers = Number(item.replyCount ?? item.replyTimes ?? item.answers ?? 0)
  return {
    ...item,
    userName: item.userName || ('用户' + (item.userId || '')),
    courseName: item.courseName || courseName(item.courseId),
    answers,
    status: answers > 0 ? 'solved' : 'unsolved',
    answerList: item.answerList || []
  }
}
const normalizeReply = item => ({
  ...item,
  userName: item.userName || ('用户' + (item.userId || '')),
  isAccepted: Boolean(item.isAccepted)
})

const loadCourses = async () => {
  try {
    const response = await getCoursesPage({ pageNo: 1, pageSize: 200 })
    const data = responseData(response) || {}
    const rows = data.list || data.rows || data
    courses.value = (Array.isArray(rows) ? rows : []).map(item => ({
      id: item.id,
      name: item.name || item.title || item.courseName
    }))
  } catch (error) {
    courses.value = []
  }
}

const loadQAStats = async () => {
  try {
    const page = pageRows(await getQuestionPage({ pageNo: 1, pageSize: 200 }))
    const values = page.rows.map(normalizeQA)
    total.value = page.total || values.length
    solvedCount.value = values.filter(item => item.status === 'solved').length
    unsolvedCount.value = values.filter(item => item.status === 'unsolved').length
  } catch (error) {
    total.value = solvedCount.value = unsolvedCount.value = 0
  }
}

const getQAList = async () => {
  loading.value = true
  try {
    const response = await getQuestionPage({
      pageNo: pagination.page,
      pageSize: pagination.pageSize,
      status: searchForm.status || undefined,
      courseId: searchForm.courseId || undefined,
      keyword: searchForm.keyword || undefined
    })
    const page = pageRows(response)
    qaList.value = page.rows.map(normalizeQA)
    pagination.total = page.total
  } catch (error) {
    qaList.value = []
    pagination.total = 0
    ElMessage.error(error?.message || '问答加载失败')
  } finally {
    loading.value = false
  }
}

const loadQuestionDetails = async id => {
  const question = normalizeQA(ensureSuccess(await getQuestionsDetails(id)))
  const replyPage = pageRows(await getReplies({ questionId: id, pageNo: 1, pageSize: 200 }))
  question.answerList = replyPage.rows.map(normalizeReply)
  question.answers = question.answerList.length
  question.status = question.answers > 0 ? 'solved' : 'unsolved'
  return question
}

const handleSearch = () => {
  pagination.page = 1
  getQAList()
  loadQAStats()
}
const handleReset = () => {
  Object.assign(searchForm, { status: '', courseId: '', keyword: '' })
  handleSearch()
}
const handleSizeChange = () => {
  pagination.page = 1
  getQAList()
}
const handleCurrentChange = () => getQAList()

const handleView = async row => {
  try {
    currentQA.value = await loadQuestionDetails(row.id)
  } catch (error) {
    currentQA.value = normalizeQA(row)
    ElMessage.error(error?.message || '问答详情加载失败')
  }
  viewDialogVisible.value = true
}
const handleReply = row => {
  currentQA.value = normalizeQA(row)
  replyForm.content = ''
  replyDialogVisible.value = true
}
const handleSubmitReply = async () => {
  if (!replyForm.content.trim()) return ElMessage.warning('请输入回复内容')
  if (!currentQA.value?.id) return ElMessage.error('问题编号缺失，无法回复')
  try {
    ensureSuccess(await saveQuestionsReply({
      questionId: currentQA.value.id,
      content: replyForm.content.trim()
    }))
    ElMessage.success('回复成功')
    replyDialogVisible.value = false
    currentQA.value = await loadQuestionDetails(currentQA.value.id)
    await Promise.all([getQAList(), loadQAStats()])
  } catch (error) {
    ElMessage.error(error?.message || '回复失败')
  }
}
const handleDelete = row => ElMessageBox.confirm(
  '确定要隐藏该问答吗？隐藏后用户端将不可见。',
  '提示',
  { type: 'warning' }
).then(async () => {
  try {
    ensureSuccess(await setQuestionsFolded({ id: row.id, hidden: true }))
    ElMessage.success('问答已隐藏')
    await Promise.all([getQAList(), loadQAStats()])
  } catch (error) {
    ElMessage.error(error?.message || '隐藏失败')
  }
}).catch(() => {})

onMounted(async () => {
  await loadCourses()
  await Promise.all([getQAList(), loadQAStats()])
})
</script>

<style scoped>
.qa-management {
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
  font-size: 24px;
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

/* 问题内容 */
.question-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.question-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.question-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #909399;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  padding: 20px;
}

/* 问答详情 */
.qa-detail {
  max-height: 600px;
  overflow-y: auto;
}

.question-section {
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
  margin-bottom: 20px;
}

.question-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.question-time {
  font-size: 13px;
  color: #909399;
}

.question-section h3 {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 12px;
}

.question-body p {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
  margin: 0;
}

.question-info {
  display: flex;
  gap: 24px;
  margin-top: 12px;
  font-size: 13px;
  color: #909399;
}

/* 回答区域 */
.answers-section h4 {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 16px;
}

.answer-item {
  padding: 16px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  margin-bottom: 12px;
}

.answer-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.answer-user {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.answer-time {
  font-size: 12px;
  color: #909399;
}

.answer-content p {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
  margin: 0;
}
</style>
