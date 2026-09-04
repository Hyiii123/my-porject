<template>
  <div class="cart-container">
    <div class="container">
      <h2 class="page-title">我的购物车 <span class="cart-count">共 {{ cartList.length }} 门课程</span></h2>

      <!-- 购物车列表 -->
      <div v-if="cartList.length > 0" class="cart-content">
        <!-- 表头 -->
        <div class="cart-header">
          <div class="header-checkbox">
            <el-checkbox v-model="isAllChecked" @change="handleCheckAll">全选</el-checkbox>
          </div>
          <div class="header-course">课程信息</div>
          <div class="header-price">单价</div>
          <div class="header-action">操作</div>
        </div>

        <!-- 课程列表 -->
        <div class="cart-list">
          <div v-for="item in cartList" :key="item.id" class="cart-item">
            <div class="item-checkbox">
              <el-checkbox v-model="item.checked" @change="handleCheckItem" />
            </div>
            <div class="item-course" @click="$router.push(`/details/index?id=${item.courseId}`)">
              <div class="course-cover">
                <img :src="item.cover" :alt="item.courseName" />
              </div>
              <div class="course-info">
                <h4 class="course-title">{{ item.courseName }}</h4>
                <p class="course-teacher">{{ item.teacherName }}</p>
              </div>
            </div>
            <div class="item-price">
              <span class="current-price">¥{{ (item.price / 100).toFixed(2) }}</span>
              <span v-if="item.originalPrice > item.price" class="original-price">¥{{ (item.originalPrice / 100).toFixed(2) }}</span>
            </div>
            <div class="item-action">
              <el-button type="danger" text @click="handleRemove(item)">
                <el-icon><Delete /></el-icon>
                删除
              </el-button>
            </div>
          </div>
        </div>

        <!-- 结算栏 -->
        <div class="cart-footer">
          <div class="footer-left">
            <el-checkbox v-model="isAllChecked" @change="handleCheckAll">全选</el-checkbox>
            <el-button type="danger" text @click="handleRemoveSelected" :disabled="checkedCount === 0">
              删除选中
            </el-button>
          </div>
          <div class="footer-right">
            <div class="total-info">
              <div class="total-count">
                已选 <span class="count">{{ checkedCount }}</span> 门课程
              </div>
              <div class="total-amount">
                <span class="label">合计：</span>
                <span class="price">¥{{ (totalPrice / 100).toFixed(2) }}</span>
              </div>
            </div>
            <el-button
              type="primary"
              size="large"
              class="checkout-btn"
              :disabled="checkedCount === 0"
              @click="handleCheckout"
            >
              去结算
            </el-button>
          </div>
        </div>
      </div>

      <!-- 空购物车 -->
      <div v-else class="empty-cart">
        <el-empty description="购物车是空的">
          <el-button type="primary" @click="$router.push('/search/index')">去逛逛</el-button>
        </el-empty>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete } from '@element-plus/icons-vue'
import { getCarts, delCarts } from '@/api/order.js'

const router = useRouter()

// 购物车数据
const cartList = ref([])

const normalizeCart = (item = {}) => ({
  ...item,
  id: item.id,
  courseId: item.courseId,
  courseName: item.courseName || item.title || `课程 ${item.courseId || ''}`,
  teacherName: item.teacherName || '讲师团队',
  cover: item.cover || item.coverUrl || '',
  price: Number(item.price || 0),
  originalPrice: Number(item.originalPrice ?? item.price ?? 0),
  checked: item.checked !== false
})

const loadCart = async () => {
  try {
    const response = await getCarts()
    if (response?.code !== 200) throw new Error(response?.msg || '购物车加载失败')
    const rows = Array.isArray(response.data) ? response.data : (response.data?.list || [])
    cartList.value = rows.map(normalizeCart)
  } catch (error) {
    cartList.value = []
    ElMessage.error(error?.message || '购物车加载失败，请先登录')
  }
}

// 全选状态
const isAllChecked = computed({
  get: () => cartList.value.length > 0 && cartList.value.every(item => item.checked),
  set: () => {}
})

// 选中数量
const checkedCount = computed(() => cartList.value.filter(item => item.checked).length)

// 总价
const totalPrice = computed(() => {
  return cartList.value
    .filter(item => item.checked)
    .reduce((sum, item) => sum + item.price, 0)
})

// 全选/取消全选
const handleCheckAll = (val) => {
  cartList.value.forEach(item => {
    item.checked = val
  })
}

// 单个选择
const handleCheckItem = () => {
  // 触发计算属性更新
}

// 删除单个
const handleRemove = (item) => {
  ElMessageBox.confirm('确定要从购物车移除该课程吗？', '提示', {
    type: 'warning'
  }).then(async () => {
    const response = await delCarts([item.id])
    if (response?.code !== 200) throw new Error(response?.msg || '删除失败')
    const index = cartList.value.findIndex(c => c.id === item.id)
    if (index !== -1) cartList.value.splice(index, 1)
    ElMessage.success('已从购物车移除')
  }).catch(error => {
    if (error !== 'cancel' && error !== 'close') ElMessage.error(error?.message || '购物车删除失败')
  })
}

