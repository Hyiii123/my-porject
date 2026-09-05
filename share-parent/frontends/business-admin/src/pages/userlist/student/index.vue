<template>
  <div class="student-management">
    <div class="page-header">
      <div>
        <h2>学员管理</h2>
        <p>维护学员账号、联系方式和学习状态，数据来自系统用户表。</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="handleAdd">新增学员</el-button>
    </div>

    <div class="stat-cards">
      <el-card v-for="item in statCards" :key="item.label" shadow="hover" class="stat-card">
        <div class="stat-content">
          <div class="stat-icon" :style="{ background: item.color }">{{ item.value }}</div>
          <div><strong>{{ item.label }}</strong><span>{{ item.tip }}</span></div>
        </div>
      </el-card>
    </div>

    <el-card class="search-card" shadow="hover">
      <el-form :model="searchForm" inline @submit.prevent>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" clearable placeholder="全部状态" style="width: 120px">
            <el-option label="正常" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="性别">
          <el-select v-model="searchForm.gender" clearable placeholder="全部" style="width: 110px">
            <el-option label="男" :value="0" />
            <el-option label="女" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="searchForm.keyword" clearable placeholder="姓名/账号/手机号" style="width: 230px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card" shadow="hover">
      <el-table v-loading="loading" :data="studentList" stripe>
        <el-table-column prop="id" label="ID" width="86" />
        <el-table-column label="学员信息" min-width="250">
          <template #default="{ row }">
            <div class="user-info">
              <img class="user-avatar" :src="row.avatar || defaultAvatar" alt="" />
              <div>
                <div class="user-name">{{ row.nickname }}</div>
                <div class="user-account">{{ row.username }} · {{ row.phone || '未绑定手机号' }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="性别" width="80"><template #default="{ row }">{{ genderText(row.gender) }}</template></el-table-column>
        <el-table-column prop="email" label="邮箱" min-width="190" show-overflow-tooltip />
        <el-table-column label="在学课程" width="105"><template #default="{ row }">{{ row.courses ?? row.courseCount ?? '—' }}</template></el-table-column>
        <el-table-column label="积分" width="90"><template #default="{ row }">{{ row.points ?? '—' }}</template></el-table-column>
        <el-table-column label="状态" width="105">
          <template #default="{ row }">
            <div class="status-cell"><el-switch v-model="row.status" :active-value="1" :inactive-value="0" @change="handleStatusChange(row)" /><el-tag size="small" :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '正常' : '禁用' }}</el-tag></div>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="注册时间" width="180" />
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleView(row)">查看</el-button>
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="primary" link @click="handleResetPwd(row)">重置密码</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination">
        <el-pagination v-model:current-page="pagination.page" v-model:page-size="pagination.pageSize" :page-sizes="[10, 20, 50]" :total="pagination.total" layout="total, sizes, prev, pager, next, jumper" @size-change="handleSizeChange" @current-change="getStudentList" />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="92px">
        <el-form-item label="登录账号" prop="username"><el-input v-model="formData.username" :disabled="Boolean(formData.id)" placeholder="请输入登录账号" /></el-form-item>
        <el-form-item label="姓名" prop="nickname"><el-input v-model="formData.nickname" maxlength="20" show-word-limit placeholder="请输入姓名" /></el-form-item>
        <el-form-item label="性别"><el-radio-group v-model="formData.gender"><el-radio :label="0">男</el-radio><el-radio :label="1">女</el-radio></el-radio-group></el-form-item>
        <el-form-item label="手机号" prop="phone"><el-input v-model="formData.phone" placeholder="请输入手机号" /></el-form-item>
        <el-form-item label="邮箱" prop="email"><el-input v-model="formData.email" placeholder="请输入邮箱" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="formData.remark" type="textarea" :rows="3" maxlength="200" show-word-limit placeholder="补充学员备注" /></el-form-item>
        <el-form-item v-if="!formData.id" label="初始密码" prop="password"><el-input v-model="formData.password" type="password" show-password placeholder="不少于 6 位" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="学员详情" width="560px">
      <div v-if="currentStudent" class="student-detail">
        <div class="detail-header"><img class="detail-avatar" :src="currentStudent.avatar || defaultAvatar" alt="" /><div><h3>{{ currentStudent.nickname }}</h3><p>{{ currentStudent.username }} · {{ genderText(currentStudent.gender) }}</p></div><el-tag :type="currentStudent.status === 1 ? 'success' : 'info'">{{ currentStudent.status === 1 ? '正常' : '禁用' }}</el-tag></div>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="用户编号">{{ currentStudent.id }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ currentStudent.phone || '未绑定' }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ currentStudent.email || '未填写' }}</el-descriptions-item>
          <el-descriptions-item label="在学课程">{{ currentStudent.courses ?? currentStudent.courseCount ?? '暂无统计' }}</el-descriptions-item>
          <el-descriptions-item label="积分">{{ currentStudent.points ?? '暂无统计' }}</el-descriptions-item>
          <el-descriptions-item label="注册时间">{{ currentStudent.createTime || '—' }}</el-descriptions-item>
          <el-descriptions-item label="备注">{{ currentStudent.remark || '—' }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import defaultAvatar from '@/assets/images/users/default-avatar.svg'
import { getStudents } from '@/api/students'
import { editUser, pwdReset, removeUser, saveUser, usersStatus } from '@/api/user'

const searchForm = reactive({ status: '', gender: '', keyword: '' })
const pagination = reactive({ page: 1, pageSize: 10, total: 0 })
const studentList = ref([])
const allStudents = ref([])
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const detailVisible = ref(false)
const dialogTitle = ref('')
const currentStudent = ref(null)
const formRef = ref(null)
const formData = reactive({ id: null, username: '', nickname: '', gender: null, phone: '', email: '', remark: '', password: '', type: 'student' })
const formRules = {
  username: [{ required: true, message: '请输入登录账号', trigger: 'blur' }],
  nickname: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }, { pattern: /^1\d{10}$/, message: '手机号格式不正确', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
  password: [{ required: true, message: '请输入初始密码', trigger: 'blur' }, { min: 6, message: '密码长度不能少于 6 位', trigger: 'blur' }]
}

const statCards = computed(() => [
  { label: '学员总数', value: allStudents.value.length, tip: '当前学员账号', color: 'linear-gradient(135deg,#667eea,#764ba2)' },
  { label: '正常学员', value: allStudents.value.filter(item => item.status === 1).length, tip: '可以正常学习', color: 'linear-gradient(135deg,#43e97b,#38f9d7)' },
  { label: '禁用学员', value: allStudents.value.filter(item => item.status === 0).length, tip: '暂时禁止登录', color: 'linear-gradient(135deg,#f093fb,#f5576c)' }
])

function normalize(item) {
  return {
    ...item,
    id: String(item.id ?? item.userId ?? ''),
    username: item.username || item.userName || '',
    nickname: item.nickname || item.nickName || item.name || item.username || '未命名学员',
    phone: item.phone || item.phonenumber || item.cellPhone || '',
    gender: item.gender ?? (item.sex === undefined || item.sex === null ? null : Number(item.sex)),
    status: Number(item.enabled ?? (String(item.status) === '0' ? 1 : 0)),
    remark: item.remark || ''
  }
}

async function getStudentList() {
  loading.value = true
  try {
    const response = await getStudents({ pageNum: 1, pageSize: 200 })
    const source = Array.isArray(response?.rows) ? response.rows.map(normalize) : []
    allStudents.value = source
    let rows = source
    if (searchForm.status !== '' && searchForm.status !== null && searchForm.status !== undefined) rows = rows.filter(item => item.status === Number(searchForm.status))
    if (searchForm.gender !== '' && searchForm.gender !== null && searchForm.gender !== undefined) rows = rows.filter(item => Number(item.gender) === Number(searchForm.gender))
    const keyword = searchForm.keyword.trim().toLowerCase()
    if (keyword) rows = rows.filter(item => `${item.nickname}${item.username}${item.phone}${item.email}`.toLowerCase().includes(keyword))
    pagination.total = rows.length
    const start = (pagination.page - 1) * pagination.pageSize
    studentList.value = rows.slice(start, start + pagination.pageSize)
  } catch (error) {
    studentList.value = []
    pagination.total = 0
    ElMessage.error('学员列表加载失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() { pagination.page = 1; getStudentList() }
function handleReset() { Object.assign(searchForm, { status: '', gender: '', keyword: '' }); handleSearch() }
function handleSizeChange() { pagination.page = 1; getStudentList() }
function handleAdd() { dialogTitle.value = '新增学员'; Object.assign(formData, { id: null, username: `student_${Date.now()}`, nickname: '', gender: null, phone: '', email: '', remark: '', password: '', type: 'student' }); dialogVisible.value = true }
function handleEdit(row) { dialogTitle.value = '编辑学员'; Object.assign(formData, { id: row.id, username: row.username, nickname: row.nickname, gender: row.gender, phone: row.phone, email: row.email, remark: row.remark || '', password: '', type: 'student' }); dialogVisible.value = true }
function handleView(row) { currentStudent.value = row; detailVisible.value = true }

function payload() {
  return { id: formData.id, username: formData.username.trim(), nickname: formData.nickname.trim(), gender: formData.gender, phone: formData.phone.trim(), email: formData.email.trim(), remark: formData.remark.trim(), type: 'student', ...(formData.id ? {} : { password: formData.password }) }
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const response = formData.id ? await editUser(payload()) : await saveUser(payload())
    if (response?.code !== 200) throw new Error(response?.msg || '保存失败')
    ElMessage.success(formData.id ? '学员编辑成功' : '学员新增成功')
    dialogVisible.value = false
    await getStudentList()
  } catch (error) { ElMessage.error(error?.message || '学员保存失败') } finally { submitting.value = false }
}

async function handleStatusChange(row) {
  const next = Number(row.status); const previous = next === 1 ? 0 : 1
  try {
    const response = await usersStatus({ id: row.id, status: next })
    if (response?.code !== 200) throw new Error(response?.msg || '状态更新失败')
    const source = allStudents.value.find(item => item.id === row.id); if (source) source.status = next
    ElMessage.success(`学员已${next === 1 ? '启用' : '禁用'}`)
  } catch (error) { row.status = previous; ElMessage.error(error?.message || '状态更新失败') }
}

async function handleResetPwd(row) {
  try {
    await ElMessageBox.confirm(`确定重置“${row.nickname}”的密码吗？重置后密码为 123456。`, '请确认', { type: 'warning' })
    const response = await pwdReset(row.id); if (response?.code !== 200) throw new Error(response?.msg || '密码重置失败')
    ElMessage.success('密码已重置为 123456')
  } catch (error) { if (error !== 'cancel' && error !== 'close') ElMessage.error(error?.message || '密码重置失败') }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除学员“${row.nickname}”吗？删除后不可恢复。`, '请确认', { type: 'warning' })
    const response = await removeUser(row.id); if (response?.code !== 200) throw new Error(response?.msg || '删除失败')
    ElMessage.success('学员删除成功'); await getStudentList()
  } catch (error) { if (error !== 'cancel' && error !== 'close') ElMessage.error(error?.message || '学员删除失败') }
}

function genderText(value) { return Number(value) === 0 ? '男' : Number(value) === 1 ? '女' : '—' }
onMounted(getStudentList)
</script>

<style scoped>
.student-management { padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-header h2 { margin: 0; color: #303133; font-size: 20px; }
.page-header p { margin: 6px 0 0; color: #909399; font-size: 13px; }
.stat-cards { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; margin-bottom: 20px; }
.stat-card { border: 0; border-radius: 12px; }
.stat-content { display: flex; align-items: center; gap: 14px; }
.stat-icon { display: flex; width: 54px; height: 54px; align-items: center; justify-content: center; border-radius: 12px; color: #fff; font-size: 22px; font-weight: 700; }
.stat-content strong, .stat-content span { display: block; }
.stat-content strong { color: #303133; font-size: 15px; }.stat-content span { margin-top: 6px; color: #909399; font-size: 12px; }
.search-card { margin-bottom: 20px; }.search-card :deep(.el-card__body) { padding-bottom: 4px; }.table-card :deep(.el-card__body) { padding: 0; }
.user-info { display: flex; align-items: center; gap: 10px; }.user-avatar { width: 40px; height: 40px; border-radius: 50%; object-fit: cover; }.user-name { color: #303133; font-weight: 600; }.user-account { margin-top: 4px; color: #909399; font-size: 12px; }
.status-cell { display: flex; align-items: center; gap: 6px; }.pagination { display: flex; justify-content: flex-end; padding: 18px; }
.student-detail { padding: 4px 0; }.detail-header { display: flex; align-items: center; gap: 14px; margin-bottom: 18px; }.detail-header > :last-child { margin-left: auto; }.detail-avatar { width: 62px; height: 62px; border-radius: 50%; object-fit: cover; }.detail-header h3 { margin: 0 0 6px; }.detail-header p { margin: 0; color: #909399; font-size: 13px; }
@media (max-width: 900px) { .stat-cards { grid-template-columns: 1fr; } }.page-header { gap: 12px; }
@media (max-width: 700px) { .page-header { align-items: flex-start; flex-direction: column; } }
</style>
