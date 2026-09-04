<template>
  <div class="coupon-management">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2>优惠券管理</h2>
      <el-button type="primary" :icon="Plus" @click="handleAdd">新增优惠券</el-button>
    </div>

    <!-- 搜索区域 -->
    <el-card class="search-card" shadow="hover">
      <el-form :model="searchForm" inline>
        <el-form-item label="优惠类型">
          <el-select v-model="searchForm.type" placeholder="全部类型" clearable style="width: 150px">
            <el-option label="全部" value="" />
            <el-option label="满减券" value="fixed" />
            <el-option label="折扣券" value="percent" />
            <el-option label="立减券" value="direct" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部状态" clearable style="width: 120px">
            <el-option label="全部" value="" />
            <el-option label="有效" :value="1" />
            <el-option label="已过期" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="searchForm.keyword" placeholder="优惠券名称" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 优惠券列表 -->
    <el-card class="table-card" shadow="hover">
      <el-table :data="couponList" stripe style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="name" label="优惠券名称" min-width="200" />
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="getTypeTag(row.type)">{{ getTypeText(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="优惠规则" width="180">
          <template #default="{ row }">
            <span v-if="row.type === 'fixed'">满{{ row.minAmount / 100 }}减{{ row.value / 100 }}</span>
            <span v-else-if="row.type === 'percent'">打{{ row.value / 10 }}折</span>
            <span v-else>立减{{ row.value / 100 }}元</span>
          </template>
        </el-table-column>
        <el-table-column label="领取情况" width="150">
          <template #default="{ row }">
            <span>{{ row.usedCount }}/{{ row.totalCount }}</span>
            <el-progress :percentage="Math.round(row.usedCount / row.totalCount * 100)" :stroke-width="4" style="margin-top: 4px" />
          </template>
        </el-table-column>
        <el-table-column label="有效期" width="200">
          <template #default="{ row }">
            <span>{{ row.startTime }} 至 {{ row.endTime }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '有效' : '已过期' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="primary" link @click="handleGrant(row)">发放</el-button>
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
        label-width="120px"
      >
        <el-form-item label="优惠券名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入优惠券名称" />
        </el-form-item>
        <el-form-item label="优惠类型" prop="type">
          <el-select v-model="formData.type" placeholder="请选择类型" style="width: 100%">
            <el-option label="满减券" value="fixed" />
            <el-option label="折扣券" value="percent" />
            <el-option label="立减券" value="direct" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="formData.type !== 'direct'" label="最低消费" prop="minAmount">
          <el-input-number v-model="formData.minAmount" :min="0" :step="1000" />
          <span style="margin-left: 10px; color: #909399">单位：分</span>
        </el-form-item>
        <el-form-item label="优惠值" prop="value">
          <el-input-number v-model="formData.value" :min="1" />
          <span style="margin-left: 10px; color: #909399">
            {{ formData.type === 'percent' ? '折扣值（如80表示8折）' : '单位：分' }}
          </span>
        </el-form-item>
        <el-form-item label="发放总量" prop="totalCount">
          <el-input-number v-model="formData.totalCount" :min="1" />
        </el-form-item>
        <el-form-item label="有效期" prop="dateRange">
          <el-date-picker
            v-model="formData.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 发放弹窗 -->
    <el-dialog
      v-model="grantDialogVisible"
      title="发放优惠券"
      width="500px"
    >
      <el-form :model="grantForm" label-width="100px">
        <el-form-item label="优惠券">
          <span>{{ currentCoupon?.name }}</span>
        </el-form-item>
        <el-form-item label="发放方式">
          <el-radio-group v-model="grantForm.type">
            <el-radio label="user">指定用户</el-radio>
            <el-radio label="all">全部用户</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="grantForm.type === 'user'" label="用户ID">
          <el-input v-model="grantForm.userIds" placeholder="多个用户ID用逗号分隔" />
        </el-form-item>
        <el-form-item label="发放数量">
          <el-input-number v-model="grantForm.count" :min="1" :max="10" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="grantDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmGrant">确定发放</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  getMarketPage,
  saveMarket,
  updateCoupon,
  deleteMarket,
  configGrant
} from '@/api/marketing'

const searchForm = reactive({ type: '', status: '', keyword: '' })
const pagination = reactive({ page: 1, pageSize: 10, total: 0 })
const couponList = ref([])
const loading = ref(false)

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref(null)
const formData = reactive({
  id: null,
  name: '',
  type: 'fixed',
  minAmount: 0,
  value: 0,
  totalCount: 100,
  dateRange: []
})
const formRules = {
  name: [{ required: true, message: '请输入优惠券名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  value: [{ required: true, message: '请输入优惠值', trigger: 'blur' }],
  totalCount: [{ required: true, message: '请输入发放总量', trigger: 'blur' }]
}

const grantDialogVisible = ref(false)
const grantForm = reactive({ type: 'user', userIds: '', count: 1 })
const currentCoupon = ref(null)

const responseData = response => response?.data ?? response
const ensureSuccess = response => {
  if (response?.code !== 200) throw new Error(response?.msg || '请求失败')
  return responseData(response)
}
const toDateValue = value => value ? new Date(String(value).replace(' ', 'T')) : null
const toDateTime = value => {
  if (!value) return ''
  if (value instanceof Date) return value.toISOString().slice(0, 19)
  return String(value)
}

const getCouponList = async () => {
  loading.value = true
  try {
    const response = await getMarketPage({
      pageNo: pagination.page,
      pageSize: pagination.pageSize,
      type: searchForm.type || undefined,
      status: searchForm.status === '' ? undefined : searchForm.status,
      keyword: searchForm.keyword || undefined
    })
    const data = ensureSuccess(response) || {}
    couponList.value = Array.isArray(data.list) ? data.list : (data.rows || [])
    pagination.total = Number(data.total ?? couponList.value.length)
  } catch (error) {
    couponList.value = []
    pagination.total = 0
    ElMessage.error(error?.message || '优惠券加载失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => { pagination.page = 1; getCouponList() }
const handleReset = () => {
  Object.assign(searchForm, { type: '', status: '', keyword: '' })
  handleSearch()
}
const handleSizeChange = () => { pagination.page = 1; getCouponList() }
const handleCurrentChange = () => getCouponList()

const handleAdd = () => {
  dialogTitle.value = '新增优惠券'
  Object.assign(formData, { id: null, name: '', type: 'fixed', minAmount: 0, value: 0, totalCount: 100, dateRange: [] })
  dialogVisible.value = true
}
const handleEdit = row => {
  dialogTitle.value = '编辑优惠券'
  Object.assign(formData, { ...row, dateRange: [toDateValue(row.startTime), toDateValue(row.endTime)] })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value || !(await formRef.value.validate().catch(() => false))) return
  const { dateRange, ...rest } = formData
  const payload = {
    ...rest,
    startTime: toDateTime(dateRange?.[0]),
    endTime: toDateTime(dateRange?.[1])
  }
  try {
    const response = formData.id ? await updateCoupon(payload) : await saveMarket(payload)
    ensureSuccess(response)
    ElMessage.success(formData.id ? '编辑成功' : '新增成功')
    dialogVisible.value = false
    await getCouponList()
  } catch (error) { ElMessage.error(error?.message || '保存失败') }
}

const handleGrant = row => {
  currentCoupon.value = row
  Object.assign(grantForm, { type: 'user', userIds: '', count: 1 })
  grantDialogVisible.value = true
}
const confirmGrant = async () => {
  if (!currentCoupon.value) return
  try {
    ensureSuccess(await configGrant({
      id: currentCoupon.value.id,
      type: grantForm.type,
      userIds: grantForm.userIds,
      count: grantForm.count
    }))
    ElMessage.success('优惠券发放成功！')
    grantDialogVisible.value = false
    await getCouponList()
  } catch (error) { ElMessage.error(error?.message || '发放失败') }
}

const handleDelete = row => ElMessageBox.confirm('确定要删除该优惠券吗？', '提示', { type: 'warning' }).then(async () => {
  try {
    ensureSuccess(await deleteMarket(row.id))
    ElMessage.success('删除成功')
    await getCouponList()
  } catch (error) { ElMessage.error(error?.message || '删除失败') }
}).catch(() => {})

// 类型标签
const getTypeTag = (type) => {
  const map = { fixed: '', percent: 'success', direct: 'warning' }
  return map[type] || 'info'
}

// 类型文本
const getTypeText = (type) => {
  const map = { fixed: '满减券', percent: '折扣券', direct: '立减券' }
  return map[type] || '未知'
}

onMounted(() => {
  getCouponList()
})
</script>

<style scoped>
.coupon-management {
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

.search-card {
  margin-bottom: 20px;
}

.search-card :deep(.el-card__body) {
  padding-bottom: 0;
}

.table-card :deep(.el-card__body) {
  padding: 0;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  padding: 20px;
}
</style>
