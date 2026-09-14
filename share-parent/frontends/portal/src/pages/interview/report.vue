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
            <div class="num">{{ sessionData.score !== undefined && sessionData.score !== null ? sessionData.score : 0 }}</div>
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
                {{ axis.name }} ({{ radarScores[axis.key] ?? 75 }})
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
          <pre class="speech-content">{{ formattedSpeechRefactoring }}</pre>
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
            @click="handleJumpCourse(course)"
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
                <el-tag size="small" :type="getStageTagType(turn.stage)">{{ turn.stageName || ('第 ' + turn.turnNum + ' 题') }}</el-tag>
                <el-tag v-if="turn.depthLevel" size="small" effect="plain" type="info">L{{ turn.depthLevel }}</el-tag>
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
              <div v-if="turn.standardReference" class="row standard-ref-row">
                <span class="label ref-label">📖 标杆示范满分答案：</span>
                <div class="text standard-ref-content">
                  <pre>{{ turn.standardReference }}</pre>
                </div>
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
const sessionId = route.params.id || route.query.id

const loading = ref(true)
const sessionData = ref({})
const reportData = ref(null)
const activeTurns = ref([1])

const getStageTagType = (stage) => {
  if (stage === 1) return 'info'
  if (stage === 2) return 'primary'
  if (stage === 3) return 'warning'
  return 'primary'
}

// 六维雷达参数：根据目标岗位赛道自适应维度名称 (BUG-53)
const axes = computed(() => {
  const job = (sessionData.value?.targetJob || '').toLowerCase()
  let coreName = 'Java核心并发'
  let archName = '系统设计架构'
  let storageName = '存储与数据库'
  let distName = '分布式高并发'

  if (job.includes('前端') || job.includes('web') || job.includes('vue') || job.includes('react')) {
    coreName = 'Web前端核心'
    archName = '前端工程架构'
    storageName = '浏览器存储管线'
    distName = '跨端与离线方案'
  } else if (job.includes('go') || job.includes('golang') || job.includes('c++') || job.includes('系统') || job.includes('高性能')) {
    coreName = '系统并发与调度'
    archName = '高性能系统设计'
    storageName = '存储引擎与I/O'
    distName = '分布式共识网络'
  } else if (job.includes('ai') || job.includes('算法') || job.includes('大模型') || job.includes('llm') || job.includes('python')) {
    coreName = 'Transformer机制'
    archName = '大模型架构设计'
    storageName = '向量检索与RAG'
    distName = '分布式训练推理'
  } else if (job.includes('数仓') || job.includes('数据') || job.includes('flink') || job.includes('spark')) {
    coreName = '流批一体计算'
    archName = '数据架构治理'
    storageName = '湖仓一体与存储'
    distName = '海量分布式调度'
  } else if (job.includes('sre') || job.includes('运维') || job.includes('云原生') || job.includes('k8s')) {
    coreName = 'Linux与容器底层'
    archName = '云原生微服务'
    storageName = '持久化存储卷'
    distName = '集群治理可观测'
  } else if (job.includes('测试') || job.includes('qa') || job.includes('安全')) {
    coreName = '自动化与质量门禁'
    archName = '全链路压测设计'
    storageName = '数据Mock与注入'
    distName = '安全攻防与混沌'
  }

  return [
    { key: 'core', name: coreName },
    { key: 'architecture', name: archName },
    { key: 'storage', name: storageName },
    { key: 'distributed', name: distName },
    { key: 'coding', name: '算法代码工程' },
    { key: 'communication', name: '沟通与表达' }
  ]
})

