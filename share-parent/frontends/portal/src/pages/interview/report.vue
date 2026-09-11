<template>
  <div class="interview-report-container" v-loading="loading">
    <!-- 顶部导航栏 -->
    <div class="report-top-bar">
      <el-button link class="back-link" @click="handleBack">
        &larr; 返回模拟面试大厅
      </el-button>
      <div class="report-actions">
        <el-button type="primary" plain size="small" @click="handlePrint">
          🖨️ 导出诊断报告
        </el-button>
        <el-button type="primary" size="small" @click="handleRestart">
          🚀 再来一场挑战
        </el-button>
      </div>
    </div>

    <div v-if="reportData" class="report-content">
      <!-- 终局裁决 Hero 横幅 -->
      <div class="decision-hero" :class="getOfferClass(reportData.offerDecision)">
        <div class="hero-left">
          <div class="offer-tag">大厂面试官委员会终局裁定</div>
          <h1 class="decision-title">{{ reportData.offerDecision }}</h1>
          <div class="level-match">
            <span class="level-icon">🏅</span>
            <span class="level-text">{{ reportData.levelMatch }}</span>
          </div>
        </div>
        <div class="hero-right">
          <div class="score-circle">
            <div class="num">{{ sessionData.score || 80 }}</div>
            <div class="unit">综合得分</div>
          </div>
          <div class="session-meta">
            <div>目标岗位：{{ sessionData.targetJob }}</div>
            <div>目标企业：{{ sessionData.companyTarget }}</div>
            <div>面试总耗时：{{ formatDuration(sessionData.durationSeconds) }}</div>
          </div>
        </div>
      </div>

      <!-- 中间主体：六维雷达图 + 委员会评语 -->
      <div class="report-grid">
        <!-- 左侧：六维能力雷达图 -->
        <el-card class="grid-card radar-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span class="header-title">📊 全维胜任力雷达评估</span>
              <el-tag size="small" type="primary">六维模型</el-tag>
            </div>
          </template>

          <div class="radar-wrapper">
            <svg viewBox="0 0 400 360" class="radar-svg">
              <!-- 背景蛛网环 (20%, 40%, 60%, 80%, 100%) -->
              <polygon
                v-for="level in [0.2, 0.4, 0.6, 0.8, 1.0]"
                :key="level"
                :points="getWebPoints(level)"
                class="radar-grid-polygon"
              />
              <!-- 轴线 -->
              <line
                v-for="(axis, index) in axes"
                :key="index"
                x1="200"
                y1="175"
                :x2="getVertex(index, 1.0).x"
                :y2="getVertex(index, 1.0).y"
                class="radar-axis-line"
              />
              <!-- 学员能力数据多边形 -->
              <polygon :points="radarPoints" class="radar-data-polygon" />
              <!-- 数据端点小圆圈 -->
              <circle
                v-for="(pt, idx) in radarDataCoords"
                :key="'pt-' + idx"
                :cx="pt.x"
                :cy="pt.y"
                r="4"
                class="radar-point-circle"
              />
              <!-- 维度文字标签 -->
              <text
                v-for="(axis, index) in axes"
                :key="'label-' + index"
                :x="getLabelCoord(index).x"
                :y="getLabelCoord(index).y"
                class="radar-label"
                text-anchor="middle"
              >
                {{ axis.name }} ({{ radarScores[axis.key] || 75 }})
              </text>
            </svg>
          </div>
        </el-card>

        <!-- 右侧：终局综述与优势短板 -->
        <el-card class="grid-card summary-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span class="header-title">📝 委员会全景诊断综述</span>
              <el-tag size="small" type="success">专家联合裁定</el-tag>
            </div>
          </template>

          <div class="summary-body">
            <div class="overall-text">
              {{ reportData.overallSummary }}
            </div>

            <div class="diagnostic-box strengths">
              <div class="diag-title">
                <span class="icon">✨</span>
                <span>核心竞争优势与闪光点</span>
              </div>
              <div class="diag-content">
                {{ reportData.coreStrengths }}
              </div>
            </div>

            <div class="diagnostic-box weaknesses">
              <div class="diag-title">
                <span class="icon">⚠️</span>
                <span>致命短板与技术失分点</span>
              </div>
              <div class="diag-content">
                {{ reportData.criticalWeaknesses }}
              </div>
            </div>
          </div>
        </el-card>
      </div>

      <!-- 答题话术重塑模块 (STAR 标杆对比) -->
      <el-card class="section-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <span class="header-title">💡 答题话术与表达逻辑重塑 (大厂 STAR 原则)</span>
            <el-tag size="small" type="warning">高阶话术演练</el-tag>
          </div>
        </template>
        <div class="speech-body">
          <pre class="speech-content">{{ reportData.speechRefactoring }}</pre>
        </div>
      </el-card>

      <!-- 定向推荐补强课程 -->
      <el-card class="section-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <span class="header-title">🎯 平台定向补强课程与真题特训</span>
            <span class="header-sub">针对本次暴露出的底层漏洞定制推荐</span>
          </div>
        </template>
        <div class="course-list">
          <div
            v-for="(course, idx) in parsedCourses"
            :key="idx"
            class="course-card"
            @click="handleJumpCourse"
          >
            <div class="course-badge">推荐 {{ idx + 1 }}</div>
            <div class="course-name">{{ course }}</div>
            <div class="course-footer">
              <span class="tag">大厂实战专项</span>
              <el-button link type="primary" size="small">前往学习 &rarr;</el-button>
            </div>
          </div>
        </div>
      </el-card>

      <!-- 历史问答实录全景复盘 (折叠面板) -->
      <el-card class="section-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <span class="header-title">📜 本场面试问答全景逐轮复盘</span>
            <span class="header-sub">共 {{ sessionData.turns?.length || 0 }} 轮考查实录</span>
          </div>
        </template>
        <el-collapse v-model="activeTurns" class="transcript-collapse">
          <el-collapse-item
            v-for="turn in sessionData.turns"
            :key="turn.id"
            :name="turn.turnNum"
          >
            <template #title>
              <div class="turn-title-row">
                <el-tag size="small" type="primary">第 {{ turn.turnNum }} 轮</el-tag>
                <span class="turn-dimension">{{ turn.dimension }}</span>
                <span class="turn-score">得分：<b>{{ turn.turnScore || 0 }} 分</b></span>
              </div>
            </template>
            <div class="turn-detail-box">
              <div class="row">
                <span class="label">问：</span>
                <span class="text q">{{ turn.question }}</span>
              </div>
              <div class="row">
                <span class="label">答：</span>
                <span class="text a">{{ turn.userAnswer || '（未作答）' }}</span>
              </div>
              <div v-if="turn.aiFeedback" class="row feedback-row">
                <span class="label">评：</span>
                <span class="text f">{{ turn.aiFeedback }}</span>
              </div>
            </div>
          </el-collapse-item>
        </el-collapse>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getInterviewDetail } from '@/api/interview.js'

