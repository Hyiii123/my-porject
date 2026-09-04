<template>
  <div class="payment-container">
    <div class="container">
      <h2 class="page-title">支付订单</h2>

      <div class="payment-content">
        <!-- 订单信息 -->
        <el-card class="order-card" shadow="hover">
          <div class="order-info">
            <div class="order-status">
              <el-icon :size="48" color="#409eff"><WalletFilled /></el-icon>
              <div>
                <h3>订单待支付</h3>
                <p v-if="order.orderNo">订单号：{{ order.orderNo }}</p>
                <p>请尽快完成支付，超时后将取消订单</p>
              </div>
            </div>
            <div class="order-amount">
              <span>应付金额：</span>
              <span class="amount">¥{{ formatAmount(order.payableAmount) }}</span>
            </div>
          </div>
        </el-card>

        <!-- 支付方式 -->
        <el-card class="payment-card" shadow="hover">
          <template #header>
            <span>选择支付方式</span>
          </template>
          <div v-loading="loadingMethods" class="payment-methods">
            <div
              v-for="method in paymentMethods"
              :key="method.id"
              class="payment-method"
              :class="{ active: selectedMethod === method.id }"
              @click="selectMethod(method)"
            >
              <div class="method-icon" :style="{ background: method.color }">
                <span>{{ method.icon }}</span>
              </div>
              <span class="method-name">{{ method.name }}</span>
              <el-icon v-if="selectedMethod === method.id" class="check-icon" color="#409eff"><SuccessFilled /></el-icon>
            </div>
          </div>
        </el-card>

        <!-- 二维码区域 -->
        <el-card v-if="showQRCode" class="qrcode-card" shadow="hover">
          <template #header>
            <span>{{ selectedMethodName }}支付</span>
          </template>
          <div class="qrcode-content">
            <div class="qrcode-box">
              <qrcode-vue v-if="payUrl" :value="payUrl" :size="220" level="M" />
              <div v-else class="qrcode-placeholder">
                <el-icon :size="120" color="#c0c4cc"><Iphone /></el-icon>
                <p>请选择支付方式生成二维码</p>
              </div>
            </div>
            <div class="qrcode-tip">
              <p>请使用 <span class="highlight">{{ selectedMethodName }}</span> 扫一扫</p>
              <p>二维码完成支付</p>
            </div>
            <div class="payment-actions">
              <el-button type="primary" size="large" :loading="paying" @click="handleSimulatePay">
                模拟支付成功
              </el-button>
              <el-button size="large" @click="handleCancel">
                取消支付
              </el-button>
            </div>
          </div>
        </el-card>

        <!-- 支付倒计时 -->
        <div v-if="showQRCode" class="countdown">
          <el-icon><Clock /></el-icon>
          <span>支付剩余时间：{{ countdown }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Clock, Iphone, WalletFilled, SuccessFilled } from '@element-plus/icons-vue'
import QrcodeVue from 'qrcode.vue'
import { getOrderDetails, getPayMethod, getPayUrl, getPayState, simulatePaySuccess, cancelOrder } from '@/api/order.js'

const route = useRoute()
const router = useRouter()
const orderId = String(route.query.orderId || '')
const order = ref({})
const paymentMethods = ref([])
const loadingMethods = ref(false)
const paying = ref(false)
const payUrl = ref('')
const paymentState = ref(0)

const selectedMethod = ref(String(route.query.method || ''))
const showQRCode = ref(false)

const selectedMethodName = computed(() => {
  const method = paymentMethods.value.find(item => item.id === selectedMethod.value)
  return method ? method.name : ''
})

const countdown = ref('--:--')
let countdownTimer = null
let stateTimer = null

const formatAmount = (value) => (Number(value || 0) / 100).toFixed(2)

const loadPaymentData = async () => {
  if (!orderId) {
    ElMessage.error('缺少订单编号，请从订单或购物车进入支付')
    router.replace('/pay/carts')
    return
  }
  loadingMethods.value = true
  try {
    const [orderResponse, methodResponse] = await Promise.all([
      getOrderDetails(orderId),
      getPayMethod()
    ])
    if (orderResponse?.code !== 200) throw new Error(orderResponse?.msg || '订单加载失败')
    order.value = orderResponse.data || {}
    paymentState.value = Number(order.value.paymentStatus || 0)
    const methods = Array.isArray(methodResponse?.data) ? methodResponse.data : []
    paymentMethods.value = methods.map(method => ({
      ...method,
      id: method.id || method.type,
      name: method.name || method.title || method.type,
      icon: method.type === 'alipay' ? '支' : '微',
      color: method.type === 'alipay' ? '#1677ff' : '#07c160'
    }))
    if (!selectedMethod.value && paymentMethods.value.length) selectedMethod.value = paymentMethods.value[0].id
    startCountdown(order.value.expireTime)
    if (paymentState.value === 1) router.replace({ path: '/pay/success', query: { orderId } })
    if (selectedMethod.value) {
      const method = paymentMethods.value.find(item => item.id === selectedMethod.value)
      if (method) await selectMethod(method)
    }
  } catch (error) {
    ElMessage.error(error?.message || '支付信息加载失败')
  } finally {
    loadingMethods.value = false
  }
}

