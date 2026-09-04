<template>
  <div class="personal-container" v-loading="loading">
    <div class="container">
      <h2 class="page-title">个人中心</h2>

      <!-- 用户信息卡片 -->
      <el-card class="user-card" shadow="hover">
        <div class="user-info">
          <div class="user-avatar">
            <img :src="userInfo.avatar || defaultAvatar" alt="" />
          </div>
          <div class="user-detail">
            <h2>{{ userInfo.nickname }}</h2>
            <p>ID: {{ userInfo.id }} | 手机: {{ userInfo.phone }}</p>
          </div>
        </div>
      </el-card>

      <!-- 统计数据 -->
      <div class="stats-grid">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ stats.learning }}</div>
          <div class="stat-label">在学课程</div>
        </el-card>
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ stats.points }}</div>
          <div class="stat-label">积分</div>
        </el-card>
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ stats.orders }}</div>
          <div class="stat-label">订单</div>
        </el-card>
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ stats.coupons }}</div>
          <div class="stat-label">优惠券</div>
        </el-card>
      </div>

      <!-- 功能菜单 -->
      <div class="menu-grid">
        <div class="menu-item" @click="$router.push('/my-class/index')">
          <div class="menu-icon" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%)">
            <span>课</span>
          </div>
          <div class="menu-text">我的课表</div>
        </div>
        <div class="menu-item" @click="$router.push('/notes/index')">
          <div class="menu-icon" style="background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)">
            <span>笔</span>
          </div>
          <div class="menu-text">我的笔记</div>
        </div>
        <div class="menu-item" @click="$router.push('/points/index')">
          <div class="menu-icon" style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%)">
            <span>分</span>
          </div>
          <div class="menu-text">我的积分</div>
        </div>
        <div class="menu-item" @click="$router.push('/pay/carts')">
          <div class="menu-icon" style="background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)">
            <span>车</span>
          </div>
          <div class="menu-text">购物车</div>
        </div>
      </div>

      <!-- 最近学习 -->
      <el-card class="recent-card" shadow="hover">
        <template #header>
          <span>最近学习</span>
        </template>
        <div v-if="recentLearning.length" class="recent-list">
          <div v-for="item in recentLearning" :key="item.courseId" class="recent-item" @click="$router.push(`/learning/index?courseId=${item.courseId}`)">
            <div class="course-cover">
              <img :src="item.cover" alt="" />
            </div>
            <div class="course-info">
              <h4>{{ item.courseName }}</h4>
              <div class="progress-bar">
                <div class="progress-fill" :style="{ width: `${item.progress}%` }"></div>
              </div>
              <span class="progress-text">学习进度 {{ item.progress }}%</span>
            </div>
          </div>
        </div>
        <div v-else class="recent-empty">
          <el-empty description="暂无最近学习记录" />
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getUserInfo } from '@/api/user.js'
import { getMylessons, getTodayPoints, getMyCoupon } from '@/api/class.js'
import { getOrderListes } from '@/api/order.js'
import defaultAvatar from '@/assets/images/users/default-avatar.svg'

const loading = ref(false)
const userInfo = ref({})
const learning = ref([])
const stats = ref({ learning: 0, points: 0, orders: 0, coupons: 0 })

const recentLearning = computed(() => learning.value.slice(0, 3))

const listFrom = (data) => Array.isArray(data) ? data : (data?.list || data?.rows || [])

const normalizeLearning = (item = {}) => ({
  ...item,
  courseId: item.courseId || item.id,
  courseName: item.courseName || item.title || '未命名课程',
  cover: item.cover || item.coverUrl || defaultAvatar,
  progress: Math.min(100, Math.max(0, Number(item.progress ?? item.progressPercent ?? 0)))
})

const loadData = async () => {
  loading.value = true
  try {
    const [userResponse, learningResponse, pointsResponse, orderResponse, couponResponse] = await Promise.allSettled([
      getUserInfo(),
      getMylessons({ pageNo: 1, pageSize: 20 }),
      getTodayPoints(),
      getOrderListes({ pageNo: 1, pageSize: 1 }),
      getMyCoupon({ pageNo: 1, pageSize: 1, status: 1 })
    ])
    if (userResponse.status === 'fulfilled' && userResponse.value?.code === 200) {
      userInfo.value = userResponse.value.data || {}
    }
    if (learningResponse.status === 'fulfilled' && learningResponse.value?.code === 200) {
      learning.value = listFrom(learningResponse.value.data).map(normalizeLearning)
    }
    if (pointsResponse.status === 'fulfilled' && pointsResponse.value?.code === 200) {
      stats.value.points = Number(pointsResponse.value.data?.totalPoints ?? 0)
    }
    if (orderResponse.status === 'fulfilled' && orderResponse.value?.code === 200) {
      stats.value.orders = Number(orderResponse.value.data?.total ?? listFrom(orderResponse.value.data).length)
    }
    if (couponResponse.status === 'fulfilled' && couponResponse.value?.code === 200) {
      stats.value.coupons = Number(couponResponse.value.data?.total ?? listFrom(couponResponse.value.data).length)
    }
    stats.value.learning = learning.value.length
  } catch (error) {
    ElMessage.error(error?.message || '个人中心数据加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.personal-container {
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

/* 用户信息卡片 */
.user-card {
  margin-bottom: 24px;
  border-radius: 12px;
  border: none;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 20px;
}

.user-avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
}

.user-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.user-detail h2 {
  font-size: 22px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 8px;
}

.user-detail p {
  font-size: 14px;
  color: #909399;
  margin: 0;
}

/* 统计数据 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  border-radius: 12px;
  border: none;
  text-align: center;
  padding: 20px;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: #303133;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}

/* 功能菜单 */
.menu-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.menu-item {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.menu-item:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.menu-icon {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 12px;
}

.menu-icon span {
  font-size: 24px;
  font-weight: 700;
  color: #fff;
}

.menu-text {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

/* 最近学习 */
.recent-card {
  border-radius: 12px;
  border: none;
  margin-bottom: 40px;
}

.recent-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.recent-empty {
  padding: 20px 0;
}

.recent-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
}

.recent-item:hover {
  background: #ecf5ff;
}

.course-cover {
  width: 80px;
  height: 45px;
  border-radius: 6px;
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

.course-info h4 {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 8px;
}

.progress-bar {
  height: 6px;
  background: #e4e7ed;
  border-radius: 3px;
  overflow: hidden;
  margin-bottom: 4px;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #409eff 0%, #67c23a 100%);
  border-radius: 3px;
}

.progress-text {
  font-size: 12px;
  color: #909399;
}

/* 响应式 */
@media (max-width: 768px) {
  .stats-grid,
  .menu-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