const route = useRoute()
const router = useRouter()
const sessionId = route.params.id

const loading = ref(true)
const sessionData = ref({})
const reportData = ref(null)
const activeTurns = ref([1])

// 六维雷达参数
const axes = [
  { key: 'core', name: 'Java核心并发' },
  { key: 'architecture', name: '系统设计架构' },
  { key: 'storage', name: '存储与数据库' },
  { key: 'distributed', name: '分布式高并发' },
  { key: 'coding', name: '算法代码工程' },
  { key: 'communication', name: '沟通与表达' }
]

const radarScores = computed(() => {
  if (!reportData.value || !reportData.value.radarData) {
    return { core: 85, architecture: 78, storage: 82, distributed: 75, coding: 88, communication: 80 }
  }
  try {
    if (typeof reportData.value.radarData === 'string') {
      return JSON.parse(reportData.value.radarData)
    }
    return reportData.value.radarData
  } catch (e) {
    return { core: 80, architecture: 75, storage: 80, distributed: 70, coding: 85, communication: 80 }
  }
})

// 计算 SVG 顶点
const centerX = 200
const centerY = 175
const radius = 110

const getVertex = (index, scale) => {
  const angle = (Math.PI * 2 / axes.length) * index - Math.PI / 2
  const r = radius * scale
  return {
    x: centerX + r * Math.cos(angle),
    y: centerY + r * Math.sin(angle)
  }
}

