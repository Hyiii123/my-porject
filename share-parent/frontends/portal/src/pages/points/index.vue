<template>
  <div class="points-container">
    <div class="container">
      <!-- 积分概览 -->
      <div class="points-overview">
        <el-card class="overview-card" shadow="hover">
          <div class="overview-content">
            <div class="my-points">
              <div class="points-icon">
                <el-icon :size="48" color="#f5a623"><Trophy /></el-icon>
              </div>
              <div class="points-info">
                <div class="points-number">{{ myPoints }}</div>
                <div class="points-label">我的积分</div>
              </div>
            </div>
            <div class="points-actions">
              <el-button type="primary" @click="handleSign">
                <el-icon><Check /></el-icon>
                {{ todaySigned ? '已签到' : '每日签到' }}
              </el-button>
              <div class="sign-info">
                <span>连续签到 {{ continuousDays }} 天</span>
                <span>今日获得 {{ todayPoints }} 积分</span>
              </div>
            </div>
          </div>
        </el-card>
      </div>

      <!-- 积分排行 -->
      <div class="rank-section">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>积分排行榜</span>
              <el-radio-group v-model="rankType" size="small" @change="loadRanking">
                <el-radio-button label="total">总榜</el-radio-button>
                <el-radio-button label="week">周榜</el-radio-button>
                <el-radio-button label="month">月榜</el-radio-button>
              </el-radio-group>
            </div>
          </template>

          <!-- 前三名 -->
          <div class="top-three">
            <div v-for="(user, index) in topThree" :key="user.userId" class="top-item" :class="'rank-' + (index + 1)">
              <div class="rank-badge">
                <el-icon v-if="index === 0" :size="32" color="#ffd700"><Medal /></el-icon>
                <el-icon v-else-if="index === 1" :size="32" color="#c0c0c0"><Medal /></el-icon>
                <el-icon v-else :size="32" color="#cd7f32"><Medal /></el-icon>
              </div>
              <div class="user-avatar">
                <img :src="user.avatar" :alt="user.nickname" />
              </div>
              <div class="user-info">
                <div class="user-name">{{ user.nickname }}</div>
                <div class="user-points">{{ user.totalPoints }} 积分</div>
              </div>
            </div>
          </div>

          <!-- 排行列表 -->
          <div class="rank-list">
            <div v-for="(user, index) in rankList" :key="user.userId" class="rank-item">
              <div class="rank-number" :class="{ 'top-3': index < 3 }">{{ index + 4 }}</div>
              <div class="user-avatar">
                <img :src="user.avatar" :alt="user.nickname" />
              </div>
              <div class="user-info">
                <div class="user-name">{{ user.nickname }}</div>
              </div>
              <div class="user-points">{{ user.totalPoints }}</div>
            </div>
          </div>
        </el-card>
      </div>

      <!-- 积分规则 -->
      <div class="rules-section">
        <el-card shadow="hover">
          <template #header>
            <span>积分规则</span>
          </template>
          <div class="rules-list">
            <div class="rule-item">
              <div class="rule-icon">
                <el-icon :size="24" color="#67c23a"><Check /></el-icon>
              </div>
              <div class="rule-info">
                <div class="rule-title">每日签到</div>
                <div class="rule-desc">每日签到可获得 10 积分，连续签到可获得更多积分</div>
              </div>
              <div class="rule-points">+10</div>
            </div>
            <div class="rule-item">
              <div class="rule-icon">
                <el-icon :size="24" color="#409eff"><Reading /></el-icon>
              </div>
              <div class="rule-info">
                <div class="rule-title">学习课程</div>
                <div class="rule-desc">完成课程学习可获得相应积分奖励</div>
              </div>
              <div class="rule-points">+50</div>
            </div>
            <div class="rule-item">
              <div class="rule-icon">
                <el-icon :size="24" color="#e6a23c"><Star /></el-icon>
              </div>
              <div class="rule-info">
                <div class="rule-title">课程评价</div>
                <div class="rule-desc">对已学习的课程进行评价可获得积分</div>
              </div>
              <div class="rule-points">+20</div>
            </div>
            <div class="rule-item">
              <div class="rule-icon">
                <el-icon :size="24" color="#f56c6c"><Share /></el-icon>
              </div>
              <div class="rule-info">
                <div class="rule-title">分享课程</div>
                <div class="rule-desc">分享课程给好友可获得积分奖励</div>
              </div>
              <div class="rule-points">+5</div>
            </div>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Trophy, Check, Medal, Reading, Star, Share } from '@element-plus/icons-vue'
import { getSeasons, getSignRecords, getTodayPoints, pointsSign } from '@/api/class.js'
import defaultAvatar from '@/assets/images/users/default-avatar.svg'

// 排行类型
const rankType = ref('total')

// 我的积分
const myPoints = ref(0)
const todaySigned = ref(false)
const continuousDays = ref(0)
const todayPoints = ref(0)

// 排行数据
const rankData = ref([])

