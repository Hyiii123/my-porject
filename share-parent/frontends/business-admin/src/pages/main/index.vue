<template>
  <div class="dashboard-container">
    <!-- IAIC 工作台科技大屏 Banner -->
    <div class="admin-dashboard-hero">
      <div class="home-eyebrow">
        <span class="idx">01</span>
        <span class="bar"></span>
        <span>ENTERPRISE OPERATIONS & AI TELEMETRY</span>
      </div>
      <h1 class="hero-title">智问学伴 · <span class="accent">业务协同管理工作台</span></h1>
      <p class="hero-desc">全站课程资产、学员认知画像、多智能体导学推演、订单交易履约与客服大模型实时可观测大盘</p>
    </div>

    <div class="stat-cards">
      <el-card v-for="(item, index) in statCards" :key="index" class="stat-card" shadow="never" v-loading="loading">
        <div class="stat-card-content">
          <div class="stat-icon" :style="{ backgroundColor: item.bgColor, color: item.iconColor }">
            <el-icon :size="24"><component :is="item.icon" /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ item.value }}</div>
            <div class="stat-label">{{ item.label }}</div>
          </div>
        </div>
        <div class="stat-footer">
          <span :class="['stat-change', item.changeType === 'up' ? 'up' : 'down']">
            <el-icon v-if="item.changeType === 'up'"><Top /></el-icon>
            <el-icon v-else><Bottom /></el-icon>
            {{ item.change || '--' }}
          </span>
          <span class="stat-period">较昨日</span>
        </div>
      </el-card>
    </div>

    <div class="chart-section">
      <el-card class="chart-card" shadow="never" v-loading="loading">
        <template #header>
          <div class="card-header">
            <span>访问趋势</span>
            <el-radio-group v-model="chartType" size="small">
              <el-radio-button label="week">本周</el-radio-button>
              <el-radio-button label="month">本月</el-radio-button>
            </el-radio-group>
          </div>
        </template>
        <div class="chart-placeholder" v-if="chartData.length">
          <div class="chart-bars" :class="{ 'month-bars': chartType === 'month' }">
            <div v-for="(item, index) in chartData" :key="index" class="chart-bar-item">
              <div class="bar-wrapper">
                <div class="bar" :style="{ height: item.percent + '%' }">
                  <span class="bar-value">{{ item.value }}</span>
                </div>
              </div>
              <div class="bar-label">{{ item.label }}</div>
            </div>
          </div>
        </div>
        <el-empty v-else description="暂无趋势数据" />
      </el-card>
    </div>

    <div class="bottom-section">
      <el-card class="recent-orders" shadow="never">
        <template #header>
          <div class="card-header">
            <span>最近订单</span>
            <el-button type="primary" link @click="router.push('/order/index')">查看全部</el-button>
          </div>
        </template>
        <el-table :data="recentOrders" stripe style="width: 100%" empty-text="暂无订单">
          <el-table-column prop="id" label="订单号" width="150" show-overflow-tooltip />
          <el-table-column prop="courseName" label="课程名称" show-overflow-tooltip />
          <el-table-column prop="userName" label="用户" width="100" />
          <el-table-column prop="amount" label="金额" width="100">
            <template #default="{ row }">
              <span class="price">¥{{ formatCents(row.amount) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <el-card class="hot-courses" shadow="never">
        <template #header>
          <div class="card-header">
            <span>热门课程</span>
            <el-button type="primary" link @click="router.push('/curriculum/index')">查看全部</el-button>
          </div>
        </template>
        <div v-if="hotCourses.length" class="course-list">
          <div v-for="(course, index) in hotCourses" :key="course.id || index" class="course-item">
            <div class="course-rank" :class="{ 'top-3': index < 3, [`rank-${index+1}`]: index < 3 }">{{ index + 1 }}</div>
            <div class="course-info">
              <div class="course-name">{{ course.title }}</div>
              <div class="course-meta">
                <span>{{ course.teacherName || '讲师团队' }}</span>
                <span>{{ course.learners || 0 }}人在学</span>
              </div>
            </div>
            <div class="course-price">{{ course.isFree ? '免费' : `¥${formatCents(course.price)}` }}</div>
          </div>
        </div>
        <el-empty v-else description="暂无课程数据" />
      </el-card>
    </div>

    <el-card class="todo-section" shadow="never">
      <template #header>
        <div class="card-header"><span>待办事项</span></div>
      </template>
      <div class="todo-grid">
        <div v-for="(item, index) in todoItems" :key="index" class="todo-item" @click="router.push(item.path)">
          <div class="todo-count">{{ item.count }}</div>
          <div class="todo-label">{{ item.label }}</div>
          <el-icon class="todo-arrow"><ArrowRight /></el-icon>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowRight, Bottom, Money, Reading, ShoppingCart, Top, User } from '@element-plus/icons-vue'
import { getGrantInfo, getToday, getTop10 } from '@/api/main'
import { getCoursesPage } from '@/api/curriculum'
import { getQuestionPage } from '@/api/question'
import { getOrderPage } from '@/api/order'
import { getRefundPage } from '@/api/refund'
import { getMarketPage } from '@/api/marketing'

const router = useRouter()
const loading = ref(false)
const chartType = ref('week')
const chartData = ref([])
const recentOrders = ref([])
const hotCourses = ref([])

const statCards = reactive([
  { label: '今日访问量', value: '--', icon: User, bgColor: '#EFF6FF', iconColor: '#2563EB', change: '--', changeType: 'up' },
  { label: '今日订单数', value: '--', icon: ShoppingCart, bgColor: '#F0FDF4', iconColor: '#16A34A', change: '--', changeType: 'up' },
  { label: '今日收入', value: '--', icon: Money, bgColor: '#FEF3C7', iconColor: '#D97706', change: '--', changeType: 'up' },
  { label: '新增学员', value: '--', icon: Reading, bgColor: '#EEF2FF', iconColor: '#4F46E5', change: '--', changeType: 'up' }
])

const todoItems = reactive([
  { label: '退款待审批', count: 0, path: '/order/refund' },
  { label: '优惠券待发布', count: 0, path: '/marketing/index' },
  { label: '课程待审核', count: 0, path: '/curriculum/index' },
  { label: '用户反馈', count: 0, path: '/interactive/index' }
])

const responseData = response => response?.data ?? response
const ensureSuccess = response => {
  if (response?.code !== 200) throw new Error(response?.msg || '请求失败')
  return responseData(response)
}
const toNumber = value => {
  const result = Number(value)
  return Number.isFinite(result) ? result : 0
}
const formatInteger = value => toNumber(value).toLocaleString('zh-CN')
const formatYuan = value => toNumber(value).toFixed(2)
const formatCents = value => (toNumber(value) / 100).toFixed(2)
const percentChangeType = value => String(value || '').startsWith('-') ? 'down' : 'up'

const getStatusType = status => {
  const map = { pending: 'warning', paid: 'success', refunded: 'danger', closed: 'info' }
  return map[status] || 'info'
}

const getStatusText = status => {
  const map = { pending: '待支付', paid: '已支付', refunded: '已退款', closed: '已关闭' }
  return map[status] || '未知'
}

const normalizeOrder = item => {
  const cents = item.totalAmount != null
    ? toNumber(item.totalAmount)
    : toNumber(item.orderAmount) * 100
  return {
    id: item.orderNo || item.id,
    courseName: item.courseName || '课程',
    userName: item.userName || `用户${item.userId || ''}`,
    amount: cents,
    status: item.status || 'closed'
  }
}

const loadToday = async () => {
  const data = ensureSuccess(await getToday()) || {}
  const today = data.today || data
  const changes = today.changes || data.changes || {}
  const cards = [
    { value: formatInteger(today.todayVisits), change: changes.visits },
    { value: formatInteger(today.todayOrders), change: changes.orders },
    { value: `¥${formatYuan(today.todayRevenue)}`, change: changes.revenue },
    { value: formatInteger(today.newStudents), change: changes.newStudents }
  ]
  cards.forEach((item, index) => Object.assign(statCards[index], item, { changeType: percentChangeType(item.change) }))
}

const loadChart = async () => {
  const days = chartType.value === 'month' ? 30 : 7
  const data = ensureSuccess(await getGrantInfo({ days, types: '1' })) || {}
  const values = Array.isArray(data.series?.[0]?.data) ? data.series[0].data.map(toNumber) : []
  const labels = Array.isArray(data.xaxis?.[0]?.data) ? data.xaxis[0].data : []
  const max = Math.max(...values, 1)
  chartData.value = values.map((value, index) => ({
    label: labels[index] || `第${index + 1}天`,
    value: formatInteger(value),
    percent: Math.max(value > 0 ? 5 : 0, Math.round(value / max * 100))
  }))
}

const loadOrders = async () => {
  const data = ensureSuccess(await getOrderPage({ pageNo: 1, pageSize: 5 })) || {}
  const rows = Array.isArray(data.list) ? data.list : (data.rows || [])
  recentOrders.value = (Array.isArray(rows) ? rows : []).map(normalizeOrder)
}

const loadCourses = async () => {
  const data = ensureSuccess(await getTop10()) || {}
  const rows = Array.isArray(data.list) ? data.list : (data.rows || data.courses || [])
  hotCourses.value = Array.isArray(rows) ? rows : []
}

const rowsOf = response => {
  const data = responseData(response) || {}
  return Array.isArray(data.list) ? data.list : (Array.isArray(data.rows) ? data.rows : [])
}

const loadTodos = async () => {
  const [refundResult, couponResult, courseResult, questionResult] = await Promise.allSettled([
    getRefundPage({ pageNo: 1, pageSize: 200, status: 'pending' }),
    getMarketPage({ pageNo: 1, pageSize: 200, status: 0 }),
    getCoursesPage({ pageNo: 1, pageSize: 200, status: 0 }),
    getQuestionPage({ pageNo: 1, pageSize: 200 })
  ])
  const count = (result, predicate = () => true) => result.status === 'fulfilled'
    ? rowsOf(result.value).filter(predicate).length
    : 0
  todoItems[0].count = count(refundResult)
  todoItems[1].count = count(couponResult)
  todoItems[2].count = count(courseResult)
  todoItems[3].count = count(questionResult, item => Number(item.replyCount || item.replyTimes || item.answers || 0) === 0)
}

const loadDashboard = async () => {
  loading.value = true
  const results = await Promise.allSettled([loadToday(), loadChart(), loadOrders(), loadCourses(), loadTodos()])
  if (results.some(item => item.status === 'rejected')) ElMessage.warning('部分工作台数据加载失败，请稍后刷新')
  loading.value = false
}

watch(chartType, () => loadChart().catch(() => {
  chartData.value = []
  ElMessage.error('趋势数据加载失败')
}))

onMounted(loadDashboard)
</script>

<style scoped lang="scss">
.dashboard-container {
  padding: 28px;
  background: #080B11;
  min-height: 100vh;
  color: #F8FAFC;
  font-family: var(--cn);
}

.admin-dashboard-hero {
  background: 
    radial-gradient(80% 60% at 50% 0%, rgba(56, 189, 248, 0.12), transparent 70%),
    radial-gradient(60% 50% at 85% 20%, rgba(99, 102, 241, 0.1), transparent 65%),
    rgba(14, 20, 34, 0.85);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 20px;
  padding: 30px 36px;
  margin-bottom: 24px;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.1), 0 16px 36px -8px rgba(0, 0, 0, 0.6);
}