const getWebPoints = (level) => {
  return axes.map((_, i) => {
    const pt = getVertex(i, level)
    return `${pt.x},${pt.y}`
  }).join(' ')
}

const radarDataCoords = computed(() => {
  return axes.map((axis, i) => {
    const val = radarScores.value[axis.key] || 75
    const scale = Math.max(0.1, Math.min(val / 100, 1.0))
    return getVertex(i, scale)
  })
})

const radarPoints = computed(() => {
  return radarDataCoords.value.map(pt => `${pt.x},${pt.y}`).join(' ')
})

const getLabelCoord = (index) => {
  const angle = (Math.PI * 2 / axes.length) * index - Math.PI / 2
  const r = radius + 24
  return {
    x: centerX + r * Math.cos(angle),
    y: centerY + r * Math.sin(angle) + 4
  }
}

const parsedCourses = computed(() => {
  if (!reportData.value || !reportData.value.recommendedCourses) {
    return ['《亿级流量架构实战》', '《深入理解 Java 虚拟机》', '《MySQL 实战 45 讲》']
  }
  try {
    if (typeof reportData.value.recommendedCourses === 'string') {
      return JSON.parse(reportData.value.recommendedCourses)
    }
    return reportData.value.recommendedCourses
  } catch (e) {
    return ['《高并发分布式系统实战》', '《JVM 深度剖析与调优》']
  }
})

const getOfferClass = (decision) => {
  if (!decision) return 'offer-hire'
  const lower = decision.toLowerCase()
  if (lower.includes('strong')) return 'offer-strong'
  if (lower.includes('hire')) return 'offer-hire'
  if (lower.includes('weak')) return 'offer-weak'
  return 'offer-reject'
}

const formatDuration = (seconds) => {
  if (!seconds) return '0 分钟'
  const mins = Math.ceil(seconds / 60)
  return `${mins} 分钟`
}

const loadData = async () => {
  try {
    loading.value = true
    const res = await getInterviewDetail(sessionId)
    if (res && res.data) {
      sessionData.value = res.data
      reportData.value = res.data.report
    }
  } catch (err) {
    ElMessage.error('加载诊断报告失败：' + (err.message || '网络错误'))
  } finally {
    loading.value = false
  }
}

const handlePrint = () => {
  window.print()
}

const handleRestart = () => {
  router.push({ name: 'interviewIndex' })
}

const handleBack = () => {
  router.push({ name: 'interviewIndex' })
}

const handleJumpCourse = () => {
  router.push({ path: '/search/index' })
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.interview-report-container {
  max-width: 1200px;
  margin: 20px auto 60px;
  padding: 0 16px;
}

.report-top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.back-link {
  font-size: 14px;
  color: #475569;
}

.report-actions {
  display: flex;
  gap: 12px;
}

/* 终局 Hero 横幅 */
.decision-hero {
  border-radius: 16px;
  padding: 36px 44px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #fff;
  margin-bottom: 24px;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.15);
}