// 前三名
const topThree = computed(() => rankData.value.slice(0, 3))

// 排行列表（第4名之后）
const rankList = computed(() => rankData.value.slice(3))

const normalizeRank = (item = {}) => ({
  ...item,
  userId: item.userId,
  nickname: item.userName || item.nickname || `学习者${item.userId || ''}`,
  avatar: item.avatar || defaultAvatar,
  totalPoints: Number(item.points ?? item.totalPoints ?? 0)
})

const loadRanking = async () => {
  try {
    const response = await getSeasons({ season: rankType.value === 'total' ? 0 : rankType.value, pageNo: 1, pageSize: 50 })
    if (response?.code !== 200) throw new Error(response?.msg || '积分排行加载失败')
    const rows = Array.isArray(response.data) ? response.data : (response.data?.list || response.data?.boardList || [])
    rankData.value = rows.map(normalizeRank)
  } catch (error) {
    rankData.value = []
    ElMessage.error(error?.message || '积分排行加载失败')
  }
}

const loadPoints = async () => {
  try {
    const [todayResponse, signResponse] = await Promise.all([getTodayPoints(), getSignRecords()])
    if (todayResponse?.code === 200) {
      myPoints.value = Number(todayResponse.data?.totalPoints ?? 0)
      todayPoints.value = Number(todayResponse.data?.todayPoints ?? 0)
    }
    if (signResponse?.code === 200) {
      todaySigned.value = Boolean(signResponse.data?.todaySigned)
      continuousDays.value = Number(signResponse.data?.continuousDays ?? 0)
    }
  } catch (error) {
    ElMessage.error(error?.message || '积分信息加载失败')
  }
}

// 签到
const handleSign = async () => {
  if (todaySigned.value) {
    ElMessage.info('今日已签到')
    return
  }
  try {
    const response = await pointsSign()
    if (response?.code !== 200) throw new Error(response?.msg || '签到失败')
    await loadPoints()
    await loadRanking()
    ElMessage.success(`签到成功，获得 ${response.data?.todayPoints ?? 10} 积分！`)
  } catch (error) {
    ElMessage.error(error?.message || '签到失败，请稍后重试')
  }
}

onMounted(async () => {
  await Promise.all([loadPoints(), loadRanking()])
})
</script>

<style scoped>
.points-container {
  background: #f5f7fa;
  min-height: 100vh;
  padding: 30px 0;
}

.container {
  max-width: 800px;
  margin: 0 auto;
  padding: 0 20px;
}

/* 积分概览 */
.points-overview {
  margin-bottom: 24px;
}

.overview-card {
  border-radius: 12px;
  border: none;
}

.overview-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.my-points {
  display: flex;
  align-items: center;
  gap: 16px;
}

.points-icon {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: linear-gradient(135deg, #ffd700 0%, #ffaa00 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.points-number {
  font-size: 36px;
  font-weight: 700;
  color: #303133;
}

.points-label {
  font-size: 14px;
  color: #909399;
}

.points-actions {
  text-align: center;
}

.sign-info {
  margin-top: 8px;
  font-size: 13px;
  color: #909399;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

/* 排行榜 */
.rank-section {
  margin-bottom: 24px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* 前三名 */
.top-three {
  display: flex;
  justify-content: center;
  gap: 40px;
  padding: 30px 0;
  margin-bottom: 20px;
}

.top-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.rank-badge {
  margin-bottom: -8px;
}

.user-avatar {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  overflow: hidden;
  border: 3px solid #ebeef5;
}

.rank-1 .user-avatar {
  border-color: #ffd700;
  width: 80px;
  height: 80px;
}

.rank-2 .user-avatar {
  border-color: #c0c0c0;
}

.rank-3 .user-avatar {
  border-color: #cd7f32;
}

.user-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.user-info {
  text-align: center;
}

.user-name {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.user-points {
  font-size: 16px;
  font-weight: 700;
  color: #f5a623;
}

/* 排行列表 */
.rank-list {
  border-top: 1px solid #f0f0f0;
  padding-top: 16px;
}

.rank-item {
  display: flex;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.rank-item:last-child {
  border-bottom: none;
}

.rank-number {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: #f5f7fa;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  color: #909399;
  margin-right: 16px;
}

.rank-number.top-3 {
  background: linear-gradient(135deg, #ffd700 0%, #ffaa00 100%);
  color: #fff;
}

.rank-item .user-avatar {
  width: 40px;
  height: 40px;
  margin-right: 12px;
}

.rank-item .user-info {
  flex: 1;
  text-align: left;
}

.rank-item .user-points {
  font-size: 16px;
  font-weight: 600;
  color: #f5a623;
}

/* 积分规则 */
.rules-section {
  margin-bottom: 40px;
}

.rules-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.rule-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
}

.rule-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.rule-info {
  flex: 1;
}

.rule-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.rule-desc {
  font-size: 13px;
  color: #909399;
}

.rule-points {
  font-size: 20px;
  font-weight: 700;
  color: #67c23a;
}
</style>