.hero-title {
  font-family: var(--display);
  font-size: 28px;
  font-weight: 800;
  color: #FFFFFF;
  margin: 10px 0 6px;
  letter-spacing: -0.02em;

  .accent {
    background: linear-gradient(135deg, #38BDF8 0%, #818CF8 50%, #34D399 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
  }
}

.hero-desc {
  font-size: 14px;
  color: #94A3B8;
  margin: 0;
  line-height: 1.6;
}

.home-eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  font-family: var(--mono);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: #38BDF8;

  .idx { color: #34D399; font-weight: 800; }
  .bar { width: 22px; height: 2px; background: linear-gradient(90deg, #38BDF8, #818CF8); border-radius: 2px; }
}

.stat-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

.stat-card {
  border-radius: 18px !important;
  border: 1px solid rgba(255, 255, 255, 0.08) !important;
  background: rgba(16, 23, 38, 0.72) !important;
  backdrop-filter: blur(20px) !important;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.1), 0 12px 28px -6px rgba(0, 0, 0, 0.6) !important;
  transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1) !important;

  &:hover {
    transform: translateY(-3px) !important;
    border-color: rgba(56, 189, 248, 0.4) !important;
    box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.2), 0 18px 40px -8px rgba(14, 165, 233, 0.25) !important;
  }

  :deep(.el-card__body) {
    padding: 22px;
  }
}

