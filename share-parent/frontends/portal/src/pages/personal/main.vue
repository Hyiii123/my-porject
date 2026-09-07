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
          <div class="menu-icon icon-primary">
            <span>课</span>
          </div>
          <div class="menu-text">我的课表</div>
        </div>
        <div class="menu-item" @click="$router.push('/notes/index')">
          <div class="menu-icon icon-success">
            <span>笔</span>
          </div>
          <div class="menu-text">我的笔记</div>
        </div>
        <div class="menu-item" @click="$router.push('/points/index')">
          <div class="menu-icon icon-warning">
            <span>分</span>
          </div>
          <div class="menu-text">我的积分</div>
        </div>
        <div class="menu-item" @click="$router.push('/pay/carts')">
          <div class="menu-icon icon-slate">
            <span>车</span>
          </div>
          <div class="menu-text">购物车</div>
        </div>
      </div>

      <!-- 智能学习画像与技能雷达 -->
      <el-card class="portrait-card" shadow="hover">
        <template #header>
          <div class="portrait-header">
            <div class="header-left">
              <span class="portrait-title">🎯 智能学习画像与技能图谱</span>
              <span class="portrait-sub">基于公开 IT 课程数据集参数与您的学习行为多维建模</span>
            </div>
            <el-button type="primary" size="small" plain @click="openPrefDialog">定制偏好</el-button>
          </div>
        </template>
        
        <div class="portrait-content" v-loading="portraitLoading">
          <div class="portrait-meta-row">
            <div class="meta-item">
              <span class="meta-label">目标岗位</span>
              <span class="meta-val role-badge">{{ portrait.intendedRole || '全栈开发工程师' }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">自适应阶段</span>
              <span class="meta-val diff-badge">{{ portrait.difficultyName || '中级进阶' }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">自律完课率</span>
              <span class="meta-val highlight-val">{{ portrait.completionRate || 0 }}%</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">有效学时</span>
              <span class="meta-val highlight-val">{{ portrait.totalStudyHours || 0 }}h</span>
            </div>
          </div>

          <!-- 画像特征标签 -->
          <div class="portrait-tags" v-if="portrait.tags && portrait.tags.length">
            <span class="tag-title">特征标签：</span>
            <el-tag v-for="(tag, idx) in portrait.tags" :key="idx" class="persona-tag" effect="light" round>
              {{ tag }}
            </el-tag>
          </div>

          <!-- 核心技能掌握度评估与多维雷达图 -->
          <div class="skills-section" v-if="portrait.skillsRadar && portrait.skillsRadar.length">
            <div class="skills-heading">🎯 核心技能多维雷达图与掌握度评估</div>
            <div class="radar-container">
              <!-- 左侧：动态 SVG 技能雷达图 -->
              <div class="radar-chart-wrapper">
                <svg viewBox="0 0 380 320" class="radar-svg">
                  <!-- 同心多边形背景网格 (20%, 40%, 60%, 80%, 100%) -->
                  <polygon
                    v-for="level in [0.2, 0.4, 0.6, 0.8, 1.0]"
                    :key="level"
                    :points="getWebPolygonPoints(level)"
                    class="radar-web-grid"
                  />
                  <!-- 辐射轴线 -->
                  <line
                    v-for="(axis, idx) in radarAxes"
                    :key="idx"
                    :x1="190"
                    :y1="160"
                    :x2="axis.x"
                    :y2="axis.y"
                    class="radar-axis-line"
                  />
                  <!-- 技能数据多边形 -->
                  <polygon
                    :points="radarDataPolygon"
                    class="radar-data-polygon"
                  />
                  <!-- 各技能顶点数据小圆点 -->
                  <circle
                    v-for="(pt, idx) in radarDataPoints"
                    :key="idx"
                    :cx="pt.x"
                    :cy="pt.y"
                    r="4"
                    class="radar-data-dot"
                  />
                  <!-- 技能与分值标签 -->
                  <text
                    v-for="(label, idx) in radarLabels"
                    :key="idx"
                    :x="label.x"
                    :y="label.y"
                    :text-anchor="label.anchor"
                    class="radar-label-text"
                  >
                    {{ label.text }} ({{ label.score }}分)
                  </text>
                </svg>
              </div>

              <!-- 右侧：技能进度明细条 -->
              <div class="skills-grid">
                <div v-for="s in portrait.skillsRadar" :key="s.skill" class="skill-item">
                  <div class="skill-info">
                    <span class="skill-name">{{ s.skill }}</span>
                    <span class="skill-score">{{ s.score }} 分</span>
                  </div>
                  <el-progress :percentage="s.score" :stroke-width="8" :color="getSkillColor(s.score)" :show-text="false" />
                </div>
              </div>
            </div>
          </div>
        </div>
      </el-card>

      <!-- 偏好调整弹窗 -->
      <el-dialog v-model="prefDialogVisible" title="定制您的学习画像与推荐偏好" width="460px">
        <el-form :model="prefForm" label-width="110px">
          <el-form-item label="目标岗位角色">
            <el-select v-model="prefForm.intendedRole" placeholder="请选择目标技术岗位" style="width: 100%">
              <el-option label="Java后端工程师" value="Java后端工程师" />
              <el-option label="前端开发工程师" value="前端开发工程师" />
              <el-option label="Python全栈工程师" value="Python全栈工程师" />
              <el-option label="AI大模型/算法工程师" value="AI算法工程师" />
              <el-option label="Go后端开发工程师" value="Go后端开发工程师" />
              <el-option label="数据库专家/架构师" value="数据库专家/后端架构师" />
              <el-option label="DevOps/运维工程师" value="DevOps/运维工程师" />
            </el-select>
          </el-form-item>
          <el-form-item label="适配难度偏好">
            <el-radio-group v-model="prefForm.preferredDifficulty">
              <el-radio :label="1">初级入门</el-radio>
              <el-radio :label="2">中级进阶</el-radio>
              <el-radio :label="3">高级架构</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="prefDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="savingPref" @click="savePreferences">保存生效</el-button>
        </template>
      </el-dialog>

      <!-- 最近学习 -->
      <el-card class="recent-card" shadow="hover">
        <template #header>
          <span>最近学习</span>
        </template>
        <div v-if="recentLearning.length" class="recent-list">
          <div v-for="item in recentLearning" :key="item.courseId" class="recent-item" @click="$router.push(`/learning/index?courseId=${item.courseId}`)">
            <div class="course-cover">
              <img :src="item.cover || defaultCover" alt="" @error="handleImgError($event, item)" />
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
import { getMylessons, getTodayPoints, getMyCoupon, getUserPortrait, updateUserPortraitPreferences } from '@/api/class.js'
import { getOrderListes } from '@/api/order.js'
import defaultAvatar from '@/assets/images/users/default-avatar.svg'
import defaultCover from '@/assets/images/courses/default-cover.svg'

const handleImgError = (e, item) => {
  if (item) item.cover = defaultCover
  if (e?.target && e.target.src !== defaultCover) {
    e.target.src = defaultCover
  }
}

const loading = ref(false)
const userInfo = ref({})
const learning = ref([])
const stats = ref({ learning: 0, points: 0, orders: 0, coupons: 0 })

// 用户学习画像数据与偏好弹窗
const portrait = ref({})
const portraitLoading = ref(false)
const prefDialogVisible = ref(false)
const savingPref = ref(false)
const prefForm = ref({
  intendedRole: 'Java后端工程师',
  preferredDifficulty: 2
})

const getSkillColor = (score) => {
  if (score >= 80) return '#2563EB'
  if (score >= 60) return '#059669'
  if (score >= 40) return '#D97706'
  return '#94A3B8'
}

const RADAR_CX = 190
const RADAR_CY = 160
const RADAR_R = 95

const radarAxes = computed(() => {
  const list = portrait.value.skillsRadar || []
  const n = list.length
  if (!n) return []
  return list.map((_, i) => {
    const angle = -Math.PI / 2 + (2 * Math.PI * i) / n
    return {
      x: Number((RADAR_CX + RADAR_R * Math.cos(angle)).toFixed(1)),
      y: Number((RADAR_CY + RADAR_R * Math.sin(angle)).toFixed(1))
    }
  })
})

const getWebPolygonPoints = (level) => {
  const list = portrait.value.skillsRadar || []
  const n = list.length
  if (!n) return ''
  return list.map((_, i) => {
    const angle = -Math.PI / 2 + (2 * Math.PI * i) / n
    const x = (RADAR_CX + RADAR_R * level * Math.cos(angle)).toFixed(1)
    const y = (RADAR_CY + RADAR_R * level * Math.sin(angle)).toFixed(1)
    return `${x},${y}`
  }).join(' ')
}

const radarDataPolygon = computed(() => {
  const list = portrait.value.skillsRadar || []
  const n = list.length
  if (!n) return ''
  return list.map((s, i) => {
    const angle = -Math.PI / 2 + (2 * Math.PI * i) / n
    const scoreRatio = Math.max(0.1, Math.min(1.0, (s.score || 0) / 100))
    const x = (RADAR_CX + RADAR_R * scoreRatio * Math.cos(angle)).toFixed(1)
    const y = (RADAR_CY + RADAR_R * scoreRatio * Math.sin(angle)).toFixed(1)
    return `${x},${y}`
  }).join(' ')
})

const radarDataPoints = computed(() => {
  const list = portrait.value.skillsRadar || []
  const n = list.length
  if (!n) return []
  return list.map((s, i) => {
    const angle = -Math.PI / 2 + (2 * Math.PI * i) / n
    const scoreRatio = Math.max(0.1, Math.min(1.0, (s.score || 0) / 100))
    return {
      x: Number((RADAR_CX + RADAR_R * scoreRatio * Math.cos(angle)).toFixed(1)),
      y: Number((RADAR_CY + RADAR_R * scoreRatio * Math.sin(angle)).toFixed(1)),
      score: s.score
    }
  })
})

const radarLabels = computed(() => {
  const list = portrait.value.skillsRadar || []
  const n = list.length
  if (!n) return []
  return list.map((s, i) => {
    const angle = -Math.PI / 2 + (2 * Math.PI * i) / n
    const cosVal = Math.cos(angle)
    const sinVal = Math.sin(angle)
    const x = RADAR_CX + (RADAR_R + 24) * cosVal
    const y = RADAR_CY + (RADAR_R + 14) * sinVal + 4
    let anchor = 'middle'
    if (cosVal > 0.3) anchor = 'start'
    else if (cosVal < -0.3) anchor = 'end'
    return {
      text: s.skill,
      score: s.score,
      x: Number(x.toFixed(1)),
      y: Number(y.toFixed(1)),
      anchor
    }
  })
})

const openPrefDialog = () => {
  prefForm.value = {
    intendedRole: portrait.value.intendedRole || 'Java后端工程师',
    preferredDifficulty: Number(portrait.value.preferredDifficulty || 2)
  }
  prefDialogVisible.value = true
}

const savePreferences = async () => {
  savingPref.value = true
  try {
    const res = await updateUserPortraitPreferences(prefForm.value)
    if (res?.code === 200) {
      ElMessage.success('画像偏好更新成功')
      portrait.value = res.data || {}
      prefDialogVisible.value = false
    } else {
      ElMessage.error(res?.msg || '保存失败')
    }
  } catch (err) {
    ElMessage.error(err?.message || '网络请求错误')
  } finally {
    savingPref.value = false
  }
}

const recentLearning = computed(() => learning.value.slice(0, 3))

const listFrom = (data) => Array.isArray(data) ? data : (data?.list || data?.rows || [])

const normalizeLearning = (item = {}) => ({
  ...item,
  courseId: item.courseId || item.id,
  courseName: item.courseName || item.title || '未命名课程',
  cover: item.cover || item.coverUrl || defaultCover,
  progress: Math.min(100, Math.max(0, Number(item.progress ?? item.progressPercent ?? 0)))
})

const loadData = async () => {
  loading.value = true
  try {
    const [userResponse, learningResponse, pointsResponse, orderResponse, couponResponse, portraitResponse] = await Promise.allSettled([
      getUserInfo(),
      getMylessons({ pageNo: 1, pageSize: 20 }),
      getTodayPoints(),
      getOrderListes({ pageNo: 1, pageSize: 1 }),
      getMyCoupon({ pageNo: 1, pageSize: 1, status: 1 }),
      getUserPortrait()
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
    if (portraitResponse.status === 'fulfilled' && portraitResponse.value?.code === 200) {
      portrait.value = portraitResponse.value.data || {}
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

<style scoped lang="scss">
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
  background: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 8px;
  padding: 20px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 1px 3px 0 rgba(15, 23, 42, 0.04);
}

.menu-item:hover {
  transform: translateY(-2px);
  border-color: #CBD5E1;
  box-shadow: 0 4px 14px -2px rgba(15, 23, 42, 0.08);
}

.menu-icon {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 10px;

  &.icon-primary { background: #EFF6FF; span { color: #2563EB; } }
  &.icon-success { background: #ECFDF5; span { color: #059669; } }
  &.icon-warning { background: #FFFBEB; span { color: #D97706; } }
  &.icon-slate { background: #F1F5F9; span { color: #475569; } }
}

.menu-icon span {
  font-size: 20px;
  font-weight: 700;
}

.menu-text {
  font-size: 14px;
  font-weight: 600;
  color: #0F172A;
}

/* 学习画像卡片 */
.portrait-card {
  border-radius: 12px;
  border: none;
  margin-bottom: 24px;
}

.portrait-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.portrait-title {
  font-size: 16px;
  font-weight: 600;
  color: #0f172a;
  display: block;
}

.portrait-sub {
  font-size: 12px;
  color: #64748b;
  margin-top: 4px;
  display: block;
}

.portrait-meta-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  padding: 16px;
  background: #f8fafc;
  border-radius: 8px;
  margin-bottom: 18px;
}

.meta-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.meta-label {
  font-size: 12px;
  color: #64748b;
}

.meta-val {
  font-size: 14px;
  font-weight: 600;
  color: #0f172a;

  &.role-badge {
    color: #2563eb;
  }

  &.diff-badge {
    color: #059669;
  }

  &.highlight-val {
    font-size: 16px;
    font-weight: 700;
    color: #0f172a;
  }
}

.portrait-tags {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 18px;
}

.tag-title {
  font-size: 13px;
  color: #64748b;
}

.persona-tag {
  background: #eff6ff;
  border-color: #bfdbfe;
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 500;
}

.skills-section {
  border-top: 1px dashed #e2e8f0;
  padding-top: 16px;
}

.skills-heading {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 14px;
}

.radar-container {
  display: flex;
  align-items: center;
  gap: 20px;
  background: #f8fafc;
  padding: 16px;
  border-radius: 12px;
  border: 1px solid #e2e8f0;

  @media (max-width: 900px) {
    flex-direction: column;
  }
}

.radar-chart-wrapper {
  flex: 0 0 380px;
  display: flex;
  justify-content: center;
  align-items: center;
  background: #ffffff;
  border-radius: 10px;
  padding: 8px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 1px 3px rgba(0,0,0,0.02);

  @media (max-width: 900px) {
    flex: 1;
    width: 100%;
  }
}

.radar-svg {
  width: 100%;
  max-width: 380px;
  height: auto;
  overflow: visible;
}

.radar-web-grid {
  fill: none;
  stroke: #cbd5e1;
  stroke-width: 1;
  stroke-dasharray: 2, 2;
}

.radar-axis-line {
  stroke: #e2e8f0;
  stroke-width: 1;
}

.radar-data-polygon {
  fill: rgba(37, 99, 235, 0.22);
  stroke: #2563eb;
  stroke-width: 2.5;
  transition: all 0.4s ease;
}

.radar-data-dot {
  fill: #2563eb;
  stroke: #ffffff;
  stroke-width: 2;
  transition: all 0.3s ease;
}

.radar-label-text {
  font-size: 11px;
  fill: #334155;
  font-weight: 500;
}

.skills-grid {
  flex: 1;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;

  @media (max-width: 600px) {
    grid-template-columns: 1fr;
  }
}

.skill-item {
  background: #f8fafc;
  padding: 10px 14px;
  border-radius: 6px;
  border: 1px solid #f1f5f9;
}

.skill-info {
  display: flex;
  justify-content: space-between;
  margin-bottom: 6px;
}

.skill-name {
  font-size: 13px;
  font-weight: 500;
  color: #1e293b;
}

.skill-score {
  font-size: 12px;
  font-weight: 600;
  color: #2563eb;
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
