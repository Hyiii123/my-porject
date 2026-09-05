<template>
  <div class="user-management">
    <div class="page-header">
      <div>
        <h2>后台用户管理</h2>
        <p>统一维护系统账号、业务类型、角色权限和账号状态。</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="handleAdd">新增用户</el-button>
    </div>

    <div class="stat-cards">
      <el-card v-for="item in statCards" :key="item.label" shadow="hover" class="stat-card">
        <div class="stat-content">
          <div class="stat-icon" :style="{ background: item.color }">{{ item.value }}</div>
          <div>
            <strong>{{ item.label }}</strong>
            <span>{{ item.tip }}</span>
          </div>
        </div>
      </el-card>
    </div>

    <el-card class="search-card" shadow="hover">
      <el-form :model="searchForm" inline @submit.prevent>
        <el-form-item label="用户类型">
          <el-select v-model="searchForm.type" clearable placeholder="全部类型" style="width: 140px">
            <el-option label="管理员" value="admin" />
            <el-option label="学员" value="student" />
            <el-option label="教师" value="teacher" />
            <el-option label="运营人员" value="employee" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" clearable placeholder="全部状态" style="width: 120px">
            <el-option label="正常" :value="1" />
            <el-option label="禁用" :value="0" />
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
      <el-table v-loading="loading" :data="userList" stripe>
        <el-table-column prop="id" label="ID" width="86" />
        <el-table-column label="用户信息" min-width="235">
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
        <el-table-column prop="email" label="邮箱" min-width="190" show-overflow-tooltip />
        <el-table-column label="业务类型" width="110">
          <template #default="{ row }">
            <el-tag :type="getUserTypeTag(row.type)">{{ getUserTypeText(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="角色" min-width="170">
          <template #default="{ row }">
            <div class="role-list">
              <el-tag v-for="role in row.roles" :key="role" size="small" effect="plain">{{ roleLabel(role) }}</el-tag>
              <span v-if="!row.roles?.length" class="muted">未分配</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-switch v-model="row.status" :active-value="1" :inactive-value="0" @change="handleStatusChange(row)" />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleView(row)">查看</el-button>
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="primary" link @click="handleResetPwd(row)">重置密码</el-button>
            <el-button type="danger" link :disabled="isCurrentUser(row)" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="getUserList"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="620px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="92px">
        <el-form-item label="登录账号" prop="username">
          <el-input v-model="formData.username" :disabled="Boolean(formData.id)" placeholder="请输入登录账号" />
        </el-form-item>
        <el-form-item label="姓名" prop="nickname">
          <el-input v-model="formData.nickname" maxlength="20" show-word-limit placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="用户类型" prop="type">
          <el-select v-model="formData.type" style="width: 100%">
            <el-option label="管理员" value="admin" />
            <el-option label="学员" value="student" />
            <el-option label="教师" value="teacher" />
            <el-option label="运营人员" value="employee" />
          </el-select>
        </el-form-item>
        <el-form-item label="角色" prop="roleIds">
          <el-select v-model="formData.roleIds" multiple clearable filterable style="width: 100%" placeholder="请选择角色">
            <el-option v-for="role in roleOptions" :key="role.roleId" :label="role.roleName || role.name || role.roleKey" :value="role.roleId" />
          </el-select>
          <div class="form-tip">角色决定后台菜单和接口权限；保存时会同步更新用户角色关系。</div>
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="formData.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="formData.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="formData.remark" type="textarea" :rows="3" maxlength="200" show-word-limit placeholder="补充账号用途或备注" />
        </el-form-item>
        <el-form-item v-if="!formData.id" label="初始密码" prop="password">
          <el-input v-model="formData.password" type="password" show-password placeholder="不少于 6 位" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="用户详情" width="560px">
      <div v-if="currentUser" class="user-detail">
        <div class="detail-header">
          <img class="detail-avatar" :src="currentUser.avatar || defaultAvatar" alt="" />
          <div>
            <h3>{{ currentUser.nickname }}</h3>
            <p>{{ currentUser.username }} · {{ getUserTypeText(currentUser.type) }}</p>
          </div>
          <el-tag :type="currentUser.status === 1 ? 'success' : 'info'">{{ currentUser.status === 1 ? '正常' : '禁用' }}</el-tag>
        </div>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="用户编号">{{ currentUser.id }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ currentUser.phone || '未绑定' }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ currentUser.email || '未填写' }}</el-descriptions-item>
          <el-descriptions-item label="角色">
            <div class="role-list"><el-tag v-for="role in currentUser.roles" :key="role" size="small">{{ roleLabel(role) }}</el-tag><span v-if="!currentUser.roles?.length" class="muted">未分配</span></div>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ currentUser.createTime || '—' }}</el-descriptions-item>
          <el-descriptions-item label="备注">{{ currentUser.remark || '—' }}</el-descriptions-item>
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
import { getRoleList } from '@/api/staffs'
import { editUser, getUserList as getUsersApi, pwdReset, removeUser, saveUser, usersStatus } from '@/api/user'

