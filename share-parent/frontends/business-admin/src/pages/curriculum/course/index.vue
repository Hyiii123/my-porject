<template>
  <div class="course-management">
    <!-- 统计卡片 -->
    <div class="stat-cards">
      <el-card v-for="(item, index) in statCards" :key="index" shadow="hover" class="stat-card">
        <div class="stat-card-content">
          <div class="stat-icon" :style="{ background: item.bgColor }">
            <span class="stat-number">{{ item.value }}</span>
          </div>
          <div class="stat-label">{{ item.label }}</div>
        </div>
      </el-card>
    </div>

    <!-- 搜索区域 -->
    <el-card class="search-card" shadow="hover">
      <el-form :model="searchForm" inline>
        <el-form-item label="课程状态">
          <el-tabs v-model="activeTab" @tab-change="handleTabChange">
            <el-tab-pane label="全部" name="all" />
            <el-tab-pane label="待上架" name="0" />
            <el-tab-pane label="已上架" name="1" />
            <el-tab-pane label="已下架" name="2" />
            <el-tab-pane label="已完结" name="3" />
          </el-tabs>
        </el-form-item>
      </el-form>
      <el-form :model="searchForm" inline style="margin-top: 10px">
        <el-form-item label="课程分类">
          <el-select v-model="searchForm.categoryId" placeholder="全部分类" clearable style="width: 150px">
            <el-option label="全部" value="" />
            <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="searchForm.keyword" placeholder="课程名称" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 课程列表 -->
    <el-card class="table-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span>课程列表</span>
          <el-button type="primary" :icon="Plus" @click="handleAdd">新增课程</el-button>
        </div>
      </template>

      <el-table :data="courseList" stripe style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column label="课程信息" min-width="300">
          <template #default="{ row }">
            <div class="course-info">
              <img :src="row.cover" class="course-cover" />
              <div class="course-detail">
                <div class="course-title">{{ row.title }}</div>
                <div class="course-meta">{{ row.categoryName }} · {{ row.teacherName }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="price" label="价格" width="100">
          <template #default="{ row }">
            <span class="price">¥{{ (row.price / 100).toFixed(0) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="learners" label="学习人数" width="100" />
        <el-table-column prop="lessons" label="课时" width="80" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button v-if="row.status === 0 || row.status === 2" type="success" link @click="handlePublish(row)">上架</el-button>
            <el-button v-if="row.status === 1" type="warning" link @click="handleUnpublish(row)">下架</el-button>
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
      width="600px"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="100px"
      >
        <el-form-item label="课程名称" prop="title">
          <el-input v-model="formData.title" placeholder="请输入课程名称" />
        </el-form-item>
        <el-form-item label="课程分类" prop="categoryId">
          <el-select v-model="formData.categoryId" placeholder="请选择分类" style="width: 100%">
            <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="授课教师" prop="teacherId">
          <el-select v-model="formData.teacherId" placeholder="请选择教师" style="width: 100%">
            <el-option v-for="teacher in teachers" :key="teacher.id" :label="teacher.nickname" :value="teacher.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="课程价格" prop="price">
          <el-input-number v-model="formData.price" :min="0" :step="100" />
          <span style="margin-left: 10px; color: #909399">单位：分</span>
        </el-form-item>
        <el-form-item label="课时数" prop="lessons">
          <el-input-number v-model="formData.lessons" :min="1" />
        </el-form-item>
        <el-form-item label="课程简介" prop="description">
          <el-input v-model="formData.description" type="textarea" :rows="3" placeholder="请输入课程简介" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 上架确认弹窗 -->
    <el-dialog
      v-model="publishDialogVisible"
      title="课程上架确认"
      width="400px"
    >
      <div class="publish-confirm">
        <el-icon :size="48" color="#67c23a"><SuccessFilled /></el-icon>
        <p>确定要上架课程 <strong>{{ currentCourse?.title }}</strong> 吗？</p>
        <p class="tip">上架后课程将在学员端可见</p>
      </div>
      <template #footer>
        <el-button @click="publishDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmPublish">确定上架</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, SuccessFilled } from '@element-plus/icons-vue'
import { getTypeAll } from '@/api/api'
import { getTeacherser } from '@/api/teacher'
import { getCoursesPage, baseInfoSave, baseUpShelf, baseDownShelf, baseBeforeUpShelf, deleteCourses } from '@/api/curriculum'

// 分类数据
const categories = ref([])

// 教师数据
const teachers = ref([])

// 统计卡片
const allCourses = ref([])
const statCards = computed(() => [
  { label: '课程总数', value: allCourses.value.length, bgColor: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)' },
  { label: '已上架', value: allCourses.value.filter(item => Number(item.status) === 1).length, bgColor: 'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)' },
  { label: '待上架', value: allCourses.value.filter(item => Number(item.status) === 0).length, bgColor: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)' },
  { label: '已下架', value: allCourses.value.filter(item => Number(item.status) === 2).length, bgColor: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)' },
  { label: '已完结', value: allCourses.value.filter(item => Number(item.status) === 3).length, bgColor: 'linear-gradient(135deg, #fa709a 0%, #fee140 100%)' }
])

// 搜索
const searchForm = reactive({
  categoryId: '',
  keyword: ''
})
const activeTab = ref('all')

// 分页
const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})