const radarScores = computed(() => {
  if (!reportData.value || !reportData.value.radarData) {
    return { core: 85, architecture: 78, storage: 82, distributed: 75, coding: 88, communication: 80 }
  }
  let raw = {}
  try {
    if (typeof reportData.value.radarData === 'string') {
      raw = JSON.parse(reportData.value.radarData)
    } else {
      raw = reportData.value.radarData || {}
    }
  } catch (e) {
    raw = {}
  }
  // BUG-36: 容错映射大模型可能输出的替代字段名，防止雷达图失真显示为 0 或 NaN
  const getVal = (...keys) => {
    for (const k of keys) {
      if (raw[k] !== undefined && raw[k] !== null) {
        const num = Number(raw[k])
        if (!isNaN(num)) return Math.max(10, Math.min(100, Math.round(num)))
      }
    }
    return 75
  }
  return {
    core: getVal('core', 'javaCore', 'coreTech', 'fundamental'),
    architecture: getVal('architecture', 'arch', 'systemDesign', 'design'),
    storage: getVal('storage', 'db', 'database', 'io'),
    distributed: getVal('distributed', 'dist', 'highConcurrency', 'cluster'),
    coding: getVal('coding', 'algorithm', 'code', 'problemSolving'),
    communication: getVal('communication', 'comm', 'expression', 'softSkill')
  }
})

// BUG-37: 转义字面量 \n 字符以保证 STAR 示范话术正常换行呈现
const formattedSpeechRefactoring = computed(() => {
  const text = reportData.value?.speechRefactoring || ''
  return text.replace(/\\n/g, '\n')
})

// 计算 SVG 顶点
const centerX = 200
const centerY = 175
const radius = 110

const getVertex = (index, scale) => {
  const angle = (Math.PI * 2 / axes.value.length) * index - Math.PI / 2
  const r = radius * scale
  return {
    x: centerX + r * Math.cos(angle),
    y: centerY + r * Math.sin(angle)
  }
}

const getWebPoints = (level) => {
  return axes.value.map((_, i) => {
    const pt = getVertex(i, level)
    return `${pt.x},${pt.y}`
  }).join(' ')
}

const radarDataCoords = computed(() => {
  return axes.value.map((axis, i) => {
    const val = radarScores.value[axis.key] ?? 75
    const scale = Math.max(0.1, Math.min(val / 100, 1.0))
    return getVertex(i, scale)
  })
})

const radarPoints = computed(() => {
  return radarDataCoords.value.map(pt => `${pt.x},${pt.y}`).join(' ')
})

const getLabelCoord = (index) => {
  const angle = (Math.PI * 2 / axes.value.length) * index - Math.PI / 2
  const r = radius + 24
  return {
    x: centerX + r * Math.cos(angle),
    y: centerY + r * Math.sin(angle) + 4
  }
}

const parsedCourses = computed(() => {
  if (!reportData.value || !reportData.value.recommendedCourses) {
    return ['《高并发分布式系统实战》', '《深入理解 Java 虚拟机》', '《MySQL 实战 45 讲》']
  }
  try {
    let courses = reportData.value.recommendedCourses
    if (typeof courses === 'string') {
      courses = JSON.parse(courses)
    }
    if (Array.isArray(courses) && courses.length > 0) {
      return courses.map(c => String(c).trim())
    }
  } catch (e) {
    const rawStr = String(reportData.value.recommendedCourses)
    const list = rawStr.split(/[\n,，]+/).map(s => s.trim()).filter(s => s.length > 1)
    if (list.length > 0) return list
  }
  return ['《高并发分布式系统实战》', '《JVM 深度剖析与调优》']
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
    if (res && res.code === 200 && res.data) {
      sessionData.value = res.data
      reportData.value = res.data.report
      if (!res.data.report && res.data.status === 2) {
        ElMessage.info('大厂委员会正在终局核验报告，请稍候刷新...')
      }
    } else if (res && res.code !== 200) {
      ElMessage.error(res.msg || '加载诊断报告失败')
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

const handleJumpCourse = (course) => {
  const kw = (course || '').replace(/[《》]/g, '').trim()
  router.push({ path: '/search/index', query: kw ? { keyword: kw } : {} })
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

.turn-detail-box .standard-ref-row {
  margin-top: 6px;
  background: rgba(16, 185, 129, 0.06);
  border: 1px solid rgba(16, 185, 129, 0.22);
  border-radius: 8px;
  padding: 10px 14px;
}

.turn-detail-box .ref-label {
  color: #059669;
  font-weight: 700;
  white-space: nowrap;
}

.standard-ref-content pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: inherit;
  font-size: 13px;
  line-height: 1.65;
  color: #065f46;
}
</style>
