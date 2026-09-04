<template>
  <div class="student-management">
    <div class="page-header">
      <div><h2>学员管理</h2><p>学员资料、状态和账号安全均来自系统用户表。</p></div>
      <el-button type="primary" :icon="Plus" @click="handleAdd">新增学员</el-button>
    </div>

    <div class="stat-cards">
      <el-card v-for="item in statCards" :key="item.label" shadow="hover" class="stat-card">
        <div class="stat-content"><div class="stat-icon" :style="{ background: item.color }"><span>{{ item.value }}</span></div><strong>{{ item.label }}</strong></div>
      </el-card>
    </div>

    <el-card class="search-card" shadow="hover">
      <el-form :model="searchForm" inline @submit.prevent>
        <el-form-item label="状态"><el-select v-model="searchForm.status" clearable placeholder="全部状态" style="width: 120px"><el-option label="正常" :value="1" /><el-option label="禁用" :value="0" /></el-select></el-form-item>
        <el-form-item label="关键词"><el-input v-model="searchForm.keyword" clearable placeholder="姓名/账号/手机号" style="width: 220px" @keyup.enter="handleSearch" /></el-form-item>
        <el-form-item><el-button type="primary" @click="handleSearch">搜索</el-button><el-button @click="handleReset">重置</el-button></el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card" shadow="hover">
      <el-table v-loading="loading" :data="studentList" stripe>
        <el-table-column prop="id" label="ID" width="90" />
        <el-table-column label="学员信息" min-width="240"><template #default="{ row }"><div class="user-info"><img class="user-avatar" :src="row.avatar || defaultAvatar" alt="" /><div><div class="user-name">{{ row.nickname }}</div><div class="user-phone">{{ row.username }} · {{ row.phone || '未绑定手机号' }}</div></div></div></template></el-table-column>
        <el-table-column prop="email" label="邮箱" min-width="190" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100"><template #default="{ row }"><el-switch v-model="row.status" :active-value="1" :inactive-value="0" @change="handleStatusChange(row)" /></template></el-table-column>
        <el-table-column prop="createTime" label="注册时间" width="180" />
        <el-table-column label="操作" width="210" fixed="right"><template #default="{ row }"><el-button type="primary" link @click="handleEdit(row)">编辑</el-button><el-button type="primary" link @click="handleResetPwd(row)">重置密码</el-button><el-button type="danger" link @click="handleDelete(row)">删除</el-button></template></el-table-column>
      </el-table>
      <div class="pagination"><el-pagination v-model:current-page="pagination.page" v-model:page-size="pagination.pageSize" :page-sizes="[10, 20, 50]" :total="pagination.total" layout="total, sizes, prev, pager, next, jumper" @size-change="handleSizeChange" @current-change="getStudentList" /></div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="82px">
        <el-form-item label="姓名" prop="nickname"><el-input v-model="formData.nickname" /></el-form-item>
        <el-form-item label="账号" prop="username"><el-input v-model="formData.username" :disabled="Boolean(formData.id)" /></el-form-item>
        <el-form-item label="手机号" prop="phone"><el-input v-model="formData.phone" /></el-form-item>
        <el-form-item label="邮箱" prop="email"><el-input v-model="formData.email" /></el-form-item>
        <el-form-item v-if="!formData.id" label="密码" prop="password"><el-input v-model="formData.password" type="password" show-password /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getStudents } from '@/api/students'
import { editUser, pwdReset, removeUser, saveUser, usersStatus } from '@/api/user'

const defaultAvatar = '/src/assets/images/users/default-avatar.svg'
const searchForm = reactive({ status: '', keyword: '' })
const pagination = reactive({ page: 1, pageSize: 10, total: 0 })
const studentList = ref([])
const allStudents = ref([])
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref(null)
const formData = reactive({ id: null, username: '', nickname: '', phone: '', email: '', password: '', type: 'student' })
const formRules = { nickname: [{ required: true, message: '请输入姓名', trigger: 'blur' }], username: [{ required: true, message: '请输入账号', trigger: 'blur' }], phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }], email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }], password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }] }

const statCards = computed(() => [
  { label: '学员总数', value: allStudents.value.length, color: 'linear-gradient(135deg,#667eea,#764ba2)' },
  { label: '正常学员', value: allStudents.value.filter(item => item.status === 1).length, color: 'linear-gradient(135deg,#43e97b,#38f9d7)' },
  { label: '禁用学员', value: allStudents.value.filter(item => item.status === 0).length, color: 'linear-gradient(135deg,#f093fb,#f5576c)' }
])