const searchForm = reactive({ type: '', status: '', keyword: '' })
const pagination = reactive({ page: 1, pageSize: 10, total: 0 })
const userList = ref([])
const allUsers = ref([])
const roleOptions = ref([])
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const detailVisible = ref(false)
const dialogTitle = ref('')
const currentUser = ref(null)
const formRef = ref(null)
const formData = reactive({ id: null, username: '', nickname: '', phone: '', email: '', type: 'employee', roleIds: [], remark: '', password: '' })

const formRules = {
  username: [{ required: true, message: '请输入登录账号', trigger: 'blur' }],
  nickname: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  type: [{ required: true, message: '请选择用户类型', trigger: 'change' }],
  roleIds: [{ type: 'array', required: true, min: 1, message: '至少选择一个角色', trigger: 'change' }],
  phone: [{ pattern: /^1\d{10}$/, message: '手机号格式不正确', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
  password: [{ required: true, message: '请输入初始密码', trigger: 'blur' }, { min: 6, message: '密码长度不能少于 6 位', trigger: 'blur' }]
}

const statCards = computed(() => [
  { label: '用户总数', value: allUsers.value.length, tip: '当前系统账号', color: 'linear-gradient(135deg,#667eea,#764ba2)' },
  { label: '管理员', value: allUsers.value.filter(item => item.type === 'admin').length, tip: '拥有系统管理权限', color: 'linear-gradient(135deg,#f093fb,#f5576c)' },
  { label: '正常账号', value: allUsers.value.filter(item => item.status === 1).length, tip: '可以正常登录', color: 'linear-gradient(135deg,#43e97b,#38f9d7)' },
  { label: '已禁用', value: allUsers.value.filter(item => item.status === 0).length, tip: '暂时禁止登录', color: 'linear-gradient(135deg,#f6d365,#fda085)' }
])

function dataOf(response) {
  return response?.data ?? response
}

function normalize(item) {
  const roles = Array.isArray(item.roles) ? item.roles : []
  const roleIds = Array.isArray(item.roleIds) ? item.roleIds.map(Number).filter(Number.isFinite) : []
  return {
    ...item,
    id: String(item.id ?? item.userId ?? ''),
    username: item.username || item.userName || '',
    nickname: item.nickname || item.nickName || item.name || item.username || '未命名用户',
    phone: item.phone || item.phonenumber || item.cellPhone || '',
    status: Number(item.enabled ?? (String(item.status) === '0' ? 1 : 0)),
    type: item.type || ({ '00': 'admin', '01': 'student', '02': 'teacher', '03': 'employee' }[item.userType] || 'employee'),
    roles,
    roleIds,
    remark: item.remark || ''
  }
}

async function loadRoles() {
  try {
    const response = await getRoleList()
    const data = dataOf(response)
    roleOptions.value = Array.isArray(data) ? data.map(item => ({ ...item, roleId: Number(item.roleId ?? item.id) })).filter(item => Number.isFinite(item.roleId)) : []
  } catch (error) {
    roleOptions.value = []
    ElMessage.warning('角色列表加载失败，暂时无法维护角色')
  }
}

async function getUserList() {
  loading.value = true
  try {
    const response = await getUsersApi({ pageNum: 1, pageSize: 200 })
    const source = Array.isArray(response?.rows) ? response.rows.map(normalize) : []
    allUsers.value = source
    let rows = source
    if (searchForm.type) rows = rows.filter(item => item.type === searchForm.type)
    if (searchForm.status !== '' && searchForm.status !== null && searchForm.status !== undefined) rows = rows.filter(item => item.status === Number(searchForm.status))
    const keyword = searchForm.keyword.trim().toLowerCase()
    if (keyword) rows = rows.filter(item => `${item.nickname}${item.username}${item.phone}${item.email}`.toLowerCase().includes(keyword))
    pagination.total = rows.length
    const start = (pagination.page - 1) * pagination.pageSize
    userList.value = rows.slice(start, start + pagination.pageSize)
  } catch (error) {
    userList.value = []
    pagination.total = 0
    ElMessage.error('用户列表加载失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  getUserList()
}

function handleReset() {
  Object.assign(searchForm, { type: '', status: '', keyword: '' })
  handleSearch()
}

function handleSizeChange() {
  pagination.page = 1
  getUserList()
}

function defaultRoleIds() {
  const common = roleOptions.value.find(item => item.roleKey === 'common')
  return common ? [common.roleId] : []
}

function handleAdd() {
  dialogTitle.value = '新增后台用户'
  Object.assign(formData, { id: null, username: `operator_${Date.now()}`, nickname: '', phone: '', email: '', type: 'employee', roleIds: defaultRoleIds(), remark: '', password: '' })
  dialogVisible.value = true
}

function handleEdit(row) {
  dialogTitle.value = '编辑后台用户'
  Object.assign(formData, { id: row.id, username: row.username, nickname: row.nickname, phone: row.phone, email: row.email, type: row.type, roleIds: [...(row.roleIds || [])], remark: row.remark || '', password: '' })
  dialogVisible.value = true
}

function handleView(row) {
  currentUser.value = row
  detailVisible.value = true
}

function payload() {
  return {
    id: formData.id,
    username: formData.username.trim(),
    nickname: formData.nickname.trim(),
    phone: formData.phone.trim(),
    email: formData.email.trim(),
    type: formData.type,
    roleIds: (formData.roleIds || []).map(Number),
    remark: formData.remark.trim(),
    ...(formData.id ? {} : { password: formData.password })
  }
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const response = formData.id ? await editUser(payload()) : await saveUser(payload())
    if (response?.code !== 200) throw new Error(response?.msg || '保存失败')
    ElMessage.success(formData.id ? '用户编辑成功' : '用户新增成功')
    dialogVisible.value = false
    await getUserList()
  } catch (error) {
    ElMessage.error(error?.message || '用户保存失败')
  } finally {
    submitting.value = false
  }
}

async function handleStatusChange(row) {
  const next = Number(row.status)
  const previous = next === 1 ? 0 : 1
  try {
    const response = await usersStatus({ id: row.id, status: next })
    if (response?.code !== 200) throw new Error(response?.msg || '状态更新失败')
    const source = allUsers.value.find(item => item.id === row.id)
    if (source) source.status = next
    ElMessage.success(`账号已${next === 1 ? '启用' : '禁用'}`)
  } catch (error) {
    row.status = previous
    ElMessage.error(error?.message || '状态更新失败')
  }
}

async function handleResetPwd(row) {
  try {
    await ElMessageBox.confirm(`确定重置“${row.nickname}”的密码吗？重置后密码为 123456。`, '请确认', { type: 'warning' })
    const response = await pwdReset(row.id)
    if (response?.code !== 200) throw new Error(response?.msg || '密码重置失败')
    ElMessage.success('密码已重置为 123456')
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') ElMessage.error(error?.message || '密码重置失败')
  }
}

async function handleDelete(row) {
  if (isCurrentUser(row)) return
  try {
    await ElMessageBox.confirm(`确定删除用户“${row.nickname}”吗？删除后不可恢复。`, '请确认', { type: 'warning' })
    const response = await removeUser(row.id)
    if (response?.code !== 200) throw new Error(response?.msg || '删除失败')
    ElMessage.success('用户删除成功')
    await getUserList()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') ElMessage.error(error?.message || '用户删除失败')
  }
}

function isCurrentUser(row) {
  return row.username === 'admin'
}

function getUserTypeTag(type) {
  return { admin: 'danger', teacher: 'success', employee: 'warning', student: '' }[type] || 'info'
}

function getUserTypeText(type) {
  return { admin: '管理员', teacher: '教师', employee: '运营人员', student: '学员' }[type] || '未知'
}

function roleLabel(roleKey) {
  const role = roleOptions.value.find(item => item.roleKey === roleKey || item.roleName === roleKey)
  return role?.roleName || roleKey
}

onMounted(async () => {
  await loadRoles()
  await getUserList()
})
</script>

<style scoped>
.user-management { padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-header h2 { margin: 0; color: #303133; font-size: 20px; }
.page-header p { margin: 6px 0 0; color: #909399; font-size: 13px; }
.stat-cards { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 20px; }
.stat-card { border: 0; border-radius: 12px; }
.stat-content { display: flex; align-items: center; gap: 14px; }
.stat-icon { display: flex; width: 52px; height: 52px; align-items: center; justify-content: center; flex: 0 0 auto; border-radius: 12px; color: #fff; font-size: 21px; font-weight: 700; }
.stat-content strong, .stat-content span { display: block; }
.stat-content strong { color: #303133; font-size: 15px; }
.stat-content span { margin-top: 6px; color: #909399; font-size: 12px; }
.search-card { margin-bottom: 20px; }
.search-card :deep(.el-card__body) { padding-bottom: 4px; }
.table-card :deep(.el-card__body) { padding: 0; }
.user-info { display: flex; align-items: center; gap: 10px; }
.user-avatar { width: 40px; height: 40px; border-radius: 50%; object-fit: cover; }
.user-name { color: #303133; font-weight: 600; }
.user-account { margin-top: 4px; color: #909399; font-size: 12px; }
.role-list { display: flex; flex-wrap: wrap; gap: 4px; align-items: center; }
.muted { color: #c0c4cc; font-size: 12px; }
.pagination { display: flex; justify-content: flex-end; padding: 18px; }
.form-tip { margin-top: 5px; color: #909399; font-size: 12px; line-height: 1.5; }
.user-detail { padding: 4px 0; }
.detail-header { display: flex; align-items: center; gap: 14px; margin-bottom: 18px; }
.detail-header > :last-child { margin-left: auto; }
.detail-avatar { width: 62px; height: 62px; border-radius: 50%; object-fit: cover; }
.detail-header h3 { margin: 0 0 6px; color: #303133; }
.detail-header p { margin: 0; color: #909399; font-size: 13px; }
@media (max-width: 1100px) { .stat-cards { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 700px) { .stat-cards { grid-template-columns: 1fr; } .page-header { align-items: flex-start; gap: 12px; flex-direction: column; } }
</style>
