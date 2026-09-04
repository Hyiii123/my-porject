<template>
  <div class="settlement-container">
    <div class="container">
      <h2 class="page-title">确认订单</h2>

      <div v-loading="loading" class="settlement-content">
        <!-- 课程信息 -->
        <el-card class="course-card" shadow="hover">
          <template #header>
            <span>课程信息</span>
          </template>
          <div class="course-list">
            <div v-for="course in selectedCourses" :key="course.id" class="course-item">
              <div class="course-cover">
                <img :src="course.cover" :alt="course.title" />
              </div>
              <div class="course-info">
                <h4>{{ course.title }}</h4>
                <p>{{ course.teacherName }}</p>
              </div>
              <div class="course-price">¥{{ (course.price / 100).toFixed(2) }}</div>
            </div>
          </div>
        </el-card>

        <!-- 优惠券 -->
        <el-card class="coupon-card" shadow="hover">
          <template #header>
            <span>优惠券</span>
          </template>
          <div class="coupon-select">
            <el-select v-model="selectedCoupon" placeholder="选择优惠券" style="width: 100%">
              <el-option label="不使用优惠券" value="" />
              <el-option
                v-for="coupon in availableCoupons"
                :key="coupon.id"
                :label="coupon.name"
                :value="coupon.id"
              >
                <span>{{ coupon.name }}</span>
                <span style="float: right; color: #f56c6c">
                  {{ coupon.type === 'fixed' ? `减${coupon.value / 100}元` : `${coupon.value / 10}折` }}
                </span>
              </el-option>
            </el-select>
          </div>
        </el-card>

        <!-- 支付方式 -->
        <el-card class="payment-card" shadow="hover">
          <template #header>
            <span>支付方式</span>
          </template>
          <div class="payment-methods">
            <div
              v-for="method in paymentMethods"
              :key="method.id"
              class="payment-method"
              :class="{ active: selectedPayment === method.id }"
              @click="selectedPayment = method.id"
            >
              <div class="method-icon" :style="{ background: method.color }">
                <span>{{ method.icon }}</span>
              </div>
              <span class="method-name">{{ method.name }}</span>
            </div>
          </div>
        </el-card>

        <!-- 订单汇总 -->
        <el-card class="summary-card" shadow="hover">
          <div class="summary-content">
            <div class="summary-row">
              <span>课程总价：</span>
              <span>¥{{ (totalPrice / 100).toFixed(2) }}</span>
            </div>
            <div class="summary-row" v-if="discountAmount > 0">
              <span>优惠金额：</span>
              <span class="discount">-¥{{ (discountAmount / 100).toFixed(2) }}</span>
            </div>
            <div class="summary-row total">
              <span>实付金额：</span>
              <span class="total-price">¥{{ (finalPrice / 100).toFixed(2) }}</span>
            </div>
          </div>
          <div class="submit-btn">
            <el-button type="primary" size="large" :loading="submitting" @click="handleSubmit">
              提交订单
            </el-button>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { confirmOrderInfo, setOrder } from '@/api/order.js'

const route = useRoute()
const router = useRouter()

// 选中的课程（从购物车或课程详情页传入）
const selectedCourses = ref([])

// 可用优惠券
const availableCoupons = ref([])

// 选中的优惠券
const selectedCoupon = ref('')

// 支付方式
const paymentMethods = [
  { id: 'wechat', name: '微信支付', icon: '微', color: '#07c160' },
  { id: 'alipay', name: '支付宝', icon: '支', color: '#1677ff' }
]

// 选中的支付方式
const selectedPayment = ref('wechat')

// 计算价格
const totalPrice = computed(() => {
  return selectedCourses.value.reduce((sum, course) => sum + course.price * course.quantity, 0)
})

const discountAmount = computed(() => {
  if (!selectedCoupon.value) return 0
  const coupon = availableCoupons.value.find(c => String(c.id) === String(selectedCoupon.value))
  if (!coupon) return 0

  if (coupon.type === 'direct') return coupon.value
  if (coupon.type === 'fixed' && totalPrice.value >= coupon.minAmount) return coupon.value
  if (coupon.type === 'percent' && totalPrice.value >= coupon.minAmount) return totalPrice.value - (totalPrice.value * coupon.value / 100)

  return 0
})

const finalPrice = computed(() => {
  return Math.max(0, totalPrice.value - discountAmount.value)
})

const loading = ref(false)
const submitting = ref(false)

