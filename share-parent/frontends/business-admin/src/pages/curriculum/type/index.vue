<template>
  <div class="category-management">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2>课程分类</h2>
      <el-button type="primary" :icon="Plus" @click="handleAdd">新增分类</el-button>
    </div>

    <!-- 统计卡片 -->
    <div class="stat-cards">
      <el-card shadow="hover" class="stat-card">
        <div class="stat-content">
          <div class="stat-icon" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%)">
            <span class="stat-number">{{ total }}</span>
          </div>
          <div class="stat-label">分类总数</div>
        </div>
      </el-card>
      <el-card shadow="hover" class="stat-card">
        <div class="stat-content">
          <div class="stat-icon" style="background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)">
            <span class="stat-number">{{ activeCount }}</span>
          </div>
          <div class="stat-label">启用分类</div>
        </div>
      </el-card>
      <el-card shadow="hover" class="stat-card">
        <div class="stat-content">
          <div class="stat-icon" style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%)">
            <span class="stat-number">{{ courseCount }}</span>
          </div>
          <div class="stat-label">课程总数</div>
        </div>
      </el-card>
    </div>

    <!-- 分类列表 -->
    <el-card class="table-card" shadow="hover">
      <el-table :data="categoryList" stripe style="width: 100%" v-loading="loading" row-key="id">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="name" label="分类名称" min-width="200">
          <template #default="{ row }">
            <div class="category-name">
              <div class="category-icon" :style="{ background: row.bgColor }">
                <span>{{ row.iconText }}</span>
              </div>
              <span>{{ row.name }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="250" />
        <el-table-column prop="courses" label="课程数" width="100" />
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              :active-value="1"
              :inactive-value="0"
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="primary" link @click="handleAddChild(row)">添加子分类</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="500px"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="100px"
      >
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="上级分类" prop="parentId">
          <el-select v-model="formData.parentId" placeholder="无（一级分类）" clearable style="width: 100%">
            <el-option label="无（一级分类）" value="0" />
            <el-option v-for="cat in parentCategories" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="formData.description" type="textarea" :rows="3" placeholder="请输入描述" />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="formData.sort" :min="1" :max="999" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getCurriculumType, addCurriculumType, editCurriculumType, editCurriculumStatus, deleteType } from '@/api/curriculum'

const pagination = reactive({ page: 1, pageSize: 50, total: 0 })
const categoryList = ref([])
const loading = ref(false)
const total = computed(() => categoryList.value.length)
const activeCount = computed(() => categoryList.value.filter(item => Number(item.status) === 1).length)
const courseCount = computed(() => categoryList.value.reduce((sum, item) => sum + Number(item.courseNum ?? item.courses ?? 0), 0))
const parentCategories = computed(() => categoryList.value.filter(item => Number(item.parentId || 0) === 0))

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref(null)
const formData = reactive({ id: null, name: '', parentId: '0', description: '', sort: 1 })
const formRules = { name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }] }
const gradients = [
  'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
  'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)',
  'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
  'linear-gradient(135deg, #fa709a 0%, #fee140 100%)'
]

const flatten = (items, level = 1) => (items || []).flatMap(item => [
  { ...item, parentId: item.parentId ?? 0, sort: item.sort ?? item.sortNum ?? 0, iconText: item.iconText || String(item.name || '').slice(0, 1), bgColor: item.bgColor || gradients[(Number(item.id) || 0) % gradients.length], level },
  ...flatten(item.children, level + 1)
])

const getCategoryList = async () => {
  loading.value = true
  try {
    const res = await getCurriculumType({ includeDisabled: true })
    const data = res?.data?.list || res?.data || []
    categoryList.value = flatten(Array.isArray(data) ? data : [])
    pagination.total = categoryList.value.length
  } catch (error) {
    ElMessage.error(error?.message || '分类加载失败')
  } finally { loading.value = false }
}

const handleAdd = () => {
  dialogTitle.value = '新增分类'
  Object.assign(formData, { id: null, name: '', parentId: '0', description: '', sort: 1 })
  dialogVisible.value = true
}
const handleAddChild = row => {
  dialogTitle.value = '新增子分类'
  Object.assign(formData, { id: null, name: '', parentId: row.id, description: '', sort: 1 })
  dialogVisible.value = true
}
const handleEdit = row => {
  dialogTitle.value = '编辑分类'
  Object.assign(formData, { id: row.id, name: row.name, parentId: row.parentId ?? '0', description: row.description || '', sort: row.sort || 0 })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value || !(await formRef.value.validate().catch(() => false))) return
  const payload = { ...formData, parentId: Number(formData.parentId || 0), index: Number(formData.sort || 0), status: 1 }
  try {
    const res = formData.id ? await editCurriculumType(payload) : await addCurriculumType(payload)
    if (res?.code !== 200) throw new Error(res?.msg || res?.data?.msg || '保存失败')
    ElMessage.success(formData.id ? '编辑成功' : '新增成功')
    dialogVisible.value = false
    await getCategoryList()
  } catch (error) { ElMessage.error(error?.message || '保存失败') }
}

const handleDelete = row => ElMessageBox.confirm('确定要删除该分类吗？', '提示', { type: 'warning' }).then(async () => {
  try {
    const res = await deleteType(row.id)
    if (res?.code !== 200) throw new Error(res?.msg || res?.data?.msg || '删除失败')
    ElMessage.success('删除成功')
    await getCategoryList()
  } catch (error) { ElMessage.error(error?.message || '删除失败') }
}).catch(() => {})

const handleStatusChange = async row => {
  const oldStatus = row.status === 1 ? 0 : 1
  try {
    const res = await editCurriculumStatus({ id: row.id, status: row.status })
    if (res?.code !== 200) throw new Error(res?.msg || res?.data?.msg || '状态更新失败')
    ElMessage.success(`分类 ${row.name} 已${row.status === 1 ? '启用' : '禁用'}`)
  } catch (error) { row.status = oldStatus; ElMessage.error(error?.message || '状态更新失败') }
}

onMounted(getCategoryList)
</script>

<style scoped>
.category-management {
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

/* 表格卡片 */
.table-card {
  border-radius: 12px;
  border: none;
}

.table-card :deep(.el-card__body) {
  padding: 0;
}

/* 分类名称 */
.category-name {
  display: flex;
  align-items: center;
  gap: 12px;
}

.category-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.category-icon span {
  font-size: 16px;
  font-weight: 700;
  color: #fff;
}
</style>