.offer-strong {
  background: linear-gradient(135deg, #064e3b 0%, #047857 50%, #059669 100%);
}

.offer-hire {
  background: linear-gradient(135deg, #1e3a8a 0%, #1d4ed8 50%, #2563eb 100%);
}

.offer-weak {
  background: linear-gradient(135deg, #78350f 0%, #b45309 50%, #d97706 100%);
}

.offer-reject {
  background: linear-gradient(135deg, #334155 0%, #475569 100%);
}

.offer-tag {
  font-size: 13px;
  letter-spacing: 1px;
  opacity: 0.85;
  margin-bottom: 8px;
  text-transform: uppercase;
}

.decision-title {
  font-size: 44px;
  font-weight: 800;
  margin: 0 0 12px 0;
  letter-spacing: 1px;
}

.level-match {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 600;
  color: #fef08a;
}

.hero-right {
  display: flex;
  align-items: center;
  gap: 36px;
}

.score-circle {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.15);
  border: 2px solid rgba(255, 255, 255, 0.3);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  backdrop-filter: blur(8px);
}

.score-circle .num {
  font-size: 36px;
  font-weight: 800;
  line-height: 1;
}

.score-circle .unit {
  font-size: 11px;
  opacity: 0.8;
  margin-top: 4px;
}

.session-meta {
  font-size: 13px;
  line-height: 1.8;
  opacity: 0.9;
}

/* 主体网格 */
.report-grid {
  display: grid;
  grid-template-columns: 5fr 7fr;
  gap: 24px;
  margin-bottom: 24px;
}

.grid-card {
  border-radius: 12px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-title {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
}

.header-sub {
  font-size: 12px;
  color: #94a3b8;
}

/* 雷达图 */
.radar-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 10px 0;
}

.radar-svg {
  width: 100%;
  max-width: 380px;
  height: auto;
}

.radar-grid-polygon {
  fill: none;
  stroke: #e2e8f0;
  stroke-width: 1;
}

.radar-axis-line {
  stroke: #cbd5e1;
  stroke-width: 1;
  stroke-dasharray: 2 2;
}

.radar-data-polygon {
  fill: rgba(37, 99, 235, 0.25);
  stroke: #2563eb;
  stroke-width: 2;
}

.radar-point-circle {
  fill: #1d4ed8;
  stroke: #fff;
  stroke-width: 1.5;
}

.radar-label {
  font-size: 11px;
  fill: #475569;
  font-weight: 500;
}

/* 综述卡片 */
.summary-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.overall-text {
  font-size: 14px;
  color: #334155;
  line-height: 1.7;
}

.diagnostic-box {
  border-radius: 8px;
  padding: 14px 16px;
}

.diagnostic-box.strengths {
  background: #f0fdf4;
  border-left: 4px solid #16a34a;
}

.diagnostic-box.weaknesses {
  background: #fef2f2;
  border-left: 4px solid #dc2626;
}

.diag-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  font-size: 13px;
  margin-bottom: 6px;
}

.strengths .diag-title { color: #15803d; }
.weaknesses .diag-title { color: #b91c1c; }

.diag-content {
  font-size: 13px;
  color: #374151;
  line-height: 1.6;
  white-space: pre-wrap;
}

/* 话术与课程卡片 */
.section-card {
  border-radius: 12px;
  margin-bottom: 24px;
}

.speech-content {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 16px;
  font-family: inherit;
  font-size: 13px;
  line-height: 1.7;
  color: #1e293b;
  white-space: pre-wrap;
  margin: 0;
}

.course-list {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.course-card {
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 16px;
  background: #f8fafc;
  cursor: pointer;
  transition: all 0.2s;
}

.course-card:hover {
  border-color: #2563eb;
  background: #eff6ff;
  transform: translateY(-2px);
}

.course-badge {
  font-size: 11px;
  color: #2563eb;
  font-weight: 600;
  margin-bottom: 6px;
}

.course-name {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 12px;
}

.course-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.course-footer .tag {
  font-size: 11px;
  color: #64748b;
}

/* 逐轮实录 */
.turn-title-row {
  display: flex;
  align-items: center;
  gap: 14px;
  width: 100%;
}

.turn-dimension {
  font-weight: 600;
  font-size: 13px;
  color: #334155;
}

.turn-score {
  margin-left: auto;
  font-size: 13px;
  color: #64748b;
}

.turn-score b {
  color: #e11d48;
}

.turn-detail-box {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 12px 16px;
  background: #f8fafc;
  border-radius: 8px;
}

.turn-detail-box .row {
  display: flex;
  gap: 8px;
  font-size: 13px;
  line-height: 1.6;
}

.turn-detail-box .label {
  font-weight: 700;
  color: #475569;
}

.turn-detail-box .text.q { color: #0f172a; font-weight: 500; }
.turn-detail-box .text.a { color: #2563eb; }
.turn-detail-box .text.f { color: #475569; background: #fff; padding: 8px 12px; border-radius: 6px; border: 1px dashed #cbd5e1; }
</style>