const normalizeCourse = (course = {}) => ({
  ...course,
  id: course.id,
  courseId: course.courseId,
  title: course.title || course.courseName || '未命名课程',
  teacherName: course.teacherName || '讲师团队',
  cover: course.cover || course.coverUrl || '',
  price: Number(course.price || 0),
  originalPrice: Number(course.originalPrice ?? course.price ?? 0),
  quantity: Math.max(1, Number(course.quantity || 1))
})

const normalizeCoupon = (coupon = {}) => {
  const discountType = Number(coupon.discountType)
  const type = discountType === 2 ? 'percent' : discountType === 4 ? 'fixed' : 'direct'
  const centsValue = Number(coupon.discountValue || coupon.value || 0)
  return {
    ...coupon,
    type,
    // 旧页面按分处理金额；折扣券的 value 表示百分比（80 = 8 折）。
    value: type === 'percent' ? centsValue / 10 : centsValue,
    minAmount: Number(coupon.thresholdAmount ?? coupon.minAmount ?? 0)
  }
}

const loadOrderInfo = async () => {
  loading.value = true
  try {
    const params = {}
    if (route.query.ids) params.ids = route.query.ids
    if (route.query.courseId) params.courseId = route.query.courseId
    const response = await confirmOrderInfo(params)
    if (response?.code !== 200) throw new Error(response?.msg || '订单信息加载失败')
    const data = response.data || {}
    const rows = Array.isArray(data) ? data : (data.items || data.list || [])
    selectedCourses.value = rows.map(normalizeCourse)
    availableCoupons.value = (data.availableCoupons || data.coupons || []).map(normalizeCoupon)
  } catch (error) {
    selectedCourses.value = []
    availableCoupons.value = []
    ElMessage.error(error?.message || '订单信息加载失败，请先登录')
  } finally {
    loading.value = false
  }
}

// 提交订单
const handleSubmit = async () => {
  if (selectedCourses.value.length === 0) {
    ElMessage.warning('没有可结算的课程')
    return
  }
  submitting.value = true
  try {
    const response = await setOrder({
      items: selectedCourses.value.map(course => ({
        id: course.id,
        courseId: course.courseId,
        courseName: course.title,
        cover: course.cover,
        price: course.price,
        quantity: course.quantity
      })),
      couponId: selectedCoupon.value ? Number(selectedCoupon.value) : null
    })
    if (response?.code !== 200) throw new Error(response?.msg || '订单提交失败')
    const order = response.data || {}
    router.push({
      path: '/pay/payment',
      query: { orderId: order.id, method: selectedPayment.value }
    })
  } catch (error) {
    ElMessage.error(error?.message || '订单提交失败')
  } finally {
    submitting.value = false
  }
}

onMounted(loadOrderInfo)
</script>

<style scoped>
.settlement-container {
  background: #f5f7fa;
  min-height: 100vh;
  padding: 30px 0;
}

.container {
  max-width: 800px;
  margin: 0 auto;
  padding: 0 20px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 24px;
}

.settlement-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 课程信息 */
.course-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.course-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
}

.course-cover {
  width: 80px;
  height: 45px;
  border-radius: 6px;
  overflow: hidden;
}

.course-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.course-info {
  flex: 1;
}

.course-info h4 {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 4px;
}

.course-info p {
  font-size: 13px;
  color: #909399;
  margin: 0;
}

.course-price {
  font-size: 18px;
  font-weight: 700;
  color: #f56c6c;
}

/* 支付方式 */
.payment-methods {
  display: flex;
  gap: 16px;
}

.payment-method {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 24px;
  border: 2px solid #ebeef5;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.payment-method:hover {
  border-color: #c0c4cc;
}

.payment-method.active {
  border-color: #409eff;
  background: #ecf5ff;
}

.method-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 18px;
  font-weight: 700;
}

.method-name {
  font-size: 15px;
  color: #303133;
}

/* 订单汇总 */
.summary-content {
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
  margin-bottom: 20px;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  font-size: 14px;
  color: #606266;
}

.summary-row.total {
  border-top: 1px solid #ebeef5;
  margin-top: 8px;
  padding-top: 16px;
  font-size: 16px;
  font-weight: 600;
}

.discount {
  color: #67c23a;
}

.total-price {
  font-size: 24px;
  font-weight: 700;
  color: #f56c6c;
}

.submit-btn {
  text-align: right;
}

.submit-btn .el-button {
  width: 200px;
}
</style>
