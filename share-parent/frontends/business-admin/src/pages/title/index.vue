<template>
  <div class="question-management">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2>题库管理</h2>
      <el-button type="primary" :icon="Plus" @click="handleAdd">新增题目</el-button>
    </div>

    <!-- 统计卡片 -->
    <div class="stat-cards">
      <el-card shadow="hover" class="stat-card">
        <div class="stat-content">
          <div class="stat-icon" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%)">
            <span class="stat-number">{{ total }}</span>
          </div>
          <div class="stat-label">题目总数</div>
        </div>
      </el-card>
      <el-card shadow="hover" class="stat-card">
        <div class="stat-content">
          <div class="stat-icon" style="background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)">
            <span class="stat-number">{{ singleCount }}</span>
          </div>
          <div class="stat-label">单选题</div>
        </div>
      </el-card>
      <el-card shadow="hover" class="stat-card">
        <div class="stat-content">
          <div class="stat-icon" style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%)">
            <span class="stat-number">{{ multipleCount }}</span>
          </div>
          <div class="stat-label">多选题</div>
        </div>
      </el-card>
      <el-card shadow="hover" class="stat-card">
        <div class="stat-content">
          <div class="stat-icon" style="background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)">
            <span class="stat-number">{{ judgeCount }}</span>
          </div>
          <div class="stat-label">判断题</div>
        </div>
      </el-card>
    </div>

    <!-- 搜索区域 -->
    <el-card class="search-card" shadow="hover">
      <el-form :model="searchForm" inline>
        <el-form-item label="题型">
          <el-select v-model="searchForm.type" placeholder="全部题型" clearable style="width: 150px">
            <el-option label="全部" value="" />
            <el-option label="单选题" value="single" />
            <el-option label="多选题" value="multiple" />
            <el-option label="判断题" value="judge" />
            <el-option label="填空题" value="fill" />
            <el-option label="问答题" value="essay" />
          </el-select>
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="searchForm.categoryId" placeholder="全部分类" clearable style="width: 150px">
            <el-option label="全部" value="" />
            <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="searchForm.keyword" placeholder="题目内容" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 题目列表 -->
    <el-card class="table-card" shadow="hover">
      <el-table :data="questionList" stripe style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="type" label="题型" width="100">
          <template #default="{ row }">
            <el-tag :type="getTypeTag(row.type)">{{ getTypeText(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="题目内容" min-width="300" show-overflow-tooltip />
        <el-table-column prop="categoryName" label="分类" width="120" />
        <el-table-column prop="score" label="分值" width="80" />
        <el-table-column prop="difficulty" label="难度" width="100">
          <template #default="{ row }">
            <el-rate v-model="row.difficulty" disabled :max="3" />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="primary" link @click="handleView(row)">查看</el-button>
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

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="700px"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="100px"
      >
        <el-form-item label="题目类型" prop="type">
          <el-select v-model="formData.type" placeholder="请选择题型" style="width: 100%">
            <el-option label="单选题" value="single" />
            <el-option label="多选题" value="multiple" />
            <el-option label="判断题" value="judge" />
            <el-option label="填空题" value="fill" />
            <el-option label="问答题" value="essay" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属分类" prop="categoryId">
          <el-select v-model="formData.categoryId" placeholder="请选择分类" style="width: 100%">
            <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="题目内容" prop="title">
          <el-input v-model="formData.title" type="textarea" :rows="3" placeholder="请输入题目内容" />
        </el-form-item>
        <el-form-item v-if="formData.type !== 'fill' && formData.type !== 'essay'" label="选项" prop="options">
          <div v-for="(option, index) in formData.options" :key="index" class="option-item">
            <el-input v-model="formData.options[index]" :placeholder="`选项 ${String.fromCharCode(65 + index)}`" />
            <el-button v-if="formData.options.length > 2" type="danger" :icon="Delete" circle @click="removeOption(index)" />
          </div>
          <el-button type="primary" link @click="addOption">+ 添加选项</el-button>
        </el-form-item>
        <el-form-item label="正确答案" prop="answer">
          <el-input v-model="formData.answer" placeholder="请输入正确答案" />
        </el-form-item>
        <el-form-item label="分值" prop="score">
          <el-input-number v-model="formData.score" :min="1" :max="100" />
        </el-form-item>
        <el-form-item label="难度" prop="difficulty">
          <el-rate v-model="formData.difficulty" :max="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 查看弹窗 -->
    <el-dialog
      v-model="viewDialogVisible"
      title="题目详情"
      width="600px"
    >
      <div class="question-detail" v-if="currentQuestion">
        <div class="detail-item">
          <span class="label">题型：</span>
          <el-tag :type="getTypeTag(currentQuestion.type)">{{ getTypeText(currentQuestion.type) }}</el-tag>
        </div>
        <div class="detail-item">
          <span class="label">分类：</span>
          <span>{{ currentQuestion.categoryName }}</span>
        </div>
        <div class="detail-item">
          <span class="label">分值：</span>
          <span>{{ currentQuestion.score }}分</span>
        </div>
        <div class="detail-item">
          <span class="label">难度：</span>
          <el-rate v-model="currentQuestion.difficulty" disabled :max="3" />
        </div>
        <div class="detail-item">
          <span class="label">题目内容：</span>
          <p>{{ currentQuestion.title }}</p>
        </div>
        <div class="detail-item" v-if="currentQuestion.options && currentQuestion.options.length">
          <span class="label">选项：</span>
          <div class="options-list">
            <div v-for="(option, index) in currentQuestion.options" :key="index" class="option">
              <span class="option-label">{{ String.fromCharCode(65 + index) }}.</span>
              <span>{{ option }}</span>
            </div>
          </div>
        </div>
        <div class="detail-item">
          <span class="label">正确答案：</span>
          <span class="answer">{{ currentQuestion.answer }}</span>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete } from '@element-plus/icons-vue'
import { getTypeAll } from '@/api/api'
import {
  addSubject,
  checkName,
  deleteTitle,
  editSubject,
  getDetails,
  getSubjectPage
} from '@/api/title'

const categories = ref([])
const searchForm = reactive({ type: '', categoryId: '', keyword: '' })
const pagination = reactive({ page: 1, pageSize: 10, total: 0 })
const questionList = ref([])
const loading = ref(false)
const total = ref(0)
const singleCount = ref(0)
const multipleCount = ref(0)
const judgeCount = ref(0)

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref(null)
const formData = reactive({
  id: null,
  type: 'single',
  categoryId: '',
  title: '',
  options: ['', '', '', ''],
  answer: '',
  score: 10,
  difficulty: 1
})
const formRules = {
  type: [{ required: true, message: '请选择题型', trigger: 'change' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  title: [{ required: true, message: '请输入题目内容', trigger: 'blur' }],
  answer: [{ required: true, message: '请输入正确答案', trigger: 'blur' }]
}
const viewDialogVisible = ref(false)
const currentQuestion = ref(null)

const responseData = response => response?.data ?? response
const ensureSuccess = response => {
  if (response?.code !== 200) throw new Error(response?.msg || '请求失败')
  return responseData(response)
}
const pageOf = response => {
  const data = responseData(response) || {}
  return {
    rows: Array.isArray(data.list) ? data.list : (Array.isArray(data.rows) ? data.rows : []),
    total: Number(data.total ?? 0)
  }
}
const categoryName = id => categories.value.find(item => Number(item.id) === Number(id))?.name || '未分类'
const normalizeQuestion = item => {
  let options = item.options
  if (typeof options === 'string') {
    try { options = JSON.parse(options) } catch (error) { options = options ? [options] : [] }
  }
  return {
    ...item,
    type: item.type || item.questionType || 'single',
    title: item.title || item.stem || '',
    categoryName: categoryName(item.categoryId),
    options: Array.isArray(options) ? options : [],
    answer: item.answer || item.correctAnswer || '',
    score: Number(item.score ?? 0),
    difficulty: Number(item.difficulty ?? 1)
  }
}

const loadCategories = async () => {
  try {
    const response = await getTypeAll({ admin: true, includeDisabled: true })
    const data = responseData(response) || []
    const rows = data.list || data.rows || data
    categories.value = (Array.isArray(rows) ? rows : []).flatMap(item => [
      { id: item.id, name: item.name || item.categoryName },
      ...(item.children || []).map(child => ({ id: child.id, name: child.name || child.categoryName }))
    ])
  } catch (error) { categories.value = [] }
}

const loadStats = async () => {
  try {
    const page = pageOf(await getSubjectPage({ pageNo: 1, pageSize: 200, isBank: true }))
    const rows = page.rows.map(normalizeQuestion)
    total.value = page.total || rows.length
    singleCount.value = rows.filter(item => item.type === 'single').length
    multipleCount.value = rows.filter(item => item.type === 'multiple').length
    judgeCount.value = rows.filter(item => item.type === 'judge').length
  } catch (error) {
    total.value = singleCount.value = multipleCount.value = judgeCount.value = 0
  }
}

const getQuestionList = async () => {
  loading.value = true
  try {
    const page = pageOf(await getSubjectPage({
      pageNo: pagination.page,
      pageSize: pagination.pageSize,
      isBank: true,
      type: searchForm.type || undefined,
      categoryId: searchForm.categoryId || undefined,
      keyword: searchForm.keyword || undefined
    }))
    questionList.value = page.rows.map(normalizeQuestion)
    pagination.total = page.total
  } catch (error) {
    questionList.value = []
    pagination.total = 0
    ElMessage.error(error?.message || '题目加载失败')
  } finally { loading.value = false }
}

const handleSearch = () => {
  pagination.page = 1
  getQuestionList()
}
const handleReset = () => {
  Object.assign(searchForm, { type: '', categoryId: '', keyword: '' })
  handleSearch()
}
const handleSizeChange = () => {
  pagination.page = 1
  getQuestionList()
}
const handleCurrentChange = () => getQuestionList()

const handleAdd = () => {
  dialogTitle.value = '新增题目'
  Object.assign(formData, {
    id: null, type: 'single', categoryId: '', title: '',
    options: ['', '', '', ''], answer: '', score: 10, difficulty: 1
  })
  dialogVisible.value = true
}
const handleEdit = row => {
  dialogTitle.value = '编辑题目'
  Object.assign(formData, {
    ...row,
    options: Array.isArray(row.options) ? [...row.options] : []
  })
  dialogVisible.value = true
}
const handleView = async row => {
  try {
    currentQuestion.value = normalizeQuestion(ensureSuccess(await getDetails(row.id)))
  } catch (error) {
    currentQuestion.value = row
    ElMessage.error(error?.message || '题目详情加载失败')
  }
  viewDialogVisible.value = true
}
const addOption = () => formData.options.push('')
const removeOption = index => formData.options.splice(index, 1)

const handleSubmit = async () => {
  if (!formRef.value || !(await formRef.value.validate().catch(() => false))) return
  if (['single', 'multiple', 'judge'].includes(formData.type) && !formData.options.some(item => String(item).trim())) {
    return ElMessage.warning('请至少填写一个选项')
  }
  try {
    const duplicated = ensureSuccess(await checkName({ title: formData.title, id: formData.id || undefined }))
    if (duplicated?.existed) return ElMessage.warning('题目内容已存在')
    const payload = {
      id: formData.id,
      type: formData.type,
      questionType: formData.type,
      categoryId: formData.categoryId,
      title: formData.title.trim(),
      stem: formData.title.trim(),
      options: formData.options,
      answer: formData.answer.trim(),
      correctAnswer: formData.answer.trim(),
      score: Number(formData.score || 0),
      difficulty: Number(formData.difficulty || 1)
    }
    ensureSuccess(formData.id ? await editSubject(payload) : await addSubject(payload))
    ElMessage.success(formData.id ? '编辑成功' : '新增成功')
    dialogVisible.value = false
    await Promise.all([getQuestionList(), loadStats()])
  } catch (error) { ElMessage.error(error?.message || '保存失败') }
}

const handleDelete = row => ElMessageBox.confirm('确定要删除该题目吗？', '提示', { type: 'warning' }).then(async () => {
  try {
    ensureSuccess(await deleteTitle(row.id))
    ElMessage.success('删除成功')
    await Promise.all([getQuestionList(), loadStats()])
  } catch (error) { ElMessage.error(error?.message || '删除失败') }
}).catch(() => {})

const getTypeTag = type => ({ single: '', multiple: 'success', judge: 'warning', fill: 'info', essay: 'danger' }[type] || 'info')
const getTypeText = type => ({ single: '单选题', multiple: '多选题', judge: '判断题', fill: '填空题', essay: '问答题' }[type] || '未知')

onMounted(async () => {
  await loadCategories()
  await Promise.all([getQuestionList(), loadStats()])
})
</script>

<style scoped>
.question-management {
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
  grid-template-columns: repeat(4, 1fr);
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
.table-card :deep(.el-card__body) {
  padding: 0;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  padding: 20px;
}

/* 选项 */
.option-item {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}

.option-item .el-input {
  flex: 1;
}

/* 题目详情 */
.question-detail .detail-item {
  margin-bottom: 16px;
}

.question-detail .label {
  font-weight: 600;
  color: #303133;
  margin-right: 8px;
}

.question-detail .answer {
  color: #67c23a;
  font-weight: 600;
}

.options-list {
  margin-top: 8px;
}

.option {
  margin-bottom: 8px;
  color: #606266;
}

.option-label {
  font-weight: 600;
  margin-right: 8px;
}

/* 响应式 */
@media (max-width: 1200px) {
  .stat-cards {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