const selectMethod = async (method) => {
  selectedMethod.value = method.id
  showQRCode.value = true
  try {
    const response = await getPayUrl({ orderId: Number(orderId), channel: method.id })
    if (response?.code !== 200) throw new Error(response?.msg || '支付二维码生成失败')
    payUrl.value = response.data?.payUrl || ''
    startPaymentPolling()
  } catch (error) {
    payUrl.value = ''
    ElMessage.error(error?.message || '支付二维码生成失败')
  }
}

const startCountdown = (expireTime) => {
  if (countdownTimer) clearInterval(countdownTimer)
  const parsedExpire = expireTime ? new Date(expireTime).getTime() : NaN
  const expireAt = Number.isNaN(parsedExpire) ? Date.now() + 30 * 60 * 1000 : parsedExpire
  const update = () => {
    const remaining = Math.max(0, expireAt - Date.now())
    const totalSeconds = Math.floor(remaining / 1000)
    const minutes = Math.floor(totalSeconds / 60)
    const seconds = totalSeconds % 60
    countdown.value = `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`
    if (remaining <= 0) {
      clearInterval(countdownTimer)
      ElMessage.warning('支付超时，订单已取消')
      router.push('/main/index')
    }
  }
  update()
  countdownTimer = setInterval(update, 1000)
}

const startPaymentPolling = () => {
  if (stateTimer) clearInterval(stateTimer)
  stateTimer = setInterval(async () => {
    const response = await getPayState({ orderId: Number(orderId) })
    if (response?.code === 200) {
      paymentState.value = Number(response.data?.paymentStatus ?? response.data?.status ?? 0)
      if (paymentState.value === 1) {
        clearInterval(stateTimer)
        router.replace({ path: '/pay/success', query: { orderId } })
      }
    }
  }, 3000)
}

const handleSimulatePay = () => {
  ElMessageBox.confirm('确定要模拟支付成功吗？', '确认支付', {
    confirmButtonText: '确定', cancelButtonText: '取消', type: 'info'
  }).then(async () => {
    paying.value = true
    try {
      const response = await simulatePaySuccess(orderId)
      if (response?.code !== 200) throw new Error(response?.msg || '支付失败')
      if (countdownTimer) clearInterval(countdownTimer)
      if (stateTimer) clearInterval(stateTimer)
      ElMessage.success('支付成功！')
      router.push({ path: '/pay/success', query: { orderId } })
    } catch (error) {
      ElMessage.error(error?.message || '支付失败')
    } finally {
      paying.value = false
    }
  }).catch(() => {})
}

const handleCancel = () => {
  ElMessageBox.confirm('确定要取消支付吗？订单将被取消', '取消支付', {
    confirmButtonText: '确定取消', cancelButtonText: '继续支付', type: 'warning'
  }).then(async () => {
    if (countdownTimer) clearInterval(countdownTimer)
    if (stateTimer) clearInterval(stateTimer)
    const response = await cancelOrder(orderId)
    if (response?.code !== 200) throw new Error(response?.msg || '订单取消失败')
    ElMessage.info('订单已取消')
    router.push('/main/index')
  }).catch(error => {
    if (error !== 'cancel' && error !== 'close') ElMessage.error(error?.message || '订单取消失败')
  })
}

onMounted(loadPaymentData)

onUnmounted(() => {
  if (countdownTimer) clearInterval(countdownTimer)
  if (stateTimer) clearInterval(stateTimer)
})
</script>

<style scoped>
.payment-container {
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

.payment-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 订单信息 */
.order-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.order-status {
  display: flex;
  align-items: center;
  gap: 16px;
}

.order-status h3 {
  font-size: 18px;
  font-weight: 600;
  color: #67c23a;
  margin: 0 0 4px;
}

.order-status p {
  font-size: 14px;
  color: #909399;
  margin: 0;
}

.order-amount {
  text-align: right;
}

.order-amount .amount {
  font-size: 28px;
  font-weight: 700;
  color: #f56c6c;
}

/* 支付方式 */
.payment-methods {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.payment-method {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
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
  width: 48px;
  height: 48px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 24px;
  font-weight: 700;
}

.method-name {
  flex: 1;
  font-size: 16px;
  color: #303133;
}

.check-icon {
  font-size: 24px;
}

/* 二维码区域 */
.qrcode-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 24px;
  padding: 20px;
}

.qrcode-box {
  width: 280px;
  height: 280px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
}

.qrcode-placeholder {
  text-align: center;
  color: #c0c4cc;
}

.qrcode-placeholder p {
  margin: 12px 0 0;
  font-size: 14px;
}

.qrcode-tip {
  text-align: center;
  font-size: 16px;
  color: #606266;
}

.qrcode-tip .highlight {
  color: #409eff;
  font-weight: 600;
}

.payment-actions {
  display: flex;
  gap: 16px;
  margin-top: 16px;
}

/* 倒计时 */
.countdown {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 16px;
  background: #fdf6ec;
  border-radius: 8px;
  color: #e6a23c;
  font-size: 14px;
}
</style>