// 课程列表
const courseList = ref([])
const loading = ref(false)

// 弹窗
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref(null)
const formData = reactive({
  id: null,
  title: '',
  categoryId: '',
  teacherId: '',
  price: 0,
  lessons: 1,
  description: ''
})
const formRules = {
  title: [{ required: true, message: '请输入课程名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  teacherId: [{ required: true, message: '请选择教师', trigger: 'change' }]
}

// 上架弹窗
const publishDialogVisible = ref(false)
const currentCourse = ref(null)

const normalizeCategory = item => ({ id: item.id, name: item.name || item.categoryName })
const normalizeCourse = item => ({
  ...item,
  title: item.title || item.courseName,
  cover: item.cover || item.coverUrl || '/src/assets/images/courses/vue3.svg',
  lessons: item.lessons ?? item.lessonCount ?? 0,
  learners: item.learners ?? item.learnerCount ?? 0,
  price: Number(item.price ?? 0),
  teacherName: item.teacherName || '讲师团队'
})

const loadReferenceData = async () => {
  try {
    const [categoryRes, teacherRes] = await Promise.all([
      getTypeAll({ admin: true, includeDisabled: true }),
      getTeacherser({ pageNo: 1, pageSize: 200 })
    ])
    const categoryData = categoryRes?.data?.list || categoryRes?.data || []
    categories.value = Array.isArray(categoryData) ? categoryData.flatMap(item => [normalizeCategory(item), ...(item.children || []).map(normalizeCategory)]) : []
    const teacherData = teacherRes?.data?.list || teacherRes?.data || []
    teachers.value = (Array.isArray(teacherData) ? teacherData : []).map(item => ({ id: item.id, nickname: item.nickname || item.name || item.teacherName }))
  } catch (error) {
    ElMessage.warning('分类或教师加载失败，课程列表仍可查看')
  }
}

const getCourseList = async () => {
  loading.value = true
  try {
    const params = { pageNo: pagination.page, pageSize: pagination.pageSize, keyword: searchForm.keyword }
    if (activeTab.value !== 'all') params.status = Number(activeTab.value)
    if (searchForm.categoryId) params.categoryId = searchForm.categoryId
    const res = await getCoursesPage(params)
    if (res?.code !== 200) throw new Error(res?.msg || res?.data?.msg || '课程加载失败')
    const data = res.data?.list || res.data?.rows || res.data || []
    courseList.value = (Array.isArray(data) ? data : []).map(normalizeCourse)
    pagination.total = Number(res.data?.total ?? res.total ?? courseList.value.length)
    if (activeTab.value === 'all' && !searchForm.keyword && !searchForm.categoryId) allCourses.value = courseList.value
  } catch (error) { ElMessage.error(error?.message || '课程加载失败') }
  finally { loading.value = false }
}

const loadCourseStats = async () => {
  try {
    const res = await getCoursesPage({ pageNo: 1, pageSize: 200 })
    const data = res.data?.list || res.data?.rows || res.data || []
    allCourses.value = (Array.isArray(data) ? data : []).map(normalizeCourse)
  } catch (error) { allCourses.value = [] }
}

// Tab 切换
const handleTabChange = () => {
  pagination.page = 1
  getCourseList()
}

// 搜索
const handleSearch = () => {
  pagination.page = 1
  getCourseList()
}

// 重置
const handleReset = () => {
  searchForm.categoryId = ''
  searchForm.keyword = ''
  handleSearch()
}

// 分页
const handleSizeChange = () => {
  pagination.page = 1
  getCourseList()
}

const handleCurrentChange = () => {
  getCourseList()
}

// 新增
const handleAdd = () => {
  dialogTitle.value = '新增课程'
  Object.assign(formData, { id: null, title: '', categoryId: '', teacherId: '', price: 0, lessons: 1, description: '' })
  dialogVisible.value = true
}

// 编辑
const handleEdit = (row) => {
  dialogTitle.value = '编辑课程'
  Object.assign(formData, row)
  dialogVisible.value = true
}

// 提交
const handleSubmit = async () => {
  if (!formRef.value || !(await formRef.value.validate().catch(() => false))) return
  try {
    const res = await baseInfoSave({
      id: formData.id,
      name: formData.title,
      categoryId: formData.categoryId,
      thirdCateId: formData.categoryId,
      teacherId: formData.teacherId,
      price: Number(formData.price || 0),
      lessons: Number(formData.lessons || 0),
      description: formData.description,
      introduce: formData.description,
      detail: formData.description,
      free: Number(formData.price || 0) === 0
    })
    if (res?.code !== 200) throw new Error(res?.msg || res?.data?.msg || '保存失败')
    ElMessage.success(formData.id ? '编辑成功' : '新增成功')
    dialogVisible.value = false
    await Promise.all([getCourseList(), loadCourseStats()])
  } catch (error) { ElMessage.error(error?.message || '保存失败') }
}

// 上架
const handlePublish = (row) => {
  currentCourse.value = row
  publishDialogVisible.value = true
}

// 确认上架
const confirmPublish = async () => {
  if (!currentCourse.value) return
  try {
    const check = await baseBeforeUpShelf(currentCourse.value.id)
    if (check?.code !== 200 || check.data?.pass === false) throw new Error(check?.msg || check?.data?.errors?.join('、') || '课程暂不能上架')
    const res = await baseUpShelf({ id: currentCourse.value.id })
    if (res?.code !== 200) throw new Error(res?.msg || res?.data?.msg || '上架失败')
    ElMessage.success('课程已上架'); publishDialogVisible.value = false; await Promise.all([getCourseList(), loadCourseStats()])
  } catch (error) { ElMessage.error(error?.message || '上架失败') }
}

// 下架
const handleUnpublish = row => ElMessageBox.confirm('确定要下架该课程吗？', '提示', { type: 'warning' }).then(async () => {
  try {
    const res = await baseDownShelf({ id: row.id })
    if (res?.code !== 200) throw new Error(res?.msg || res?.data?.msg || '下架失败')
    ElMessage.success('课程已下架'); await Promise.all([getCourseList(), loadCourseStats()])
  } catch (error) { ElMessage.error(error?.message || '下架失败') }
}).catch(() => {})

// 删除
const handleDelete = row => ElMessageBox.confirm('确定要删除该课程吗？', '提示', { type: 'warning' }).then(async () => {
  try {
    const res = await deleteCourses(row.id)
    if (res?.code !== 200) throw new Error(res?.msg || res?.data?.msg || '删除失败')
    ElMessage.success('删除成功'); await Promise.all([getCourseList(), loadCourseStats()])
  } catch (error) { ElMessage.error(error?.message || '删除失败') }
}).catch(() => {})

// 状态类型
const getStatusType = (status) => {
  const map = { 0: 'info', 1: 'success', 2: 'warning', 3: 'danger' }
  return map[status] || 'info'
}

// 状态文本
const getStatusText = (status) => {
  const map = { 0: '待上架', 1: '已上架', 2: '已下架', 3: '已完结' }
  return map[status] || '未知'
}

onMounted(async () => {
  await loadReferenceData()
  await Promise.all([getCourseList(), loadCourseStats()])
})
</script>

<style scoped>
.course-management {
  padding: 20px;
}

.stat-cards {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  border-radius: 12px;
  border: none;
}

.stat-card-content {
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

.search-card {
  margin-bottom: 20px;
}

.table-card :deep(.el-card__body) {
  padding: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.course-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.course-cover {
  width: 80px;
  height: 45px;
  border-radius: 6px;
  object-fit: cover;
}

.course-detail {
  flex: 1;
}

.course-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.course-meta {
  font-size: 12px;
  color: #909399;
}

.price {
  color: #f56c6c;
  font-weight: 600;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  padding: 20px;
}

.publish-confirm {
  text-align: center;
  padding: 20px;
}

.publish-confirm p {
  margin: 16px 0 8px;
  font-size: 16px;
}

.publish-confirm .tip {
  font-size: 13px;
  color: #909399;
}
</style>
