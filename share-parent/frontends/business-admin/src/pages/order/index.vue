<template>
  <div class="order-management">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2>订单管理</h2>
    </div>

    <!-- 统计卡片 -->
    <div class="stat-cards">
      <el-card shadow="hover" class="stat-card">
        <div class="stat-content">
          <div class="stat-icon" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%)">
            <span class="stat-number">{{ totalAmount }}</span>
          </div>
          <div class="stat-label">累计订单金额（元）</div>
        </div>
      </el-card>
      <el-card shadow="hover" class="stat-card">
        <div class="stat-content">
          <div class="stat-icon" style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%)">
            <span class="stat-number">{{ pendingAmount }}</span>
          </div>
          <div class="stat-label">待支付金额（元）</div>
        </div>
      </el-card>
      <el-card shadow="hover" class="stat-card">
        <div class="stat-content">
          <div class="stat-icon" style="background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)">
            <span class="stat-number">{{ paidAmount }}</span>
          </div>
          <div class="stat-label">实收金额（元）</div>
        </div>
      </el-card>
      <el-card shadow="hover" class="stat-card">
        <div class="stat-content">
          <div class="stat-icon" style="background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)">
            <span class="stat-number">{{ refundAmount }}</span>
          </div>
          <div class="stat-label">已退款金额（元）</div>
        </div>
      </el-card>
    </div>

    <!-- 搜索区域 -->
    <el-card class="search-card" shadow="hover">
      <el-form :model="searchForm" inline>
        <el-form-item label="订单状态">
          <el-select v-model="searchForm.status" placeholder="全部状态" clearable style="width: 120px">
            <el-option label="全部" value="" />
            <el-option label="待支付" value="pending" />
            <el-option label="已支付" value="paid" />
            <el-option label="已退款" value="refunded" />
            <el-option label="已关闭" value="closed" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="searchForm.keyword" placeholder="订单号/用户/课程" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 订单列表 -->
    <el-card class="table-card" shadow="hover">
      <el-table :data="orderList" stripe style="width: 100%" v-loading="loading">
        <el-table-column prop="orderNo" label="订单号" width="150" />
        <el-table-column label="用户信息" width="120">
          <template #default="{ row }">
            <div class="user-info">
              <span>{{ row.userName }}</span>
            </div>
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
        <el-table-column prop="orderAmount" label="订单金额" width="100">
          <template #default="{ row }">
            <span class="price">¥{{ row.orderAmount }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="payAmount" label="实付金额" width="100">
          <template #default="{ row }">
            <span class="price">¥{{ row.payAmount }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="payType" label="支付方式" width="100">
          <template #default="{ row }">
            <span>{{ row.payType || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="下单时间" width="180" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleView(row)">详情</el-button>
            <el-button v-if="row.status === 'paid'" type="warning" link @click="handleRefund(row)">退款</el-button>
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

    <!-- 订单详情弹窗 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="订单详情"
      width="700px"
    >
      <div class="order-detail" v-if="currentOrder">
        <div class="detail-section">
          <h4>订单信息</h4>
          <div class="detail-row">
            <span class="label">订单号：</span>
            <span>{{ currentOrder.orderNo }}</span>
          </div>
          <div class="detail-row">
            <span class="label">下单时间：</span>
            <span>{{ currentOrder.createTime }}</span>
          </div>
          <div class="detail-row">
            <span class="label">订单状态：</span>
            <el-tag :type="getStatusType(currentOrder.status)">{{ getStatusText(currentOrder.status) }}</el-tag>
          </div>
        </div>

        <div class="detail-section">
          <h4>用户信息</h4>
          <div class="detail-row">
            <span class="label">用户姓名：</span>
            <span>{{ currentOrder.userName }}</span>
          </div>
          <div class="detail-row">
            <span class="label">手机号：</span>
            <span>{{ currentOrder.phone }}</span>
          </div>
        </div>

        <div class="detail-section">
          <h4>课程信息</h4>
          <div class="detail-row">
            <span class="label">课程名称：</span>
            <span>{{ currentOrder.courseName }}</span>
          </div>
          <div class="detail-row">
            <span class="label">课程价格：</span>
            <span class="price">¥{{ currentOrder.orderAmount }}</span>
          </div>
        </div>

        <div class="detail-section">
          <h4>支付信息</h4>
          <div class="detail-row">
            <span class="label">支付方式：</span>
            <span>{{ currentOrder.payType || '-' }}</span>
          </div>
          <div class="detail-row">
            <span class="label">支付时间：</span>
            <span>{{ currentOrder.payTime || '-' }}</span>
          </div>
          <div class="detail-row">
            <span class="label">实付金额：</span>
            <span class="price">¥{{ currentOrder.payAmount }}</span>
          </div>
        </div>

        <div class="detail-section" v-if="currentOrder.status === 'refunded'">
          <h4>退款信息</h4>
          <div class="detail-row">
            <span class="label">退款时间：</span>
            <span>{{ currentOrder.refundTime }}</span>
          </div>
          <div class="detail-row">
            <span class="label">退款原因：</span>
            <span>{{ currentOrder.refundReason }}</span>
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- 退款弹窗 -->
    <el-dialog
      v-model="refundDialogVisible"
      title="确认退款"
      width="500px"
    >
      <el-form :model="refundForm" label-width="100px">
        <el-form-item label="订单号">
          <span>{{ currentOrder?.orderNo }}</span>
        </el-form-item>
        <el-form-item label="退款金额">
          <span class="price">¥{{ currentOrder?.payAmount }}</span>
        </el-form-item>
        <el-form-item label="退款原因">
          <el-input v-model="refundForm.reason" type="textarea" :rows="3" placeholder="请输入退款原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="refundDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleConfirmRefund">确认退款</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getDetails, getOrderPage, getTradeStatistics } from '@/api/order'
import { refund } from '@/api/refund'

const searchForm = reactive({ status: '', keyword: '' })
const pagination = reactive({ page: 1, pageSize: 10, total: 0 })
const orderList = ref([])
const loading = ref(false)
const totalAmount = ref('0.00')
const pendingAmount = ref('0.00')
const paidAmount = ref('0.00')
const refundAmount = ref('0.00')

const detailDialogVisible = ref(false)
const refundDialogVisible = ref(false)
const currentOrder = ref(null)
const refundForm = reactive({ reason: '' })

const responseData = response => response?.data ?? response
const ensureSuccess = response => {
  if (response?.code !== 200) throw new Error(response?.msg || '请求失败')
  return responseData(response)
}
const money = value => {
  const amount = Number(value || 0)
  return Number.isFinite(amount) ? amount.toFixed(2) : '0.00'
}
const normalizeOrder = item => ({
  ...item,
  userName: item.userName || `用户${item.userId || ''}`,
  phone: item.phone || '-',
  courseName: item.courseName || '课程',
  courseCover: item.courseCover || '/src/assets/images/courses/vue3.svg',
  orderAmount: item.orderAmount ?? money(item.totalAmount),
  payAmount: item.payAmount ?? money(item.realAmount ?? item.payableAmount),
  payType: item.payType || item.paymentChannel || '-',
  status: item.status || 'pending'
})

const getOrderList = async () => {
  loading.value = true
  try {
    const response = await getOrderPage({
      pageNo: pagination.page,
      pageSize: pagination.pageSize,
      status: searchForm.status || undefined,
      keyword: searchForm.keyword || undefined
    })
    const data = ensureSuccess(response) || {}
    const rows = data.list || data.rows || []
    orderList.value = (Array.isArray(rows) ? rows : []).map(normalizeOrder)
    pagination.total = Number(data.total ?? orderList.value.length)
  } catch (error) {
    orderList.value = []
    pagination.total = 0
    ElMessage.error(error?.message || '订单加载失败')
  } finally { loading.value = false }
}

const loadStatistics = async () => {
  try {
    const data = ensureSuccess(await getTradeStatistics()) || {}
    totalAmount.value = money(data.totalAmount ?? data.totalRevenue)
    pendingAmount.value = money(data.pendingAmount)
    paidAmount.value = money(data.paidAmount ?? data.totalRevenue)
    refundAmount.value = money(data.refundAmount)
  } catch (error) {
    // 统计接口不是订单列表的前置依赖，统计不可用时保留 0，不伪造演示数据。
    totalAmount.value = pendingAmount.value = paidAmount.value = refundAmount.value = '0.00'
  }
}

const handleSearch = () => { pagination.page = 1; getOrderList() }
const handleReset = () => { Object.assign(searchForm, { status: '', keyword: '' }); handleSearch() }
const handleSizeChange = () => { pagination.page = 1; getOrderList() }
const handleCurrentChange = () => getOrderList()

const handleView = async row => {
  try {
    const data = ensureSuccess(await getDetails(row.orderId || row.id))
    currentOrder.value = normalizeOrder(data || row)
  } catch (error) {
    currentOrder.value = row
    ElMessage.error(error?.message || '订单详情加载失败')
  }
  detailDialogVisible.value = true
}

const handleRefund = row => {
  currentOrder.value = row
  refundForm.reason = ''
  refundDialogVisible.value = true
}

const handleConfirmRefund = async () => {
  if (!refundForm.reason.trim()) return ElMessage.warning('请输入退款原因')
  if (!currentOrder.value?.id) return ElMessage.error('订单明细编号缺失，无法申请退款')
  try {
    ensureSuccess(await refund({ orderDetailId: currentOrder.value.id, refundReason: refundForm.reason.trim() }))
    ElMessage.success('退款申请已提交')
    refundDialogVisible.value = false
    await Promise.all([getOrderList(), loadStatistics()])
  } catch (error) { ElMessage.error(error?.message || '退款申请失败') }
}

// 状态类型
const getStatusType = (status) => {
  const map = { pending: 'warning', paid: 'success', refunded: 'danger', closed: 'info' }
  return map[status] || 'info'
}

// 状态文本
const getStatusText = (status) => {
  const map = { pending: '待支付', paid: '已支付', refunded: '已退款', closed: '已关闭' }
  return map[status] || '未知'
}

onMounted(() => {
  getOrderList()
  loadStatistics()
})
</script>

<style scoped>
.order-management {
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
  font-size: 18px;
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

/* 用户信息 */
.user-info {
  display: flex;
  align-items: center;
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

/* 订单详情 */
.order-detail {
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