function normalize(item) {
  return { ...item, id: String(item.id ?? item.userId ?? ''), nickname: item.nickname || item.nickName || item.name || item.username, username: item.username || item.userName || '', phone: item.phone || item.phonenumber || '', status: Number(item.enabled ?? (String(item.status) === '0' ? 1 : 0)) }
}

async function getStudentList() {
  loading.value = true
  try {
    const response = await getStudents({ pageNum: 1, pageSize: 200 })
    let rows = (response.rows || []).map(normalize)
    if (searchForm.status !== '') rows = rows.filter(item => item.status === Number(searchForm.status))
    if (searchForm.keyword) { const keyword = searchForm.keyword.trim().toLowerCase(); rows = rows.filter(item => `${item.nickname}${item.username}${item.phone}`.toLowerCase().includes(keyword)) }
    allStudents.value = (response.rows || []).map(normalize)
    pagination.total = rows.length
    const start = (pagination.page - 1) * pagination.pageSize
    studentList.value = rows.slice(start, start + pagination.pageSize)
  } catch (error) {
    studentList.value = []; pagination.total = 0; ElMessage.error('学员列表加载失败')
  } finally { loading.value = false }
}

function handleSearch() { pagination.page = 1; getStudentList() }
function handleReset() { Object.assign(searchForm, { status: '', keyword: '' }); handleSearch() }
function handleSizeChange() { pagination.page = 1; getStudentList() }
function handleAdd() { dialogTitle.value = '新增学员'; Object.assign(formData, { id: null, username: `student_${Date.now()}`, nickname: '', phone: '', email: '', password: '', type: 'student' }); dialogVisible.value = true }
function handleEdit(row) { dialogTitle.value = '编辑学员'; Object.assign(formData, { ...row, password: '', type: 'student' }); dialogVisible.value = true }

async function handleSubmit() {
  if (!(await formRef.value?.validate().catch(() => false))) return
  submitting.value = true
  try { const response = formData.id ? await editUser({ ...formData }) : await saveUser({ ...formData }); if (response.code === 200) { ElMessage.success(formData.id ? '编辑成功' : '新增成功'); dialogVisible.value = false; await getStudentList() } } finally { submitting.value = false }
}

async function handleDelete(row) { try { await ElMessageBox.confirm(`确定删除学员“${row.nickname}”吗？`, '请确认', { type: 'warning' }); const response = await removeUser(row.id); if (response.code === 200) { ElMessage.success('删除成功'); getStudentList() } } catch (error) { if (error !== 'cancel' && error !== 'close') ElMessage.error('删除失败') } }
async function handleStatusChange(row) { const response = await usersStatus({ id: row.id, status: row.status }); if (response.code !== 200) { row.status = row.status ? 0 : 1; ElMessage.error('状态更新失败') } }
async function handleResetPwd(row) { try { await ElMessageBox.confirm(`确定重置“${row.nickname}”的密码吗？重置后密码为 123456。`, '请确认', { type: 'warning' }); const response = await pwdReset(row.id); if (response.code === 200) ElMessage.success('密码已重置为 123456') } catch (error) { if (error !== 'cancel' && error !== 'close') ElMessage.error('密码重置失败') } }
onMounted(getStudentList)
</script>

<style scoped>
.student-management{padding:20px}.page-header{display:flex;justify-content:space-between;align-items:center;margin-bottom:20px}.page-header h2{margin:0;color:#303133;font-size:20px}.page-header p{margin:6px 0 0;color:#909399;font-size:13px}.stat-cards{display:grid;grid-template-columns:repeat(3,1fr);gap:16px;margin-bottom:20px}.stat-card{border:0;border-radius:12px}.stat-content{display:flex;align-items:center;gap:16px}.stat-icon{display:flex;width:56px;height:56px;align-items:center;justify-content:center;border-radius:12px;color:#fff;font-size:24px;font-weight:700}.stat-content strong{color:#606266;font-size:14px}.search-card{margin-bottom:20px}.table-card :deep(.el-card__body){padding:0}.pagination{display:flex;justify-content:flex-end;padding:18px}.user-info{display:flex;align-items:center;gap:10px}.user-avatar{width:36px;height:36px;border-radius:50%;object-fit:cover}.user-name{color:#303133}.user-phone{margin-top:4px;color:#909399;font-size:12px}@media(max-width:800px){.stat-cards{grid-template-columns:1fr}.page-header{align-items:flex-start;gap:12px;flex-direction:column}}
</style>
