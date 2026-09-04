<template>
  <div class="success-container">
    <div class="container">
      <div class="success-content">
        <el-card class="success-card" shadow="hover">
          <div class="success-icon">
            <el-icon :size="80" color="#67c23a"><SuccessFilled /></el-icon>
          </div>
          <h2>支付成功！</h2>
          <p class="success-desc">恭喜您，课程购买成功！</p>

          <div class="order-info">
            <div class="info-row">
              <span>订单编号：</span>
              <span>{{ orderDetails.orderNo || '--' }}</span>
            </div>
            <div class="info-row">
              <span>支付时间：</span>
              <span>{{ orderDetails.payTime || orderDetails.createTime || '--' }}</span>
            </div>
            <div class="info-row">
              <span>支付方式：</span>
              <span>{{ paymentChannelName }}</span>
            </div>
            <div class="info-row">
              <span>支付金额：</span>
              <span class="amount">¥{{ formatAmount(orderDetails.payableAmount) }}</span>
            </div>
          </div>

          <div class="success-actions">
            <el-button type="primary" size="large" @click="goLearning">
              开始学习
            </el-button>
            <el-button size="large" @click="goHome">
              返回首页
            </el-button>
          </div>
        </el-card>

        <!-- 推荐课程 -->
        <el-card class="recommend-card" shadow="hover">
          <template #header>
            <span>推荐课程</span>
          </template>
          <div class="recommend-list">
            <div
              v-for="course in recommendCourses"
              :key="course.id"
              class="recommend-item"
              @click="$router.push(`/details/index?id=${course.id}`)"
            >
              <img :src="course.cover" :alt="course.title" />
              <div class="recommend-info">
                <div class="recommend-title">{{ course.title }}</div>
                <div class="recommend-price">¥{{ (course.price / 100).toFixed(0) }}</div>
              </div>
            </div>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { SuccessFilled } from '@element-plus/icons-vue'
import { getOrderDetails } from '@/api/order.js'
import { getRecommendClassList } from '@/api/class.js'

const route = useRoute()
const router = useRouter()
const orderDetails = ref({})

const formatAmount = (value) => (Number(value || 0) / 100).toFixed(2)
const paymentChannelName = computed(() => ({
  wechat: '微信支付',
  alipay: '支付宝',
  balance: '余额支付'
}[orderDetails.value.paymentChannel] || orderDetails.value.paymentChannel || '--'))

// 推荐课程
const recommendCourses = ref([])

// 开始学习
const goLearning = () => {
  const firstCourse = orderDetails.value.details?.[0]
  router.push({ path: '/learning/index', query: { courseId: firstCourse?.courseId } })
}

// 返回首页
const goHome = () => {
  router.push('/main/index')
}

onMounted(async () => {
  const orderId = route.query.orderId
  const [orderResponse, recommendResponse] = await Promise.allSettled([
    orderId ? getOrderDetails(orderId) : Promise.resolve(null),
    getRecommendClassList('home')
  ])
  if (orderResponse.status === 'fulfilled' && orderResponse.value?.code === 200) {
    orderDetails.value = orderResponse.value.data || {}
  }
  if (recommendResponse.status === 'fulfilled' && recommendResponse.value?.code === 200) {
    const data = recommendResponse.value.data
    const rows = Array.isArray(data) ? data : (data?.list || [])
    recommendCourses.value = rows.slice(0, 3).map(course => ({
      ...course,
      title: course.title || course.courseName,
      cover: course.cover || course.coverUrl || '',
      price: Number(course.price || 0)
    }))
  }
})
</script>

<style scoped>
.success-container {
  background: #f5f7fa;
  min-height: 100vh;
  padding: 30px 0;
}

.container {
  max-width: 600px;
  margin: 0 auto;
  padding: 0 20px;
}

.success-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.success-card {
  text-align: center;
  border-radius: 12px;
  border: none;
}

.success-icon {
  margin-bottom: 20px;
}

.success-card h2 {
  font-size: 24px;
  font-weight: 600;
  color: #67c23a;
  margin: 0 0 8px;
}

.success-desc {
  font-size: 16px;
  color: #909399;
  margin: 0 0 30px;
}

.order-info {
  text-align: left;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
  margin-bottom: 30px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  padding: 10px 0;
  font-size: 14px;
  color: #606266;
  border-bottom: 1px solid #ebeef5;
}

.info-row:last-child {
  border-bottom: none;
}

.amount {
  font-weight: 600;
  color: #f56c6c;
}

.success-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
}

.success-actions .el-button {
  width: 160px;
}

/* 推荐课程 */
.recommend-card {
  border-radius: 12px;
  border: none;
}

.recommend-list {
  display: flex;
  gap: 16px;
}

.recommend-item {
  flex: 1;
  cursor: pointer;
  transition: transform 0.2s;
}

.recommend-item:hover {
  transform: translateY(-4px);
}

.recommend-item img {
  width: 100%;
  height: 100px;
  border-radius: 8px;
  object-fit: cover;
  margin-bottom: 8px;
}

.recommend-info {
  text-align: center;
}

.recommend-title {
  font-size: 13px;
  color: #303133;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recommend-price {
  font-size: 15px;
  font-weight: 600;
  color: #f56c6c;
}
</style>
