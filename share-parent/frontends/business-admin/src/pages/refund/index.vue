<template>
  <div class="refund-management">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2>退款管理</h2>
    </div>

    <!-- 统计卡片 -->
    <div class="stat-cards">
      <el-card shadow="hover" class="stat-card">
        <div class="stat-content">
          <div class="stat-icon" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%)">
            <span class="stat-number">{{ total }}</span>
          </div>
          <div class="stat-label">退款申请总数</div>
        </div>
      </el-card>
      <el-card shadow="hover" class="stat-card">
        <div class="stat-content">
          <div class="stat-icon" style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%)">
            <span class="stat-number">{{ pendingCount }}</span>
          </div>
          <div class="stat-label">待审批</div>
        </div>
      </el-card>
      <el-card shadow="hover" class="stat-card">
        <div class="stat-content">
          <div class="stat-icon" style="background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)">
            <span class="stat-number">{{ approvedCount }}</span>
          </div>
          <div class="stat-label">已通过</div>
        </div>
      </el-card>
      <el-card shadow="hover" class="stat-card">
        <div class="stat-content">
          <div class="stat-icon" style="background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)">
            <span class="stat-number">{{ rejectedCount }}</span>
          </div>
          <div class="stat-label">已拒绝</div>
        </div>
      </el-card>
    </div>

    <!-- 搜索区域 -->
    <el-card class="search-card" shadow="hover">
      <el-form :model="searchForm" inline>
        <el-form-item label="审批状态">
          <el-select v-model="searchForm.status" placeholder="全部状态" clearable style="width: 120px">
            <el-option label="全部" value="" />
            <el-option label="待审批" value="pending" />
            <el-option label="已通过" value="approved" />
            <el-option label="已拒绝" value="rejected" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="searchForm.keyword" placeholder="订单号/用户" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 退款列表 -->
    <el-card class="table-card" shadow="hover">
      <el-table :data="refundList" stripe style="width: 100%" v-loading="loading">
        <el-table-column prop="orderNo" label="订单号" width="150" />
        <el-table-column label="用户信息" width="120">
          <template #default="{ row }">
            <span>{{ row.userName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="课程信息" min-width="200">
          <template #default="{ row }">
            <div class="course-info">
              <img :src="row.courseCover" class="course-cover" />
              <span>{{ row.courseName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="refundAmount" label="退款金额" width="100">
          <template #default="{ row }">
            <span class="price">¥{{ row.refundAmount }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="退款原因" min-width="150" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="applyTime" label="申请时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleView(row)">详情</el-button>
            <template v-if="row.status === 'pending'">
              <el-button type="success" link @click="handleApprove(row)">通过</el-button>
              <el-button type="danger" link @click="handleReject(row)">拒绝</el-button>
            </template>
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

    <!-- 退款详情弹窗 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="退款详情"
      width="600px"
    >
      <div class="refund-detail" v-if="currentRefund">
        <div class="detail-section">
          <h4>订单信息</h4>
          <div class="detail-row">
            <span class="label">订单号：</span>
            <span>{{ currentRefund.orderNo }}</span>
          </div>
          <div class="detail-row">
            <span class="label">用户姓名：</span>
            <span>{{ currentRefund.userName }}</span>
          </div>
          <div class="detail-row">
            <span class="label">课程名称：</span>
            <span>{{ currentRefund.courseName }}</span>
          </div>
          <div class="detail-row">
            <span class="label">支付金额：</span>
            <span class="price">¥{{ currentRefund.payAmount }}</span>
          </div>
        </div>

        <div class="detail-section">
          <h4>退款信息</h4>
          <div class="detail-row">
            <span class="label">退款金额：</span>
            <span class="price">¥{{ currentRefund.refundAmount }}</span>
          </div>
          <div class="detail-row">
            <span class="label">退款原因：</span>
            <span>{{ currentRefund.reason }}</span>
          </div>
          <div class="detail-row">
            <span class="label">申请时间：</span>
            <span>{{ currentRefund.applyTime }}</span>
          </div>
          <div class="detail-row">
            <span class="label">审批状态：</span>
            <el-tag :type="getStatusType(currentRefund.status)">{{ getStatusText(currentRefund.status) }}</el-tag>
          </div>
        </div>

        <div class="detail-section" v-if="currentRefund.status !== 'pending'">
          <h4>审批信息</h4>
          <div class="detail-row">
            <span class="label">审批人：</span>
            <span>{{ currentRefund.approver }}</span>
          </div>
          <div class="detail-row">
            <span class="label">审批时间：</span>
            <span>{{ currentRefund.approveTime }}</span>
          </div>
          <div class="detail-row" v-if="currentRefund.rejectReason">
            <span class="label">拒绝原因：</span>
            <span>{{ currentRefund.rejectReason }}</span>
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- 审批弹窗 -->
    <el-dialog
      v-model="approveDialogVisible"
      :title="approveType === 'approve' ? '确认通过' : '确认拒绝'"
      width="500px"
    >
      <el-form :model="approveForm" label-width="100px">
        <el-form-item label="订单号">
          <span>{{ currentRefund?.orderNo }}</span>
        </el-form-item>
        <el-form-item label="退款金额">
          <span class="price">¥{{ currentRefund?.refundAmount }}</span>
        </el-form-item>
        <el-form-item v-if="approveType === 'reject'" label="拒绝原因">
          <el-input v-model="approveForm.reason" type="textarea" :rows="3" placeholder="请输入拒绝原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approveDialogVisible = false">取消</el-button>
        <el-button :type="approveType === 'approve' ? 'success' : 'danger'" @click="handleConfirmApprove">
          {{ approveType === 'approve' ? '确认通过' : '确认拒绝' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getDetails, getRefundPage, refundApproval } from '@/api/refund'

const searchForm = reactive({ status: '', keyword: '' })
const pagination = reactive({ page: 1, pageSize: 10, total: 0 })
const refundList = ref([])
const loading = ref(false)
const total = ref(0)
const pendingCount = ref(0)
const approvedCount = ref(0)
const rejectedCount = ref(0)

const detailDialogVisible = ref(false)
const approveDialogVisible = ref(false)
const currentRefund = ref(null)
const approveType = ref('approve')
const approveForm = reactive({ reason: '' })

const responseData = response => response?.data ?? response
const ensureSuccess = response => {
  if (response?.code !== 200) throw new Error(response?.msg || '请求失败')
  return responseData(response)
}
const money = value => {
  const amount = Number(value || 0)
  return Number.isFinite(amount) ? (amount / 100).toFixed(2) : '0.00'
}
const normalizeRefund = item => ({
  ...item,
  userName: item.userName || `用户${item.userId || ''}`,
  courseName: item.courseName || '课程',
  courseCover: item.courseCover || '/src/assets/images/courses/vue3.svg',
  payAmount: item.payAmount ?? money(item.payAmountCents),
  refundAmount: typeof item.refundAmount === 'number' ? money(item.refundAmount) : (item.refundAmount || '0.00'),
  reason: item.reason || item.refundReason || '-',
  status: item.status || 'pending'
})

const getRefundList = async () => {
  loading.value = true
  try {
    const response = await getRefundPage({
      pageNo: pagination.page,
      pageSize: pagination.pageSize,
      status: searchForm.status || undefined,
      keyword: searchForm.keyword || undefined
    })
    const data = ensureSuccess(response) || {}
    const rows = data.list || data.rows || []
    refundList.value = (Array.isArray(rows) ? rows : []).map(normalizeRefund)
    pagination.total = Number(data.total ?? refundList.value.length)
  } catch (error) {
    refundList.value = []
    pagination.total = 0
    ElMessage.error(error?.message || '退款列表加载失败')
  } finally { loading.value = false }
}

const loadStatistics = async () => {
  try {
    const response = await getRefundPage({ pageNo: 1, pageSize: 200 })
    const data = ensureSuccess(response) || {}
    const rows = Array.isArray(data.list) ? data.list : (data.rows || [])
    const values = rows.map(normalizeRefund)
    total.value = Number(data.total ?? values.length)
    pendingCount.value = values.filter(item => item.status === 'pending').length
    approvedCount.value = values.filter(item => item.status === 'approved').length
    rejectedCount.value = values.filter(item => item.status === 'rejected').length
  } catch (error) {
    total.value = pendingCount.value = approvedCount.value = rejectedCount.value = 0
  }
}

const refresh = () => Promise.all([getRefundList(), loadStatistics()])
const handleSearch = () => { pagination.page = 1; getRefundList() }
const handleReset = () => { Object.assign(searchForm, { status: '', keyword: '' }); handleSearch() }
const handleSizeChange = () => { pagination.page = 1; getRefundList() }
const handleCurrentChange = () => getRefundList()

const handleView = async row => {
  try {
    const data = ensureSuccess(await getDetails(row.id))
    currentRefund.value = normalizeRefund(data || row)
  } catch (error) {
    currentRefund.value = row
    ElMessage.error(error?.message || '退款详情加载失败')
  }
  detailDialogVisible.value = true
}
const handleApprove = row => {
  currentRefund.value = row
  approveType.value = 'approve'
  approveForm.reason = ''
  approveDialogVisible.value = true
}
const handleReject = row => {
  currentRefund.value = row
  approveType.value = 'reject'
  approveForm.reason = ''
  approveDialogVisible.value = true
}
const handleConfirmApprove = async () => {
  if (approveType.value === 'reject' && !approveForm.reason.trim()) return ElMessage.warning('请输入拒绝原因')
  if (!currentRefund.value?.id) return ElMessage.error('退款编号缺失，无法审批')
  try {
    ensureSuccess(await refundApproval({
      id: currentRefund.value.id,
      status: approveType.value,
      reason: approveForm.reason.trim()
    }))
    ElMessage.success(approveType.value === 'approve' ? '退款申请已通过' : '退款申请已拒绝')
    approveDialogVisible.value = false
    await refresh()
  } catch (error) { ElMessage.error(error?.message || '退款审批失败') }
}

// 状态类型
const getStatusType = (status) => {
  const map = { pending: 'warning', approved: 'success', rejected: 'danger' }
  return map[status] || 'info'
}

// 状态文本
const getStatusText = (status) => {
  const map = { pending: '待审批', approved: '已通过', rejected: '已拒绝' }
  return map[status] || '未知'
}

onMounted(() => {
  getRefundList()
  loadStatistics()
})
</script>

<style scoped>
.refund-management {
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
.table-card {
  border-radius: 12px;
  border: none;
}

.table-card :deep(.el-card__body) {
  padding: 0;
}

/* 课程信息 */
.course-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.course-cover {
  width: 40px;
  height: 22px;
  border-radius: 4px;
  object-fit: cover;
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

/* 退款详情 */
.refund-detail {
  max-height: 600px;
  overflow-y: auto;
}

.detail-section {
  margin-bottom: 24px;
}

.detail-section h4 {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #ebeef5;
}

.detail-row {
  display: flex;
  margin-bottom: 8px;
  font-size: 14px;
  color: #606266;
}

.detail-row .label {
  width: 100px;
  flex-shrink: 0;
  color: #909399;
}
</style>