.stat-card-content {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}

.stat-icon {
  width: 50px;
  height: 50px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.3);
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-family: var(--mono);
  font-size: 26px;
  font-weight: 800;
  color: #FFFFFF;
  margin-bottom: 4px;
  font-variant-numeric: tabular-nums;
  line-height: 1.1;
}

.stat-label {
  font-size: 13px;
  color: #94A3B8;
  font-weight: 500;
}

.stat-footer {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-top: 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.stat-change {
  font-size: 12.5px;
  font-family: var(--mono);
  display: flex;
  align-items: center;
  gap: 4px;
  font-weight: 600;

  &.up { color: #34D399; }
  &.down { color: #FB7185; }
}

.stat-period {
  font-size: 12px;
  color: #64748B;
}

.chart-section {
  margin-bottom: 24px;
}

.chart-card {
  border-radius: 18px !important;
  border: 1px solid rgba(255, 255, 255, 0.08) !important;
  background: rgba(16, 23, 38, 0.72) !important;
  backdrop-filter: blur(20px) !important;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.1) !important;

  :deep(.el-card__header) {
    border-bottom: 1px solid rgba(255, 255, 255, 0.08) !important;
    padding: 16px 24px !important;
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;

  span {
    font-size: 16px;
    font-weight: 700;
    color: #FFFFFF;
    letter-spacing: -0.01em;
  }
}

.chart-placeholder {
  height: 280px;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  padding: 24px 0 12px;
}

.chart-bars {
  display: flex;
  align-items: flex-end;
  gap: 32px;
  height: 100%;
  width: 100%;
  max-width: 960px;
  &.month-bars { gap: 6px; }
}

.chart-bar-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  height: 100%;
  min-width: 0;
}

.bar-wrapper {
  flex: 1;
  width: 100%;
  display: flex;
  align-items: flex-end;
  justify-content: center;
}

.bar {
  width: 32px;
  background: linear-gradient(180deg, #38BDF8 0%, #2563EB 100%);
  border-radius: 6px 6px 0 0;
  position: relative;
  transition: height 0.4s cubic-bezier(0.16, 1, 0.3, 1), background-color 0.2s;
  box-shadow: 0 0 12px rgba(56, 189, 248, 0.35);

  &:hover {
    background: linear-gradient(180deg, #7DD3FC 0%, #3B82F6 100%);
    box-shadow: 0 0 20px rgba(56, 189, 248, 0.6);
  }
}

.month-bars .bar { width: 14px; }

.bar-value {
  position: absolute;
  top: -24px;
  left: 50%;
  transform: translateX(-50%);
  font-family: var(--mono);
  font-size: 11px;
  color: #38BDF8;
  font-weight: 700;
  white-space: nowrap;
}

.bar-label {
  margin-top: 10px;
  font-family: var(--mono);
  font-size: 12px;
  color: #94A3B8;
  white-space: nowrap;
}

.month-bars .bar-label {
  font-size: 10px;
  transform: rotate(-45deg);
  transform-origin: top center;
  margin-top: 14px;
}

.bottom-section {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  margin-bottom: 24px;
}

.recent-orders, .hot-courses {
  border-radius: 18px !important;
  border: 1px solid rgba(255, 255, 255, 0.08) !important;
  background: rgba(16, 23, 38, 0.72) !important;
  backdrop-filter: blur(20px) !important;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.1) !important;

  :deep(.el-card__header) {
    border-bottom: 1px solid rgba(255, 255, 255, 0.08) !important;
    padding: 16px 24px !important;
  }
}

.price {
  font-family: var(--mono);
  color: #34D399;
  font-weight: 700;
}

.course-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.course-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 12px;
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);

  &:hover {
    background: rgba(255, 255, 255, 0.06);
    border-color: rgba(56, 189, 248, 0.35);
    transform: translateX(3px);
  }
}

.course-rank {
  width: 26px;
  height: 26px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.08);
  color: #94A3B8;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 800;
  font-family: var(--mono);

  &.rank-1 { background: linear-gradient(135deg, #F59E0B, #D97706); color: #fff; box-shadow: 0 0 10px rgba(245, 158, 11, 0.4); }
  &.rank-2 { background: linear-gradient(135deg, #38BDF8, #2563EB); color: #fff; box-shadow: 0 0 10px rgba(56, 189, 248, 0.4); }
  &.rank-3 { background: linear-gradient(135deg, #34D399, #059669); color: #fff; box-shadow: 0 0 10px rgba(52, 211, 153, 0.4); }
}

.course-info {
  flex: 1;
  min-width: 0;
}

.course-name {
  font-size: 13.5px;
  font-weight: 600;
  color: #FFFFFF;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.course-meta {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #94A3B8;
}

.course-price {
  font-family: var(--mono);
  font-size: 15px;
  font-weight: 700;
  color: #34D399;
  white-space: nowrap;
}

.todo-section {
  border-radius: 18px !important;
  border: 1px solid rgba(255, 255, 255, 0.08) !important;
  background: rgba(16, 23, 38, 0.72) !important;
  backdrop-filter: blur(20px) !important;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.1) !important;

  :deep(.el-card__header) {
    border-bottom: 1px solid rgba(255, 255, 255, 0.08) !important;
    padding: 16px 24px !important;
  }
}

.todo-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.todo-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 22px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 14px;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);

  &:hover {
    background: rgba(56, 189, 248, 0.08);
    border-color: rgba(56, 189, 248, 0.4);
    transform: translateY(-2px);
    box-shadow: 0 8px 20px -4px rgba(0, 0, 0, 0.4);
  }

  .todo-count {
    font-family: var(--mono);
    font-size: 28px;
    font-weight: 800;
    color: #38BDF8;
  }

  .todo-label {
    flex: 1;
    font-size: 13.5px;
    color: #CBD5E1;
    font-weight: 600;
  }

  .todo-arrow {
    color: #64748B;
    font-size: 14px;
    transition: transform 0.2s;
  }

  &:hover .todo-arrow {
    transform: translateX(3px);
    color: #38BDF8;
  }
}

@media (max-width: 1200px) {
  .stat-cards { grid-template-columns: repeat(2, 1fr); }
  .bottom-section { grid-template-columns: 1fr; }
  .todo-grid { grid-template-columns: repeat(2, 1fr); }
}

@media (max-width: 768px) {
  .stat-cards { grid-template-columns: 1fr; }
  .todo-grid { grid-template-columns: 1fr; }
}
</style>