// 删除选中
const handleRemoveSelected = () => {
  const selected = cartList.value.filter(item => item.checked)
  if (selected.length === 0) {
    ElMessage.warning('请先选择要删除的课程')
    return
  }
  ElMessageBox.confirm(`确定要移除选中的 ${selected.length} 门课程吗？`, '提示', {
    type: 'warning'
  }).then(async () => {
    const response = await delCarts(selected.map(item => item.id))
    if (response?.code !== 200) throw new Error(response?.msg || '删除失败')
    cartList.value = cartList.value.filter(item => !item.checked)
    ElMessage.success('已移除选中课程')
  }).catch(error => {
    if (error !== 'cancel' && error !== 'close') ElMessage.error(error?.message || '购物车删除失败')
  })
}

// 去结算
const handleCheckout = () => {
  const selected = cartList.value.filter(item => item.checked)
  if (selected.length === 0) {
    ElMessage.warning('请先选择要结算的课程')
    return
  }
  sessionStorage.setItem('checkoutItems', JSON.stringify(selected))
  router.push({ path: '/pay/settlement', query: { ids: selected.map(item => item.id).join(',') } })
}

onMounted(loadCart)
</script>

<style scoped>
.cart-container {
  background: #f5f7fa;
  min-height: 100vh;
  padding: 30px 0;
}

.container {
  max-width: 1000px;
  margin: 0 auto;
  padding: 0 20px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 24px;
}

.cart-count {
  font-size: 14px;
  color: #909399;
  font-weight: normal;
}

/* 购物车内容 */
.cart-content {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

/* 表头 */
.cart-header {
  display: grid;
  grid-template-columns: 60px 1fr 150px 100px;
  align-items: center;
  padding: 16px 24px;
  background: #f8f9fa;
  border-bottom: 1px solid #ebeef5;
  font-size: 14px;
  font-weight: 600;
  color: #606266;
}

/* 购物车列表 */
.cart-list {
  padding: 0;
}

.cart-item {
  display: grid;
  grid-template-columns: 60px 1fr 150px 100px;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #f0f0f0;
  transition: background 0.2s;
}

.cart-item:last-child {
  border-bottom: none;
}

.cart-item:hover {
  background: #f8f9fa;
}

.item-checkbox {
  display: flex;
  align-items: center;
  justify-content: center;
}

.item-course {
  display: flex;
  align-items: center;
  gap: 16px;
  cursor: pointer;
}

.course-cover {
  width: 120px;
  height: 68px;
  border-radius: 8px;
  overflow: hidden;
  flex-shrink: 0;
}

.course-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.course-info {
  flex: 1;
}

.course-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 4px;
}

.course-teacher {
  font-size: 13px;
  color: #909399;
  margin: 0;
}

.item-price {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.current-price {
  font-size: 16px;
  font-weight: 700;
  color: #f56c6c;
}

.original-price {
  font-size: 12px;
  color: #c0c4cc;
  text-decoration: line-through;
}

.item-action {
  display: flex;
  justify-content: center;
}

.item-action .el-button {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 结算栏 */
.cart-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  background: #f8f9fa;
  border-top: 1px solid #ebeef5;
  position: sticky;
  bottom: 0;
}

.footer-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.footer-right {
  display: flex;
  align-items: center;
  gap: 24px;
}

.total-info {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
}

.total-count {
  font-size: 14px;
  color: #606266;
}

.total-count .count {
  color: #409eff;
  font-weight: 600;
}

.total-amount {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.total-amount .label {
  font-size: 14px;
  color: #606266;
}

.total-amount .price {
  font-size: 24px;
  font-weight: 700;
  color: #f56c6c;
}

.checkout-btn {
  width: 140px;
  height: 48px;
  font-size: 16px;
  border-radius: 8px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-color: transparent;
}

.checkout-btn:hover {
  opacity: 0.9;
}

/* 空购物车 */
.empty-cart {
  background: #fff;
  border-radius: 12px;
  padding: 80px 0;
  text-align: center;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

/* 响应式 */
@media (max-width: 768px) {
  .cart-header {
    grid-template-columns: 40px 1fr 100px 80px;
    padding: 12px 16px;
  }

  .cart-item {
    grid-template-columns: 40px 1fr 100px 80px;
    padding: 16px;
  }

  .course-cover {
    width: 80px;
    height: 45px;
  }

  .course-title {
    font-size: 14px;
  }

  .cart-footer {
    flex-direction: column;
    gap: 16px;
    padding: 16px;
  }

  .footer-right {
    width: 100%;
    justify-content: space-between;
  }

  .checkout-btn {
    width: 120px;
  }
}
</style>
